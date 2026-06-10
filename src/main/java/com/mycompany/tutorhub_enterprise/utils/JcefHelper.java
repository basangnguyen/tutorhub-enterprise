package com.mycompany.tutorhub_enterprise.utils;

import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.CefInitializationException;
import me.friwi.jcefmaven.UnsupportedPlatformException;
import org.cef.CefApp;

import javax.swing.*;
import java.io.File;

public class JcefHelper {
    private static CefApp cefAppInstance = null;
    private static boolean isInitializing = false;

    public static synchronized CefApp getCefApp() {
        if (cefAppInstance != null) {
            return cefAppInstance;
        }

        if (isInitializing) {
            throw new IllegalStateException("CefApp is currently initializing on another thread!");
        }
        
        isInitializing = true;
        try {
            CefAppBuilder builder = new CefAppBuilder();
            builder.setInstallDir(new File("jcef-bundle")); // Thư mục lưu trữ Chromium
            builder.getCefSettings().windowless_rendering_enabled = false;
            
            // Cấu hình BẮT BUỘC cho WebRTC Video Call:
            // 1. Cho phép dùng Camera/Mic
            // 2. Tự động đồng ý (bỏ qua UI hỏi quyền vì JCEF không vẽ popup này)
            builder.addJcefArgs("--enable-media-stream", "--use-fake-ui-for-media-stream", "--enable-usermedia-screen-capturing", "--auto-select-desktop-capture-source=Entire screen");
            
            // Tắt cache để luôn nhận code JS mới nhất (tuỳ chọn)
            // builder.getCefSettings().cache_path = new File("jcef-cache").getAbsolutePath();
            
            // Xử lý sự kiện tải thư viện
            builder.setAppHandler(new me.friwi.jcefmaven.MavenCefAppHandlerAdapter() {
                @Override
                public void stateHasChanged(org.cef.CefApp.CefAppState state) {
                    if (state == org.cef.CefApp.CefAppState.TERMINATED) {
                        System.out.println("CefApp is terminated");
                    }
                }
            });

            // Tự động tải và cấu hình nhân Chromium (Có thể mất thời gian ở lần chạy đầu tiên)
            System.out.println("Đang khởi tạo JCEF Chromium Engine...");
            cefAppInstance = builder.build();
            System.out.println("JCEF khởi tạo thành công!");
            
        } catch (UnsupportedPlatformException | CefInitializationException | InterruptedException | java.io.IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Không thể khởi tạo nhân trình duyệt Chromium: " + e.getMessage(), "Lỗi JCEF", JOptionPane.ERROR_MESSAGE);
        } finally {
            isInitializing = false;
        }
        
        return cefAppInstance;
    }
}
