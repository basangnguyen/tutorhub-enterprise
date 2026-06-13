# Kế hoạch chi tiết triển khai TSE Controls (Dựa trên SEB)

Sau khi nghiên cứu chuyên sâu mã nguồn SEB (`SafeExamBrowser.SystemComponents`), dưới đây là kế hoạch chi tiết từng bước, từng class để triển khai các chức năng điều khiển UI của TutorHub Secure Exam.

## Phương châm
Gắn kết chức năng thật, giữ nguyên UI chuẩn, sử dụng HTML/DOM overlay để tránh xung đột với JCEF. KHÔNG dùng Swing `JOptionPane` hay `JDialog`.

---

## 1. Dịch vụ môi trường: TSEEnvironmentService.java

Sẽ tạo mới một service chạy nền độc lập với luồng UI (EDT), phục vụ việc đọc trạng thái hệ thống, giống cách SEB dùng `PowerSupply.cs` và `NetworkAdapter.cs`.

#### [NEW] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEEnvironmentService.java`
- **Mục đích**: Class Singleton chạy một `ScheduledExecutorService` (cập nhật mỗi 30s) hoặc cung cấp các static method gọi trực tiếp.
- **Tính năng Pin (Battery)**: 
  - Hàm `getBatteryInfo()` chạy lệnh PowerShell: `Get-CimInstance Win32_Battery -Property EstimatedChargeRemaining, BatteryStatus`.
  - Trả về DTO chứa `% pin` và trạng thái (Đang sạc, Dùng pin, Không có pin).
- **Tính năng Mạng (Network/WiFi)**: 
  - Hàm `getNetworkInfo()` chạy `netsh wlan show interfaces` lấy SSID (tên mạng).
  - Kết hợp `InetAddress.getByName("8.8.8.8").isReachable(...)` để xác định mạng có Internet hay không.

## 2. Quản lý Ngôn ngữ: TSELanguageManager.java

SEB dùng `.resx` và `ResourceManager`. TSE sẽ dùng một Map đơn giản.

#### [NEW] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSELanguageManager.java`
- Cung cấp cơ chế lưu trạng thái ngôn ngữ hiện tại (`VI` hoặc `EN`).
- Có danh sách Map các key string: `SUBMIT_BTN`, `REFRESH_MSG`, `EXIT_BLOCKED_MSG` v.v...
- Cung cấp interface `LanguageChangeListener` để UI (Header, Footer) tự động update text khi có sự kiện toggle ngôn ngữ.

## 3. Cập nhật Footer: ExamFooterStatusBar.java

Thay đổi các Label giả thành Label/Button có chức năng thực sự và thêm callback tương tác với Parent.

#### [MODIFY] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamFooterStatusBar.java`
- **Nút Language (VIE)**: Thêm MouseListener. Khi click sẽ đổi text VIE <-> ENG, phát sự kiện đổi ngôn ngữ toàn cục.
- **Nút WiFi**: Thêm MouseListener. Click sẽ gọi callback `onWifiClicked`. Sẽ cập nhật icon thành mờ nếu mất kết nối.
- **Nút Volume**: Thêm MouseListener. Click gọi `onVolumeClicked`. 
- **Nút Pin**: Cập nhật hàm `battery.setBatteryPercent(...)` theo dữ liệu từ `TSEEnvironmentService`. Click gọi `onBatteryClicked`.
- **Nút Power**: Đổi luồng `onExit` từ `ExitConfirmDialog` (vì cấm dùng JDialog) sang callback nội bộ gửi lên `TSEExamChildClient` để xử lý chặn/cho phép bằng HTML Overlay an toàn.
- *Thiết kế API*:
  ```java
  public interface FooterCallbacks {
      void onWifiClicked();
      void onVolumeClicked();
      void onBatteryClicked();
      void onPowerClicked();
  }
  public ExamFooterStatusBar(FooterCallbacks callbacks) { ... }
  ```

## 4. Cập nhật Header: ExamHeaderBar.java

Bổ sung 2 nút mới: Brightness và About (cùng style với nút Refresh hiện tại).

#### [MODIFY] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamHeaderBar.java`
- **Thêm nút**: `btnBrightness` (icon sun) và `btnAbout` (icon help-circle) đặt cạnh `btnRefresh`.
- **Callback**: Thêm các Runnable/Listener cho `onRefresh`, `onBrightness`, `onAbout`.
- Lắng nghe `TSELanguageManager` để update text chữ "Thời gian còn lại" và "Nộp Bài" nếu ngôn ngữ thay đổi.

## 5. Điều phối chính: TSEExamChildClient.java

Đây là Controller lớn nhất xử lý các thao tác thực sự qua JCEF bridge.

#### [MODIFY] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEExamChildClient.java`
- Triển khai interface `FooterCallbacks` và truyền vào `ExamFooterStatusBar`.
- **Logic WiFi/Pin/Âm thanh/About**: Khi click, inject mã JavaScript vào JCEF để hiển thị thông báo.
  ```java
  browserPanel.executeJavaScript("TSEOverlay.showInfo('Pin hiện tại: 90% (Đang sạc)');");
  ```
- **Logic Power (Thoát)**:
  - Nếu `finalSubmitInProgress` = false: Inject JS Overlay "Bạn không thể thoát khi đang làm bài. Hãy nộp bài trước."
  - Nếu đã nộp bài thành công (hiện thông báo Nộp thành công): Cho phép đóng JFrame và exit(0) an toàn.
- **Logic Refresh**:
  - Khi click: Inject JS Overlay "Bạn có muốn làm mới? Hệ thống sẽ lưu tạm...".
  - Nếu User click "Đồng ý" (JS gửi msg về Java qua JcefLifecycleManager): Java sẽ bắt đầu quy trình Autosave, đợi xong -> gọi `browserPanel.executeJavaScript("window.location.reload(true);")`.
- **Logic Brightness**:
  - Có 1 biến `currentBrightnessState = 0`. Toggle từ `0 -> 1 -> 2 -> 0`.
  - Gọi JS: `document.body.style.filter = "brightness(80%)"` (hoặc 100%, 60%) để làm mờ UI của Chrome, cực kỳ an toàn mà không động tới native API của màn hình.

---

## Acceptance Criteria
1. Nút Language (VIE/ENG) dịch text UI không làm ảnh hưởng nội dung.
2. Các click đều được ghi log: `[TSE_CONTROL] ... clicked`.
3. Brightness làm mờ được JCEF UI thông qua JS Filter.
4. Refresh gọi autosave trước khi reload.
5. Exit bị chặn nếu bài thi đang diễn ra.
6. Wifi và Battery hiển thị trạng thái thực tế.
7. Mọi Overlay đều dùng HTML Inject vào JCEF, không dùng `JOptionPane`.
8. Final Submit (Nộp Bài) chạy trơn tru như cũ, sinh file `.enc`.
