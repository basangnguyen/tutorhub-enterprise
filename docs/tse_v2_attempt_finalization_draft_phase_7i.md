# TSE V2 Attempt Finalization Draft (Phase 7I)

## 1. Mục tiêu phase
Xây dựng cơ chế "Draft Finalization" (bản nháp chốt bài) trên server cho hệ thống TSE V2.
Sau khi client nộp thành công payload và trạng thái là `RECEIVED_DEBUG` hoặc `VALIDATED_DEBUG`, client có thể gọi lệnh chốt nháp. Đây là bước đệm cuối cùng trước khi có grading/chấm điểm, đảm bảo record được khóa lại ở trạng thái đã sẵn sàng, và legacy system hoàn toàn không bị ảnh hưởng.

## 2. Submit record state machine
Các trạng thái vòng đời của một V2 submit record hiện tại:
- `RECEIVED_DEBUG`: Khởi tạo ban đầu khi server vừa nhận dữ liệu và pass vòng hash.
- `VALIDATED_DEBUG`: Tương lai sẽ dùng khi validation job chạy xong.
- `FINALIZATION_DRAFTED`: Đã chốt nháp, sẽ không thay đổi dữ liệu nộp nữa, sẵn sàng cho grading.
- (Không sử dụng các trạng thái legacy như `SUBMITTED`, `GRADED`, `FINAL_SUBMITTED`).

## 3. Attempt finalization draft flow
1. Client gửi package `EXAM_SUBMIT_V2_FINALIZATION_DRAFT` kèm `submitRecordId`.
2. `ClientHandler` kiểm tra quyền (STUDENT) và feature flag.
3. Chuyển cho `V2AttemptFinalizationDraftService`:
   - Xác thực sự tồn tại và matching userId của submit record.
   - Kiểm tra hash bảo vệ (`payloadHash` phải chuẩn SHA-256).
   - Kiểm tra current status (chỉ chấp nhận `RECEIVED_DEBUG` hoặc `VALIDATED_DEBUG`).
   - Nếu record đã ở trạng thái `FINALIZATION_DRAFTED`, trả về kết quả thành công ngay lập tức (Idempotency).
   - Tiến hành update DB trạng thái thành `FINALIZATION_DRAFTED`.
4. Trả về `V2AttemptFinalizationDraftResult` cho client (chỉ bao gồm metadata an toàn: id, status mới, no answers/score/key).

## 4. Vì sao chưa grading/chưa Final Submit
Đây là rào cản chia phase. Phase này chuyên tâm vào state transition và lock record trên DB ở mức độ "V2 Only". 
- Chưa grading: Tránh gọi nhầm logic chấm điểm cũ, đảm bảo DB không bị ô nhiễm (ví dụ `exam_results` vẫn an toàn).
- Chưa Final Submit: Nhằm test riêng module state machine trước khi mở khóa flow hoàn chỉnh có giao tiếp Parent / QuickSettings.

## 5. Feature flag
- Biến: `tse.v2.attemptFinalizationDraft.enabled`
- Mặc định: `false`

## 6. Socket action
- Action name: `EXAM_SUBMIT_V2_FINALIZATION_DRAFT`
- Đã được đăng ký trong `ClientHandler` với response chuẩn V2 (`..._OK` hoặc `..._ERROR`).

## 7. Security validation
- Service và DAO được thiết kế strict "Read metadata / Update status".
- Không có bất kỳ lệnh `SELECT * ... exam_results` hay `UPDATE exam_attempts` nào.
- DTO không chứa key, score, answers.
- `findstr` / security scan xác nhận không có lỗ hổng rò rỉ dữ liệu legacy.

## 8. Unit test result
- `V2AttemptFinalizationDraftServiceTest` bao phủ 100% logic:
  - Reject khi feature flag tắt.
  - Reject khi user mismatch hoặc record not found.
  - Reject khi invalid hash, invalid status.
  - Xử lý Idempotency (gọi 2 lần).
  - Update status thành công khi đúng trạng thái.
- Tổng số bài test đã chạy: 185, Failures: 0, Errors: 0.

## 9. Maven build result
- `mvn clean install` PASS.

## 10. Portable build result
- `build_portable.ps1` PASS.

## 11. run_input_test status
- `run_input_test.bat` legacy: PENDING - VM-only / skipped by fast-track rule.

## 12. Rủi ro còn lại
- Không có rủi ro kĩ thuật nghiêm trọng đối với luồng nộp bài cũ.

## 13. Phase tiếp theo đề xuất
- Tiến hành tích hợp Client-side cho nút Final Submit để gửi lệnh `EXAM_SUBMIT_V2_FINALIZATION_DRAFT` thay thế cho hàm submit cũ.
- Tích hợp giao diện UI (Phase 7J).
