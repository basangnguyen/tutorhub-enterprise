# Phase 7A: NetworkService Read-only / Scan-only Foundation

## 1. Mục tiêu Phase 7A
Chuẩn hóa phần WiFi/Network thành một daemon service riêng biệt, chuẩn bị nền tảng để sau này có thể scan WiFi thật giống SEB. Phase này **không** connect WiFi, **không** mở UI mạng của Windows, giữ trạng thái an toàn nhất.

## 2. File đã tạo/sửa
- [NEW] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/NetworkService.java`
- [MODIFY] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamFooterStatusBar.java`

## 3. NetworkService lifecycle
- `initialize()`: Tạo một daemon thread scheduler bằng `Executors.newSingleThreadScheduledExecutor`, lên lịch tác vụ quét mạng (`refreshNow()`) mỗi 15 giây.
- `refreshNow()`: Thực hiện việc lấy trạng thái network và gọi `QuickSettingsStateStore.updateWifi(...)`. Chạy hoàn toàn trên luồng nền (daemon thread).
- `terminate()`: Dừng an toàn `scheduler.shutdownNow()`.
- `isRunning()`: Trả về trạng thái hoạt động của scheduler.

## 4. Chọn Option A hay Option B và vì sao
Đã chọn **Option A** (Refactor read-only hiện tại bằng `netsh` và `TSENetworkStatusProvider` cũ).
- Lý do: Bảo đảm ổn định tối đa (pass build, không crash), không gặp rắc rối với Memory Access Violation khi dùng JNA `wlanapi.dll`.

## 5. Cách cập nhật QuickSettingsStateStore
Trong `refreshNow()`, đọc `snapshot.wifiMode` từ `TSESecurityPolicy`:
- Nếu `DISABLED`, ghi trạng thái `status="DISABLED"`.
- Nếu có adapter nhưng mất kết nối, ghi `status="DISCONNECTED"`.
- Nếu không có adapter, ghi `status="NO_ADAPTER"`.
- Nếu kết nối, ghi `status="CONNECTED"`, và cập nhật `ssid`, `signal`.

Dữ liệu được cập nhật thread-safe thông qua `stateStore.updateWifi(...)`.

## 6. Cách footer render WiFi từ snapshot
`ExamFooterStatusBar.java` đã được xóa bỏ `SwingWorker` gọi trực tiếp `TSENetworkStatusProvider`. Thay vào đó, trong hàm listener của `QuickSettingsStateStore`:
- Dịch `snapshot.wifiStatus` thành tooltip phù hợp.
- Tính toán boolean `hasWifi` và `isWifiConnected`.
- Gọi hàm update duy nhất lên EDT thông qua `cluster.updateWifi(hasWifi, isWifiConnected, wifiTooltip)`.

## 7. Security policy WiFi hiện tại
Sử dụng `TSESecurityPolicy.WifiMode`:
- `DISABLED`: UI/NetworkService chủ động ngừng quét thật và trả về lỗi/tooltip "WiFi bị vô hiệu hóa".
- Mặc định là `READ_ONLY` ở cấu hình hiện tại (forLogin / forExam / forConfig), cho phép xem mạng đang kết nối.

## 8. Test đã chạy
- `mvn clean install`: SUCCESS (Total time: ~31s)
- `build_portable.ps1`: SUCCESS

## 9. Rủi ro còn lại
- Tooling `netsh` bị phụ thuộc nhiều vào command-line tiếng Việt / tiếng Anh. Option A hiện tại vẫn sử dụng regex thủ công nên có thể bị lỗi trên phiên bản Windows ngôn ngữ lạ.

## 10. Những gì chưa làm
- Chưa can thiệp vào Parent popup UI / Exam popup UI (Click vào icon WiFi sẽ chưa mở gì sâu thêm).
- Chưa động tới tính năng quét tất cả mạng (`WlanEnumInterfaces` / Option B).
- Chưa kết nối WiFi mới.
