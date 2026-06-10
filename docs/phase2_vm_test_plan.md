# Phase 2: Integration Testing Plan (VM Only)

**CẢNH BÁO:** Tuyệt đối KHÔNG CHẠY các bài test này trên máy ảo Host của Developer. CHỈ thực hiện trên môi trường máy ảo cách ly (VMware / VirtualBox / Windows Sandbox).

### Ghi chú về Bản Build (Debug vs Production)
- **Debug Build (chạy test VM):** Có chứa Auto-kill timer 120s, hỗ trợ Dev Panic Key (`Ctrl+Shift+Alt+F12` hoặc `Ctrl+Shift+P` qua polling) và log chi tiết.
- **Production Build (môi trường thật):** Hoàn toàn KHÔNG CÓ Auto-kill, KHÔNG CÓ panic fallback, và loại bỏ hoàn toàn polling thread cũng như log nhạy cảm để chống bị bypass.

## 1. Test: Java spawn Rust → kết nối pipe → gửi LOCK → nhận LOCKED
- **Trạng thái:** **PASS** (Đã test thành công luồng LOCK -> PING -> UNLOCK đồng bộ trên Windows 10 VMware).
- **Mục tiêu:** Xác minh Java có thể khởi chạy lõi Rust, kết nối Named Pipe, gửi tín hiệu LOCK và chuyển sang Desktop Kiosk thành công.
- **Lệnh/chương trình cần chạy:** Mở 1 test app Java gọi `LockdownManager.spawnRustProcess`, tiếp theo gọi `waitForPipeReady()`, và `sendLockCommand()`.
- **Điều kiện pass/fail:**
  - Pass: Màn hình chuyển sang một Desktop trống, IPC client nhận tín hiệu `LOCKED`.
  - Fail: Crash tiến trình, lỗi "Pipe không tồn tại", hoặc không chuyển được Desktop.
- **Rủi ro:** Trung bình (bị nhốt vào Desktop mới mà Java không duy trì được).
- **Cách rollback nếu bị kẹt:** Gửi tổ hợp phím `Ctrl+Alt+Del` (qua tính năng Send SAS của VM) rồi chọn Switch User/Sign out.

## 2. Test: PING loop 30 phút không lỗi
- **Trạng thái:** **PASS** một phần (Đã test thành công liên tục 5 phút với 150 PING/PONG. Tạm thời bỏ qua stress test 30 phút trong giai đoạn dev, sẽ thực hiện lại trước release/production).
- **Mục tiêu:** Đảm bảo Heartbeat watchdog hoạt động ổn định trong thời gian dài (Stress test 30 phút).
- **Lệnh/chương trình cần chạy:** Chạy Java gọi `startPingThread()` sau khi `LOCKED`. Cứ 2s gửi 1 `PING`.
- **Điều kiện pass/fail:**
  - Pass: Màn hình Kiosk duy trì vững chắc ít nhất 30 phút, Rust luôn trả về `PONG`.
  - Fail: Đang test bị văng ra màn hình cũ (Watchdog tự ngắt).
- **Rủi ro:** Thấp.
- **Cách rollback nếu bị kẹt:** Đợi 10s (timeout watchdog) để hệ thống tự thoát, hoặc dùng `Ctrl+Alt+Del`.

## 3. Test: UNLOCK an toàn
- **Mục tiêu:** Xác minh Java gửi lệnh UNLOCK và Rust trả quyền điều khiển về Desktop cũ.
- **Lệnh/chương trình cần chạy:** Gọi `LockdownManager.shutdown()`.
- **Điều kiện pass/fail:**
  - Pass: Màn hình nháy về Desktop gốc, IPC nhận được `UNLOCKED`.
  - Fail: Bị kẹt lại ở Kiosk Desktop vĩnh viễn, hoặc tiến trình Rust treo.
- **Rủi ro:** Trung bình.
- **Cách rollback nếu bị kẹt:** Bấm Dev Panic Key (`Ctrl+Shift+Alt+F12`) hoặc `Ctrl+Alt+Del`.

## 4. Test: Kill Java process (Task Manager)
- **Trạng thái:** **PASS** (Đã test thành công trên Windows 10 VMware. Rust phát hiện Broken Pipe và tự dọn dẹp lập tức).
- **Mục tiêu:** Đảm bảo khi Java App (Client) bị crash hoặc bị kill đột ngột, Watchdog bên Rust sẽ tự động dọn dẹp và trả về màn hình cũ sau ≤ 10s.
- **Lệnh/chương trình cần chạy:** Đang trong Kiosk Desktop, dùng PsExec từ Host hoặc Task Manager (nếu bật được) kill `java.exe`.
- **Điều kiện pass/fail:**
  - Pass: Trong vòng 10 giây, màn hình tự văng về Desktop gốc do Heartbeat Timeout.
  - Fail: Bị nhốt lại Kiosk Desktop vĩnh viễn.
- **Rủi ro:** Cao.
- **Cách rollback nếu bị kẹt:** `Ctrl+Alt+Del` -> Sign out, hoặc Reboot cứng máy ảo.

## 5. Test: Kill Rust process (Task Manager)
- **Trạng thái:** **PASS** (Đã test thành công trên Windows 10 VMware. Java gọi thành công emergency reset khi phát hiện pipe bị đứt).
- **Mục tiêu:** Xác định khả năng phục hồi của Java (`emergencyUnlock`) khi tiến trình cốt lõi `TutorHub_LockdownCore.exe` bị crash hoặc kill.
- **Lệnh/chương trình cần chạy:** Dùng Java App để lockdown. Thông qua shell ngầm, chạy `taskkill /f /im TutorHub_LockdownCore.exe`.
- **Điều kiện pass/fail:**
  - Pass: Java phát hiện Pipe bị đứt, kích hoạt `disconnectListener` và gọi `emergencyUnlock()`.
  - Fail: Java App treo cứng vì đợi `PONG` hoặc bị chặn hoàn toàn.
- **Rủi ro:** Trung bình.
- **Cách rollback nếu bị kẹt:** Tắt nóng Java App từ IDE/Console do Desktop đã được trả về khi Rust crash.

## 6. Test: Mở OBS → màn hình đen
- **Trạng thái:** **PASS** (Đã test thành công trên Windows 10 VMware bằng test obs-screen-protection).
- **Mục tiêu:** Đảm bảo `SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)` chặn thành công OBS Studio (screen/video capture).
- **Lệnh/chương trình cần chạy:** Chạy test `obs-screen-protection` từ Java Test Harness. Bật OBS Studio capture Kiosk Desktop.
- **Điều kiện pass/fail:**
  - Pass: OBS recording chỉ thấy màn hình đen/xanh/trống, không lộ nội dung Kiosk. *(Lưu ý: Chống ghi âm/audio capture không thuộc phạm vi của API này, sẽ được xử lý ở Phase sau nếu cần)*.
  - Fail: OBS vẫn quay được rõ ràng nội dung Kiosk.
- **Rủi ro:** Thấp.
- **Cách rollback nếu bị kẹt:** Gọi lệnh UNLOCK từ Java App để trở lại.

## 7. Test: Mở TeamViewer → nhận PROCESS_ALERT
- **Trạng thái:** **PASS** (Đã test thành công trên Windows 10 VMware với tiến trình cấm tạm thời là `notepad.exe`).
- **Mục tiêu:** Đảm bảo Process Scanner phát hiện các phần mềm cấm và đẩy event về Java.
- **Lệnh/chương trình cần chạy:** Cấu hình config JSON chứa `["teamviewer.exe"]`. Khởi động Lockdown. Chạy file `teamviewer.exe` trên VM.
- **Điều kiện pass/fail:**
  - Pass: Java Listener kích hoạt và báo `PROCESS_ALERT: teamviewer.exe`.
  - Fail: Không có alert nào được bắt.
- **Rủi ro:** Thấp.
- **Cách rollback nếu bị kẹt:** UNLOCK từ Java App.

## 8. Test: Bật VirtualBox → VM detected
- **Trạng thái:** **PASS** (Đã test thành công trên Windows 10 VMware nhờ nhận diện đúng VMware MAC Address prefix).
- **Mục tiêu:** Kiểm thử logic nhận diện máy ảo của hệ thống Rust.
- **Lệnh/chương trình cần chạy:** Chạy trên VM VirtualBox bằng test harness `vm-detection`. Với test này, config được truyền vào là `enable_vm_detection: true`. (Lưu ý: Các test khác như `lock-unlock`, `ping-loop` đều dùng `enable_vm_detection: false` để có thể chạy trong môi trường VM).
- **Điều kiện pass/fail:**
  - Pass: Tiến trình Rust phát hiện VM và tự động thoát sớm (exit) trước khi kịp kết nối IPC pipe, kèm theo log thông báo báo hiệu `VmDetected`. Test harness sẽ kết thúc thành công vì kết quả đúng với kỳ vọng.
  - Fail: Màn hình chuyển sang Kiosk trót lọt không bị chặn lại, hoặc tiến trình kết nối pipe thành công.
- **Rủi ro:** Thấp.
- **Cách rollback nếu bị kẹt:** Không đáng kể.

## 9. Test: Dev Panic Key (Ctrl+Shift+Alt+F12)
- **Trạng thái:** **PASS** (Đã test thành công trên Windows 10 VMware. Sử dụng phím fallback `Ctrl+Shift+P` qua polling thread do VMware nuốt phím `Ctrl+Alt`/`F12`).
- **Mục tiêu:** Đảm bảo Keyboard Hook hỗ trợ tổ hợp phím thoát hiểm khẩn cấp cho Dev.
- **Lệnh/chương trình cần chạy:** Phải đảm bảo RustCore được compile với feature `debug_mode`. Chạy test `dev-panic-key` từ Java Test Harness. Bấm tổ hợp `Ctrl+Shift+Alt+F12` (hoặc fallback `Ctrl+Shift+P` trên VMware).
- **Điều kiện pass/fail:**
  - Pass: RustCore lập tức cleanup toàn bộ và văng về màn hình gốc ngay tức thì. Java nhận exit code 99.
  - Fail: Tổ hợp phím không có tác dụng, bị kẹt lại cho đến khi auto-kill (exit code 98) kích hoạt.
- **Rủi ro:** Thấp.
- **Cách rollback nếu bị kẹt:** Chờ Auto-kill timeout (120s) hoặc kill process.

## 10. Test: Auto-kill debug timer 120s
- **Trạng thái:** **PASS** (Đã test thành công trên Windows 10 VMware).
- **Mục tiêu:** Đảm bảo trong bản build Debug, tiến trình sẽ tự sát sau đúng 120 giây để chống kẹt lỗi DEV.
- **Lệnh/chương trình cần chạy:** Chạy test `auto-kill` từ Java Test Harness. Chờ 120 giây.
- **Điều kiện pass/fail:**
  - Pass: Đúng/xấp xỉ 120s, hệ thống tự động thoát Kiosk với exit code 98.
  - Fail: Tiến trình sống mãi mãi, hoặc thoát với code khác.
- **Ghi chú quan trọng:**
  * Auto-kill timer hiện là 120s trong `debug_mode`.
  * exit code 98 = PASS cho auto-kill.
  * exit code 98 = FAIL nếu đang test dev-panic-key.
- **Rủi ro:** Thấp.
- **Cách rollback nếu bị kẹt:** Khởi động lại VM.

## Test Harness Usage

Bạn có thể chạy các test trên máy ảo VM bằng Java Test Harness đã được tạo sẵn. Chỉ chạy các lệnh này trong VM, không chạy trên máy host.

### Compile test harness

```powershell
cd D:\Ban_sao_du_an

javac .\src\main\java\com\mycompany\tutorhub_enterprise\client\managers\LockdownIntegrationTestHarness.java -cp ".\src\main\java"
```

### Chạy Test Harness

```powershell
java -cp ".\src\main\java" com.mycompany.tutorhub_enterprise.client.managers.LockdownIntegrationTestHarness --test <type> [--minutes <min>]
```

Các tham số `<type>` hỗ trợ:
- `lock-unlock`: Mở Kiosk 5 giây rồi đóng an toàn.
- `ping-loop`: Chạy Kiosk và gửi PING liên tục theo số phút chỉ định (ví dụ: `--minutes 1`).
- `process-alert`: Chạy Kiosk và chờ bạn mở phần mềm cấm (OBS/TeamViewer) để kiểm tra luồng bắn PROCESS_ALERT.
- `vm-detection`: Kiểm tra module nhận diện máy ảo chặn lockdown.
- `auto-kill`: Mở Kiosk và tự động chờ 120s để Rust tự sát (Debug build).

## Troubleshooting

- Nếu test `lock-unlock` kẹt ở "Sending LOCK command...", hãy kiểm tra file log sinh ra bởi Rust (`lockdown_sess_xxxx.log`) để xem Rust bị kẹt ở bước nào (ví dụ: tạo desktop, đổi desktop, cài hook) nhờ các log chi tiết đã được thêm vào.
- Nếu test `vm-detection` kết thúc nhanh và không ghi nhận "Pipe connected", đó là kết quả ĐÚNG vì test này không cần kết nối pipe (tiến trình Rust đã phát hiện VM và thoát sớm).
