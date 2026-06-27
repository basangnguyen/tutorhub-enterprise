# Portable EXAM_SUBMIT Smoke Test Harness

Đây là kịch bản kiểm tra khói (Smoke Test) dành cho hệ thống TutorHub khi đóng gói ở dạng Portable (Standalone app). Mục tiêu là xác minh tính đúng đắn của tính năng Submit Bài Thi thông qua cả Legacy Route và V2 Route (chỉ bật bằng flag trên VM test).

## Ghi Chú Hiện Trạng Hệ Thống
`run_input_test.bat`: PENDING / NOT RUN - script not found or VM-only

## Kịch Bản Kiểm Tra (Smoke Test)

1. **Khởi động Portable App**: Đảm bảo app portable start thành công, CEF/JavaFX hoạt động mượt mà.
2. **Đăng Nhập**: Login vào tài khoản test (nếu đang chạy trên VM có sẵn thông tin account).
3. **Bắt Đầu Bài Thi**: Khởi tạo và Start exam bình thường.
4. **Submit Qua UI**: Nút submit trên UI khi bấm vẫn phải phát ra `EXAM_SUBMIT` network signal.

### Test Case 1: V2 Flags OFF (Mặc định Production)
5. **Route Selected**: Với V2 flags off, legacy route phải được chọn tự động. Xác nhận file debug/logs trỏ tới Legacy Fallback Route hoặc Legacy Handler gốc.
6. **Không Lỗi**: Hệ thống submit bình thường.
7. **Không Lộ Lọt Dữ Liệu**: Gói trả về không lộ score/detail answers.

### Test Case 2: VM Test-Profile Flags ON (V2 Activated)
8. **Route Selected**: Với các flag V2 test-profile on, hệ thống phải chọn V2 route (Execution Bridge).
9. **Hoàn Thành Chu Trình V2**: Route chạy trọn vẹn 7 stages và trạng thái đạt tới `COMPLETED`.
10. **Ghi Nhận Cơ Sở Dữ Liệu**: DB (hoặc local storage ở bản portable) phải có record `exam_results`.
11. **Không Lộ Lọt Dữ Liệu (V2)**: KHÔNG expose payloadJson, score, answerKey, perQuestionResults, hay selectedOptionId ra DTO phản hồi.
12. **Bảo Vệ Fallback (Post-Write)**: Xác nhận nếu fail ở POST-WRITE stage, hệ thống KHÔNG fallback về legacy (nhận `V2_WRITE_STARTED_FALLBACK_FORBIDDEN` hoặc tương tự).
