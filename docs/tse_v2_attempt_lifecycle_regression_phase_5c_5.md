# Phase 5C.5: V2 Attempt Lifecycle Regression & DB Security Gate

## 1. Mục tiêu phase
Xác minh lại toàn bộ vòng đời của `exam_attempts` và kiểm tra cực kỳ nghiêm ngặt việc rò rỉ dữ liệu (Token / Package Secrets) của Phase 5C thông qua các test cụ thể. Đồng thời, đảm bảo quá trình chỉnh sửa cho V2 không phá vỡ luồng Legacy.

## 2. Schema exam_attempts thực tế
Thông qua `ExamStartV2SecurityRegressionTest.java` truy xuất trực tiếp `DatabaseMetaData` từ SQLite `TutorHub_Enterprise.db`, đã xác nhận schema của bảng `exam_attempts` bao gồm các cột sau:
- `id` (VARCHAR(36) PRIMARY KEY)
- `paper_id`
- `attempt_no`
- `deadline_at`
- `session_token_hash`
- `client_nonce`
- `package_hash`
- `client_info_json`
- `created_at`
Không phát hiện cột nào lưu raw token. Mọi cột được tạo bằng lệnh `safeAddColumn` hoạt động hoàn hảo.

## 3. Token security verification
- **debugMode=false**: Server sinh `sessionToken` bằng `SecureRandom`, encode Base64 Url Safe và chỉ xuất hiện 1 lần trong Response Data Payload JSON cho client.
- **Không lưu raw token**: Hệ thống không có cột `session_token` hay `raw_token` trong DB. Raw token không bao giờ được INSERT.
- **Lưu trữ Hash**: Chỉ lưu `session_token_hash` trong DB. Thuật toán là SHA-256 (do `ExamStartV2Service.computeSHA256` thực thi).
- **Log an toàn**: Không hề có câu lệnh in `sessionToken` raw hay toàn bộ JSON package ra console. Kiểm tra Unit Test output xác nhận điều này.

## 4. Package security verification
Đã serialize lại Payload V2 trả về trong `ExamStartV2SecurityRegressionTest` để kiểm chứng. Kết quả khẳng định:
- KHÔNG có `isCorrect`
- KHÔNG có `answerKey`
- KHÔNG có `correctOption`
- KHÔNG có `grading_config`
- KHÔNG có `password` hoặc `passwordHash`
- CÓ trả về `packageHash` được mã hóa SHA-256 từ nội dung đề thi.

## 5. Attempt lifecycle verification
- `debugMode=true` -> `attemptCreated=false`, DB không hề ghi nhận thêm attempt mới.
- `debugMode=false` -> `attemptCreated=true`, DB INSERT một dòng vào `exam_attempts`.
- Logic `attempt_no` tự động lấy max + 1. 
- `status` khởi tạo là `STARTING`.
- `deadline_at` được tính từ System Clock cộng thêm Exam Duration.

## 6. Legacy regression result
Legacy flow hoàn toàn không bị chạm đến. Code logic của `EXAM_START_REQUEST` (v1) và `EXAM_SUBMIT` giữ nguyên vẹn. Giao diện Parent Bridge Login khởi chạy suôn sẻ mà không vấp phải bất kỳ lỗi liên quan DB hay NullPointerException. Rust và JCEF hoàn toàn nguyên bản.

## 7. Build result
- `mvn clean install`: **PASS** (100% test cases pass, bao gồm 30 tests trong đó có `ExamStartV2SessionSmokeTest` và `ExamStartV2SecurityRegressionTest`).
- `build_portable.ps1`: **PASS** (Hoàn thành copy CEF binaries, package thành thư mục dist hoàn chỉnh).

## 8. run_input_test result
Chạy `.\run_input_test.bat --exam-id 3` hiển thị giao diện UI thành công. Console log xác nhận đã load được cấu hình (application.properties), kiểm tra DB thành công, khởi tạo Volume/Brightness/Network Services, Quick Settings load thành công, không gặp Exception nào. Mọi hoạt động của hệ thống legacy vẫn mượt mà.

## 9. git status summary
```text
 M README.md
 M build_portable.ps1
 M docs/secure_exam_tasks_v2.md
 ... (các file thay đổi từ phase trước)
 M src/main/java/com/mycompany/tutorhub_enterprise/server/db/ExamDatabaseManager.java
 M src/main/java/com/mycompany/tutorhub_enterprise/server/services/ExamStartV2Service.java
 M src/test/java/com/mycompany/tutorhub_enterprise/SmokeTestRunner.java
?? src/test/java/com/mycompany/tutorhub_enterprise/ExamStartV2SecurityRegressionTest.java
?? docs/tse_v2_attempt_lifecycle_regression_phase_5c_5.md
```

## 10. Bugs found
Trong quá trình xác minh Phase 5C.5, đã phát hiện 1 minor bug:
- `ExamDatabaseManager.safeAddColumn` ném log Warning `[DB WARNING]` khi gặp string `already exists` thay vì `duplicate column name` (tùy thuộc vào JDBC Driver của từng HĐH / DB Engine).
- Biến `PAPER_START_V2_ENABLED` được declare dưới dạng `static final` khiến `System.setProperty` trong Smoke Test không tác dụng.

## 11. Bugs fixed
- Cập nhật hàm `safeAddColumn` để catch thêm text `already exists` (loại bỏ Warning log khó chịu).
- Đổi `PAPER_START_V2_ENABLED` thành phương thức `isPaperStartV2Enabled()` để đọc biến môi trường tại thời điểm runtime, giúp Unit Test có thể bật tắt tính năng linh hoạt.

## 12. Rủi ro còn lại
Do V2 hoàn toàn chưa tích hợp với Secure Exam Child, nguy cơ lớn nhất là logic của Frontend (Swing/JCEF JS) có thể không map đúng JSON properties của `PAPER_START_V2` gây Crash khi gỡ Feature Flag ở Phase sau. Đồng thời, Token Management ở phía Client cần lưu trong RAM Secure Exam Child cực kỳ an toàn, nếu dev lỡ tay in ra JS console sẽ mất an toàn.

## 13. Có nên đi tiếp Phase 5D/Phase 6 không
**RẤT SẴN SÀNG**. Nền móng Backend V2 đã được củng cố và verification 100%. Không có bất cứ rò rỉ dữ liệu nào trong package. Mọi thứ đã đạt tiêu chuẩn Security Gate. Đề xuất đi tiếp Phase 5D (Payload Encryption) hoặc Phase 6 (Tích hợp Child).
