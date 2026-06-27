package com.mycompany.tutorhub_enterprise.client.exam.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.tutorhub_enterprise.client.exam.ui.QuickSettingsController;
import com.mycompany.tutorhub_enterprise.client.exam.ui.QuickSettingsStateStore;
import com.mycompany.tutorhub_enterprise.client.exam.ui.TSESecurityPolicy;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.net.URL;

public class TSEParentHtmlQuickSettingsPopup {

    private static JWindow popupWindow;
    private static JFXPanel jfxPanel;
    private static WebView webView;
    private static WebEngine webEngine;
    private static boolean isShowing = false;
    private static AWTEventListener outsideClickListener;
    private static Rectangle currentAnchorBounds;
    
    private static volatile long lastPopupInternalMouseAt = 0L;
    private static volatile boolean brightnessFallbackDragging = false;
    private static volatile int lastBrightnessFallbackPercent = -1;

    // Controllers
    private static QuickSettingsStateStore stateStore;
    private static QuickSettingsController controller;
    private static JavaAppBridge appBridge;

    private static Timer refreshTimer;
    private static final int POPUP_WIDTH = 354;
    private static final int POPUP_HEIGHT = 285;
    private static final int BRIGHTNESS_SLIDER_X = 71;
    private static final int BRIGHTNESS_SLIDER_Y = 127;
    private static final int BRIGHTNESS_SLIDER_WIDTH = 204;
    private static final int BRIGHTNESS_SLIDER_HEIGHT = 4;
    private static final int BRIGHTNESS_SLIDER_HIT_X_PAD = 18;
    private static final int BRIGHTNESS_SLIDER_HIT_Y_PAD = 24;

    public static class JavaAppBridge {
        public String getSnapshotJson() {
            if (controller != null) {
                return controller.getSnapshot().toJson();
            }
            return "{}";
        }

        public void testSoundCommand() {
            System.out.println("[TSE_PARENT_QS_HTML] testSoundCommand called");
            try {
                TSETestSoundPlayer.playTestToneAsync();
            } catch (Exception e) {
                System.err.println("[TSE_PARENT_QS_HTML] Failed to play test sound: " + e.getMessage());
            }
        }

        public void setVolumeCommand(String jsonStr) {
            System.out.println("[TSE_PARENT_QS_HTML] setVolumeCommand raw=" + jsonStr);
            try {
                JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
                int percent = obj.has("percent") ? obj.get("percent").getAsInt() : 0;
                String requestId = obj.has("requestId") ? obj.get("requestId").getAsString() : "req-vol-" + System.currentTimeMillis();
                if (controller != null) {
                    controller.setVolume(percent, requestId);
                }
            } catch (Exception e) {
                System.err.println("[TSE_PARENT_QS_HTML] Failed to parse setVolumeCommand: " + e.getMessage());
            }
        }

        public void setMutedCommand(String jsonStr) {
            System.out.println("[TSE_PARENT_QS_HTML] setMutedCommand raw=" + jsonStr);
            try {
                JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
                boolean muted = obj.has("muted") ? obj.get("muted").getAsBoolean() : false;
                String requestId = obj.has("requestId") ? obj.get("requestId").getAsString() : "req-mute-" + System.currentTimeMillis();
                if (controller != null) {
                    controller.setMuted(muted, requestId);
                }
            } catch (Exception e) {
                System.err.println("[TSE_PARENT_QS_HTML] Failed to parse setMutedCommand: " + e.getMessage());
            }
        }

        public void setBrightnessCommand(String jsonStr) {
            try {
                System.out.println("[TSE_PARENT_QS_HTML] setBrightnessCommand raw=" + jsonStr);
                int percent = parseBrightnessPercent(jsonStr);
                String requestId = "parent-brightness-" + System.currentTimeMillis();
                System.out.println("[TSE_PARENT_QS_HTML] setBrightness percent=" + percent);
                if (controller != null) {
                    controller.setBrightness(percent, requestId);
                }
            } catch (Exception e) {
                System.err.println("[TSE_PARENT_QS_HTML] Failed to parse setBrightnessCommand: " + e.getMessage());
            }
        }

        public void refreshCommand() {
            if (controller != null) {
                controller.requestRefresh();
            }
        }

        public void closeCommand() {
            javax.swing.SwingUtilities.invokeLater(() -> {
                TSEParentHtmlQuickSettingsPopup.hidePopup();
            });
        }
        
        public void notifyPopupPointerActivity() {
            lastPopupInternalMouseAt = System.currentTimeMillis();
            System.out.println("[TSE_PARENT_QS_HTML] notifyPopupPointerActivity");
        }
        
        public void log(String message) {
            System.out.println("[TSE_PARENT_QS_JS] " + message);
        }
    }

    private static boolean htmlReady = false;

    private static int parseBrightnessPercent(String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Brightness command payload is empty");
        }

        String raw = jsonStr.trim();
        try {
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            if (obj.has("percent")) {
                return clampPercent(obj.get("percent").getAsInt());
            }
        } catch (Exception ignored) {
            // Fall through to plain-number parsing. The bridge must never crash WebView.
        }

        return clampPercent(Integer.parseInt(raw));
    }

    private static int clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    private static boolean isInsidePopup(MouseEvent e) {
        if (popupWindow == null || !popupWindow.isVisible() || e == null) {
            return false;
        }
        Point p = e.getLocationOnScreen();
        Rectangle bounds = popupWindow.getBounds();
        return bounds.contains(p);
    }

    private static Point getPopupRelativePoint(MouseEvent e) {
        if (popupWindow == null || e == null) {
            return new Point(-1, -1);
        }
        Point p = e.getLocationOnScreen();
        return new Point(p.x - popupWindow.getX(), p.y - popupWindow.getY());
    }

    private static Rectangle getBrightnessSliderHitBounds() {
        return new Rectangle(
            BRIGHTNESS_SLIDER_X - BRIGHTNESS_SLIDER_HIT_X_PAD,
            BRIGHTNESS_SLIDER_Y - BRIGHTNESS_SLIDER_HIT_Y_PAD,
            BRIGHTNESS_SLIDER_WIDTH + (BRIGHTNESS_SLIDER_HIT_X_PAD * 2),
            BRIGHTNESS_SLIDER_HEIGHT + (BRIGHTNESS_SLIDER_HIT_Y_PAD * 2)
        );
    }

    private static int brightnessPercentFromPopupX(int relativeX) {
        double ratio = (relativeX - BRIGHTNESS_SLIDER_X) / (double) Math.max(1, BRIGHTNESS_SLIDER_WIDTH);
        return clampPercent((int) Math.round(ratio * 100.0));
    }

    private static String jsQuote(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\r", "")
            .replace("\n", "\\n");
    }

    private static void mirrorBrightnessFallbackToJs(int percent, String source) {
        String safeSource = jsQuote(source);
        safeExecuteScript(
            "try {"
                + " if (typeof setSliderUIValue === 'function') { setSliderUIValue('slider-brightness', " + percent + "); }"
                + " if (typeof logToJava === 'function') { logToJava('[TSE_QS_PARENT_JS] brightness input percent=" + percent + " source=" + safeSource + "'); }"
                + "} catch (e) {"
                + " if (window.javaApp && window.javaApp.log) { window.javaApp.log('[TSE_QS_PARENT_JS] awt fallback mirror failed ' + e); }"
                + "}"
        );
    }

    private static boolean handleBrightnessFallbackMouse(MouseEvent me) {
        int id = me.getID();
        if (id != MouseEvent.MOUSE_PRESSED
            && id != MouseEvent.MOUSE_DRAGGED
            && id != MouseEvent.MOUSE_RELEASED) {
            return false;
        }

        Point relative = getPopupRelativePoint(me);
        Rectangle hitBounds = getBrightnessSliderHitBounds();

        if (id == MouseEvent.MOUSE_PRESSED) {
            if (!hitBounds.contains(relative)) {
                brightnessFallbackDragging = false;
                return false;
            }

            brightnessFallbackDragging = true;
            lastPopupInternalMouseAt = System.currentTimeMillis();
            int percent = brightnessPercentFromPopupX(relative.x);
            lastBrightnessFallbackPercent = percent;
            System.out.println("[TSE_PARENT_QS_HTML] AWT brightness fallback pointerdown percent=" + percent
                + " relative=" + relative.x + "," + relative.y);
            safeExecuteScript("if (typeof logToJava === 'function') { logToJava('[TSE_QS_PARENT_JS] brightness pointerdown value="
                + percent + " source=awt-fallback'); }");
            mirrorBrightnessFallbackToJs(percent, "awt-fallback");
            me.consume();
            return true;
        }

        if (!brightnessFallbackDragging) {
            return false;
        }

        lastPopupInternalMouseAt = System.currentTimeMillis();
        int percent = brightnessPercentFromPopupX(relative.x);
        lastBrightnessFallbackPercent = percent;
        mirrorBrightnessFallbackToJs(percent, "awt-fallback-" + (id == MouseEvent.MOUSE_DRAGGED ? "drag" : "release"));

        if (id == MouseEvent.MOUSE_DRAGGED) {
            System.out.println("[TSE_PARENT_QS_HTML] AWT brightness fallback drag percent=" + percent
                + " relative=" + relative.x + "," + relative.y);
            me.consume();
            return true;
        }

        brightnessFallbackDragging = false;
        System.out.println("[TSE_PARENT_QS_HTML] AWT brightness fallback commit percent=" + percent);
        safeExecuteScript("if (typeof logToJava === 'function') {"
            + " logToJava('[TSE_QS_PARENT_JS] brightness pointerup commit percent=" + percent + " source=awt-fallback');"
            + " logToJava('[TSE_QS_PARENT_JS] send setBrightnessCommand payload={\"percent\":" + percent + ",\"requestId\":\"parent-brightness-awt\"}');"
            + " if (typeof showStatusMessage === 'function') showStatusMessage('brightness');"
            + "}");
        String payload = "{\"percent\":" + percent + ",\"requestId\":\"parent-brightness-awt-" + System.currentTimeMillis() + "\"}";
        new JavaAppBridge().setBrightnessCommand(payload);
        me.consume();
        return true;
    }

    private static void safeExecuteScript(String script) {
        if (script == null || script.trim().isEmpty()) {
            return;
        }

        Runnable task = () -> {
            try {
                if (webEngine == null) {
                    System.err.println("[TSE_PARENT_QS_HTML] webEngine is null, skip script.");
                    return;
                }

                if (!htmlReady) {
                    System.out.println("[TSE_PARENT_QS_HTML] HTML not ready, skip/defer script.");
                    return;
                }

                webEngine.executeScript(script);
            } catch (Exception e) {
                System.err.println("[TSE_PARENT_QS_HTML] executeScript failed: " + e.getMessage());
            }
        };

        if (Platform.isFxApplicationThread()) {
            task.run();
        } else {
            Platform.runLater(task);
        }
    }

    public static void initialize() {
        if (popupWindow != null) return;
        System.out.println("[TSE_PARENT_QS_HTML] Initializing JavaFX WebView popup.");
        
        if (stateStore == null) {
            stateStore = new QuickSettingsStateStore(TSESecurityPolicy.forLogin());
            controller = new QuickSettingsController(stateStore);
            // Spin up standalone services for parent
            com.mycompany.tutorhub_enterprise.client.exam.ui.ClockService clockService = new com.mycompany.tutorhub_enterprise.client.exam.ui.ClockService(stateStore);
            clockService.initialize();
            com.mycompany.tutorhub_enterprise.client.exam.ui.BatteryService batteryService = new com.mycompany.tutorhub_enterprise.client.exam.ui.BatteryService(stateStore);
            batteryService.initialize();
            com.mycompany.tutorhub_enterprise.client.exam.ui.VolumeService volumeService = new com.mycompany.tutorhub_enterprise.client.exam.ui.VolumeService(stateStore);
            volumeService.initialize();
            com.mycompany.tutorhub_enterprise.client.exam.ui.NetworkService networkService = new com.mycompany.tutorhub_enterprise.client.exam.ui.NetworkService(stateStore);
            networkService.initialize();
            com.mycompany.tutorhub_enterprise.client.exam.ui.BrightnessService brightnessService = new com.mycompany.tutorhub_enterprise.client.exam.ui.BrightnessService(stateStore);
            brightnessService.initialize();
            
            controller.setNativeServices(volumeService, brightnessService);
        }

        popupWindow = new JWindow();
        popupWindow.setSize(POPUP_WIDTH, POPUP_HEIGHT);
        popupWindow.setBackground(new java.awt.Color(0, 0, 0, 0)); // Transparent window

        jfxPanel = new JFXPanel();
        jfxPanel.setOpaque(false);
        jfxPanel.setBackground(new java.awt.Color(0, 0, 0, 0));
        popupWindow.setContentPane(jfxPanel);

        Platform.setImplicitExit(false);
        Platform.runLater(() -> {
            webView = new WebView();
            webView.setContextMenuEnabled(false);
            webView.setPageFill(Color.TRANSPARENT);
            webView.setStyle("-fx-background-color: transparent;");
            
            // Make WebView background transparent
            webView.getEngine().setUserStyleSheetLocation("data:text/css,body{background-color:transparent;}");
            
            Scene scene = new Scene(webView, POPUP_WIDTH, POPUP_HEIGHT);
            scene.setFill(Color.TRANSPARENT);
            jfxPanel.setScene(scene);

            webEngine = webView.getEngine();
            webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED) {
                    System.out.println("[TSE_PARENT_QS_HTML] WebView load SUCCEEDED.");
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    appBridge = new JavaAppBridge();
                    window.setMember("javaApp", appBridge);
                    htmlReady = true;
                    if (controller != null) {
                        System.out.println("[TSE_PARENT_QS_HTML] HTML ready. Triggering JS init.");
                        String script = "if (typeof onAppReady === 'function') { onAppReady(); }";
                        // Already in runLater block, but safeExecuteScript will handle it correctly
                        safeExecuteScript(script);
                    }
                } else if (newValue == Worker.State.FAILED) {
                    System.err.println("[TSE_PARENT_QS_HTML] WebView load FAILED.");
                }
            });

            try {
                URL url = TSEParentHtmlQuickSettingsPopup.class.getResource("/tse/quick-settings/parent-quick-settings.html");
                if (url != null) {
                    System.out.println("[TSE_PARENT_QS_HTML] HTML template loaded successfully.");
                    webEngine.load(url.toExternalForm());
                } else {
                    System.err.println("[TSE_PARENT_QS_HTML] HTML template not found. Using built-in fallback template.");
                    String fallbackHtml = "<html><head><meta charset='utf-8'><style>" +
                        "body { margin: 0; padding: 0; background: #3B3B3B; color: #F5F5F5; font-family: 'Segoe UI', sans-serif; overflow: hidden; width: 354px; height: 285px; display: flex; align-items: center; justify-content: center; text-align: center; border-radius: 16px; border: 1px solid rgba(255,255,255,0.1); box-sizing: border-box; }" +
                        "</style></head><body>" +
                        "<div><p style='font-size: 14px; font-weight: bold;'>Quick Settings Unavailable</p><p style='font-size: 12px; color: #A0A0A0;'>Resource missing or failed to load.</p></div>" +
                        "<script>" +
                        "window.updateState = function(state) { console.log('Fallback received state'); };" +
                        "</script>" +
                        "</body></html>";
                    webEngine.loadContent(fallbackHtml);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Setup timer for auto-refresh
        refreshTimer = new Timer(5000, e -> {
            if (isShowing && controller != null) {
                controller.requestRefresh();
                String script = "if (typeof pollState === 'function') { pollState(); }";
                safeExecuteScript(script);
            }
        });
    }

    public static void showPopup(Component anchor, String screen) {
        initialize();
        System.out.println("[TSE_PARENT_QS_HTML] Showing HTML popup screen=" + screen + " source=cluster");
        
        if (anchor != null && anchor.isShowing()) {
            Point loc = anchor.getLocationOnScreen();
            currentAnchorBounds = new Rectangle(loc.x, loc.y, anchor.getWidth(), anchor.getHeight());
            
            int popupX = loc.x - (POPUP_WIDTH / 2) + (anchor.getWidth() / 2);
            int popupY = loc.y - POPUP_HEIGHT - 16;
            
            // Clamp to screen
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            if (popupX < 12) popupX = 12;
            if (popupX + POPUP_WIDTH > screenSize.width - 12) {
                popupX = screenSize.width - POPUP_WIDTH - 12;
            }
            
            System.out.println("[TSE_PARENT_QS_HTML] Popup bounds clamped x=" + popupX + " y=" + popupY + " w=" + POPUP_WIDTH + " h=" + POPUP_HEIGHT);
            popupWindow.setLocation(popupX, popupY);
        } else {
            popupWindow.setLocationRelativeTo(null);
            currentAnchorBounds = null;
        }

        popupWindow.setVisible(true);
        popupWindow.setAlwaysOnTop(true);
        isShowing = true;
        
        if (controller != null) {
            controller.requestRefresh();
            String script = "if (typeof pollState === 'function') { pollState(); }";
            safeExecuteScript(script);
        }
        
        refreshTimer.start();

        if (outsideClickListener == null) {
            outsideClickListener = event -> {
                if (event.getID() == MouseEvent.MOUSE_PRESSED
                    || event.getID() == MouseEvent.MOUSE_DRAGGED
                    || event.getID() == MouseEvent.MOUSE_RELEASED) {
                    MouseEvent me = (MouseEvent) event;
                    if (isShowing && popupWindow != null && popupWindow.isVisible()) {
                        Point clickPoint = me.getLocationOnScreen();
                        boolean inAnchor = (currentAnchorBounds != null && currentAnchorBounds.contains(clickPoint));
                        
                        if (isInsidePopup(me)) {
                            if (handleBrightnessFallbackMouse(me)) {
                                return;
                            }
                            if (event.getID() == MouseEvent.MOUSE_PRESSED) {
                                System.out.println("[TSE_PARENT_QS_HTML] Click inside popup, ignore outside close.");
                            }
                            return;
                        }
                        
                        long delta = System.currentTimeMillis() - lastPopupInternalMouseAt;
                        if (delta >= 0 && delta < 700) {
                            System.out.println("[TSE_PARENT_QS_HTML] Ignore outside click during popup pointer activity delta=" + delta);
                            return;
                        }
                        
                        if (!inAnchor) {
                            System.out.println("[TSE_PARENT_QS_HTML] Outside click detected. Hiding popup.");
                            SwingUtilities.invokeLater(TSEParentHtmlQuickSettingsPopup::hidePopup);
                        }
                    }
                }
            };
            Toolkit.getDefaultToolkit().addAWTEventListener(
                outsideClickListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK
            );
        }
    }

    public static void hidePopup() {
        if (!isShowing) return;
        isShowing = false;
        
        if (popupWindow != null) {
            popupWindow.setVisible(false);
            System.out.println("[TSE_PARENT_QS_HTML] Popup hidden.");
        }
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
        if (outsideClickListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
            outsideClickListener = null;
        }
    }
}
