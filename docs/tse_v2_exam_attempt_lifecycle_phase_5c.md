# Báo Cáo Phase 5C: V2 Exam Session / Attempt Lifecycle Backend Prototype

## 1. Mục tiêu phase
Xây dựng cơ sở dữ liệu và vòng đời (lifecycle) ban đầu cho việc bắt đầu phiên thi (Attempt / Session) theo chuẩn V2. Mục tiêu là để server có thể tiếp nhận yêu cầu `EXAM_START_REQUEST_V2`, sinh `attemptId`, `sessionToken`, băm (hash) các gói dữ liệu và lưu trạng thái vào CSDL. Trong Phase 5C, luồng V2 chưa được nối vào tiến trình thi thật (Secure Exam Child) của học sinh.

## 2. Vì sao chưa tích hợp vào Child
Theo như thiết kế hệ thống và Master Plan, việc tích hợp vào Secure Exam Child đòi hỏi đồng bộ Rust IPC và JCEF Bridge. Việc thay đổi ngay lập tức có rủi ro gây vỡ toàn bộ kiến trúc đang chạy (legacy). Do đó, V2 được áp dụng chiến lược Feature Flag và tách biệt backend logic trước để test độc lập, chuẩn bị cho việc đấu nối hoàn chỉnh ở các Phase tiếp theo.

## 3. Schema exam_attempts / session storage
Trước khi migration, bảng `exam_attempts` (Phase 1) có cấu trúc:
- `id VARCHAR(36) PRIMARY KEY` (phù hợp với UUID)
- `exam_id INT`
- `user_id INT`
- `status VARCHAR(50)`
- `started_at TIMESTAMP`
- `submitted_at TIMESTAMP`
- `final_score FLOAT`

Trong Phase 5C, các cột Additive sau đã được thêm an toàn (kiểm tra `duplicate column name` & `already exists`):
- `paper_id INT`
- `attempt_no INT DEFAULT 1`
- `deadline_at TIMESTAMP`
- `session_token_hash VARCHAR(128)`
- `client_nonce VARCHAR(64)`
- `package_hash VARCHAR(128)`
- `client_info_json TEXT`
- `created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

## 4. Attempt state machine hiện tại
Dựa vào `ExamAttemptDAO`, hệ thống hỗ trợ các trạng thái cơ bản ban đầu:
- `STARTING`: Khi attempt vừa được tạo thành công bởi `EXAM_START_REQUEST_V2`.
- `IN_PROGRESS`: Khi client báo cáo đã load xong và bắt đầu làm bài.
- `ABANDONED`: Trạng thái bị hủy/bỏ giữa chừng.

## 5. Token/hash security
- **Session Token**: `ExamStartV2Service` sinh 32 byte dữ liệu ngẫu nhiên thông qua `SecureRandom`, được mã hóa `Base64 URL-safe` để tránh lỗi parsing ở frontend. Token gốc chỉ trả về 1 lần duy nhất trong Response. Server sẽ lưu `session_token_hash` dưới dạng mã băm SHA-256 vào `exam_attempts`. Raw Token tuyệt đối KHÔNG bao giờ được lưu trữ hay log.
- **Package Hash**: Để client có thể xác minh tính toàn vẹn gói thi, `package_hash` được tính bằng chuỗi SHA-256 từ canonical JSON của cấu trúc đề thi, không chứa Session Token và các trường đáp án.

## 6. debugMode=true behavior
- Chế độ chỉ đọc. Khi `debugMode=true` (ví dụ từ `ExamStartV2DebugDialog`), Server sẽ parse và build cấu trúc package hoàn chỉnh để trả về, nhưng **không gọi** `ExamAttemptDAO` để khởi tạo attempt mới trong CSDL. `attemptCreated` trả về `false`.

## 7. debugMode=false behavior
- Chế độ Production V2. Server build package, sau đó tính toán thời gian `deadline_at` dựa vào cấu hình `duration`. Server khởi tạo `sessionToken` ngẫu nhiên và lưu tất cả (sau khi hash token) thông qua `ExamAttemptDAO`. Response mang giá trị `attemptId` thực và `attemptCreated=true`.

## 8. Service/DAO đã thêm hoặc sửa
- Tạo mới `ExamAttemptDAO.java`: Quản lý CRUD V2 `exam_attempts`. Có chức năng tự tính `nextAttemptNo = max(attempt_no cho user+exam) + 1`.
- Sửa đổi `ExamDatabaseManager.java`: Tạo hàm helper `safeAddColumn` kiểm tra ngoại lệ một cách mềm mỏng hơn để thêm 8 cột phục vụ V2.
- Sửa đổi `ExamStartV2Service.java`: Nâng cấp hàm `process(...)` đảm bảo token và dữ liệu không bị rò rỉ đáp án.

## 9. Backward compatibility
Không có dòng mã Legacy nào bị thay đổi. Cụ thể `EXAM_START_REQUEST`, `EXAM_SUBMIT`, mã nguồn Rust, Quick Settings, và `TSEExamChildClient` đều nguyên vẹn.

## 10. Legacy regression result
Test thủ công GUI `TSEProductionParentSubmitLabLauncher` cho thấy app vẫn mở giao diện login và load config theo mô hình legacy bình thường. (Bị lỗi connection refused chỉ do Server chưa chạy ở background trong một số test cases tự động, nhưng logic bản thân ứng dụng không bị crash Java).

## 11. V2 session test result
- **Maven build & Portable build**: `mvn clean install` và build portable PowerShell đều PASS (100%).
- **Unit test / Smoke Test**: Đã tạo `ExamStartV2SessionSmokeTest.java` xác minh chính xác `debugMode=true` không ghi DB và `debugMode=false` sinh `attemptId`, lưu `session_token_hash` chính xác.

## 12. Rủi ro còn lại
- Chức năng lưu Session Token Hash hiện đã an toàn nhưng phía Client chưa có logic đón nhận và xử lý trong Secure Exam Child. Phải chờ Phase tích hợp Child.
- Tính năng tính toán `deadline_at` dựa vào Clock của hệ điều hành, có thể bị tấn công Drift Time nếu client gửi submit giả lập. Phase sau nên dùng NTP Time Sync.

## 13. Phase tiếp theo đề xuất
- **Phase 5D: Submit Payload Encryption Transition (V2)** hoặc **Phase 6: Tích hợp V2 vào Secure Exam Child**. Tùy vào yêu cầu hiện hành, hệ thống cần tiến hành nới rộng client side (Swing/JCEF) để nhận và parse V2 Package hiển thị lên giao diện thay thế `EXAM_START_REQUEST` legacy.
