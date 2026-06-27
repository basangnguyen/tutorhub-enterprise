# Phase 10C + 10D: Manual Candidate V2 Submit Route + Candidate Submit Orchestrator Gate

## Mục tiêu
Tạo router cho action "nộp bài bằng tay" của Candidate (Phase 10C) và thiết lập Gate kiểm tra trạng thái trước khi nộp (Phase 10D). Toàn bộ thao tác chỉ là "Read-only" để kiểm tra tính hợp lệ và sẵn sàng.

## Các component đã thêm/sửa

### 1. Feature Flags
Thêm vào `V2SubmitFeatureFlags.java`:
- `isManualCandidateSubmitEnabled()`: Cờ bật/tắt check nộp bài thủ công.
- `isCandidateSubmitOrchestratorGateEnabled()`: Cờ bật/tắt Gate orchestrator tổng thể.

### 2. Result & Service (Phase 10C)
- `V2ManualCandidateSubmitCheckResult.java`: DTO chứa kết quả readiness của attempt.
- `V2ManualCandidateSubmitCheckService.java`: Service gọi lại check từ 10A/10B và kiểm tra attempt.
  - Phải có trạng thái in-progress (`STARTING`, `STARTED`, `IN_PROGRESS`, `DOING`, `IN_EXAM`).
  - Chặn ngay lập tức nếu là `SUBMITTED`, `COMPLETED`, `GRADED`.

### 3. Result & Service (Phase 10D)
- `V2CandidateSubmitOrchestratorGateResult.java`: DTO trả về Gate status.
- `V2CandidateSubmitOrchestratorGateService.java`: Orchestrator Gate kiểm tra để đảm bảo dữ liệu chưa từng tồn tại ở Legacy hoặc V2.
  - Chặn nếu `V2SubmitRecord` đã tồn tại (`V2SubmitRecordDAO.findLatestByAttemptId()`).
  - Chặn nếu `V2ResultPublicationLedgerRecord` đã tồn tại (`V2ResultPublicationLedgerDAO.findByAttemptId()`).
  - Chặn nếu `exam_results` Legacy đã tồn tại thông qua `V2ExamResultsReadOnlyProbe.existsResultForAttempt()`.
  - Chặn nếu `V2FinalAttemptStatusLedgerRecord` đã tồn tại (`V2FinalAttemptStatusLedgerDAO.existsByAttemptId()`).

### 4. ClientHandler Routes
Thêm routing vào `ClientHandler.java`:
- `EXAM_SUBMIT_V2_MANUAL_CANDIDATE_SUBMIT_CHECK`
- `EXAM_SUBMIT_V2_CANDIDATE_SUBMIT_ORCHESTRATOR_GATE`

## Ràng buộc đã tuân thủ
- **Không ghi dữ liệu:** Các hàm kiểm tra chỉ chạy lệnh SELECT để đọc DAO.
- **Không đổi Attempt Status:** Không có bất kỳ truy vấn UPDATE nào được chạy.
- **Không expose thông tin nhạy cảm:** Không có rawScore, percentage, answerKey,... được expose. Chỉ trả về `success`, `ready`, `errorCode`, và danh sách `blockingReasons`.

## Kết quả Build & Test
- Đã chạy quét security `findstr`: Không có từ khóa vi phạm (answerKey, isCorrect, INSERT, UPDATE, DELETE,... trong logic của 10C/10D).
- Maven Build: `mvn clean install` PASS (409 tests).
- Portable Build: `build_portable.ps1` PASS.
