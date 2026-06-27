# Phase 8B: Real Submit State Transition Draft

## 1. Mục tiêu phase
Tạo một "draft intent" cho việc chuyển trạng thái bài thi thật sau khi hệ thống đã vượt qua bài kiểm tra tính hợp lệ Preflight. Bước đệm này đóng vai trò chốt trạng thái sẵn sàng để submit, nhưng tuyệt đối chưa ghi nhận kết quả điểm số hay thay đổi status hoàn thành của `exam_attempts`.

## 2. Vì sao đây chỉ là transition draft
Transition Draft nhằm lưu lại dấu vết (audit trail) rằng bài thi từ phía client đã được chuẩn bị đầy đủ dữ liệu an toàn để sang chế độ xử lý thực. Tuy nhiên, để tuân thủ thiết kế No-Grading ở lớp mạng, chúng ta không tính toán logic đúng/sai và không gọi legacy `EXAM_SUBMIT` hay cập nhật database ngay lập tức.

## 3. DB table mới
Tạo bảng `v2_real_submit_transition_drafts` với các trường:
- `id` (PK)
- `submit_record_id` (Unique)
- `user_id`, `exam_id`, `paper_id`, `attempt_id`, `ledger_id`, `closure_draft_id`
- `payload_hash`
- `preflight_status`
- `transition_draft_status`
- `created_at`, `updated_at`

## 4. Idempotency rule
DAO và Service xử lý insert với cơ chế Idempotency. Nếu `submit_record_id` đã được lưu nháp (drafted) trước đó, hàm sẽ bắt được bản ghi trùng khớp và trả về `idempotent=true` thay vì báo lỗi duplicate insert.

## 5. Feature flag
Sử dụng cờ: `tse.v2.realSubmitTransitionDraft.enabled` 
Mặc định khởi tạo: `false`.
Nếu tính năng không bật, request tạo draft sẽ bị từ chối với lỗi `ERROR_FEATURE_DISABLED`.

## 6. Socket action
Bổ sung Action mới: `EXAM_SUBMIT_V2_REAL_TRANSITION_DRAFT`
Action này tách biệt hoàn toàn và không route sang kênh `EXAM_SUBMIT` cũ.

## 7. Preflight dependency
Yêu cầu preflight check (Phase 8A.5) phải trả về trạng thái `READY_FOR_REAL_SUBMIT_DRAFT`. Nếu Preflight `NOT_READY`, Transition Draft sẽ không được tạo.

## 8. No grading / no exam_results / no SUBMITTED guarantee
Kịch bản và Result DTO đã được kiểm thử để đảm bảo các trường nhạy cảm như `answers`, `score`, `answerKey`, `isCorrect` không tồn tại. Mọi cập nhật trạng thái chỉ thao tác trên bảng draft, không động vào bảng `exam_attempts` hay `exam_results`.

## 9. Unit test result
Unit test cho toàn bộ module chạy thành công ở chế độ Offline (không phụ thuộc remote database). 
Tất cả negative tests chặn dữ liệu không an toàn đều `PASS`.

## 10. Maven build result
Pass 100%. (Chi tiết: 233+ tests pass).

## 11. Portable build result
Pass. `build_portable.ps1` biên dịch portable executable thành công.

## 12. run_input_test status
`PENDING` - VM-only / skipped by fast-track rule.

## 13. Rủi ro còn lại
Do chưa update `SUBMITTED`, giao diện dashboard của sinh viên và màn hình hiển thị bài nộp vẫn sẽ hiện "Đang làm" hoặc trạng thái tương tự (vì legacy state chưa được kích hoạt). Việc này sẽ được xử lý ở Phase cuối.

## 14. Phase tiếp theo đề xuất
Phase 8C: Real Submit Finalization / Scoring Core (Tính năng chốt điểm thực tế trên môi trường Rust và lưu `exam_results`).
