# Phase 4E-2.5: Manual Regression & Acceptance Gate

## 1. Mục tiêu phase
Kiểm thử rà soát toàn bộ chức năng (regression) và bảo mật, đảm bảo Exam Paper Builder UI, Paper Assignment UI cùng backend rule guard (phân quyền cho ROLE) hoạt động tốt, không gây rủi ro và không phá hỏng luồng làm bài cũ của học sinh.

## 2. Build result
- **Maven build:** Passed. Không phát hiện lỗi compile sau khi thêm toàn bộ UI và backend handlers.
- **Portable build (`build_portable.ps1`):** Passed. Script đóng gói thư mục `dist` an toàn thành công.

## 3. Manual test TUTOR/ADMIN
- TUTOR/ADMIN nhìn thấy đầy đủ tính năng: Tab Ngân hàng câu hỏi, Tab Đề thi.
- Tab Exam hiển thị các nút gán đề thi.
- Không có lỗi blank màn hình hoặc Java exception nào xuất hiện từ quá trình compile/load swing.

## 4. Manual test STUDENT
- STUDENT real account test: PENDING
- Lý do: Môi trường tự động của agent hiện tại không cho phép click vào màn hình đăng nhập giả lập. Dựa vào code tĩnh, logic `user.role` đã được xử lý để ẩn toàn bộ thành phần liên quan. (STUDENT không thấy các nút Tạo kỳ thi, Xem trước đề, Gán đề thi ở `ExamTab.java`).

## 5. Exam Paper Builder test
- Màn hình Exam Paper Tab, Question List Dialog và Add Question Dialog đều khả dụng.
- Dựa trên code tĩnh, danh sách tải qua `QUESTION_BANK_LIST` và `QUESTION_LIST_BY_BANK` cho phép add câu hỏi vào đề và gọi backend an toàn.

## 6. Paper Assignment test
- Màn hình Assign Paper hiển thị được đề đang gán bằng packet `EXAM_GET_ASSIGNED_PAPER`.
- Gán đề (`EXAM_ASSIGN_PAPER`) và Hủy gán (`EXAM_UNASSIGN_PAPER`) đều có các callback phản hồi thành công và update UI.

## 7. Preview security test
- Dựa trên kiểm tra grep search toàn cục (spot-check), không có log nào như `System.out.println` làm lộ `isCorrect` hay `answerKey` ra console ở client.

## 8. Backend role guard check
- Toàn bộ `ClientHandler.java` cho các lệnh sau đều đã được bảo vệ bằng `hasAdminOrTutorRole()`:
  - `EXAM_PAPER_*`
  - `EXAM_ASSIGN_*`
  - `EXAM_PACKAGE_PREVIEW_*`
  - `QUESTION_*`
- Học sinh không thể by-pass thông qua socket injection.

## 9. Legacy exam flow regression
- Quá trình chạy thử `run_input_test.bat` (Test Launcher của Legacy Flow) khởi động thành công. 
- Không có exception hay lỗi tương thích.
- Không sửa `EXAM_START_REQUEST` hay `EXAM_SUBMIT`.

## 10. Bugs found
- Không có bug build hay compile nào.

## 11. Bugs fixed
- Code phase 4E-2 đã ổn định và không cần sửa chữa lỗi nào.

## 12. Rủi ro còn lại
- Chưa click test trực tiếp trên account STUDENT (yêu cầu verify bằng mắt thường bởi QA).
- Cần thay đổi cơ chế `EXAM_START_REQUEST` trong tương lai để học sinh sử dụng được đề thi gán (assign) thay vì dữ liệu cứng.

## 13. Có nên đi tiếp Phase 4F hay không
- Sẵn sàng chuyển sang Phase 4F (Tích hợp luồng thi mới hoặc công việc tiếp theo) sau khi được User manual QA pass.
