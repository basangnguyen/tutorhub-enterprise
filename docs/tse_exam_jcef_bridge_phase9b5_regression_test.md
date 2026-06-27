# Phase 9B.5: Exam JCEF Bridge Regression Test & Service Duplication Audit

## 1. Mục tiêu
- Kiểm tra toàn diện sự ổn định của Phase 9B (Exam JCEF Bridge Integration).
- Audit và sửa lỗi Service Duplication để không bị rò rỉ thread/service (đặc biệt là BrightnessService và PowerShell).
- Kiểm tra Parent Regression và tiến hành Final Submit.

## 2. Build Result
- **Maven Build:** PASSED (`mvn clean install` thành công 100%).

## 3. Portable Build Result
- **Portable Build:** PASSED (`build_portable.ps1` copy đầy đủ JDK, CEF, không có lỗi).

## 4. Exam Quick Settings Test
- *(Pending User Verification - App đang chạy trên desktop)*
- Vui lòng kiểm tra:
  - Popup mở lên bình thường, không blank, không thanh ngang đen.
  - Cập nhật Volume, Brightness nhanh, đúng.
  - Các service chỉ được start 1 lần (do lỗi duplicate service đã được fix).

## 5. Parent Regression Test
- *(Pending User Verification - App đang chạy trên desktop)*
- Kiểm tra popup Parent Quick Settings mở lên bình thường, cập nhật các service đúng trạng thái.

## 6. Final Submit Result
- *(Pending User Verification)*
- Sau khi submit, exit code của Rust cần bằng 0.

## 7. Rust Exit Code
- *(Pending User Verification)*

## 8. Process Cleanup Result
- *(Pending User Verification)*
- Sau khi app đóng lại, cần kiểm tra không còn process Java, Rust (TutorHub_LockdownCore), hoặc PowerShell treo.

## 9. Service Duplication Audit
- **Phát hiện:** Có tình trạng duplicate `BrightnessService` và `VolumeService`. Một bản được tạo và polling trong `TSEQuickSettingsManager`, một bản static khác lại được instantiate ngầm thông qua `TSEBrightnessController` và `TSEVolumeController` khi `QuickSettingsController` gọi tới nó.
- **Cách khắc phục:** 
  - Thêm phương thức `setNativeServices(VolumeService volume, BrightnessService brightness)` vào `QuickSettingsController`.
  - Cập nhật `TSEQuickSettingsManager.java` và `TSEParentHtmlQuickSettingsPopup.java` để **inject** trực tiếp các service vừa được initialize vào `QuickSettingsController`.
  - `QuickSettingsController` sẽ sử dụng trực tiếp reference của native service này thay vì tạo controller facade legacy (qua đó tránh được việc spawn thêm một `BrightnessService` thread mới polling).

## 10. Các file đã sửa
1. `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/QuickSettingsController.java` (Thêm `setNativeServices`).
2. `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEQuickSettingsManager.java` (Inject services).
3. `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEParentHtmlQuickSettingsPopup.java` (Inject services).

## 11. Lỗi còn lại nếu có
- Hiện tại chưa phát hiện thêm lỗi ở phía codebase. Đợi test UI thực tế.

## 12. Kết luận
- Đã sẵn sàng để chuyển sang Phase 10 sau khi test thủ công phía Desktop hoàn thành và các process thoát sạch sẽ.
