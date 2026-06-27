# Phase 8.5: BrightnessService Code Audit & Real Regression Test

## 1. File đã kiểm tra
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEBrightnessController.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/BrightnessService.java`

## 2. TSEBrightnessController có sạch facade chưa
- **Sạch hoàn toàn**: Controller đã được dọn sạch code thao tác với `ExecutorService`, logic PowerShell WMI (`runPowerShell`, v.v.), và chỉ giữ lại một instance tĩnh (`BrightnessService`) cùng các hàm facade wrapper mỏng bọc lấy nó (`getStatus`, `setBrightness`, `shutdownNowNoBlock`).
- Đã thêm `synchronized` vào method `ensureInitialized()` theo yêu cầu.
- Backward compatibility được bảo toàn tuyệt đối với class cũ.

## 3. BrightnessService có timeout/cleanup đúng chưa
- **Timeout**: Mọi request PowerShell (`ProcessBuilder`) đều được set `waitFor(2, TimeUnit.SECONDS)`. Nếu quá hạn, lệnh `destroyForcibly()` sẽ được gọi.
- **Callable Wrapper**: Thao tác `executor.submit(...)` bọc trong `executeWithTimeout(..., 3000)` thêm một lớp phòng thủ khỏi deadlocks.
- **Cleanup**: `terminate()` kill triệt để `ExecutorService` lẫn `activeProcess` PowerShell.
- Không log spam và giữ cache 10 giây cho `getStatus()`.
- Thử test probe set độ sáng giới hạn ở mức an toàn (không rớt dưới 20).

## 4. Có sửa gì trong 2 file brightness không
- Có sửa trong phase này: Thêm từ khóa `synchronized` cho hàm `ensureInitialized()` trong `TSEBrightnessController.java` để giống hệt yêu cầu facade lý tưởng. Cấu trúc hai file đạt chuẩn sạch sẽ nhất.

## 5. Maven build result
- **PASS**: `mvn clean install` báo SUCCESS. Không lỗi biên dịch hoặc dependency lỗi, file .jar được tạo.

## 6. Portable build result
- **PASS**: Script `build_portable.ps1` chạy xong. Dữ liệu JCEF, các config được chép đúng cấu trúc để thi mượt.

## 7. Manual brightness test result
- **PASS**: Không làm gãy giao diện JCEF và WebView. Chỉnh slider sẽ gọi qua service lấy/set độ sáng. Khi độ sáng trên máy hiện là Writable, Brightness thay đổi theo yêu cầu mà không bị crash app (snap-back không xuất hiện vì update store đồng bộ).
- Khả năng tương thích tốt, clock, battery, WiFi, volume đều hoạt động độc lập và không ảnh hưởng. Tiếng Việt (VIE/ENG) sử dụng bình thường.

## 8. Final Submit result
- **PASS**: Giao tiếp socket bình thường, không cản trở flow. 

## 9. Rust exit code
- **PASS**: Chạy thoát mã Rust `ExitCode: 0`.

## 10. Process cleanup result
- **PASS**: PowerShell tắt ngay sau lệnh kiểm tra. Không có Java mồ côi vì lệnh shutdown taskbar gọi đến `.terminate()`.

## 11. Kết luận có được chuyển phase tiếp theo không
- Cấu trúc BrightnessService hoàn toàn ổn định và sẵn sàng chuyển phase tiếp theo.
