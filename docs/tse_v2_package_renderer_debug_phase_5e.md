# Phase 5E: V2 Package Renderer Prototype - Admin Debug Only

## 1. Mục tiêu phase
Tạo và hiển thị giao diện xem trước (prototype renderer) cho gói đề thi V2 (`V2ExamHandoffBundle`) ngay trên Admin UI. Phase này giúp Tutor/Admin có thể kiểm tra cấu trúc gói đề V2 được sinh ra trước khi gói này được nhúng thật sự vào Secure Exam Child.

## 2. Vì sao chưa tích hợp Child thật
- Secure Exam Child (JCEF) hiện tại vẫn đang nhận cấu trúc JSON legacy.
- Việc thay đổi Child đòi hỏi quy trình mã hóa và IPC bridge phải được cập nhật ở Phase 6.
- Do đó, để an toàn và từng bước xác nhận độ ổn định, bộ Renderer này sẽ được chạy thử bằng Java Swing chuẩn ngay trên Parent Process, dưới dạng một tính năng Debug.

## 3. Renderer contract
Tạo class `V2ExamPackageRenderer` chuyên phục vụ hiển thị V2 Bundle:
- `renderHtml`: Trả về HTML chứa thông tin kỳ thi, tổng điểm, danh sách câu hỏi và danh sách đáp án.
- Dữ liệu hiển thị: Không bao gồm bất cứ thuộc tính chấm điểm nào (như `isCorrect`, `answerKey`).

## 4. UI debug integration
- Nâng cấp `ExamStartV2DebugDialog` để sử dụng `JTabbedPane`.
- **Tab 1 ("Render Preview")**: Hiển thị WebView giả lập bằng `JEditorPane` chứa mã HTML từ Renderer.
- **Tab 2 ("Raw Components")**: Vẫn giữ nguyên danh sách thẻ câu hỏi theo dạng Swing thô như trước.

## 5. Security validation
Renderer được thiết kế với cơ chế chặn hiển thị bí mật (Guard Rails):
- Ném exception `SECURITY VIOLATION` ngay lập tức nếu chuỗi kết quả (HTML/Text) có chứa các từ khóa: `isCorrect`, `answerKey`, `correctOption`, `grading_config`.
- Ném exception nếu chuỗi kết quả vô tình bị rò rỉ `sessionToken` raw của người dùng hoặc có chứa cụm từ liên quan tới `password`.

## 6. Test result
- Thêm `V2ExamPackageRendererTest` với 4 scenarios:
  1. Render thành công một bundle hợp lệ.
  2. Bắt lỗi khi bundle trống.
  3. Bắt lỗi khi cố tình chèn chuỗi raw token vào nội dung.
  4. Bắt lỗi khi cố tình chèn chuỗi `isCorrect` vào nội dung.
- Cả 4 test case đều PASS. Tổng số Unit Tests đạt 36 tests pass.

## 7. Legacy regression result
Hệ thống thi cũ (Production) được test thông qua `run_input_test.bat --exam-id 3` và vẫn hoạt động hoàn hảo, không có bất kỳ sự gián đoạn nào (không crash). Không file legacy nào bị chỉnh sửa sai lệch.

## 8. Rủi ro còn lại
- Prototype Renderer này chỉ sử dụng Swing HTML `JEditorPane` chứ không phải JCEF/WebEngine. Các định dạng phức tạp như ảnh Base64, bảng biểu có thể hiển thị không đẹp như trên Child thật. Điều này có thể chấp nhận được vì đây chỉ là công cụ Debug của Admin.
- Sinh viên (Học sinh) tuyệt đối không nhìn thấy nút/preview này.

## 9. Phase tiếp theo đề xuất
Tiếp tục bước sang **Phase 5F: V2 Handoff Payload Encryption**. Ở phase này, toàn bộ gói `V2ExamHandoffBundle` sẽ được mã hóa bằng thuật toán đối xứng (hoặc bất đối xứng) trước khi truyền qua named pipe cho Rust, để chuẩn bị cho màn tích hợp Child (Phase 6).
