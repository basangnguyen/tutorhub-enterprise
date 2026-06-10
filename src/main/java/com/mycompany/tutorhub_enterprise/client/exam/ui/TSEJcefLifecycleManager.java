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
    private static java.util.function.Consumer<String> submitCallback = null;

    public static synchronized void setSubmitCallback(java.util.function.Consumer<String> callback) {
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
                        if (request != null && request.startsWith("SUBMIT_PAYLOAD:")) {
                            String payload = request.substring("SUBMIT_PAYLOAD:".length());
                            if (submitCallback != null) {
                                submitCallback.accept(payload);
                            }
                            callback.success("OK");
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
