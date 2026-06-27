package com.mycompany.tutorhub_enterprise.client.home;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class HomeSocialWebPanel extends JPanel {

    private static final int PREFERRED_HEIGHT = 480;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final JFXPanel fxPanel;
    private final HomeSocialBridge bridge = new HomeSocialBridge();
    private java.util.function.BiConsumer<String, String> eventListener;
    private WebEngine webEngine;
    private HomeSocialState pendingState = new HomeSocialState();
    private boolean webViewReady;

    public HomeSocialWebPanel() {
        System.out.println("[HOME_SOCIAL] HomeSocialWebPanel initialized");
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(0, PREFERRED_HEIGHT));
        setMinimumSize(new Dimension(0, PREFERRED_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, PREFERRED_HEIGHT));

        fxPanel = new JFXPanel();
        fxPanel.setOpaque(false);
        add(fxPanel, BorderLayout.CENTER);

        Platform.setImplicitExit(false);
        Platform.runLater(this::initWebView);
    }

    public void setEventListener(java.util.function.BiConsumer<String, String> listener) {
        this.eventListener = listener;
    }

    public void setBannerItems(List<HomeBannerItem> banners) {
        synchronized (this) {
            HomeSocialState nextState = pendingState == null ? new HomeSocialState() : pendingState.copy();
            nextState.banners = banners == null ? new ArrayList<>() : new ArrayList<>(banners);
            pendingState = nextState;
        }
        queueStatePush();
    }

    public void setLocketItems(List<HomeLocketItem> items) {
        synchronized (this) {
            HomeSocialState nextState = pendingState == null ? new HomeSocialState() : pendingState.copy();
            nextState.locketItems = items == null ? new ArrayList<>() : new ArrayList<>(items);
            pendingState = nextState;
        }
        queueStatePush();
    }

    public void setHomeSocialState(HomeSocialState state) {
        synchronized (this) {
            pendingState = state == null ? new HomeSocialState() : state.copy();
        }
        queueStatePush();
    }

    public void refreshState() {
        queueStatePush();
    }

    public void notifyLegacyLocketDataAvailable(int itemCount) {
        System.out.println("[HOME_SOCIAL] Legacy locket data available, count=" + itemCount);
    }

    private void initWebView() {
        try {
            WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            webEngine = webView.getEngine();

            URL htmlUrl = HomeSocialWebPanel.class.getResource("/home-social/home-social.html");
            if (htmlUrl == null) {
                showFallback("Khong tai duoc Home Social UI.");
                System.err.println("[HOME_SOCIAL][ERROR] Missing /home-social/home-social.html");
                return;
            }

            webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    try {
                        JSObject window = (JSObject) webEngine.executeScript("window");
                        window.setMember("javaApp", bridge);
                        webEngine.executeScript("if (window.TutorHubHomeSocialBridgeReady) { window.TutorHubHomeSocialBridgeReady(); }");
                        webViewReady = true;
                        flushStateToWebView();
                        System.out.println("[HOME_SOCIAL] Loaded home-social.html");
                    } catch (Exception e) {
                        System.err.println("[HOME_SOCIAL][ERROR] Bridge setup failed: " + e.getMessage());
                    }
                } else if (newState == Worker.State.FAILED) {
                    Throwable error = webEngine.getLoadWorker().getException();
                    String message = error == null ? "unknown" : error.getMessage();
                    System.err.println("[HOME_SOCIAL][ERROR] WebView load failed: " + message);
                    showFallback("Khong tai duoc Home Social UI.");
                }
            });

            System.out.println("[HOME_SOCIAL] Loading /home-social/home-social.html");
            webEngine.load(htmlUrl.toExternalForm());
            fxPanel.setScene(new Scene(webView));
        } catch (Exception e) {
            System.err.println("[HOME_SOCIAL][ERROR] WebView init failed: " + e.getMessage());
            showFallback("Khong tai duoc Home Social UI.");
        }
    }

    private void queueStatePush() {
        if (!webViewReady || webEngine == null) {
            System.out.println("[HOME_SOCIAL] State queued");
            return;
        }
        Platform.runLater(this::flushStateToWebView);
    }

    private void flushStateToWebView() {
        if (!webViewReady || webEngine == null) {
            System.out.println("[HOME_SOCIAL] State queued");
            return;
        }
        try {
            HomeSocialState stateToPush;
            synchronized (this) {
                stateToPush = pendingState == null ? new HomeSocialState() : pendingState.copy();
            }
            String json = gson.toJson(stateToPush);
            String safeBase64 = java.util.Base64.getEncoder().encodeToString(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            webEngine.executeScript("window.TutorHubHomeSocial && window.TutorHubHomeSocial.setStateBase64('" + safeBase64 + "');");
            System.out.println("[HOME_SOCIAL] State pushed to WebView (Base64)");
        } catch (Exception e) {
            System.err.println("[HOME_SOCIAL][ERROR] State push failed: " + e.getMessage());
        }
    }

    public void updateLocketReaction(long postId, boolean reacted) {
        if (webEngine == null) return;
        try {
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("postId", postId);
            obj.addProperty("reacted", reacted);
            String b64 = java.util.Base64.getEncoder().encodeToString(obj.toString().getBytes("UTF-8"));
            javafx.application.Platform.runLater(() -> {
                webEngine.executeScript("if (window.updateLocketReactionBase64) window.updateLocketReactionBase64('" + b64 + "');");
            });
        } catch(Exception e) { e.printStackTrace(); }
    }

    public void updateLocketComments(String payload) {
        if (webEngine == null) return;
        try {
            String b64 = java.util.Base64.getEncoder().encodeToString(payload.getBytes("UTF-8"));
            javafx.application.Platform.runLater(() -> {
                webEngine.executeScript("if (window.updateLocketCommentsBase64) window.updateLocketCommentsBase64('" + b64 + "');");
            });
        } catch(Exception e) { e.printStackTrace(); }
    }

    public void addLocketComment(String payload) {
        if (webEngine == null) return;
        try {
            String b64 = java.util.Base64.getEncoder().encodeToString(payload.getBytes("UTF-8"));
            javafx.application.Platform.runLater(() -> {
                webEngine.executeScript("if (window.addLocketCommentBase64) window.addLocketCommentBase64('" + b64 + "');");
            });
        } catch(Exception e) { e.printStackTrace(); }
    }

    public void deleteLocketComment(long commentId) {
        if (webEngine == null) return;
        javafx.application.Platform.runLater(() -> {
            webEngine.executeScript("if (window.deleteLocketComment) window.deleteLocketComment(" + commentId + ");");
        });
    }

    private void showFallback(String message) {
        SwingUtilities.invokeLater(() -> {
            removeAll();
            JPanel fallback = new JPanel(new BorderLayout());
            fallback.setOpaque(true);
            fallback.setBackground(Color.decode("#F8FAFC"));
            JLabel label = new JLabel(message, SwingConstants.CENTER);
            label.setForeground(Color.decode("#64748B"));
            fallback.add(label, BorderLayout.CENTER);
            add(fallback, BorderLayout.CENTER);
            revalidate();
            repaint();
        });
    }

    public class HomeSocialBridge {
        public void onEvent(String type, String payloadJson) {
            String safeType = type == null ? "" : type;
            String safePayload = payloadJson == null ? "{}" : payloadJson;
            System.out.println("[HOME_SOCIAL] Bridge event: " + safeType + " payload=" + safePayload);
            if (eventListener != null) {
                eventListener.accept(safeType, safePayload);
            }
        }

        public void log(String message) {
            System.out.println("[HOME_SOCIAL] JS log: " + (message == null ? "" : message));
        }
    }
}
