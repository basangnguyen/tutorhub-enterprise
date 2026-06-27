# TSE V2 Server-side Submit Record Prototype (Phase 7G)

## Mục tiêu (Goal)
Phase 7G thiết lập cơ sở dữ liệu và API xử lý ban đầu cho cơ chế nộp bài mới của TSE V2.
Mục tiêu là lưu trữ an toàn payload submit cuối cùng (record) do client gửi lên, tách biệt hoàn toàn với hệ thống `EXAM_SUBMIT` cũ. Không thực hiện chấm bài (grading) hay thay đổi trạng thái attempt trong phase này.

## Các thay đổi chính

### 1. Database Schema (`v2_submit_records`)
Bảng mới được tạo thông qua `V2SubmitRecordDAO` (tương tự như Phase 7E đã làm với dry-run):
- `id` (INT AUTO_INCREMENT PRIMARY KEY)
- `attempt_id` (VARCHAR)
- `exam_id` (INT)
- `paper_id` (INT)
- `payload_hash` (VARCHAR): Đảm bảo tính toàn vẹn và chống trùng lặp.
- `payload_json` (TEXT): Chứa toàn bộ cấu trúc V2 submit payload (không chứa đáp án, key, token).
- `submit_status` (VARCHAR): Trạng thái (hiện tại gán `RECEIVED_DEBUG`).
- `source` (VARCHAR): Nguồn submit (`V2_DEBUG`).

### 2. Service Layer (`V2SubmitRecordService`)
- Dựa trên kết quả từ `V2SubmitDryRunValidationService`, kiểm tra nghiêm ngặt an toàn của payload:
  - Phải đúng bài thi (`examId`, `paperId`).
  - Không được phép chứa bất kỳ trường nhạy cảm nào (`answerKey`, `isCorrect`, v.v.).
- Nếu validation PASS, ghi bản ghi vào bảng `v2_submit_records`.
- Tích hợp kiểm tra feature flag `tse.v2.submitRecord.enabled`.

### 3. Server Handler (`ClientHandler.java`)
- Bổ sung handler cho action `EXAM_SUBMIT_V2_RECORD_CREATE`.
- Chặn quyền đối với user thông thường (trừ khi dùng forceStudent flag) để đảm bảo không rò rỉ sớm tính năng cho production.
- Trả về `EXAM_SUBMIT_V2_RECORD_CREATE_OK` nếu lưu trữ thành công, kèm thông tin chi tiết id, hash.

## Unit Tests
- Xây dựng `V2SubmitRecordServiceTest` với mock cho Database và Validation Service.
- Chuyển đổi các bài test sử dụng JUnit 5 để đồng bộ công nghệ hiện tại của TutorHub.
- Verify tính toàn vẹn dữ liệu: đảm bảo không có logic dò rỉ thông tin trong response packet.

## Kết quả
- Toàn bộ 176 tests đã pass.
- Build Maven thành công.
- Build Portable thành công.
- Sẵn sàng chuyển tiếp sang Phase 7H (Final Submit Verification Workflow - No Auto-Grading).
