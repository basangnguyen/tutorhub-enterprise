# Báo cáo: Phase 2 — Practice Player v1

**1. Đã đọc những file/tài liệu nào**
- `docs/practice_tab_research_and_roadmap_v2.md`
- `src/main/resources/tse/practice-template.html`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/ExamHtmlTemplateRenderer.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java`

**2. Đã sửa/tạo file nào**
- Sửa lại hoàn toàn (overwrite): `src/main/resources/tse/practice-template.html`.
- Cập nhật: `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java` (Thêm JavaFX `locationProperty()` listener để bắt được link `tutorhub://practice/back`).

**3. Template player mới hoạt động thế nào**
- Giao diện được thiết kế lại gọn gàng, hiển thị **1 câu trên một màn hình**.
- Có thành phần Header chứa tiêu đề đề thi, bộ đếm câu (VD: Câu 1/10) và thanh tiến trình (Progress bar) trực quan.
- Controls điều hướng (Trước, Tiếp, Bỏ qua, Đánh dấu, Nộp bài) được đặt riêng bên dưới. Nút Nộp bài chỉ hiện ở câu cuối cùng (nếu người dùng bấm Bỏ qua ở câu cuối cũng sẽ tự nhảy sang màn Kết quả).
- Tích hợp tính năng làm lại câu sai (Lọc lại những câu chưa làm/bỏ qua/sai và chạy mode Retry) hoặc làm lại toàn bộ.

**4. State JS đang quản lý những gì**
- `currentMode`: Lưu chế độ làm bài ('ALL' hoặc 'WRONG_ONLY').
- `currentData`: Lưu mảng câu hỏi hiện tại đang chạy (có thể là tất cả, hoặc chỉ những câu làm sai).
- `currentIndex`: Chỉ số câu hỏi hiện tại.
- `answers`: Object lưu trữ lựa chọn của người dùng (`originalIndex -> optionIndex`).
- `markedQuestions`: Lưu trạng thái những câu được đánh dấu để review.
- `skippedQuestions`: Lưu trạng thái những câu bị bỏ qua (chưa trả lời).

**5. Feedback đúng/sai hoạt động thế nào**
- Khi người dùng click chọn 1 đáp án, thuộc tính disabled được kích hoạt trên tất cả các đáp án (không cho chọn lại).
- Tùy vào giá trị `isCorrect` mà đáp án vừa chọn sẽ đổi màu viền xanh (đúng) hoặc đỏ (sai).
- Khung Feedback hiện lên ngay phía dưới câu hỏi, hiển thị dấu ✅ Chính xác hoặc ❌ Sai rồi. Đồng thời render giải thích (nếu có từ biến `explanation`).

**6. Result summary gồm gì**
- Giao diện kết thúc bài hiển thị các box thông tin: Số câu đúng, Tỉ lệ chính xác (%), Số câu sai, Số câu bỏ qua.
- Dưới cùng có 3 tùy chọn CTA: 
  - 🔄 Làm lại câu sai (Chỉ hiện nếu có câu sai/bỏ qua)
  - ♻ Làm lại toàn bộ
  - 📋 Quay về danh sách (Kích hoạt URL scheme `tutorhub://practice/back`).

**7. Làm lại câu sai local hoạt động ra sao**
- Nút "Làm lại câu sai" sẽ kích hoạt hàm `startMode('WRONG_ONLY')`.
- State `currentData` được filter lại từ `window.practiceData`, chỉ lấy các câu mà `answers[originalIndex]` không đúng (`isCorrect == false`) hoặc chưa được trả lời (`undefined`).
- Tình trạng `answers` của những câu này được reset. Sau đó, giao diện quay lại `currentIndex = 0` chỉ cho hiển thị tập dữ liệu đã filter. Không gọi lên server.

**8. Có đụng Exam/TSE/Login/Schedule Email không**
- **Hoàn toàn KHÔNG.** Dữ liệu và Renderer cho Exam/TSE được đặt ở `ExamQuestionViewDTO` và gọi `renderExam()`. Luồng Practice này chạy hoàn toàn độc lập ở `PracticeQuestionViewDTO` và `renderPractice()`. Các module Login/Email không liên quan.

**9. Build/test kết quả thế nào**
- Mã nguồn chạy lệnh build `mvn clean compile assembly:single -DskipTests` pass 100%.

**10. Rủi ro còn lại**
- JS local state không được lưu khi đóng WebView/Tab, nên mọi tiến độ tạm thời sẽ biến mất nếu người dùng chưa kịp Nộp bài. Điều này sẽ được xử lý khi làm cơ chế Resume ở Phase 3.
- Dữ liệu `isCorrect` đang được gửi ngầm theo mảng options xuống client để phục vụ practice. Việc này đã được comment kĩ trong Renderer là cấm áp dụng cho Exam.

**11. Có đủ điều kiện sang Phase 3 chưa**
- **Đã đủ điều kiện.** Việc có một Player hoàn chỉnh làm cơ sở vững chắc để lưu Log từng câu trả lời lên server (lưu attempt, progress, resume).
