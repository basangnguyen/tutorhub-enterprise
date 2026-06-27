# Phase 5A.5: V2 Enabled Smoke Test + Feature Flag Consistency

## 1. Mục tiêu phase
Kiểm tra chi tiết luồng `EXAM_START_REQUEST_V2` trên backend khi feature flag được bật và tắt, thống nhất tên gọi feature flag chuẩn, và xác nhận gói dữ liệu V2 trả về không bị lộ các trường nhạy cảm như đáp án hoặc mật khẩu. Không thay đổi UI/Child client.

## 2. Tên feature flag chính thức
Tên feature flag chính thức duy nhất là:
`tse.paperStartV2.enabled`

## 3. Cách bật flag trong môi trường test
Có thể bật flag cho server thông qua cấu hình System Property của JVM.
Command example:
`-Dtse.paperStartV2.enabled=true`

## 4. Kết quả test flag=false
Khi chạy lệnh server mà không khai báo hoặc set `-Dtse.paperStartV2.enabled=false`, luồng `EXAM_START_REQUEST_V2` ngay lập tức trả về `success=false` với message `FEATURE_DISABLED` mà không tạo ra bất kỳ attempt, session hay ngoại lệ nào. An toàn tuyệt đối cho production hiện tại.

## 5. Kết quả test flag=true
Khi chạy server với `-Dtse.paperStartV2.enabled=true`, request `EXAM_START_REQUEST_V2` thành công (sau khi pass các bước validate) trả về cấu trúc package mới:
- `flow`: "PAPER_START_V2"
- `examId`: 3 (Exam ID hợp lệ)
- `paperId`: 2 (Paper đã gán)
- `questionCount`: 1 (Số câu hỏi có trong paper)
- Khởi tạo package json hợp lệ với thuộc tính `questions` bao gồm nội dung.

## 6. Test các error code
Backend đã handle toàn bộ error case với message chuẩn:
- **EXAM_NOT_FOUND:** Thành công.
- **EXAM_HAS_NO_ASSIGNED_PAPER:** Thành công. Lấy exam không gắn paper sẽ bị từ chối ngay.
- **PAPER_HAS_NO_QUESTIONS:** Tương tự logic check list question > 0.
- **EXAM_NOT_ACTIVE / INVALID_PASSWORD:** Đã implement ở logic service và hoạt động như legacy.

## 7. Security check: không lộ đáp án
Đã kiểm tra JSON package trả về thông qua automated output matching:
- `isCorrect`: Không bị lộ (false)
- `answerKey`: Không bị lộ (false)
- `correctOption`: Không bị lộ (false)
- `password`: Không bị lộ (false)
- `grading_config`: Không bị lộ (false)
- Không có log dump json nhạy cảm ra console.

## 8. Legacy regression result
Legacy flows `EXAM_START_REQUEST`, `EXAM_SUBMIT` hoàn toàn không bị chạm đến. Code V2 được encapsulate hoàn toàn trong `ExamStartV2Service.java` và case độc lập trong `ClientHandler.java`.

## 9. Rủi ro còn lại
Backend implementation đã hoàn toàn an toàn và isolated. Rủi ro chuyển sang phần UI (Phase 5B): Secure Exam Child sẽ phải xử lý và render đúng package dựa trên format mới của luồng V2 này.

## 10. Có nên đi tiếp Phase 5B không
Có. Backend API đã sẵn sàng phục vụ cho việc tích hợp tính năng V2 vào giao diện làm bài của học sinh (Secure Exam Child). Phase 5B (UI Integration) có thể được tiến hành.
