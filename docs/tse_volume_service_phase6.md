# Phase 6: VolumeService Refactor & COM Lifecycle Cleanup

## 1. Mục tiêu Phase 6
Tách logic lấy và cấu hình âm lượng khỏi `TSEVolumeController`, chuyển vào `VolumeService`. Đặc biệt, chuẩn hóa COM lifecycle để mỗi operation (`CoInitializeEx`) đều có `CoUninitialize` tương ứng, đảm bảo an toàn và không gây treo UI hay leak COM resources.

## 2. File đã tạo/sửa
- [NEW] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/VolumeService.java`
- [MODIFY] `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEVolumeController.java`

## 3. Kiến trúc VolumeService
- **VolumeService.java**: Implement các function core cho Volume (get/set/mute). Hỗ trợ polling async dùng `ScheduledExecutorService` để chạy `refreshNow()` cập nhật vào `QuickSettingsStateStore`. 
- **TSEVolumeController.java**: Hoạt động như một Singleton Wrapper / Facade gọi các hàm từ `VolumeService` để đảm bảo tương thích ngược hoàn toàn với code cũ (không làm hỏng Parent popup / Exam popup).

## 4. COM lifecycle trước và sau khi sửa
- **Trước**: `TSEVolumeController` gọi `CoInitializeEx` ở `getEndpointVolume()` nhưng chỉ có hàm `getStatus()` gọi `CoUninitialize()`. Các hàm `setVolume()` và `setMuted()` không giải phóng COM đúng cách, và khi khởi tạo `IAudioEndpointVolume` lỗi cũng không dọn dẹp, gây memory leak.
- **Sau (Option A)**: `VolumeService` dùng mẫu helper `withCOM(COMOperation)` bảo vệ mọi call bằng try-finally:
  - Khởi tạo `CoInitializeEx(null, Ole32.COINIT_MULTITHREADED)`.
  - Thực thi (get/set).
  - Giải phóng COM bằng `CoUninitialize()` nằm gọn trong `finally`.
  - Cách này giảm thiểu side effects đối với UI thread khi gọi setVolume và không gây lỗi block thread.

## 5. Cách giữ backward compatibility với TSEVolumeController
`TSEVolumeController` được giữ lại dưới dạng:
```java
public class TSEVolumeController {
    private static final VolumeService service = new VolumeService();
    public static TSEVolumeStatus getStatus() { return service.getStatus(); }
    public static int getLastNonZeroVolume() { return service.getLastNonZeroVolume(); }
    public static TSEVolumeStatus setVolume(int percent) { return service.setVolume(percent, null); }
    public static TSEVolumeStatus setMuted(boolean muted) { return service.setMuted(muted, null); }
    public static void shutdownNowNoBlock() { service.terminate(); }
}
```
Điều này đảm bảo mọi Component đang dùng nó đều tiếp tục biên dịch và hoạt động bình thường.

## 6. Test đã chạy
- `mvn clean install`: SUCCESS
- `build_portable.ps1`: SUCCESS

## 7. Rủi ro còn lại
- Hiện tại nếu user kéo slider âm lượng với tốc độ cực nhanh, việc tạo và hủy phiên bản COM liên tục qua `CoInitializeEx`/`CoUninitialize` (Option A) có thể tạo overhead nhỏ thay vì giữ instance xuyên suốt (Option B). Tuy nhiên overhead này chỉ tính bằng ms, không ảnh hưởng UX thực tế.

## 8. Những gì chưa làm
- Chưa tích hợp auto-restore original volume khi thoát app (mới chỉ handle lastNonZeroVolume).
- Chưa động tới UI của Volume Slider popup.
- Chưa cập nhật WiFi/Brightness.
