# Phase 8: BrightnessService Adaptive Foundation

## 1. Mục tiêu Phase 8
Tách riêng logic điều chỉnh độ sáng màn hình ra khỏi các controller cũ thành một `BrightnessService` độc lập.
Service này sẽ hoạt động an toàn theo kiến trúc mới: Service -> Store -> UI, đảm bảo tuân thủ tính backward compatibility cho `TSEBrightnessController` cũ để không làm gãy UI.

## 2. Kết quả test brightness ngoài app
Thông qua PowerShell trực tiếp ngoài ứng dụng:
- Đọc `CurrentBrightness`: Lấy được giá trị (ví dụ: 31)
- Set độ sáng sang 80 bằng `WmiSetBrightness(1, 80)`: Thành công
- Đọc lại `CurrentBrightness`: Lấy được giá trị mới (80)
- **Kết luận**: Màn hình thiết bị thật có hỗ trợ WMI để đọc và cài đặt độ sáng. (Thiết bị hiện tại: **Writable**)

## 3. File đã tạo/sửa
- [NEW] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/BrightnessService.java`
- [MODIFY] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEBrightnessController.java`

## 4. Kiến trúc BrightnessService
- Sử dụng Single Thread `ExecutorService` daemon thread (`TSE-BrightnessService`) để không chặn luồng chính.
- `initialize()` để nhận diện khả năng chỉnh sáng.
- `refreshNow()` cập nhật state nhanh.
- Tự động gọi `stateStore.updateBrightness(...)` khi trạng thái thay đổi.
- Bọc lại các cuộc gọi PowerShell bằng Callable timeout (executeWithTimeout).

## 5. Adaptive detection logic
- Bước 1: Gọi WMI thử lấy độ sáng hiện tại (timeout 3s). Nếu Fail -> unsupported.
- Bước 2: Nếu lấy thành công -> supported = true.
- Bước 3: Thử set độ sáng lại mức hiện tại (chỉ set >=20 để không tối đen). Nếu set thành công -> writable = true.
- Bước 4: Lưu trạng thái và cập nhật store.

## 6. Cách giữ backward compatibility với TSEBrightnessController
- `TSEBrightnessController` được cấu trúc lại như một facade tĩnh, bao bọc một instance tĩnh của `BrightnessService`.
- Khi các component cũ như Parent hay Exam (JCEF) gọi `TSEBrightnessController.getStatus()` hoặc `setBrightness`, nó sẽ route đến `service.getStatus()` tương ứng.
- Khi gọi `shutdownNowNoBlock`, executor và process của PowerShell sẽ bị terminated thông qua `service.terminate()`.

## 7. Timeout/process cleanup
- Hàm `runPowerShell` bọc `ProcessBuilder` sẽ đợi process tối đa 2s.
- Nếu không xong (`!finished`), lập tức gọi `process.destroyForcibly()`.
- Thread chạy PowerShell được giám sát bằng `Future.get(3000, TimeUnit.MILLISECONDS)`.
- Hàm `terminate()` destroy active process.

## 8. Test đã chạy
- [x] Maven build `mvn clean install`
- [x] Portable build `build_portable.ps1`
- [x] Chạy debug test `run_input_test.bat --exam-id 3`
- [x] Phân tích log xem Service có khởi chạy và nhận diện được không.

## 9. Kết quả brightness thật
Thiết bị hiện tại: **Writable**

## 10. Rủi ro còn lại
- Một số màn hình rời (desktop monitors) hỗ trợ giao thức DDC/CI thay vì WMI (như laptop). Nếu vậy, WMI method sẽ Fail. Tạm thời giải pháp là unsupported.
- Hàm gọi PowerShell vẫn là I/O nặng và delay khi user thao tác thanh trượt quá nhanh (dù đã có timeout, nhưng nó vẫn là lệnh gọi Process mới). Tốt nhất sau này cần native code qua DLL hoặc JNA.

## 11. Những gì chưa làm
- Không implement DDC/CI (JNA Native Monitor API) trong phase này.
- Chưa test và thay thế toàn bộ lệnh gọi WMI bằng các method native nhẹ hơn.
