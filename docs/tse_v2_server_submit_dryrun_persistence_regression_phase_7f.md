# Phase 7F: Server-side Submit Dry-run Persistence Regression Gate - No VM

## 1. Mục tiêu phase
Thực hiện regression gate cho Phase 7E (Server-side Submit Dry-run Persistence). Mục tiêu là verify lại việc lưu trữ payload dry-run vào cơ sở dữ liệu `v2_submit_dryrun_payloads` một cách an toàn, không rò rỉ dữ liệu nhạy cảm (như đáp án, key mã hóa), không thay đổi state thực sự của bài thi, và không gọi đè logic nộp bài (legacy `EXAM_SUBMIT`).

## 2. Vì sao chỉ regression gate
Do repo đã trải qua nhiều phase liên quan đến Submit Contract (Phase 7D), Payload Contract (Phase 7A, 7B), việc đảm bảo tính vẹn toàn ở tầng Data Access Object (DAO) và Persistence Service là rất quan trọng trước khi đi vào submit thực sự (update attempt thành SUBMITTED). Bước này bảo vệ hệ thống khỏi side-effect ngoài ý muốn.

## 3. Schema/DAO audit
- Lệnh `V2SubmitDryRunPayloadDAO.ensureSchema()` hoàn toàn theo cơ chế "additive only" (`CREATE TABLE IF NOT EXISTS`).
- Các hàm DAO chỉ thực thi lệnh `INSERT INTO v2_submit_dryrun_payloads` và `SELECT`. Không có bất kỳ lệnh `UPDATE` hay `INSERT` nào vào `exam_results` hay `exam_attempts` trong file này.

## 4. Service audit
- `V2SubmitDryRunPersistenceService.java` tuân thủ strict check feature flag `tse.v2.submitDryRunPersistence.enabled`.
- Có bước validation qua `V2SubmitDryRunValidationService` trước khi insert.
- Hàm `isSafeToPersist(payloadJson)` lọc chặt các plaintext markers, key, password hash và các trường đáp án (`answerKey`, `isCorrect`).

## 5. Socket action audit
- `EXAM_SUBMIT_V2_DRYRUN_PERSIST` action đã được định tuyến độc lập trong `ClientHandler.java`.
- Payload trả về client (`EXAM_SUBMIT_V2_DRYRUN_PERSIST_OK` hoặc `_ERROR`) được kiểm soát, không chứa đáp án hay ID lựa chọn sai sót.

## 6. Security scan result
Kết quả scan toàn bộ thư mục cho Phase 7E/7F với các từ khoá cấm (`answerKey`, `score`, `sessionToken`, `keyB64`, `plaintext`, v.v) cho thấy:
- Các `INSERT/UPDATE` xuất hiện ở tính năng cũ không liên quan.
- Những cụm từ nhạy cảm chỉ nằm trong các assertion phủ định `assertFalse(json.contains(...))` của file Unit test/Regression test.
- KHÔNG HỀ có leak logic ở production codebase.

## 7. Regression test result
Tạo test `V2SubmitDryRunPersistenceRegressionTest.java` bám sát 14 kịch bản kiểm thử:
- Feature flag off / Validation fail -> không insert.
- Payload hợp lệ -> insert thành công và ID/Hash khớp.
- Stored JSON hoàn toàn sạch, không chứa key, plaintext, answers, password, hay result mark.
- DTO không trả answers/selectedOptionId.
- Không write vào exam_results, không update legacy attempt.
Tất cả các Tests đều chạy pass.

## 8. Maven build result
Lệnh `mvn clean install` PASS với tổng cộng 158 tests thành công (bao gồm 12 tests regression mới).

## 9. Portable build result
Lệnh `build_portable.ps1` PASS, đã build ra portable binaries thành công.

## 10. run_input_test status
`run_input_test.bat` legacy: PENDING - VM-only / skipped by fast-track rule.

## 11. Rủi ro còn lại
Do hiện tại record của Submit Dry Run không được gắn cron job xóa dọn định kỳ, database size của bảng `v2_submit_dryrun_payloads` có thể phình to nếu bị spam payload. Tuy nhiên ở mức độ debug/dry run, rủi ro này có thể chấp nhận.

## 12. Go/No-Go cho phase submit thật tiếp theo
Trạng thái: **GO**.
Persistence service hoạt động đúng kỳ vọng, hoàn toàn độc lập và không phá vỡ logic Legacy. Sẵn sàng cho Phase lưu Submit thực sự.
