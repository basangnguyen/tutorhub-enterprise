# Báo cáo: Rewrite PracticeTab với HTML/JS (quiz-practice-template.html)

## 1. Mục tiêu
- Loại bỏ hoàn toàn giao diện luyện thi Swing (cũ, khó nâng cấp).
- Tích hợp giao diện `quiz-practice-template.html` đẹp, hiện đại vào hệ thống.
- Chuyển đổi cơ chế render đề thi từ "render HTML tại server" sang "trả về dữ liệu JSON" và "render bằng JavaScript tại client".
- Đảm bảo cơ chế bảo mật đáp án (`show_answers_policy`) được thực thi chặt chẽ ở backend (không gửi `isCorrect` hay `explanation` nếu policy chặn).
- Nộp bài theo cơ chế ACK-driven: JavaScript chỉ hiển thị điểm và đáp án sau khi nhận được ACK từ máy chủ.
- Hỗ trợ đầy đủ các luồng: Làm đề chung (Free Practice), Làm bài tập về nhà (Assignment), và Luyện câu sai (Wrong Questions).

## 2. Các thay đổi chính

### 2.1 Backend (Java Server)
- **`ExamHtmlTemplateRenderer.java`**: Thay vì gen ra một khối HTML lớn, class này được refactor để sinh ra DTO cấu trúc chuẩn (JSON) bao gồm danh sách câu hỏi, options, có/không có đáp án phụ thuộc vào `showAnswersPolicy` và `status` của attempt.
- **`PracticeAttemptService.java`**: Bổ sung logic kiểm tra `showAnswersPolicy` (`IMMEDIATELY`, `AFTER_SUBMIT`, `NEVER`). Nếu là `NEVER` hoặc chưa nộp bài đối với `AFTER_SUBMIT`, trường `isCorrect` và `explanation` sẽ bị xóa (đặt thành `false`/`null`).
- **`ExamController.java`**: Chỉnh sửa các handler như `PRACTICE_START_SUCCESS`, `PRACTICE_SUBMIT_SUCCESS` để đính kèm payload JSON cấu trúc.

### 2.2 Client (JavaFX + Swing Bridge)
- **`PracticeTab.java`**: Viết lại toàn bộ, thay thế các panel Swing bằng một `JFXPanel` chứa `WebView`.
- **`JavaBridge`**: Lớp trung gian được nhúng vào `window.JavaBridge` của trình duyệt. 
  - Cung cấp các hàm cho JavaScript gọi xuống Java: `loadMenu`, `startPractice`, `startAssignment`, `saveAnswer`, `submitQuiz`, `showAssignDialog`.
  - Cung cấp các hàm cho Java gọi lên JavaScript: `TutorHubPractice.loadMenu`, `TutorHubPractice.loadQuiz`, `TutorHubPractice.onSaveAck`, `TutorHubPractice.onSubmitAck`.

### 2.3 Giao diện HTML/JS (`quiz-practice-template.html`)
- Xóa các biến cứng `DECKS`, `TITLES` tĩnh.
- Khởi tạo namespace `window.TutorHubPractice`.
- **Menu:** Hàm `loadMenu` động sinh ra danh sách "Đề chung" hoặc "Bài tập về nhà". Nếu đề có `weakCount > 0`, nút "Luyện câu sai" sẽ xuất hiện.
- **Header Navigation:** Bổ sung các nút bấm "Đề chung", "Bài tập về nhà", và "Giao bài (Giáo viên)".
- **Nộp bài:** Hàm `grade()` gọi `window.submitQuiz()` và đợi `onSubmitAck()` từ backend mới hiển thị kết quả qua `window.showResult()`.
- **Chấm điểm UI:** Dựa trên mảng `correctIndices` mà server gửi về. Nếu mảng rỗng (do policy), JS sẽ không hiện thị đúng/sai, chỉ khóa lựa chọn.

## 3. Kiến trúc Luồng Dữ liệu (ACK-Driven)

1. **Tải danh sách:** `PracticeTab.java` khởi tạo -> Gửi `PRACTICE_LIST` -> Nhận `PRACTICE_LIST_SUCCESS` -> Gọi `window.TutorHubPractice.loadMenu(...)`.
2. **Bắt đầu làm bài:** User click "Làm đề chung" -> Gọi `window.JavaBridge.startPractice(paperId)` -> Gửi request.
3. **Hiển thị đề thi:** Server tạo `attempt`, check policy, trả về `quizData` JSON -> `PracticeTab` nhận `PRACTICE_START_SUCCESS` -> Gọi `window.TutorHubPractice.loadQuiz(quizData)`. JS render câu hỏi, thời gian.
4. **Lưu đáp án:** Khi user chọn đáp án, JS gọi `window.saveAnswer()` -> `JavaBridge.saveAnswer()` -> Server ghi vào DB. (Không cần khóa giao diện chờ lưu).
5. **Nộp bài:** User bấm nộp bài -> JS đổi nút thành "Đang nộp..." và gọi `window.submitQuiz()`.
6. **Chấm bài (Backend):** Server chấm bài, tính điểm, tạo bản ghi `showAnswersPolicy` đầy đủ (nếu được phép) -> Gửi `PRACTICE_SUBMIT_SUCCESS`.
7. **Hiển thị kết quả:** Java gọi `TutorHubPractice.onSubmitAck()` với payload -> JS parse và gọi `window.showResult()`, hiển thị đúng/sai chi tiết và giải thích.

## 4. Tình trạng
- Phase A (Xóa parse HTML): **Hoàn thành**
- Phase B (Tạo Bridge UI): **Hoàn thành**
- Phase C (ACK-Driven Nộp bài): **Hoàn thành**
- Phase D (Assign & Luyện câu sai): **Hoàn thành**

## 5. Các bước tiếp theo
- Triển khai chức năng thống kê (Analytics) cho giáo viên.
- Viết unit tests kiểm tra `showAnswersPolicy` chặt chẽ hơn.
