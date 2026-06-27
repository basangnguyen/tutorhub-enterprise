# Phase 5F: Encrypted V2 Runtime Handoff Dry-run

## 1. Mục tiêu
- Tạo cơ chế mã hóa (AES-GCM) an toàn để lưu dữ liệu đề thi dưới dạng file `v2_handoff_runtime.enc`.
- Tạo cơ chế meta file `v2_handoff_runtime.meta.json` để check mã băm của file enc, tăng cường tính toàn vẹn.
- Kiểm tra tính bảo mật (chặn `isCorrect`) trước khi mã hóa vào JSON.
- Đảm bảo cơ chế có thể được trigger từ Debug Mode Dialog bằng một nút riêng biệt.

## 2. Các file đã tạo/sửa
- **Tạo mới**: `src/main/java/com/mycompany/tutorhub_enterprise/client/services/V2RuntimeHandoffService.java`
  - Thực hiện serialize `V2ExamHandoffBundle`.
  - Nếu không phải Debug Mode, yêu cầu có `sessionToken` hoặc ném lỗi bảo mật.
  - Kiểm tra chuỗi JSON trước khi mã hóa (không được phép có từ khoá `isCorrect`, `answerKey`, `correctOption`).
  - Sử dụng `CryptoUtils.encryptWrapper` (chuẩn AES-GCM) để mã hoá file với base64 key.
  - Tính SHA-256 nội dung đã mã hóa để lưu vào meta file.
- **Tạo mới**: `src/test/java/com/mycompany/tutorhub_enterprise/V2RuntimeHandoffServiceTest.java`
  - Unit test encrypt/decrypt.
  - Đảm bảo file mã hóa không rò rỉ `sessionToken`.
  - Đảm bảo `sessionToken` thiếu thì bị chặn encrypt (non-debug).
  - Kiểm tra cơ chế tự động chặn `isCorrect`.
- **Sửa**: `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamStartV2DebugDialog.java`
  - Thêm nút "Create encrypted handoff dry-run" ở footer để tự sinh ngẫu nhiên một AES key, test thử ghi file và hiển thị popup đường dẫn `v2_handoff_runtime.enc`.

## 3. Lệnh đã chạy & Kết quả Test
- Chạy `mvn clean install`:
  ```text
  [INFO] Running com.mycompany.tutorhub_enterprise.V2RuntimeHandoffServiceTest
  [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
  ...
  [INFO] BUILD SUCCESS
  ```
  Pass tất cả 41 tests. File `v2_handoff_runtime.enc` và `v2_handoff_runtime.meta.json` được ghi thành công, đúng quy chuẩn bảo mật.

## 4. Trạng thái Checklist
- [x] Không sửa core logic `EXAM_START_REQUEST` legacy.
- [x] Ghi file `.enc` an toàn.
- [x] Ghi file `.meta.json` an toàn (không leak sessionToken).
- [x] Bắn lỗi `SECURITY VIOLATION` nếu dính từ khoá đáp án trước khi encrypt.
- [x] Tạo nút Dry-run trên giao diện Admin.

## 5. Đề xuất Task tiếp theo
- **Phase 6: Integration into Secure Exam Child**
  - Parent sẽ tự render ra AES key, ghi file enc, spawn Secure Exam Child với command line arguments truyền key này qua pipe.
  - Secure Exam Child đọc file, giải mã bằng key, hiển thị UI V2.
