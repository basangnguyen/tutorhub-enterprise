# Phase 8C: Real Submit Attempt Status Transition Gate - No Grading / No exam_results

## 1. Mục tiêu
Tạo một Gate Service (`V2RealSubmitAttemptStatusTransitionGateService`) đóng vai trò gác cổng trước khi quyết định việc chuyển trạng thái attempt (Đang làm -> Đã nộp/Đã hoàn thành) trong Phase 8D. Gate này chỉ kiểm tra các điều kiện, hoàn toàn không thao tác thay đổi bất kỳ trạng thái nào và không chấm điểm.

## 2. Các ràng buộc tuân thủ
- **Không update `exam_attempts.status = SUBMITTED` hay các trạng thái khác**.
- **Không ghi `exam_results`**.
- **Không chấm điểm, không tính đúng/sai**.
- **Không đọc hoặc dùng `answerKey`**.
- **Không trả `answerKey`, `isCorrect`, `correctOption`, `score`, `gradingResult` trong result DTO**.
- **Không gọi legacy `EXAM_SUBMIT`**.
- **Không gọi Final Submit hay Rust submit**.
- **Không tạo bảng database mới (chỉ read-only từ các bảng cũ nếu cần thiết hoặc dựa vào các service khác)**.

## 3. Kiến trúc Gate Service
Service tái sử dụng hai tầng xác thực an toàn:
1. `V2RealSubmitPreflightService`: Đảm bảo luồng pipeline No Grading và các record phụ trợ (Ledger, Closure) đã hoàn thiện. Trạng thái bắt buộc phải là `READY_FOR_REAL_SUBMIT_DRAFT`.
2. `V2RealSubmitTransitionDraftDAO`: Truy vấn bảng draft để đảm bảo trạng thái nháp thực tế đã tồn tại, được tạo bởi cùng userId, và có status `REAL_SUBMIT_TRANSITION_DRAFTED`.

Nếu qua tất cả các bước, Gate Service sẽ trả về object `V2RealSubmitAttemptStatusTransitionGateResult` có `ready = true` và `statusTransitionGate = READY_FOR_ATTEMPT_STATUS_TRANSITION_DRAFT`.

## 4. Feature Flag & Socket Action
- Feature flag: `tse.v2.realSubmitAttemptStatusTransitionGate.enabled` (mặc định false).
- Socket action: `EXAM_SUBMIT_V2_REAL_ATTEMPT_STATUS_GATE` route riêng trong `ClientHandler.java`.

## 5. Offline Testing
Unit tests (`V2RealSubmitAttemptStatusTransitionGateServiceTest`) sử dụng hoàn toàn mock/fake DAOs và services, do đó chạy cô lập, không kết nối internet, không gọi vào Neon DB. Tổng cộng có 7 cases kiểm tra đầy đủ mọi ràng buộc.

## 6. Kết quả Validation
- Security scan: Pass. Không lọt bất kỳ API chấm điểm hay lệnh gọi legacy nào.
- Full Maven Build: Pass toàn bộ >246 tests.
- Portable Build: Pass.
- `run_input_test.bat`: PENDING theo quy tắc không chạy legacy test trên host.

## 7. Bước tiếp theo đề xuất
Phase 8D: Attempt Status Transition Draft/Persistence. Nơi lưu trữ trạng thái Transition sau khi đi qua Gate vào một bảng cơ sở dữ liệu Draft mới (vì chúng ta vẫn kiên quyết chưa được update `exam_attempts` trực tiếp ở hệ thống legacy).
