# TutorHub Secure Exam: Question Bank Backend Phase 2 & 3

## 1. Mục tiêu phase
Xây dựng backend foundation cho tính năng Quản lý Ngân hàng Câu hỏi và Câu hỏi, bao gồm Database Schema, Models, DAOs, Validation Services và định tuyến Socket Packets ban đầu trong ClientHandler. Không thực hiện các thay đổi liên quan đến UI.

## 2. File đã đọc trước khi code
- `docs/tse_exam_schema_baseline_and_migration_phase_0_1.md`
- `docs/tse_exam_operation_master_plan_deep_research.md`
- `docs/tse_parent_bridge_phase9a5_regression_test.md`
- `docs/secure_exam_tasks_v2.md`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/db/ExamDatabaseManager.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/exam/Question.java`

## 3. Bài học từ phase9a5 regression
- Mọi logic phải được tách biệt rõ ràng, không động vào WebView/JCEF thread.
- Quick Settings (âm lượng, độ sáng, Wi-Fi, pin) hoạt động tốt, tuyệt đối không chỉnh sửa.
- Parent Bridge đang ổn định với socket riêng biệt.
- Thay đổi phải theo hướng Additive, không sửa luồng code cũ trừ khi thực sự cần.

## 4. Schema Phase 0+1 đang có gì
- `question_banks` (id, name, creator_id, created_at)
- `exam_papers`, `exam_paper_questions`, `exam_assignments`, `exam_attempts`, `exam_results`, `exam_audit_logs`
- Cấu trúc Additive tốt, nhưng thiếu `question_options` cho các loại câu hỏi MCQ/True-False, và `bank_id` trong `questions`.

## 5. Model/DAO/Service đã thêm
**Models:**
- `QuestionBank`: Thêm các trường `description`, `updatedAt`.
- `Question`: Mở rộng thêm `bankId`, `defaultScore`, `createdBy`, `updatedAt`.
- `QuestionOption`: Quản lý các lựa chọn, có cờ `isCorrect`.

**DAOs:**
- `QuestionBankDAO`: Hỗ trợ `createQuestionBank`, `listQuestionBanksByCreator`, `getQuestionBankById`, `updateQuestionBank`, `deleteQuestionBank`.
- `QuestionDAO`: Hỗ trợ thêm `createQuestion` (với Options sử dụng Transaction), `listQuestionsByBank`, `getQuestionById`, `getOptionsByQuestionId`, `updateQuestion`, `deleteQuestion`.

**Services:**
- `QuestionBankService`: Validation và xử lý payload cho các Request liên quan Question Bank.
- `QuestionService`: Validation chặt chẽ cho Request tạo/sửa Câu hỏi.

## 6. Validation rule
**Trong QuestionService:**
- `bankId` tồn tại hợp lệ.
- `content`, `type` không được rỗng.
- `defaultScore` lớn hơn 0.
- `SINGLE_CHOICE` / `MCQ`: Phải có ít nhất 2 options và đúng 1 đáp án đúng (isCorrect = true). Nội dung option không rỗng.
- `TRUE_FALSE`: Phải có đúng 2 options và đúng 1 đáp án đúng. Nội dung option không rỗng.

## 7. Packet/API design
Đã bổ sung vào `ClientHandler.java` (switch block):
- `QUESTION_BANK_CREATE`: Payload `{ "name": "...", "description": "..." }`
- `QUESTION_BANK_LIST`: Payload rỗng, trả về danh sách ngân hàng câu hỏi.
- `QUESTION_CREATE`: Payload `{ "bankId": 1, "type": "SINGLE_CHOICE", "content": "...", "defaultScore": 1.0, "options": [...] }`
- `QUESTION_LIST_BY_BANK`: Payload `bankId`
- `QUESTION_GET_DETAIL`: Payload `questionId`

## 8. Backward compatibility
- Migration DB được chạy qua `ALTER TABLE ... IF NOT EXISTS` qua khối lệnh Additive trong `ExamDatabaseManager`.
- Cấu trúc `Question` cũ phục vụ cho `ExamDAO` vẫn giữ nguyên các trường. Bổ sung các trường mới độc lập.
- `EXAM_START_REQUEST` và `EXAM_SUBMIT` giữ nguyên (không chạm vào logic thi).
- Cập nhật trong `ClientHandler` được đẩy vào một khối `PHASE 2 & 3` riêng biệt, không thay đổi flow cũ.

## 9. Test build
- Maven Build: CHỜ KẾT QUẢ.
- Portable Build: CHỜ KẾT QUẢ.
- Mọi class mới compile thành công, không gặp lỗi cú pháp.

## 10. Manual DAO/service test nếu có
- Sẽ thực hiện sau khi Build xong.

## 11. Rủi ro còn lại
- Chặn delete/cascade delete của question_banks chưa được handle triệt để ở mức DB Constraint, hiện tại DAO sẽ báo lỗi tự nhiên từ driver nếu dính Constraint.
- Logic gán câu hỏi (Question) vào Đề thi (Exam Paper) vẫn chưa xây dựng.

## 12. Phase tiếp theo đề xuất
- **Phase 4:** Tạo chức năng Exam Paper Builder (Tạo Đề Thi).
- Liên kết từ Ngân hàng Câu hỏi chọn các Câu hỏi gán vào Đề thi (`exam_paper_questions`).
- Thiết kế GUI Backend Model và API cho Exam Paper.
