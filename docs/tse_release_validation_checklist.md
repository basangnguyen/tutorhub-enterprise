# TSE Release Validation Checklist (Installer v1.0.0)

This checklist is used to validate the TutorHub Secure Exam Production Installer before release or transitioning to Step 2I.8.5 (jlink optimization) and Step 2I.8.6 (code signing).

## A. Installation Test
- [ ] Cài mới từ `TutorHubSecureExamSetup.exe`.
- [ ] Cài vào `LocalAppData\TutorHubSecureExam` đúng vị trí (hoặc đường dẫn được chọn).
- [ ] Desktop shortcut hoạt động.
- [ ] Start Menu shortcut hoạt động.
- [ ] App mở thành công không cần IDE/NetBeans/Maven.
- [ ] App không phụ thuộc Java hệ thống.

## B. Basic Runtime Test
- [ ] Production Launcher mở được (hiển thị UI `TutorHub Secure Exam - Production Launcher`).
- [ ] `app\application.properties` tồn tại trong thư mục cài đặt.
- [ ] `runtime\bin\java.exe` tồn tại trong thư mục cài đặt.
- [ ] `app\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar` (FAT JAR) tồn tại.
- [ ] Thư mục `logs\` và `temp_jcef\` tồn tại (hoặc được tạo thành công khi chạy).

## C. Server Connectivity Test
- [ ] Khi server tắt: app báo lỗi rõ ràng, không crash.
- [ ] Khi server bật (`localhost:7860`): app kết nối thành công.
- [ ] Login dev/test account thành công.

## D. Secure Exam E2E Test
- [ ] Bấm Start Secure Exam.
- [ ] Rust chuyển sang Secure Desktop thành công.
- [ ] Java Child Client (`TSEExamChildClient`) mở trên Secure Desktop.
- [ ] JCEF render được nội dung bài thi.
- [ ] Chọn đáp án hoặc nhập tự luận bình thường.
- [ ] Bấm Nộp bài (Submit).
- [ ] Hệ thống tự động quay về Default Desktop.
- [ ] Parent Client submit đáp án lên Server thành công.
- [ ] DB (Neon) lưu trữ `exam_answers` chính xác.
- [ ] Trạng thái `exam_sessions.status` chuyển thành `SUBMITTED`.

## E. Retry Submit Test
- [ ] Child tạo file `submit_payload.enc` thành công.
- [ ] Tắt server (fail) trước khi Parent có cơ hội submit.
- [ ] Parent giữ nguyên temp folder.
- [ ] Giao diện Parent hiện trạng thái lỗi và nút **Retry Submit**.
- [ ] Bật lại server.
- [ ] Bấm **Retry Submit** -> Submit thành công.
- [ ] Hệ thống tự dọn dẹp temp folder (Temp cleanup) sau khi success.

## F. Auto-save & Recovery Test
- [ ] Đợi quá trình autosave định kỳ chạy, file `autosave_payload.enc` được tạo trong `%TEMP%`.
- [ ] Mô phỏng Child crash hoặc đóng đột ngột trước khi bấm nộp bài.
- [ ] Parent phát hiện được file autosave.
- [ ] Parent tự động submit autosave thành công.
- [ ] DB có ghi nhận answer từ autosave.

## G. DPAPI Recovery Test
- [ ] Mô phỏng Parent crash ngay sau khi có autosave/submit payload.
- [ ] Mở lại app Launcher.
- [ ] Bấm nút **Recover Pending Submission**.
- [ ] DPAPI giải mã `recovery_key.enc` thành công (trên cùng Windows user).
- [ ] Đọc và giải mã payload, submit recovery thành công.
- [ ] Temp cleanup chạy dọn dẹp sau thành công.
- [ ] Ghi rõ limitation: Nếu chuyển sang Windows user khác hoặc máy khác sẽ KHÔNG decrypt được recovery key.

## H. Process Cleanup Test
- [ ] Sau luồng thi (hoặc khi đóng app), **không còn** process `TutorHub_LockdownCore.exe` bị treo trong Task Manager.
- [ ] **Không còn** Java Child process (`TSEExamChildClient`) bị treo.
- [ ] *Lưu ý: Bỏ qua Java IDE/Server process, chỉ check process của Client.*

## I. Uninstall Test
- [ ] Gỡ cài đặt app từ **Apps & Features** (hoặc Control Panel).
- [ ] Shortcut Desktop và Start Menu bị xóa.
- [ ] Thư mục cài đặt gốc bị xóa (hoặc chỉ còn dữ liệu user/log nếu được thiết kế để giữ lại).
- [ ] Chú ý: Không làm mất các temp recovery chưa submit trong `%TEMP%` (nếu chưa được yêu cầu xóa).

## J. Security / Anti-cheat Smoke Test
- [ ] Phím Panic key (hoặc Exit debug) hoạt động.
- [ ] Secure Desktop tự quay về Default Desktop nếu Child bị timeout hoặc crash.
- [ ] **Không có plaintext** `exam_context.json` hoặc `submit_payload.json` trong thư mục `%TEMP%`.
- [ ] Chỉ tồn tại các file nén/mã hóa (`.enc`).
- [ ] **Không log plaintext** key, answers, hay toàn bộ payload ra file log text.

## K. Known Limitations
- [ ] **Chưa Code Signing:** SmartScreen của Windows có thể hiện cảnh báo "Unknown Publisher".
- [ ] **Runtime Dung Lượng Lớn:** Do copy toàn bộ JDK/JRE thay vì jlink, bộ cài khá nặng.
- [ ] **DPAPI Lock:** Tính năng Recovery hiện tại gắn chặt với account Windows hiện hành.
- [ ] **Chưa Auto-Update:** Sẽ xử lý ở các phase sau.
- [ ] **Chưa có jlink mini runtime:** Sẽ xử lý ở Step 2I.8.5.
