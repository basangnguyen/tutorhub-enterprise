# Phase 8D + 8E: Attempt Status Transition Draft Persistence + Real Submit Readiness Orchestrator

## 1. Mục tiêu
Cụm Phase 8D và 8E đóng vai trò hoàn thiện khâu chuẩn bị trước khi một bài thi V2 được coi là "đã sẵn sàng để chuyển trạng thái".
- **Phase 8D (Attempt Status Transition Draft Persistence):** Ghi nhận ý định chuyển trạng thái attempt vào bảng lưu nháp `v2_attempt_status_transition_drafts`.
- **Phase 8E (Real Submit Readiness Orchestrator):** Tổng hợp và kiểm tra lại toàn bộ chuỗi gate từ đầu đến cuối (Preflight -> Transition Draft -> Status Gate -> Status Draft) để xuất ra một cờ Readiness tổng quan.

> [!WARNING]
> Cả hai Phase này **tuyệt đối KHÔNG** được phép cập nhật bảng `exam_attempts` hay ghi `exam_results`. Trường `targetAttemptStatus = SUBMITTED` chỉ là siêu dữ liệu (metadata) của bản nháp, chưa được apply vào hệ thống thật.

## 2. Kiến trúc & Data Model

### Phase 8D
Bảng mới: `v2_attempt_status_transition_drafts`
Lưu trữ các trường metadata như: `submitRecordId`, `userId`, `examId`, `attemptId`, `payloadHash` cùng với trạng thái của các gate trước đó. Record được đánh dấu với `attemptStatusTransitionDraftStatus = ATTEMPT_STATUS_TRANSITION_DRAFTED`.
Thao tác `insertDraft` được thiết kế theo cơ chế idempotent, nếu đã có bản nháp từ trước cho cùng `submitRecordId`, hệ thống sẽ trả về luôn bản cũ thay vì tạo mới.

### Phase 8E
Service `V2RealSubmitReadinessOrchestratorService` kiểm tra tuần tự:
1. Feature flag: `tse.v2.realSubmitReadinessOrchestrator.enabled`.
2. `V2RealSubmitPreflightService` phải trả về `READY_FOR_REAL_SUBMIT_DRAFT`.
3. Bảng `v2_real_submit_transition_drafts` phải có record với status `REAL_SUBMIT_TRANSITION_DRAFTED`.
4. `V2RealSubmitAttemptStatusTransitionGateService` phải pass.
5. Bảng `v2_attempt_status_transition_drafts` phải có record với status `ATTEMPT_STATUS_TRANSITION_DRAFTED`.
Nếu thỏa mãn tất cả, Orchestrator trả về trạng thái tổng: `READY_FOR_REAL_SUBMIT_STATUS_EXECUTION_DRAFT`.

## 3. Ràng buộc an toàn & Ngăn rò rỉ dữ liệu
- Mọi DTO (Result classes) cho cả 8D và 8E đều không được chứa các trường rủi ro: `answerKey`, `score`, `gradingResult`, `isCorrect`.
- Code Java được kiểm tra (security scan) qua regex từ chối chứa `exam_results`, lệnh `EXAM_SUBMIT` legacy, và logic Final Submit/Rust submit.

## 4. Các điểm tích hợp (ClientHandler)
- `EXAM_SUBMIT_V2_ATTEMPT_STATUS_TRANSITION_DRAFT`: Kích hoạt lưu bản nháp trạng thái.
- `EXAM_SUBMIT_V2_REAL_READINESS_ORCHESTRATOR`: Kích hoạt trình tổng hợp trạng thái chuẩn bị submit.

## 5. Kết quả kiểm tra
- Toàn bộ unit tests đã được chạy offline, sử dụng mock/fake DAOs.
- Tích hợp liên tục bằng build script `mvn clean install` (>255 test pass) và đóng gói `build_portable.ps1`.
- `run_input_test.bat` (Test hệ thống GUI trên Windows) tiếp tục được dán nhãn `PENDING` theo quy tắc Fast-Track không chạy legacy GUI test.

## 6. Bước tiếp theo
Sang giai đoạn tiếp theo của V2: Execute Status Transition hoặc Scoring Core. Sẽ chờ hướng dẫn kế tiếp.
