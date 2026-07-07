package com.mycompany.tutorhub_enterprise.client;

import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;

import javax.swing.*;
import java.io.File;

import org.cef.browser.CefMessageRouter;

public class JcefManager {
    private static CefApp cefApp = null;
    private static CefClient cefClient = null;
    private static CefMessageRouter sharedMessageRouter = null;

    public static synchronized CefMessageRouter getSharedMessageRouter() {
        if (sharedMessageRouter == null) {
            CefMessageRouter.CefMessageRouterConfig config = new CefMessageRouter.CefMessageRouterConfig("cefQuery", "cefQueryCancel");
            sharedMessageRouter = CefMessageRouter.create(config);
            getClient().addMessageRouter(sharedMessageRouter);
        }
        return sharedMessageRouter;
    }

    public static synchronized CefClient getClient() {
        if (cefClient == null) {
            try {
                // Sửa lỗi xung đột Pop-up của Java
                JPopupMenu.setDefaultLightWeightPopupEnabled(false);
                ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

                CefAppBuilder builder = new CefAppBuilder();
                // Fix lỗi UnsatisfiedLinkError do đường dẫn chứa tiếng Việt có dấu (Bản sao dự án)
                File installDir = new File(System.getProperty("user.home"), ".jcef_core_v2");
                builder.setInstallDir(installDir);
                
                // Tắt OSR để tránh kẹt Message Pump trên Windows (làm đơ nút bấm JFrame)
                builder.getCefSettings().windowless_rendering_enabled = false;
                
                // Set unique cache path to avoid Error 32 (Lock file cannot be created)
                File cacheDir = new File(System.getProperty("java.io.tmpdir"), "jcef_cache_" + System.currentTimeMillis());
                builder.getCefSettings().root_cache_path = cacheDir.getAbsolutePath();
                builder.getCefSettings().cache_path = cacheDir.getAbsolutePath();
                
                // Thông số cho Video Call & Lavie
                builder.addJcefArgs("--enable-media-stream"); 
                builder.addJcefArgs("--use-fake-ui-for-media-stream"); 
                builder.addJcefArgs("--enable-usermedia-screen-capturing");
                builder.addJcefArgs("--auto-select-desktop-capture-source=Entire screen");
                builder.addJcefArgs("--disable-web-security");
                builder.addJcefArgs("--allow-file-access-from-files"); // Quan trọng cho Lavie
                
                // Bật Debug
                builder.addJcefArgs("--remote-allow-origins=*");
                builder.addJcefArgs("--remote-debugging-port=9222");
                builder.addJcefArgs("--no-proxy-server");
                builder.addJcefArgs("--enable-webgl");
                builder.addJcefArgs("--ignore-gpu-blocklist");

                // RAM & CPU Optimizations (C6)
                builder.addJcefArgs("--disable-gpu-compositing");
                builder.addJcefArgs("--js-flags=\"--max-old-space-size=256\"");

                cefApp = builder.build();
                cefClient = cefApp.createClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cefClient;
    }

    public static synchronized CefApp getCefApp() {
        if (cefApp == null) {
            getClient(); // Ensure initialization
        }
        return cefApp;
    }
}
