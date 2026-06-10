package com.mycompany.tutorhub_enterprise.client;

import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;

import javax.swing.*;
import java.io.File;

public class JcefManager {
    private static CefApp cefApp = null;
    private static CefClient cefClient = null;

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
                
                // Thông số cho Video Call & Lavie
                builder.addJcefArgs("--enable-media-stream"); 
                builder.addJcefArgs("--use-fake-ui-for-media-stream"); 
                builder.addJcefArgs("--enable-usermedia-screen-capturing");
                builder.addJcefArgs("--auto-select-desktop-capture-source=Entire screen");
                builder.addJcefArgs("--disable-web-security");
                builder.addJcefArgs("--allow-file-access-from-files"); // Quan trọng cho Lavie

                cefApp = builder.build();
                cefClient = cefApp.createClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cefClient;
    }
}
