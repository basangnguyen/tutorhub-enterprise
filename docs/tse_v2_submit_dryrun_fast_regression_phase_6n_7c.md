# Submit Dry-run Fast Regression Gate - No VM (Phase 6N / 7C)

## 1. Mục tiêu phase
Xây dựng chốt chặn regression tự động (Fast Regression Gate) cho toàn bộ quy trình Submit Dry-run của chế độ V2_DEBUG, đảm bảo logic tạo/lưu/đọc/verify file mã hóa hoạt động ổn định và an toàn, trước khi tiến tới kết nối mạng hoặc backend.

## 2. Vì sao no-VM fast gate
Do TutorHub Secure Exam sử dụng Safe Exam Browser, việc test GUI đầy đủ cần chạy trên Virtual Machine, tốn rất nhiều thời gian.
Phase 6N / 7C tạo ra một gate No-VM bằng cách dùng jUnit test: `TSEV2SubmitDryRunRegressionTest.java`. Test này mô phỏng toàn bộ round-trip in-memory: từ `TSEV2AnswerDraftSnapshot` $\to$ `TSEV2SubmitPayload` $\to$ file `.enc` $\to$ đọc lại $\to$ verify, hoàn toàn không cần GUI và không cần VM.

## 3. Submit dry-run round-trip flow
Quy trình được test bao phủ các bước:
1. Tạo mock `TSEV2ReadOnlyExamRenderModel` và `TSEV2AnswerDraftSnapshot`.
2. Map qua `TSEV2SubmitPayloadService` để ra `TSEV2SubmitPayload`.
3. Lưu xuống thư mục tạm dùng `TSEV2EncryptedSubmitDryRunService`.
4. Sinh ra `v2_submit_payload_dryrun.enc` và `v2_submit_payload_dryrun.meta.json`.
5. Đọc ngược file bằng `loadEncryptedSubmitPayloadDryRun`.
6. Verify dữ liệu (ExamId, PaperId, AnsweredCount, SelectedOptions) và `payloadHash` khớp hoàn toàn.

## 4. Security validation
Chốt chặn security đảm bảo:
- `v2_submit_payload_dryrun.meta.json` không chứa `answers`, `selectedOptionId`, `key`, `token`, `plaintext`.
- File `.enc` giả mạo (bị bit-flip) lập tức bị văng Exception.
- Key lưu mã hóa không đúng (wrong key) lập tức bị văng Exception.
- Từ khoá nguy hiểm lọt vào JSON Payload sẽ tự động trigger Exception khi lưu.

## 5. Test coverage
Thêm mới file `TSEV2SubmitDryRunRegressionTest.java` với 4 kịch bản lớn (Round-trip, Wrong Key Fails Safely, Tamper Fails, Unsafe Payload Rejected).
Pass toàn bộ 4/4 kịch bản.

## 6. Maven build result
PASS. Tổng cộng 137 tests pass, 0 failure, 0 error.

## 7. Portable build result
PASS. Build `TutorHubSecureExam` hoàn thiện với JCEF bundle.

## 8. run_input_test status
`run_input_test.bat legacy` (GUI test trên legacy flow): PENDING - Do là VM-only và bị skip theo rule fast-track.

## 9. Rủi ro còn lại
Do mọi thứ đều in-memory & file-system (no-VM), các rủi ro liên quan đến UI freeze, memory leak trên DOM, hay network timeout lúc submit thật vẫn chưa được bộc lộ. Rủi ro này sẽ được xử lý tại phase submit backend thật.

## 10. Go/No-Go cho backend submit contract phase
**GO.**
Phase in-memory submit (payload creation & crypto wrapper) đã hoàn toàn cứng cáp và an toàn. Đã sẵn sàng cho Phase 7D (Backend Submit Contract) hoặc các phase persistence khác.
