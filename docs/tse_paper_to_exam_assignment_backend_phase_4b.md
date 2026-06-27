# Phase 4B: Paper-to-Exam Assignment Backend

## 1. Mục tiêu
Xây dựng backend foundation cho phép gán (assign) một Đề thi (Exam Paper) vào một Kỳ thi (Exam).

## 2. File đã tạo và chỉnh sửa
- `src/main/java/com/mycompany/tutorhub_enterprise/server/db/ExamDatabaseManager.java`:
  - Thêm cơ chế migration tự động (additive): Thêm cột `paper_id INT` vào bảng `exams`.
- `src/main/java/com/mycompany/tutorhub_enterprise/models/exam/Exam.java`:
  - Thêm trường `paperId`.
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/ExamDAO.java`:
  - Thêm ánh xạ `paper_id` khi đọc ResultSet.
  - Thêm method `assignPaperToExam(int examId, int paperId)`.
  - Thêm method `unassignPaperFromExam(int examId)`.
  - Thêm method `getAssignedPaperId(int examId)`.
  - Thêm method `getExamWithAssignedPaper(int examId)`.
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/ExamAssignmentService.java` (Tạo mới):
  - Kiểm tra quyền sở hữu (creatorId).
  - Validation: Chỉ cho phép thao tác assign/unassign khi kỳ thi ở trạng thái `DRAFT`.
  - Không cho phép gán đề thi đã `ARCHIVED`.
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`:
  - Bổ sung 3 action cho giao thức kết nối Socket:
    - `EXAM_ASSIGN_PAPER`: Client gửi request gán đề thi.
    - `EXAM_UNASSIGN_PAPER`: Client gửi request hủy gán đề thi.
    - `EXAM_GET_ASSIGNED_PAPER`: Client gửi request lấy thông tin đề thi đang gán cho kỳ thi hiện tại.

## 3. Lệnh đã chạy
- `mvn.cmd clean install`: Kiểm tra quá trình build sau khi thêm schema mới và mapping. Đảm bảo Java backend code được biên dịch không lỗi (Passed).
- `powershell -ExecutionPolicy Bypass -File .\build_portable.ps1`: Build version portable đảm bảo có thể đóng gói ra folder `dist` thành công (Passed).

## 4. Kết quả test
- Backend build success.
- Cấu trúc DB additive không gây vỡ DB cũ.
- Socket Action đã được đăng ký trên server.
- Các module khác như Rust, Taskbar, JCEF Exam UI không bị chạm đến.

## 5. Rủi ro còn lại
- Hiện tại bảng `questions` cũ (kèm bảng `exam_answers`) vẫn còn tồn tại. Khi chuyển hoàn toàn sang Exam Paper, cần điều chỉnh `EXAM_START_REQUEST` để fetch câu hỏi qua `ExamPaperDAO` thay vì `ExamDAO.getQuestionsByExam` cũ.
- Chưa có UI bên JavaFX Client để quản lý thao tác gán đề thi.

## 6. Task tiếp theo đề xuất
- **Phase 4C: API Routing & Legacy Override**: Chỉnh sửa luồng lấy đề thi hiện tại của Học sinh (`EXAM_START_REQUEST`) để sử dụng cấu trúc `Exam Paper` mới (đọc `paperId` từ `exams` và fetch `exam_paper_questions`). Sửa đổi `submit_payload` nếu cần thiết để tương thích với UUID của questions mới.
