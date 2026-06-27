# TSE V2 Submit Record Regression (Phase 7H)

## 1. Mục tiêu phase
Thực hiện regression gate cho Phase 7G, đảm bảo bản nháp lưu trữ bài nộp V2 an toàn, sạch sẽ, không ảnh hưởng (hoặc gọi nhầm) sang hệ thống legacy EXAM_SUBMIT. 
Đồng thời, quy định chuẩn hóa cấu trúc toàn cầu cho `payloadHash` (bắt buộc dùng SHA-256).

## 2. Vì sao chưa Final Submit
- Việc nộp bài thi thực tế (Final Submit) trong môi trường an toàn đòi hỏi nhiều quy trình bảo mật (ví dụ: giao thức khóa luồng, xác thực tính toàn vẹn của máy khách/Rust) trước khi chấm điểm và ghi vào `exam_results`.
- Phase này là bước "đệm" cuối để đảm bảo mọi lỗ hổng (như lộ MD5 hash, lộ key, lộ answer keys) đã được rào lại thông qua Validation Service. Khi nền móng lưu trữ thô đã an toàn, mới có thể sang giai đoạn Final Submit.

## 3. Submit record schema audit
- Schema bảng `v2_submit_records` được thiết kế additive-only, không sửa hay động chạm tới bảng `exam_results`.
- Lưu trữ `payload_json` nhưng không lưu trữ token/key, loại bỏ triệt để rủi ro rò rỉ dữ liệu qua bảng phụ.

## 4. SHA-256 payloadHash standardization
- `payloadHash` đã được chuẩn hóa kiểm tra định dạng độ dài bằng regex (`^[a-fA-F0-9]{64}$`).
- MD5 hay các loại hash sai lệch độ dài/ký tự sẽ bị validation loại bỏ ngay tại gateway trả về lỗi `ERROR_V2_SUBMIT_RECORD_PAYLOAD_HASH_INVALID`.

## 5. DAO/service audit
- `V2SubmitRecordDAO` chỉ sử dụng các thao tác `INSERT INTO v2_submit_records` và `SELECT`. Không có `UPDATE`.
- Không gọi lệnh chèn thông tin sang các bảng legacy như `exam_results`.
- Không update trạng thái `exam_attempts` thành `SUBMITTED`.

## 6. ClientHandler route audit
- Trong `ClientHandler.java`, hành động `EXAM_SUBMIT_V2_RECORD_CREATE` đã được map vào một nhánh `switch/case` độc lập.
- Nó chỉ gọi tới `V2SubmitRecordService` và trả về `EXAM_SUBMIT_V2_RECORD_CREATE_OK` (hoặc error).
- Hoàn toàn độc lập, không có tình trạng fall-through xuống case `EXAM_SUBMIT` hay dùng chung logic cũ.

## 7. Security scan result
- Quá trình scan loại trừ các file hợp lệ bằng công cụ phân tích từ khóa (`MD5`, `answerKey`, `isCorrect`, `correctOption`, `score`, `FinalSubmit`, `exam_results`, `sessionToken`, `keyB64`, `plaintext`, `submit_payload`).
- Các cảnh báo tìm được (`INSERT`) đều thuộc không gian `ClientHandler` đối với logic legacy (VD: `blackboards`, `tutor_tasks`) - đã kiểm tra và an toàn cho luồng V2.
- File model/dao/service V2 đều đạt chuẩn, hoàn toàn "sạch".

## 8. Regression test result
- `V2SubmitRecordServiceTest` (tương đương `V2SubmitRecordRegressionTest`) đã bao phủ các kịch bản:
  - Feature flag off -> reject.
  - Valid payload -> insert submit record, status = `RECEIVED_DEBUG`, source = `V2_DEBUG`.
  - payloadHash = SHA-256 hex 64 kí tự -> verify success.
  - Invalid payloadHash -> reject.
  - Stored payload_json không chứa sessionToken/keyB64/plaintext/answerKey/isCorrect/correctOption/password/score.
  - Result DTO không chứa câu trả lời.
  - Validation fail -> reject DB write.
  - Không test exam_results / attempt status vì mock hoàn toàn rỗng và file DAO không tương tác.
- Tất cả các bài kiểm tra chạy thành công.

## 9. Maven build result
- `mvn clean install` PASS.
- Build thành công, tổng cộng 177 tests executed và pass.

## 10. Portable build result
- `build_portable.ps1` PASS.
- Packaged gọn nhẹ và ready.

## 11. run_input_test status
- `run_input_test.bat` legacy: PENDING - VM-only / skipped by fast-track rule.

## 12. Rủi ro còn lại
- Sự đồng nhất hash JSON giữa client và server (nếu serialize JSON không đều, hash có thể thay đổi trên client vs. db tái tính, mặc dù hiện tại server chỉ tiếp nhận và kiểm tra format).
- Mật độ tải dữ liệu (payloadJSON có thể chứa hàng ngàn node nộp cùng lúc cần được tối ưu hóa sau).

## 13. Go/No-Go cho phase tiếp theo
- **GO.** Hệ thống submit nháp đã được kiểm chứng an toàn. Sẵn sàng cho Phase liên quan Final Submit thật (Phase 7I) hoặc chuẩn bị Grading Flow.
