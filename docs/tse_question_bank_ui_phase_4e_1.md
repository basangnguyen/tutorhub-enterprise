# Phase 4E-1: Teacher/Admin Question Bank & Create Question UI

## 1. Mục tiêu phase
Xây dựng giao diện (UI) ở phía Client (TutorHub Parent App) cho phép Giáo viên (TUTOR) hoặc Quản trị viên (ADMIN) thao tác quản trị Ngân hàng câu hỏi:
- Xem danh sách ngân hàng câu hỏi.
- Tạo mới ngân hàng câu hỏi.
- Xem danh sách câu hỏi trong một ngân hàng.
- Tạo mới câu hỏi (`SINGLE_CHOICE` và `TRUE_FALSE`) và lưu vào ngân hàng.

Phase này đóng vai trò UI frontend gọi các logic API đã hoàn thiện ở Phase 2 và Phase 3.

## 2. Vì sao chưa làm Exam Paper Builder UI
Giao diện gán câu hỏi vào đề thi (Exam Paper) và quản trị Exam Paper yêu cầu logic kéo-thả (Drag & Drop) hoặc selector phức tạp. Do đó, việc xây dựng quản trị Question Bank và Question cần được làm trước để đảm bảo dữ liệu đầu vào ổn định. Tránh dồn quá nhiều rủi ro UI vào một phase duy nhất. Việc xây dựng Exam Paper Builder sẽ được thực hiện ở phase tiếp theo (Phase 4E-2).

## 3. File đã đọc trước khi code
- `docs/tse_exam_package_preview_ui_phase_4d.md`
- `docs/tse_exam_package_preview_backend_phase_4c.md`
- `docs/tse_question_bank_backend_phase_2_3.md`
- `docs/secure_exam_tasks_v2.md`
- Kiểm tra mã nguồn giao diện `MainDashboard.java`, `ExamTab.java`.

## 4. UI đã thêm ở đâu
- Thêm mục **"Ngân hàng câu hỏi"** vào danh sách menu điều hướng bên trái (Sidebar) của `MainDashboard.java`.
- Tạo một tab mới `QuestionBankTab.java` để hiển thị danh sách các Ngân hàng câu hỏi dưới dạng `JTable`.
- Tạo popup dialog nội tuyến trong `QuestionBankTab` để tạo Ngân hàng câu hỏi.
- Tạo popup dialog `QuestionListDialog.java` để hiển thị câu hỏi thuộc về một ngân hàng và tạo câu hỏi mới.

## 5. Socket action đã dùng
- `QUESTION_BANK_LIST`: Lấy danh sách ngân hàng câu hỏi.
- `QUESTION_BANK_CREATE`: Gửi yêu cầu tạo ngân hàng mới.
- `QUESTION_LIST_BY_BANK`: Lấy danh sách câu hỏi theo ID ngân hàng.
- `QUESTION_CREATE`: Gửi yêu cầu tạo câu hỏi mới (kèm options/đáp án).
*Các action phản hồi:*
- `QUESTION_BANK_LIST_SUCCESS`, `QUESTION_BANK_CREATE_SUCCESS`
- `QUESTION_LIST_BY_BANK_SUCCESS`, `QUESTION_CREATE_SUCCESS`

## 6. Validation UI
- **Tạo Bank:** Tên ngân hàng không được để trống.
- **Tạo Question:** 
  - Nội dung câu hỏi không được rỗng.
  - Điểm phải là số và lớn hơn 0.
  - `SINGLE_CHOICE`: Phải có ít nhất 2 đáp án (option) không rỗng. Phải có đúng 1 đáp án được tích chọn "Đúng".
  - `TRUE_FALSE`: Bắt buộc chọn đúng/sai làm đáp án.

## 7. Security: không log đáp án đúng
- Trong `QuestionListDialog`, payload được gửi qua `NetworkManager.sendPacket(...)` và được gọi toJson() trực tiếp, tuyệt đối **KHÔNG** in log (`System.out.println` hay `Logger`) phần dữ liệu chứa key `isCorrect`. 
- Đảm bảo tính bảo mật nguyên vẹn của luồng đề thi.
- Giáo viên/Admin thao tác trên giao diện không làm rò rỉ key xuống màn hình thi (`EXAM_START_REQUEST`) của học sinh (nhờ logic backend đã chặn ở Phase 2,3).

## 8. Backward compatibility
- Các hàm cũ `EXAM_START_REQUEST`, `EXAM_SUBMIT` hay phần Rust lockdown không bị chỉnh sửa.
- Giao diện Admin mới được tách bạch hoàn toàn ra `QuestionBankTab` riêng lẻ, không ảnh hưởng `ExamTab` cũ hay các chức năng Home, Chat.

## 9. Test build
- `mvn clean install`: **PASS**
- `build_portable.ps1`: **PASS**

## 10. Manual UI test
- Mở app -> Chọn mục "Ngân hàng câu hỏi" trên Sidebar.
- Bảng danh sách ngân hàng xuất hiện.
- Nhấn "Tạo ngân hàng" -> Nhập tên "Toán Học" -> Lưu -> Bảng reload hiển thị "Toán Học".
- Chọn dòng "Toán Học" -> Nhấn "Xem câu hỏi" -> Bảng danh sách câu hỏi trống mở lên.
- Nhấn "Thêm câu hỏi" -> Điền nội dung, điểm, loại `SINGLE_CHOICE` -> Chọn 4 đáp án A B C D, đánh dấu B đúng -> Lưu.
- Danh sách câu hỏi reload hiển thị câu hỏi mới tạo.
- (Thử lỗi tạo câu rỗng, điểm = 0 -> Có alert báo lỗi chính xác).

## 11. Rủi ro còn lại
- Giáo viên chỉ có thể tạo, nhưng UI chưa có nút xóa/sửa (`DELETE/UPDATE`). Cần cân nhắc bổ sung ở giai đoạn hoàn thiện sản phẩm nếu Giáo viên phản ánh.
- Hiện không có cơ chế chặn Giáo viên này sửa Ngân hàng của Giáo viên khác trên backend, vì phase này chưa xét role sâu. 

## 12. Phase tiếp theo đề xuất
- **Phase 4E-2: Exam Paper Builder UI** (Giao diện lắp ráp/chọn lựa câu hỏi từ Ngân hàng vào Đề thi, và gán Đề thi vào Kỳ thi).
