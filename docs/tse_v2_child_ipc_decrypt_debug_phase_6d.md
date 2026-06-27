# TutorHub Secure Exam V2 - Phase 6D: Child V2 Debug IPC + Decrypt + Safe Summary

## 1. Mục tiêu Phase 6D
Mục tiêu của Phase 6D là thực hiện bước trung gian quan trọng trong Child Mode V2: tích hợp Loopback IPC client để lấy khóa AES an toàn từ Parent, giải mã tệp `.enc`, xác minh chữ ký SHA-256 (hash mismatch), và phân tích ngữ nghĩa (parse) dữ liệu mã hóa. Kết quả trả về một bản tóm tắt an toàn (Safe Summary) qua đối tượng DTO.

## 2. Vì sao chỉ là debug decrypt summary?
Do tính chất nhạy cảm của đề thi V2, việc ngăn chặn rò rỉ đề thi ở cấp độ bộ nhớ (memory layer) rất quan trọng. Bằng cách tách biệt thành một phase debug summary, chúng ta có thể:
- Xác nhận IPC fetch key chỉ được phép dùng 1 lần (Consume once).
- Xác nhận logic giải mã và kiểm tra hash hoạt động tốt trước khi UI can thiệp.
- Chặn đứng bất kỳ từ khóa bảo mật nào (như `isCorrect`) bị lọt xuống thông qua `V2RuntimeHandoffReader`.

## 3. Files/classes đã tạo hoặc sửa
- `TSEV2ChildDebugLoader.java`: Orchestrator chính để thực thi quá trình tải an toàn, fetch key qua IPC và parse bundle.
- `TSEV2ChildDebugLoadResult.java`: Data Transfer Object chứa thông tin tổng hợp (summary) không bao gồm khóa, sessionToken, hay dữ liệu plaintext.
- `TSEV2ChildDebugLoaderTest.java`: Chứa 5 Unit test mô phỏng trọn vẹn luồng loopback handoff.
- `TSEExamChildClient.java`: Chỉnh sửa nhánh `V2_DEBUG` để gọi loader nếu chạy với `--v2-debug-only`.

## 4. Child V2_DEBUG flow
Khi TutorHub Secure Exam chạy với cờ `--v2-debug-only`, `TSEExamChildClient` gọi phương thức tĩnh `TSEV2ChildDebugLoader.loadV2DebugHandoff()`. Quá trình này sẽ tạo log, thực thi toàn bộ luồng key fetch + decrypt, và sau cùng hiển thị bản tóm tắt lên một `JFrame` trống không cho mục đích debug.

## 5. IPC key fetch flow
- Bước 1: Đọc tệp `.meta.json` để lấy `handoffId` và `clientNonce`.
- Bước 2: Gọi `V2LoopbackKeyHandoffClient.requestKey` với thông số trên.
- Bước 3: HTTP POST gửi tới `127.0.0.1:23000/v2/handoff/key/consume`.
- Bước 4: Parent kiểm tra tính hợp lệ của token/nonce, nếu đúng trả về `SecretKey` (chuỗi Base64), nếu sai hoặc đã bị consume trả về `INVALID_NONCE_OR_HANDOFF`.

## 6. Hash/decrypt/parse validation
- `V2RuntimeHandoffService.decryptRuntimeHandoff()` sẽ giải mã `.enc` sử dụng `SecretKey`.
- Trước khi parse dữ liệu, hash của mảng byte vừa giải mã được băm qua SHA-256 và đối chiếu lại với `packageHash` khai báo trong `.meta.json`. Nếu lệch, tiến trình bị ngắt.
- File JSON plaintext không bị ghi ra đĩa mà chỉ tồn tại dưới dạng byte array trong Memory.

## 7. Error handling
- Trả về mã lỗi an toàn trong `TSEV2ChildDebugLoadResult` thay vì throw exception có thể chứa trace nhạy cảm.
- Các mã lỗi tiêu biểu: `ERROR_MISSING_META_FILE`, `ERROR_KEY_FETCH_FAILED`, `ERROR_HASH_MISMATCH`, `ERROR_SECURITY_VIOLATION`.

## 8. Security validation
- Đã chạy rà quét `findstr /S /I /N` không phát hiện các lệnh in `System.out` hay `printStackTrace` nhạy cảm đối với các biến `keyB64`, `sessionToken` hay `plaintextJson`.
- Dữ liệu bị cấm bao gồm các tag XML/JSON như `isCorrect` hoặc `answerKey`. Nếu tìm thấy, parser ném ngoại lệ bảo mật.

## 9. Unit test result
- **67/67 bài Test pass hoàn toàn.**
- 5 tests riêng của `TSEV2ChildDebugLoaderTest` đều pass (Valid loads, Missing meta, Consume twice fails, Hash mismatch, Security violation).

## 10. Maven build result
- `mvn clean install` kết thúc thành công với trạng thái **BUILD SUCCESS**.
- Thời gian chạy khoảng 1 phút 16 giây.

## 11. Portable build result
- `build_portable.ps1` kết thúc thành công, gom đủ JCEF locales, cấu trúc thư mục bảo toàn.
- Output tại `dist/TutorHubSecureExam`.

## 12. Legacy run_input_test result
- Legacy Secure Exam gọi bằng `run_input_test.bat --exam-id 3` hoạt động bình thường, không bị vỡ giao diện (JCEF vẫn render), chứng tỏ hệ thống backwards-compatible không bị ảnh hưởng.

## 13. Rủi ro còn lại
- Không có rủi ro hiển thị mã nguồn hay khóa ở Layer mã hóa.
- Tuy nhiên, V2_DEBUG chưa liên kết với UI Engine cuối cùng, khi sang Phase 6E cần đảm bảo JCEF renderer không vô tình leak `sessionToken` qua Javascript DOM.

## 14. Có nên đi tiếp Phase 6E không?
Hoàn toàn có thể. Phase 6D đã đạt mốc Regression Gate thành công. Hệ thống IPC loopback và Decrypt memory-only đã được chứng minh là ổn định. Có thể tiến tới Phase 6E để tích hợp UI hoàn chỉnh.
