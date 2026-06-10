package com.mycompany.tutorhub_enterprise.client;

import me.friwi.jcefmaven.CefAppBuilder;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class VideoCallWindow {

    private static CefApp cefApp = null;
    private static CefClient client = null;
    private static JFrame frame = null;
    private static CefBrowser browser = null;

    public static void open(String roomName) {
        // QUAN TRỌNG NHẤT: Ép toàn bộ quá trình cấu hình CEF chạy trên Luồng giao diện (EDT)
        // Việc này giải quyết dứt điểm 100% lỗi xung đột làm liệt chuột và bàn phím!
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Khởi tạo lõi Chrome thông qua JcefManager dùng chung
                if (client == null) {
                    client = JcefManager.getClient();
                }

                // 2. Nếu cửa sổ đang mở rồi thì chỉ cần đưa nó lên trên cùng
                if (frame != null && frame.isVisible()) {
                    frame.toFront();
                    return;
                }

                // 3. Tạo cửa sổ Video phụ
                frame = new JFrame("TutorHub Video Call");
                frame.setSize(350, 600);
                frame.setLayout(new BorderLayout());
                frame.setAlwaysOnTop(true); 
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Chỉ tắt cửa sổ này, app chính vẫn sống

                // 4. Tạo Browser và gắn vào Frame TRƯỚC KHI Frame hiện lên (Tránh sốc đồ họa)
                String meetUrl = "https://meet.jit.si/TutorHub_" + roomName + "#config.prejoinPageEnabled=false";
                browser = client.createBrowser(meetUrl, false, false);
                frame.add(browser.getUIComponent(), BorderLayout.CENTER);

                // 5. Xử lý khi bấm nút X tắt Video Call
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        if (browser != null) {
                            browser.loadURL("about:blank"); // Ngắt camera ngay lập tức
                        }
                    }
                });

                // 6. Cho phép hiển thị lên màn hình
                frame.setLocationRelativeTo(null); 
                frame.setVisible(true);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Lỗi khởi tạo Camera: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}