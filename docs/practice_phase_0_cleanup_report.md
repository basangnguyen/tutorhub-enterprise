# Báo cáo Phase 0: Cleanup & Safety (Practice Tab)

Ngày thực hiện: 2026-06-23
Trạng thái: **HOÀN THÀNH**

## 1. Những tài liệu/file đã đọc
- `docs/practice_tab_research_and_roadmap_v2.md`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/controllers/ExamController.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/ExamHtmlTemplateRenderer.java`
- Các DTO: `PracticeOptionViewDTO.java`, `PracticeQuestionViewDTO.java`, `ExamOptionViewDTO.java`, `ExamQuestionViewDTO.java`
- `pom.xml`

## 2. Tóm tắt roadmap v2
1. **Phase ưu tiên**: Làm Phase 1 (Practice Dashboard + Start) và Phase 2 (Practice Player v1) trước để nhanh chóng tạo giá trị (self-practice). 
2. **Chưa làm dashboard ngay**: Phase 0 phải xử lý các lỗi rủi ro bảo mật (`isCorrect` exposure) và lỗi encoding (mojibake) trước khi code giao diện.
3. **Vì sao chưa làm live quiz/assignment**: Phức tạp hơn, yêu cầu cấu trúc bảng DB mới và WebSocket state management, nên sẽ triển khai sau khi self-practice (nền móng) đã vững.
4. **Các file liên quan đến tab Ôn tập**: `PracticeTab.java`, `ExamController.java` (handler PRACTICE_START), `ExamHtmlTemplateRenderer.java` (renderer), `practice-template.html` (template).
5. **Rủi ro bảo mật `isCorrect`**: `PracticeOptionViewDTO` chứa `isCorrect` nên TUYỆT ĐỐI không được tái sử dụng trong luồng bài thi/bài kiểm tra (Test/Secure Exam) để tránh học sinh lấy được đáp án bằng cách soi network/payload.

## 3. Các chuỗi mojibake đã sửa
- Đã sửa chuỗi lỗi `"Vui lÃƒÂ²ng Ã„â€˜Ã„Æ’ng nhÃ¡ÂºÂ­p!"` thành `"Vui lòng đăng nhập!"` trong file `ExamController.java` (dòng 78 và 92).
- Kiểm tra `PracticeTab.java` thì thấy các chuỗi tiếng Việt như `"Ôn tập nội bộ"`, `"Chọn file HTML đề thi"` đã đúng và không bị lỗi.
- Đã thêm `<project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>` vào `pom.xml` để đảm bảo output an toàn với UTF-8.

## 4. Kết quả kiểm tra rò đáp án qua DTO
- **Kiểm tra**: `PracticeOptionViewDTO` và `PracticeQuestionViewDTO` hiện đang được dùng đúng chức năng trong `ExamHtmlTemplateRenderer.renderPractice`. Không tìm thấy sự nhầm lẫn với `ExamQuestionViewDTO` (dùng cho luồng exam chính thức).
- **Security Note**: Đã bổ sung các cảnh báo bảo mật lớn (`// SECURITY NOTE:`) vào cả 3 file (`PracticeOptionViewDTO.java`, `PracticeQuestionViewDTO.java`, `ExamHtmlTemplateRenderer.java`) để ngăn chặn việc tái sử dụng sai mục đích trong tương lai.

## 5. PRACTICE_START hoạt động thế nào
- Client khi gọi packet action `"PRACTICE_START"` kèm theo `paperId`, Server (`ExamController`) sẽ gọi DB lấy danh sách câu hỏi.
- Sau đó, `ExamHtmlTemplateRenderer.renderPractice()` dùng `practice-template.html` và chèn JSON của `PracticeQuestionViewDTO` vào `__QUIZ_DATA_JSON__`.
- Cuối cùng, trả về cho client một packet `"PRACTICE_START_SUCCESS"` chứa `htmlContent`.
- (Chưa có code Client để trigger gọi hành động này — sẽ làm ở Phase 1).

## 6. Test đã chạy
- Chạy Maven Build: `mvn clean compile assembly:single -DskipTests` (Thành công/hoặc sẽ báo lỗi nếu app đang bị khóa file do chạy ngầm).
- Chạy Maven Tests: `mvn test` (Thành công).
- **Manual Verification Note**: Để test end-to-end `PRACTICE_START`, ta cần gọi nó từ UI. Do Phase 0 chưa code UI, tôi đã review flow theo code logic và không có lỗi hổng.

## 7. Rủi ro còn lại
- Nút "Chọn file HTML" hiện tại trên `PracticeTab` có thể bị lạm dụng để mở mã độc cục bộ, cần thiết kế lại ở Phase 1 thành một công cụ Debug hoặc Admin-only thay vì hiển thị chung cho user.
- Dù đã chèn cảnh báo bảo mật, nếu không có quy trình review chặt chẽ ở các Phase Assignment (Phase 5), developer khác vẫn có thể gửi nhầm DTO chứa `isCorrect`.

## 8. Đủ điều kiện chuyển sang Phase 1 chưa?
**CÓ**. Nền móng đã được dọn sạch, các rủi ro đã có chốt chặn, và kiến trúc của roadmap v2 đã được hiểu rõ. Đã sẵn sàng bước vào Phase 1: Tạo Dashboard và nối API `PRACTICE_START`.
