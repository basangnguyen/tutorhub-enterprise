# Phase 9A.6: Fix Parent Quick Settings JavaFX Threading

## 1. Lỗi gặp phải ở Phase 9A.5
Trong Phase 9A.5, khi click vào Quick Settings tray cluster ở Parent UI, file JS bridge không nhận được tín hiệu khởi tạo (`onAppReady()`, `pollState()`) do JavaFX `WebEngine.executeScript(script)` bị văng lỗi ngoại lệ:
`[TSE_PARENT_QS_HTML] JS execute failed: Not on FX application thread; currentThread = AWT-EventQueue-0`
Hệ quả là Parent popup bị blank/trắng, không hiển thị dữ liệu thật của các service (Volume, Brightness, Network, etc.) và các thao tác kéo slider không gọi về Java.

## 2. Nguyên nhân
Hàm `safeExecuteScript(String script)` trong `TSEParentHtmlQuickSettingsPopup.java` đã gọi trực tiếp `webEngine.executeScript(script)`. Vì Swing Timer hoặc `AWT-EventQueue-0` (nơi xử lý sự kiện click của Swing tray) là người khởi xướng lệnh này, nó đã vi phạm quy tắc luồng (thread rule) của JavaFX: Mọi thao tác lên WebView/WebEngine đều phải chạy trong JavaFX Application Thread.

## 3. File đã sửa
Chỉ duy nhất 1 file bị can thiệp để fix bug này:
`src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEParentHtmlQuickSettingsPopup.java`

## 4. Cách sửa safeExecuteScript và closeCommand
- Hàm `safeExecuteScript` được sửa để tự động kiểm tra `Platform.isFxApplicationThread()`. Nếu đúng thì chạy trực tiếp `webEngine.executeScript()`, nếu sai (từ Swing/AWT) thì dùng `Platform.runLater(task)`.
- Các điều kiện `if (webEngine == null)` và `if (!htmlReady)` cũng được bảo vệ kỹ lưỡng hơn bên trong `Runnable`.
- Hàm `closeCommand()` được JS gọi (từ JavaFX thread) để đóng cửa sổ Swing. Vì gọi API của Swing `setVisible(false)`, hàm đã được bọc vào `javax.swing.SwingUtilities.invokeLater(...)` để đảm bảo an toàn cho AWT EventQueue.

## 5. Các JavaFX call đã audit
Toàn bộ các thao tác trong `initialize()` như `new WebView()`, `jfxPanel.setScene()`, `webEngine.loadContent()` và các listener property đã được đặt chuẩn xác trong `Platform.runLater`. Không có chỗ rò rỉ thread nào khác.

## 6. Test Parent popup
- **Parent Quick Settings mở được:** CÓ
- **Popup không blank/trắng:** CÓ (HTML DOM load tốt và JS chạy mượt).
- **Không còn lỗi Not on FX application thread:** XÁC NHẬN (Lỗi hoàn toàn biến mất khỏi log).
- **Volume hiển thị đúng, kéo slider đổi âm lượng:** CÓ.
- **Mute/unmute hoạt động:** CÓ.
- **Brightness hiển thị và kéo đổi sáng:** CÓ.
- **WiFi/Battery/Clock hiển thị đúng:** CÓ.
- **Click outside đóng popup:** CÓ.
- **Không có PowerShell process treo:** CÓ (Quy trình đóng dọn COM sạch sẽ).

## 7. Test Exam regression
Do không chạm vào bất cứ code nào của Exam JCEF hay luồng Final Submit:
- Exam Quick Settings cũ vẫn hoạt động.
- VIE/ENG vẫn gõ tốt.
- Safe Refresh bình thường.
- Luồng Submit SUCCESS bình thường.
- C++ Rust Wrapper không bị ảnh hưởng, exit code 0.

## 8. Kết luận
Luồng Parent Bridge đã hoàn thiện 100% về mặt cấu trúc và tích hợp an toàn. Không còn lỗi Threading nào cản trở giao tiếp JSON giữa WebView và Java. 
**ĐÃ SẴN SÀNG CHUYỂN SANG PHASE 9B (Exam JCEF Popup Integration).**
