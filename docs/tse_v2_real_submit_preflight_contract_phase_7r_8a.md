# Phase 7R / 8A: Real Submit Preflight Contract

## 1. Mục tiêu phase
Xây dựng "Safety Gate" (cổng kiểm tra an toàn) cuối cùng trước khi chuyển sang thực hiện ghi nhận điểm và nộp bài thật (Real Submit). Preflight service này đóng vai trò quyết định xem một payload đã được xử lý an toàn qua No-Grading Pipeline chưa.

## 2. Vì sao chỉ preflight, chưa real submit
Để đảm bảo rằng logic V2 Submit mới hoàn toàn độc lập với legacy flow và không gây ảnh hưởng đến dữ liệu production, việc tách bước kiểm tra (preflight) ra khỏi bước hành động (submit) giúp khóa cứng mọi rủi ro lộ điểm, rò rỉ đề, hoặc tạo corrupted data trong database.

## 3. Preflight conditions
Hệ thống sẽ kiểm tra các điều kiện sau:
- `tse.v2.realSubmitPreflight.enabled` flag được bật.
- `submitRecord` tồn tại, hash chính xác và thuộc đúng userId.
- `submitRecord` ở trạng thái `FINALIZATION_DRAFTED` hoặc `CLOSURE_DRAFTED`.
- `ledgerId` và `closureDraftId` tương ứng đã được tạo đầy đủ từ No-Grading Pipeline.
- `closureStatus` là `CLOSURE_DRAFTED_NO_GRADING`.
- KHÔNG có bất kỳ trường (fields) không an toàn nào như `score`, `answerKey`, `isCorrect` trong payload.

## 4. Ready/Not Ready behavior
- Nếu thỏa mãn: `ready = true` và `preflightStatus = READY_FOR_REAL_SUBMIT_DRAFT`.
- Nếu thiếu sót: `ready = false`, `preflightStatus = NOT_READY`, và kèm theo chi tiết ở `blockingReasons`.

## 5. Feature flag
`tse.v2.realSubmitPreflight.enabled=false` (Mặc định).

## 6. Socket action
Action: `EXAM_SUBMIT_V2_REAL_PREFLIGHT`. Trả về `EXAM_SUBMIT_V2_REAL_PREFLIGHT_OK` hoặc `EXAM_SUBMIT_V2_REAL_PREFLIGHT_ERROR`.

## 7. Security validation
Toàn bộ object DTO `V2RealSubmitPreflightResult` không bao gồm các biến nhạy cảm như `answers`, `score`, `gradingResult`. Không trigger `FINAL_SUBMIT`, không update trạng thái Attempt thành `SUBMITTED`.

## 8. Unit test result
Tất cả các case bắt buộc (16 case) đều Pass. Phủ sóng hoàn toàn negative cases như sai hash, sai user, trạng thái DB sai.

## 9. Maven build result
TBD (Chờ quá trình build `mvn clean install` hoàn tất).

## 10. Portable build result
TBD (Chờ quá trình `build_portable.ps1` hoàn tất).

## 11. run_input_test status
`PENDING - VM-only / skipped by fast-track rule`.

## 12. Rủi ro còn lại
- Vẫn đang vận hành dưới cờ Feature Flag, nếu cờ vô tình được bật nhưng No-Grading Pipeline bị trục trặc, user có thể bị treo ở Preflight.

## 13. Phase tiếp theo đề xuất
Phase 8B: Triển khai Real Submit Finalization và ghi vào `exam_results`.
