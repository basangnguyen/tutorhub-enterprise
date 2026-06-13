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
                File installDir = new File(System.getProperty("user.home"), ".jcef_core_tse");
                builder.setInstallDir(installDir);
                
                builder.getCefSettings().windowless_rendering_enabled = false;
                builder.addJcefArgs("--disable-web-security");

                cefApp = builder.build();
                cefClient = cefApp.createClient();
                
                msgRouter = org.cef.browser.CefMessageRouter.create();
                msgRouter.addHandler(new org.cef.handler.CefMessageRouterHandlerAdapter() {
                    @Override
                    public boolean onQuery(org.cef.browser.CefBrowser browser, org.cef.browser.CefFrame frame, long queryId, String request, boolean persistent, org.cef.callback.CefQueryCallback callback) {
                        if (request != null && request.startsWith("TSE_BRIGHTNESS_SET:")) {
                            try {
                                int percent = Integer.parseInt(request.substring("TSE_BRIGHTNESS_SET:".length()));
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
