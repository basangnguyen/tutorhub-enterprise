package com.mycompany.tutorhub_enterprise.client.exam.ui;

import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefMessageRouter;

import javax.swing.*;
import java.io.File;

/**
 * TSEJcefLifecycleManager – Quản lý vòng đời JCEF an toàn cho Phase 1 Recovery.
 * Đảm bảo init singleton và cleanup triệt để.
 */
public class TSEJcefLifecycleManager {
    private static CefApp cefApp = null;
    private static CefClient cefClient = null;

    private static CefMessageRouter msgRouter = null;
    private static java.util.function.BiConsumer<String, org.cef.callback.CefQueryCallback> submitCallback = null;

    public static synchronized void setSubmitCallback(java.util.function.BiConsumer<String, org.cef.callback.CefQueryCallback> callback) {
        submitCallback = callback;
    }

    public static synchronized CefClient getClient() {
        if (cefClient == null) {
            try {
                // Sửa lỗi xung đột Pop-up của Java (Swing vs CEF)
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

                CefAppBuilder builder = new CefAppBuilder();
                String procType = System.getProperty("tse.process.type", "parent");
                String cacheFolder = "child".equals(procType) ? ".jcef_core_tse_child" : ".jcef_core_tse_parent";
                
                String appRoot = System.getProperty("tutorhub.app.root");
                File installDir;
                if (appRoot != null) {
                    installDir = new File(appRoot, "jcef");
                    System.out.println("[TSE_JCEF_INIT] Using bundled CEF binaries: " + installDir.getAbsolutePath());
                } else {
                    installDir = new File(System.getProperty("user.home"), ".jcef_core_tse_binaries");
                    System.out.println("[TSE_JCEF_INIT] Using dev CEF binaries: " + installDir.getAbsolutePath());
                }
                builder.setInstallDir(installDir);
                
                File cacheDir = new File(System.getProperty("user.home"), cacheFolder);
                builder.getCefSettings().root_cache_path = cacheDir.getAbsolutePath();
                System.out.println("[TSE_JCEF_INIT] Process type: " + procType + ", cache path: " + cacheDir.getAbsolutePath());
                
                builder.getCefSettings().resources_dir_path = installDir.getAbsolutePath();
                File localesDir = new File(installDir, "locales");
                builder.getCefSettings().locales_dir_path = localesDir.getAbsolutePath();

                System.out.println("[TSE_JCEF_INIT] resources_dir_path=" + installDir.getAbsolutePath());
                System.out.println("[TSE_JCEF_INIT] locales_dir_path=" + localesDir.getAbsolutePath());

                if (!localesDir.exists()) {
                    System.err.println("[TSE_JCEF_INIT] ERROR: Locales directory not found at " + localesDir.getAbsolutePath());
                }
                builder.getCefSettings().windowless_rendering_enabled = false;
                builder.addJcefArgs("--disable-web-security");

                cefApp = builder.build();
                cefClient = cefApp.createClient();
                
                msgRouter = org.cef.browser.CefMessageRouter.create();
                msgRouter.addHandler(new org.cef.handler.CefMessageRouterHandlerAdapter() {
                    @Override
                    public boolean onQuery(org.cef.browser.CefBrowser browser, org.cef.browser.CefFrame frame, long queryId, String request, boolean persistent, org.cef.callback.CefQueryCallback callback) {
                        System.out.println("[TSE_JCEF_BRIDGE] request=" + request);

                        if (request != null && request.startsWith("TSE_BRIGHTNESS_SET:")) {
                            try {
                                int percent = Integer.parseInt(request.substring("TSE_BRIGHTNESS_SET:".length()));
                                System.out.println("[TSE_QS_EXAM] setBrightness percent=" + percent);
                                TSEQuickSettingsManager.setBrightness(percent, result -> {
                                    if ("SUCCESS".equals(result)) {
                                        callback.success("SUCCESS");
                                    } else {
                                        callback.failure(500, "ERROR");
                                    }
                                });
                            } catch (Exception e) {
                                callback.failure(400, "INVALID_VALUE");
                            }
                            return true;
                        }

                        if (request != null && request.startsWith("TSE_VOLUME_SET:")) {
                            try {
                                int percent = Integer.parseInt(request.substring("TSE_VOLUME_SET:".length()));
                                TSEQuickSettingsManager.setVolume(percent, result -> {
                                    if ("SUCCESS".equals(result)) {
                                        callback.success("SUCCESS");
                                    } else {
                                        callback.failure(500, "ERROR");
                                    }
                                });
                            } catch (Exception e) {
                                callback.failure(400, "INVALID_VALUE");
                            }
                            return true;
                        }

                        if (request != null && request.startsWith("TSE_VOLUME_MUTE:")) {
                            try {
                                boolean muted = Boolean.parseBoolean(request.substring("TSE_VOLUME_MUTE:".length()));
                                TSEQuickSettingsManager.setMuted(muted, result -> {
                                    if ("SUCCESS".equals(result)) {
                                        callback.success("SUCCESS");
                                    } else {
                                        callback.failure(500, "ERROR");
                                    }
                                });
                            } catch (Exception e) {
                                callback.failure(400, "INVALID_VALUE");
                            }
                            return true;
                        }
                        
                        if (request != null && request.startsWith("TSE_TEST_SOUND_PLAY")) {
                            TSETestSoundPlayer.playTestToneAsync();
                            callback.success("OK");
                            return true;
                        }
                        
                        if (request != null && request.startsWith("SUBMIT_PAYLOAD:")) {
                            String payload = request.substring("SUBMIT_PAYLOAD:".length());
                            if (submitCallback != null) {
                                submitCallback.accept(payload, callback);
                            } else {
                                callback.success("OK");
                            }
                            return true;
                        }
                        return false;
                    }
                }, true);
                cefClient.addMessageRouter(msgRouter);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cefClient;
    }

    public static synchronized void cleanup() {
        if (cefApp != null) {
            cefApp.dispose();
            cefApp = null;
            cefClient = null;
        }
    }
}
