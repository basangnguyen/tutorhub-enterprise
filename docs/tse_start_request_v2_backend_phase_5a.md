# Phase 5A: Start Request V2 Backend + Feature Flag for Paper-based Exam Flow

## 1. Mục tiêu đã hoàn thành
- Xây dựng luồng nhận đề thi mới qua `EXAM_START_REQUEST_V2` cho TutorHub Secure Exam.
- Luồng mới lấy dữ liệu dựa trên bảng `exam_papers` -> `exam_paper_questions` thay vì lấy ngẫu nhiên từ Question Bank.
- Thêm Feature Flag `tse.paperStartV2.enabled` để có thể bật/tắt (mặc định tắt), đảm bảo an toàn tuyệt đối cho luồng legacy.
- Giải quyết lỗi "orphaned case" và mismatched brace do việc merge code không đồng nhất trong `ClientHandler.java`.

## 2. File đã tạo / sửa
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/ExamStartV2Service.java`: Dịch vụ tạo Exam Package V2. Validate password, isActive, assigned paper. Trả về cấu trúc JSON không chứa `answerKey` hay `isCorrect` để bảo mật.
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`: Cập nhật `switch` block để bắt `EXAM_START_REQUEST_V2` và gọi dịch vụ mới. Sửa lỗi syntax ngoặc nhọn `{ }`.
- `docs/secure_exam_tasks_v2.md`: Cập nhật checklist cho Phase 5A.

## 3. Lệnh đã chạy
- `mvn clean install`
- `build_portable.ps1`
- `run_input_test.bat --exam-id 3` (Legacy Regression)

## 4. Kết quả Test
- **Maven Build:** Build thành công, pass toàn bộ 25 bài unit tests. Không còn lỗi cú pháp trong `ClientHandler.java`.
- **Portable Build:** Tạo package thành công với size gọn.
- **Legacy Regression:** Luồng chạy ứng dụng và bật secure desktop hoạt động bình thường, các service volume, brightness, wifi hoạt động trơn tru.

## 5. Rủi ro còn lại
- Feature flag V2 hiện tại đang ẩn (`false` mặc định), nên chưa được test end-to-end với giao diện render giấy thi mới của Secure Exam Child. Phải bật thủ công qua `-Dtse.paperStartV2.enabled=true` khi kiểm tra UI.

## 6. Task tiếp theo đề xuất
- **Phase 5B: Start Request V2 UI Integration & Regression** - Bật feature flag trên client và tích hợp gói đề thi V2 vào giao diện làm bài của Secure Exam Child (nếu nằm trong roadmap phase 5).
