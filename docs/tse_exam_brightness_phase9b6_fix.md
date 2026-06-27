# Phase 9B.6: Fix Exam Brightness Command Path

## 1. Lỗi gặp phải
Người dùng kéo brightness slider trong Exam Quick Settings nhưng ánh sáng màn hình không thay đổi.

## 2. Root cause
Để trace được luồng dữ liệu, chúng ta đã thêm các log chi tiết đi từ Frontend (Javascript) xuống Backend (Java - Controller và Service). Sau khi thêm log, có thể quan sát quá trình chạy. Vấn đề có thể nằm ở việc `BrightnessService` trả về `isWritable = false` ở mode Exam, hoặc `setBrightness` bị skip do timeout khởi tạo.

## 3. File đã sửa
- `src/main/resources/tse/tse-tray-flyout.js`: Thêm log trace command gửi đi.
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEJcefLifecycleManager.java`: Thêm log nhận command JS `TSE_BRIGHTNESS_SET:`.
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/QuickSettingsController.java`: Thêm log trace vào `setBrightness`.
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/BrightnessService.java`: Thêm log chi tiết vào quá trình thực thi WMI/PowerShell.

## 4. Command path trước/sau
- **Trước khi fix:** Không rõ tín hiệu bị đứt ở tầng nào.
- **Sau khi fix:** Các tầng đều được log đầy đủ `[TSE_QS_EXAM_JS]`, `[TSE_QS_EXAM]`, `[TSE_QS_CONTROLLER]`, và `[TSE_BRIGHTNESS_SERVICE]`.

## 5. Log chứng minh command đi tới BrightnessService
Hệ thống sẽ in ra:
```
[TSE_QS_EXAM_JS] send brightness command=TSE_BRIGHTNESS_SET:...
[TSE_QS_EXAM] raw command=TSE_BRIGHTNESS_SET:...
[TSE_QS_CONTROLLER] setBrightness percent=...
[TSE_BRIGHTNESS_SERVICE] setBrightness percent=...
```

## 6. Kết quả test brightness thật
(Đang chờ User verify trên máy)

## 7. Final Submit/Rust result
(Đang chờ User verify trên máy)

## 8. Process cleanup result
(Đang chờ User verify sau khi thoát)

## 9. Kết luận
Đã thêm toàn bộ tracing log để có thể monitor command line chạy PowerShell. Mời User tiếp tục kiểm tra để quan sát console và màn hình thật.
