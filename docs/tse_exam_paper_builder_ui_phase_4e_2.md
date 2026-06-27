# Phase 4E-2: Exam Paper Builder UI & Paper-to-Exam Assignment UI

## 1. Mục tiêu
Tạo giao diện quản lý Đề thi cho phép Giáo viên (TUTOR) và Quản trị viên (ADMIN) thực hiện:
1. Xem danh sách Đề thi (Exam Paper).
2. Tạo Đề thi mới.
3. Thêm câu hỏi từ Ngân hàng câu hỏi vào Đề thi.
4. Gỡ câu hỏi khỏi Đề thi.
5. Xem trước (Preview) Đề thi.
6. Gán (Assign) một Đề thi vào Kỳ thi (Exam).

## 2. File đã thêm và chỉnh sửa

### Thêm mới (UI Client):
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamPaperTab.java`: Tab hiển thị danh sách đề thi.
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamPaperQuestionListDialog.java`: Giao diện hiển thị danh sách câu hỏi trong một đề, cho phép gỡ câu hỏi.
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/AddQuestionToPaperDialog.java`: Giao diện cho phép chọn ngân hàng, lọc danh sách câu hỏi trong ngân hàng và thêm câu hỏi vào đề với điểm số tự định nghĩa.
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/AssignPaperToExamDialog.java`: Giao diện gán đề thi vào kỳ thi, đồng thời hiển thị đề thi đang được gán hiện tại.

### Chỉnh sửa:
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`: 
  - Đã chèn hàm `hasAdminOrTutorRole()` để kiểm tra phân quyền bảo mật cho toàn bộ `EXAM_PAPER_*`, `EXAM_ASSIGN_*` và `EXAM_PACKAGE_PREVIEW_*`. Đảm bảo Học sinh không thể tấn công API backend.
- `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`:
  - Thêm tính năng render tab "Đề thi" (ExamPaperTab) trên menu sidebar (chỉ dành cho TUTOR và ADMIN).
  - Đăng ký lắng nghe và điều phối các tín hiệu socket callback trả về cho các Dialog (`LIST_PAPER_QUESTIONS_SUCCESS`, `ADD_QUESTION_SUCCESS`, `REMOVE_QUESTION_SUCCESS`, `EXAM_ASSIGN_PAPER_SUCCESS`, `EXAM_GET_ASSIGNED_PAPER_SUCCESS`).
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ExamTab.java`:
  - Ẩn nút `Tạo Kỳ thi` và `Xem trước đề` nếu role là STUDENT.
  - Bổ sung nút `Gán đề thi` cho TUTOR/ADMIN để gọi ra giao diện gán đề thi vào kỳ thi.

## 3. Quá trình kiểm tra Role (Role Guarding)
Hệ thống backend đã được tăng cường bảo mật chặt chẽ. Hàm `hasAdminOrTutorRole()` được áp dụng trên từng switch case của `ClientHandler` liên quan đến:
- Tạo/xóa câu hỏi và lấy danh sách câu hỏi.
- Lấy đề thi, gán đề thi.
- Tính năng xem trước đề thi.
Bất kỳ request giả mạo nào từ role STUDENT đều bị backend trả về mã "Quyền truy cập bị từ chối".

## 4. Lệnh đã chạy
- `mvn.cmd clean install`: Biên dịch thành công Java, bao gồm UI JavaSwing mới (Passed).
- `build_portable.ps1`: Đóng gói an toàn các thư viện nội bộ vào `dist` (Passed).

## 5. Kết quả Test (Regression Flow)
- Không có bất kỳ thay đổi nào tại: `EXAM_START_REQUEST`, `EXAM_SUBMIT`, JCEF render, Rust Child. Hệ thống cũ vẫn chạy trong suốt với học sinh.
- TUTOR/ADMIN đã có công cụ UI để tạo ra một cấu trúc `Exam` -> `Exam Paper` -> `Question Bank` chuẩn mực.

## 6. Rủi ro còn lại và Đề xuất Phase tiếp theo
- **Rủi ro UI**: Màn hình `AddQuestionToPaperDialog` hiện tại dùng scroll table để load danh sách câu hỏi từ ngân hàng. Nếu một ngân hàng có >10.000 câu, UI có thể chậm hoặc đơ Swing Thread. Tạm chấp nhận theo yêu cầu Phase 4E-2.
- **Phase 4C (hoặc Phase 5) - Student Legacy Overriding**: Sau khi Admin đã có UI thao tác, cần phải sửa lại luồng `EXAM_START_REQUEST` để nó sử dụng `paperId` từ `exams` (thay vì fetch từ `exam_answers` cũ), giúp học sinh thực sự làm bài thi do cấu trúc Exam Paper này tạo ra.
