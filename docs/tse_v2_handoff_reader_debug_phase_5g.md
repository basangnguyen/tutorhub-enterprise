# Phase 5G: V2 Child Handoff Reader Prototype - Debug Only

## 1. Mục tiêu phase
- Xây dựng prototype (bản thử nghiệm) phía Secure Exam Child để có thể đọc được file `v2_handoff_runtime.enc` và `v2_handoff_runtime.meta.json` do Parent sinh ra.
- Thực hiện xác minh tính nguyên vẹn (SHA256 checksum) và tính bảo mật của dữ liệu (chặn `isCorrect`, `answerKey`).
- Giải mã và parse chuỗi JSON thành `V2ExamHandoffBundle` object hợp lệ để chuẩn bị cho giai đoạn dựng UI phía Child.

## 2. Vì sao chưa tích hợp Child thật
- Secure Exam Child thực tế yêu cầu AES Key từ Parent để có thể decrypt.
- Hiện tại Parent mới chỉ tạo Test Key ngẫu nhiên và hiển thị cho ADMIN trong Debug Dialog, chứ chưa hề giao key này cho Child (ví dụ thông qua pipe IPC hoặc command line secure params).
- Do chưa có cơ chế truyền key giữa process này, phase này chỉ xây dựng **module tiện ích đọc file (Reader)**, thực hiện test cục bộ (Unit Test), hoàn toàn không mở Secure Exam Child bằng dữ liệu của luồng V2.

## 3. Runtime encrypted handoff reader design
- Sử dụng class `V2RuntimeHandoffReader` với các phương thức tĩnh độc lập: `readMeta`, `verifyEncryptedFileHash`, `decryptRuntimeHandoff`, `parseBundle` và `validateBundleForChildPrototype`.
- Thiết kế Stateless, không phụ thuộc vào trạng thái hệ thống, chỉ phụ thuộc vào file và secret key đầu vào.

## 4. Metadata verification
- Class tạo thêm: `V2RuntimeHandoffMeta` DTO.
- Phương thức `verifyEncryptedFileHash` sử dụng `MessageDigest` (SHA-256) đọc toàn bộ nội dung byte của `v2_handoff_runtime.enc`, tính mã băm và so sánh với trường `encryptedFileSha256` trong meta file. Sai lệch ném ra lỗi `ERROR_HASH_MISMATCH`.

## 5. Decrypt/parse flow
- Giải mã: `CryptoUtils.decryptWrapper` giải mã mảng dữ liệu JSON trả về Plaintext String.
- Parse: Thay vì Gson parse trực tiếp (do `sessionToken` là `transient` nên bị null), flow sử dụng `JsonParser` để bóc tách lấy JsonObject, copy thủ công `sessionToken` ra, sau đó parse các thuộc tính còn lại, rồi gán `sessionToken` vào model Bundle đã được tạo.
- Validate: Nếu không phải Debug Mode mà thiếu `attemptId` thì reject.

## 6. Key handling limitation
- Do giới hạn chưa có IPC truyền khóa bảo mật, tất cả key sử dụng trong bài kiểm tra hiện được sinh trong bộ nhớ (in-memory) bằng hàm `CryptoUtils.generateAESKey()` của môi trường test.
- Production nghiêm cấm hardcode key và log key.
- Việc truyền khóa Parent -> Child sẽ được bàn ở thiết kế trong các phase kế tiếp.

## 7. Security validation
- Sau khi Decrypt, chuỗi JSON được quét bằng `.contains()` với các từ khóa: `isCorrect`, `answerKey`, `correctOption`, `grading_config`, `password`, `passwordHash`.
- Bất kỳ chuỗi JSON nào vi phạm sẽ ném `ERROR_SECURITY_VIOLATION_ANSWER_FIELDS`.
- Không có bất kỳ plaintext JSON nào hay sessionToken nguyên bản nào được log ra console hoặc ghi ra file ở thư mục gốc.

## 8. Unit test result
- Khởi chạy test case `V2RuntimeHandoffReaderTest`: Pass (5/5 cases thành công, 1 issue ban đầu parse bị null sessionToken đã được xử lý bằng JsonObject wrapping). Cụ thể đã check các fail safe cases:
  - Sai hash -> Bị chặn (`ERROR_HASH_MISMATCH`)
  - Sai key -> Bị chặn (`ERROR_DECRYPT_FAILED`)
  - JSON chứa key đáp án -> Bị chặn (`ERROR_SECURITY_VIOLATION_ANSWER_FIELDS`)
  - Session Token Plaintext không rò rỉ.

## 9. Legacy regression result
- Thực thi bằng lệnh `build_portable.ps1` hoàn thành suôn sẻ tạo file Portable chạy độc lập.
- Script `run_input_test.bat --exam-id 3` hoạt động bình thường, load được UI Parent (TutorHub Secure Exam Desktop) ở thẻ Login.
- Luồng `EXAM_START_REQUEST` legacy, Submit hay Rust IPC không bị sửa đổi hay ghi đè, do đó an toàn tuyệt đối 100% không side effects.

## 10. Rủi ro còn lại
- Kênh truyền Key từ Parent sang Child chưa được xây dựng, có khả năng hacker sẽ đọc command line parameters lúc Child bị spawn nếu dùng arguments. Sẽ cần xem xét truyền key qua NamedPipe IPC tương tự module Rust hiện hành.

## 11. Phase tiếp theo đề xuất
- **Phase 5H: Handoff IPC Key Exchange Mechanism** (Thiết kế Pipe hoặc Socket truyền Key tạm thời từ Parent qua Java Child). Hoặc có thể tiến hành thẳng **Phase 6: Child V2 UI Integrations**.
