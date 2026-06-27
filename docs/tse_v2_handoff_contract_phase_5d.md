# Phase 5D: V2 Handoff Contract + Debug Handoff Artifact

## 1. Mục tiêu phase
Thiết kế và chuẩn bị hợp đồng truyền dữ liệu (Contract) từ Parent sang Secure Exam Child cho luồng thi V2. Khởi tạo một Service chung để xử lý và validate Handoff Bundle, đồng thời tích hợp tính năng xuất Debug Artifact cho Admin/Tutor xem trước gói dữ liệu V2 sẽ được truyền sang Child (không bao gồm dữ liệu nhạy cảm). 

## 2. Vì sao chưa tích hợp Child
Secure Exam Child sử dụng JCEF và Rust IPC, việc sửa đổi trực tiếp vào luồng render bài thi thật mà chưa chốt được định dạng contract có thể dẫn đến crash hoặc lộ đề. Do đó, Phase 5D đóng vai trò kiểm thử DTO và Security Filter ở phía Parent trước khi chính thức mở cầu nối (Bridge) sang Child.

## 3. V2 handoff bundle contract
Contract được chuẩn hóa thành 3 class:
- `V2ExamHandoffBundle`
- `V2ExamHandoffQuestion`
- `V2ExamHandoffOption`

Các trường được truyền bao gồm `examId`, `paperId`, `attemptId`, `sessionToken`, `packageHash`, `questionCount`, `totalScore`, và danh sách `questions` đã được loại bỏ các trường đáp án đúng.

## 4. Debug artifact path
Artifact debug được lưu an toàn tại:
`[User Directory]/debug/v2_handoff_preview.json`
`[User Directory]/debug/v2_handoff_preview.sha256` (Chữ ký băm toàn vẹn của gói Handoff).

## 5. Validation rule
`V2ExamHandoffService.validateHandoffBundle` thực hiện:
- Kiểm tra `flow` phải là `PAPER_START_V2`.
- `examId`, `paperId` phải hợp lệ (>0).
- Khi `debugMode=false`, yêu cầu phải có `attemptId` và `sessionToken`.
- `packageHash`, `questionCount`, và `questions` không được rỗng.
- Xác minh KHÔNG chứa chuỗi json `"isCorrect"`, `"answerKey"`, `"correctOption"`, hoặc `"grading_config"`.

## 6. Security: no answer leak / no raw token log
- **Answer Leak**: DTO parsing bằng Gson sẽ tự động lược bỏ (strip) các biến không được định nghĩa trong class model (như `isCorrect`). Để an toàn tuyệt đối, Service vẫn quét lại toàn bộ chuỗi JSON trả ra để đảm bảo không có từ khóa nhạy cảm. Unit test `testSecurityValidation` đã kiểm tra và pass điều kiện này.
- **Raw Token Log**: Không có lệnh `System.out.println` nào in token. Việc tạo Debug Artifact cũng chỉ được phép khi chạy với Admin/Tutor test mode, file này lưu trữ local tại thư mục `debug`. Không có session_token thực sự khi `debugMode=true` (giá trị là null/rỗng).

## 7. UI debug integration
Trong `ExamStartV2DebugDialog.java`, đã tích hợp:
- Khi Dialog hiện lên, gọi `V2ExamHandoffService.writeDebugHandoffArtifact(bundle)` để lưu preview JSON.
- Thêm giao diện hiển thị đường dẫn Artifact màu xanh lục ở góc trái bên dưới dialog, trước nút `Đóng`.
- Code chỉ bắt lỗi, in ra stderr (không in token) nếu có ngoại lệ.

## 8. Backward compatibility
- Luồng `EXAM_START_REQUEST` legacy hoàn toàn không thay đổi.
- Luồng `EXAM_SUBMIT` legacy không thay đổi.
- Child renderer hiện chưa được gọi bằng luồng V2 nên Student không bị ảnh hưởng.
- Test Start V2 UI hoạt động chỉ cho Admin/Tutor, và sinh file `debug` chứ không tạo `submit_payload.enc`.

## 9. Build result
- `mvn clean install` PASS (32 tests run, 0 failures, 100% successful).
- `build_portable.ps1` PASS, output artifact TutorHubSecureExam thành công.

## 10. Legacy regression result
`run_input_test.bat` hoạt động hoàn hảo, ứng dụng load đúng giao diện Login Parent Lab, không ảnh hưởng đến luồng Legacy.

## 11. Manual/debug test result
Unit Test `V2ExamHandoffServiceTest` đã thay thế hoàn toàn cho manual debug test với các assert sau:
- Xây dựng Handoff Bundle hợp lệ từ Payload giả lập thành công.
- `debugMode=false` (Production mode) bắt buộc phải có attemptId và sessionToken.
- Chặn đứng hoàn toàn gói tin chứa từ khóa `isCorrect`.
- Artifact file JSON và SHA256 được tạo và lưu trữ thành công trên disk.

## 12. Rủi ro còn lại
- File debug JSON hiện đang lưu plaintext vào đĩa nội bộ (Local Disk). Dù không chứa đáp án nhưng vẫn chứa cấu trúc đề. Việc này an toàn đối với Admin/Tutor, nhưng nếu áp dụng thực tế ở máy học sinh thì cần mã hóa đường truyền IPC, đó là nhiệm vụ của Phase 5D.5 hoặc Phase 6.

## 13. Phase tiếp theo đề xuất
- **Phase 5D.5**: Mã hóa Handoff Bundle (Payload Encryption Transition). Áp dụng thuật toán AES hiện có cho gói V2 này để truyền sang Child an toàn hơn là gửi plaintext IPC.
- Hoặc có thể tiến thẳng lên **Phase 6**: Tích hợp Backend Prototype vào luồng Child thật sự.
