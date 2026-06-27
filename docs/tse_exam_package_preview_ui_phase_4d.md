# Phase 4D: Admin/Teacher Paper Preview UI

## 1. Mục tiêu phase
Xây dựng giao diện (UI) ở phía Client (TutorHub Parent App) cho phép Giáo viên/Admin xem trước đề thi dựa trên package DTO đã tạo ở Phase 4C. Giao diện này phục vụ mục đích kiểm tra nội dung và số lượng câu hỏi trước khi học sinh bắt đầu làm bài.

## 2. Vì sao đây chỉ là Admin/Teacher Preview, chưa phải Exam Taking UI
Giao diện này chỉ đơn thuần là `JDialog` hiển thị text tĩnh để kiểm duyệt. Nó không chứa các thành phần điều hướng phức tạp, chức năng tương tác làm bài (chọn đáp án, chuyển câu, nộp bài), màn hình khóa (lockdown), và nhất là KHÔNG sử dụng `EXAM_START_REQUEST` để fetch dữ liệu mà dùng API riêng (`EXAM_PACKAGE_PREVIEW_*`). Việc tách biệt này đảm bảo an toàn và không gây ảnh hưởng (regression) đến quá trình học sinh thi thật.

## 3. File đã đọc trước khi code
- `docs/tse_exam_package_preview_backend_phase_4c.md`
- `docs/tse_paper_to_exam_assignment_backend_phase_4b.md`
- `docs/tse_exam_paper_backend_phase_3_5_and_4a.md`
- `docs/tse_question_bank_backend_phase_2_3.md`
- `docs/tse_parent_bridge_phase9a5_regression_test.md`
- `docs/secure_exam_tasks_v2.md`

## 4. UI đã thêm ở đâu
- Thêm nút "Xem trước đề" vào Toolbar/ActionPanel trong `ExamTab.java` (chỉ hiển thị khi `role` là `TUTOR` hoặc `ADMIN`). Nút này yêu cầu chọn một kỳ thi trong danh sách `examTable` trước khi nhấn.
- Tạo màn hình popup non-modal `ExamPreviewDialog.java` hiển thị tiêu đề, thông tin kỳ thi, tổng điểm, và danh sách câu hỏi kèm `JScrollPane`.

## 5. Socket action đã dùng
- `EXAM_PACKAGE_PREVIEW_BY_EXAM`: Gửi request JSON với `examId`.
- Catch action `EXAM_PACKAGE_PREVIEW_SUCCESS` trong `MainDashboard.java` -> Parse JSON payload và mở màn hình `ExamPreviewDialog.java`.
- Catch action `EXAM_PACKAGE_PREVIEW_FAILED` trong `MainDashboard.java` -> Hiển thị cảnh báo lỗi tương ứng.

## 6. Error handling
Khi bắt được `EXAM_PACKAGE_PREVIEW_FAILED` từ server, các mã lỗi string được map sang thông báo tiếng Việt cho thân thiện:
- `"EXAM_HAS_NO_ASSIGNED_PAPER"` -> "Kỳ thi này chưa được gán đề thi."
- `"PAPER_HAS_NO_QUESTIONS"` -> "Đề thi chưa có câu hỏi."
- `"PAPER_NOT_FOUND"` -> "Không tìm thấy đề thi."
- Lỗi khác -> "Không thể tải preview. Vui lòng thử lại."

## 7. Security: không hiển thị isCorrect/answer key
DTO `ExamPackagePreview` trả về từ server không chứa bất kỳ cờ nào liên quan đến đáp án đúng (`isCorrect`). UI cũng hoàn toàn không render đáp án đúng hay explanation, đảm bảo an toàn tuyệt đối.

## 8. Backward compatibility
Tất cả các logic gọi API/Socket cũ đều được bảo toàn nguyên vẹn. Không có file core nào bị ghi đè hay xóa action. 
Cấu trúc gói tin socket hoàn toàn tuân thủ cách viết của `ClientHandler.java` và `MainDashboard.java`. Màn hình làm bài cũ, Parent Bridge, và Rust Lockdown không hề bị chỉnh sửa, đảm bảo Phase 4D hoàn toàn trong suốt với học sinh đang thi.

## 9. Test build
- `mvn clean install`: **PASS**
- `build_portable.ps1`: **PASS**

## 10. Manual UI test
- Chọn một kỳ thi chưa gán đề: Bấm "Xem trước đề" -> Thông báo "Kỳ thi này chưa được gán đề thi."
- Chọn một kỳ thi đã gán đề: Bấm "Xem trước đề" -> Bảng Preview mở lên an toàn không crash, cuộn trang tốt.
- UI đáp ứng layout gọn gàng, không che khuất màn hình chính.

## 11. Rủi ro còn lại
- Hiện UI chỉ hiển thị văn bản tĩnh cơ bản (Text-based). Tuy nhiên, có thể một số câu hỏi có nội dung HTML hoặc công thức toán học từ Editor, màn hình JTextArea này có thể không render HTML chuẩn. Sẽ cần nâng cấp nếu Giáo viên phản ánh lỗi hiển thị rich-text.
- Cần một UI Admin để tạo `ExamPaper` và gán câu hỏi (CRUD Question, Paper). Hiện tại Backend đã có, nhưng Frontend quản lý thì chưa.

## 12. Phase tiếp theo đề xuất
- **Phase 4E: Exam Paper Management UI** (Giao diện Ngân hàng câu hỏi & Lắp ráp đề thi) hoặc nâng cấp UI Exam Taking cho học sinh dựa trên cấu trúc JCEF.
