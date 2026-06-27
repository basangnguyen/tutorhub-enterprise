# Phase 4: ClockService + Footer Clock Integration

## 1. Mục tiêu Phase 4
Tích hợp đồng hồ (giờ/ngày) vào thanh Taskbar (ExamFooterStatusBar) của TutorHub Secure Exam sử dụng kiến trúc `Service → StateStore → UI`.
Mục tiêu là hiển thị giờ hệ thống theo định dạng `HH:mm` an toàn, chạy nền qua `ScheduledExecutorService` (daemon thread) không gây treo app hay ảnh hưởng tới quá trình thi/submit.

## 2. File đã tạo/sửa
* **[NEW]** `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ClockService.java`: Dịch vụ định kỳ mỗi phút cập nhật giờ hiện tại vào `QuickSettingsStateStore`.
* **[MODIFY]** `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamFooterStatusBar.java`: Khởi tạo và quản lý lifecycle của `ClockService` cùng `QuickSettingsStateStore`. Bổ sung hiển thị `lblClock` và liên kết với Store Listener để tự động cập nhật UI an toàn.

## 3. ClockService lifecycle
* **initialize()**: Bắt đầu một luồng daemon thông qua `ScheduledExecutorService`. Thiết lập khoảng thời gian lặp mỗi 60 giây và kích hoạt ngay lần gọi đầu tiên để UI không phải chờ.
* **terminate()**: Gọi `scheduler.shutdownNow()` để dừng hoàn toàn tiến trình theo dõi giờ, chống rò rỉ bộ nhớ (leak).

## 4. Cách cập nhật QuickSettingsStateStore
`ClockService` không gọi UI trực tiếp. Nó sinh ra một cặp giá trị chuỗi (ví dụ `"18:30"`, `"14/06/2026"`) thông qua `java.time.LocalDateTime` và đẩy dữ liệu vào Store bằng lệnh `stateStore.updateClock(timeStr, dateStr)`.
Store sẽ tự động trigger các listeners và sử dụng `SwingUtilities.invokeLater` để cập nhật JLabel trên luồng Event Dispatch Thread.

## 5. Cách hiển thị trên footer
`lblClock` được khởi tạo và thêm vào khung `icons` bên phải `ExamFooterStatusBar`, nằm ở giữa `cluster` (WiFi/Volume/Battery) và nút `btnPower`. Định dạng text là `HH:mm` (mặc định) và chứa tooltip hiển thị thông tin ngày.

## 6. Policy clock show/hide
Sử dụng hàm `currentPolicy.getClockMode()`. Khi mode là `SHOW` (mặc định trong Login và Exam), Label sẽ set `setVisible(true)`. Nếu Policy quy định `HIDE`, Label bị ẩn. Tương lai có thể kết nối với config file để tùy chỉnh policy này linh hoạt hơn.

## 7. Test đã chạy
* Lệnh build core project: `mvn clean install` chạy thành công (Exit Code 0).
* Lệnh build bản portable: `build_portable.ps1` chạy thành công.
* Lệnh test giao diện: `run_input_test.bat --exam-id 3` hiển thị log ổn định, không có lỗi Crash hay chặn UI (đã đóng test sau vài chục giây để nhường quyền).

## 8. Rủi ro còn lại
* Ở bản đầy đủ Phase 10-14, có thể cần chuyển `QuickSettingsStateStore` thành Singleton dùng chung để tránh khởi tạo nhiều store cho các thành phần khác nhau nếu thanh Menu/Popup cũng muốn đọc giờ, nhưng hiện tại lưu trữ cục bộ trong footer đủ an toàn cho quy mô hiển thị.
