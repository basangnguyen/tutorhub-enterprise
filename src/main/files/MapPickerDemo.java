package com.tutorhub.mappicker;

import org.cef.CefApp;
import org.cef.CefSettings;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Demo / ví dụ tích hợp nút "Chọn địa chỉ" vào một form Swing có sẵn.
 *
 * QUAN TRỌNG: Bạn nói đã có sẵn lõi JCEF trong app — nghĩa là bạn đã có
 * một {@link CefApp} được khởi tạo ở đâu đó (thường là 1 singleton dùng
 * chung cho toàn app, khởi tạo 1 lần lúc app start). Trong demo này,
 * {@link #bootstrapStandaloneCefAppForDemoOnly()} chỉ là ví dụ tối giản để
 * class này tự chạy được — trong app thật của bạn, HÃY THAY bằng cách lấy
 * CefApp hiện có, ví dụ: {@code MyJcefManager.getInstance().getCefApp()}.
 */
public class MapPickerDemo {

    // TODO: đổi thành API key Goong thật của bạn (https://account.goong.io)
    private static final String GOONG_API_KEY = "YOUR_GOONG_API_KEY";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MapPickerDemo::createAndShowUi);
    }

    private static void createAndShowUi() {
        // Trong app thật: KHÔNG gọi hàm bootstrap này — dùng CefApp đã có sẵn của bạn.
        CefApp cefApp = bootstrapStandaloneCefAppForDemoOnly();

        JFrame frame = new JFrame("TutorHub — Hồ sơ gia sư (demo)");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(560, 220);
        frame.setLocationRelativeTo(null);

        JLabel addressLabel = new JLabel("Địa chỉ dạy học: (chưa chọn)");

        JButton pickButton = new JButton("📍 Chọn địa chỉ trên bản đồ");
        pickButton.addActionListener(e -> {
            // Mở dialog, CHẶN tới khi người dùng xác nhận hoặc đóng.
            // Truyền initialValue khác null nếu đang ở chế độ "Sửa địa chỉ" để
            // dialog tự fly-to và đặt marker sẵn ở vị trí cũ.
            MapAddressResult result = MapPickerDialog.pickAddress(frame, cefApp, GOONG_API_KEY);

            if (result != null) {
                addressLabel.setText(String.format(
                        "Địa chỉ dạy học: %s  (lat=%.6f, lng=%.6f)",
                        result.getAddress(), result.getLat(), result.getLng()));
                // TODO: lưu result.getAddress() / getLat() / getLng() vào form hoặc DB của bạn
            } else {
                addressLabel.setText("Địa chỉ dạy học: (đã hủy chọn)");
            }
        });

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(pickButton);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        centerPanel.add(addressLabel);

        frame.setLayout(new BorderLayout());
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    /**
     * CHỈ DÙNG CHO DEMO ĐỘC LẬP. Trong app thật của bạn đã có JCEF core
     * (theo bạn mô tả), nên hãy xóa hàm này và lấy CefApp hiện có thay vào.
     *
     * Cách khởi tạo JCEF cấp thấp này giả định native libraries của CEF
     * đã được giải nén/cài sẵn (ví dụ qua jcefmaven CefAppBuilder, hoặc
     * cách bạn đang dùng trong app). Nếu bạn dùng jcefmaven, hãy dùng
     * {@code me.friwi.jcefmaven.CefAppBuilder} thay cho đoạn này.
     */
    private static CefApp bootstrapStandaloneCefAppForDemoOnly() {
        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = false;
        return CefApp.getInstance(settings);
    }
}
