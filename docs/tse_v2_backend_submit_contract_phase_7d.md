# Phase 7D: Backend Submit Contract - Server-side Dry-run Validation

## Đã tạo/sửa file nào
1. **[NEW]** `src/main/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitDryRunValidationResult.java`
   - DTO chứa kết quả validation, **đảm bảo KHÔNG TRẢ VỀ answerKey, isCorrect, score**.

2. **[NEW]** `src/main/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitDryRunValidationService.java`
   - Chứa 17 validation rules cho payload `TSEV2SubmitPayload`.
   - Kiểm tra `examId`, `paperId`, `attemptId`.
   - Đối chiếu số lượng câu hỏi, số câu trả lời hợp lệ, không chứa keyword nguy hiểm (`answerKey`, `score`, v.v.).

3. **[MODIFY]** `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
   - Đã chèn thêm socket action: `EXAM_SUBMIT_V2_DRYRUN_VALIDATE`.
   - Xử lý parsing từ JSON sang `TSEV2SubmitPayload` và gọi `V2SubmitDryRunValidationService`.

4. **[NEW]** `src/test/java/com/mycompany/tutorhub_enterprise/server/services/V2SubmitDryRunValidationServiceTest.java`
   - Unit test không sử dụng Mockito (sử dụng subclass để override dao method).
   - Test các case: success, sai flow, câu hỏi trùng, số lượng sai, từ khóa không an toàn.

## Đã chạy lệnh gì
- `mvn clean test -Dtest=V2SubmitDryRunValidationServiceTest`
- Lệnh regex thay thế trong file `ClientHandler.java` và xóa kí tự BOM bằng PowerShell.
- Lệnh quyét security scan với `findstr`.

## Kết quả test
- Security Scan PASS: Các từ khóa `answerKey`, `isCorrect` chỉ nằm trong test hoặc logic chặn.
- Unit Test PASS: Các case validate payload đều hoạt động tốt. 

## Checklist nào đã cập nhật
- Đã tick đầy đủ các task liên quan đến **Phase 7D** trong `docs/secure_exam_tasks_v2.md`.

## Rủi ro còn lại
- Hiện tại service sử dụng mock data/override cho DB calls trong test, có thể phát sinh lỗi nếu `ExamAttemptDAO` thật có behaviors lạ (ví dụ status đã thay đổi ngoài ý muốn). Cần theo dõi thêm trong integration.

## Task tiếp theo đề xuất
- Tiến hành Phase tiếp theo: **Phase 7E: Server-side Payload Persistence** (lưu json payload thật vào database hoặc storage).
