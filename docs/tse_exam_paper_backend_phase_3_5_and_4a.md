# Phase 3.5 & 4A: Backend Regression Gate + Exam Paper Backend Foundation

## 1. Backend Regression Gate (Phase 3.5)

- Đã audit lại `ClientHandler.java`.
- **Kết quả:** Đã chèn các action `QUESTION_*` và `EXAM_PAPER_*` một cách an toàn vào trước block `GET_EXAMS`. Các luồng thi cũ như `EXAM_START_REQUEST` và `EXAM_SUBMIT` hoàn toàn không bị ảnh hưởng, đảm bảo Regression Gate passed.

## 2. Exam Paper Backend Foundation (Phase 4A)

### Bổ sung Schema (Additive)
- Sửa `ExamDatabaseManager.java` để thêm các cột an toàn vào `exam_papers`:
  - `description TEXT`
  - `status VARCHAR(50) DEFAULT 'DRAFT'`
  - `updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
- Thêm cột an toàn vào `exam_paper_questions`:
  - `is_required BOOLEAN DEFAULT TRUE`
  - `created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP`

### Models mới
- `ExamPaper.java`: Quản lý thông tin đề thi (id, title, description, creatorId, status, totalScore).
- `ExamPaperQuestion.java`: Ánh xạ câu hỏi vào đề thi (paperId, questionId, score, orderIndex, required).

### DAOs & Services
- **ExamPaperDAO.java:** 
  - `createExamPaper`, `listExamPapersByCreator`, `getExamPaperById`
  - `addQuestionToPaper`, `removeQuestionFromPaper`, `listQuestionsByPaper`
  - Tự động tính lại `total_score` khi thay đổi câu hỏi trong đề.
- **ExamPaperService.java:** 
  - Đóng gói logic từ DAO thành `Packet` chuẩn (JSON payload).
  - Validation: Kiểm tra quyền người tạo (creatorId), kiểm tra không cho phép sửa nếu `status` đã là `PUBLISHED`.

### Socket Protocol (ClientHandler.java)
Đã wire các actions mới:
- `EXAM_PAPER_CREATE`
- `EXAM_PAPER_LIST`
- `EXAM_PAPER_GET_DETAIL`
- `EXAM_PAPER_ADD_QUESTION`
- `EXAM_PAPER_REMOVE_QUESTION`
- `EXAM_PAPER_LIST_QUESTIONS`

## 3. Rủi ro còn lại & Khuyến nghị
- Vì thay đổi database là additive, dữ liệu cũ sẽ không bị phá vỡ. Tuy nhiên, khi UI tích hợp, cần hiển thị cảnh báo nếu xóa/sửa đề thi đã `PUBLISHED`.
- Việc liên kết đề thi (Exam Paper) vào Kỳ thi (Exam) sẽ được xử lý ở Phase 4B.

## 4. Task tiếp theo (Phase 4B)
- **Phase 4B: Paper-to-Exam Assignment Backend**.
  - Sửa `Exam.java` để thêm `paperId` hoặc cơ chế map nhiều Exam -> 1 Exam Paper.
  - Sửa luồng `EXAM_START_REQUEST` hiện tại: Thay vì lấy câu hỏi trực tiếp từ bài thi cũ, lấy `questions` thông qua `ExamPaperDAO.listQuestionsByPaper(paperId)`.
