# Nghiên cứu tab Ôn tập và roadmap phát triển

Ngày tạo: 2026-06-22

## 1. Mục tiêu tài liệu

Tài liệu này tổng hợp nghiên cứu hiện trạng tab **Ôn tập** trong TutorHub Enterprise, đánh giá ưu/nhược điểm, đối chiếu với các nền tảng lớn như Quizizz/Wayground, Kahoot và Azota, sau đó đề xuất roadmap phát triển tab Ôn tập theo hướng chuyên nghiệp, có thể mở rộng lâu dài.

Phạm vi nghiên cứu tập trung vào:

- Giao diện và trải nghiệm tab Ôn tập hiện tại.
- Logic xử lý câu hỏi, đề, import HTML quiz và render bài ôn tập.
- Các hướng phát triển chức năng tương tự Quizizz, Kahoot, Azota.
- Kiến trúc phù hợp với app Java desktop hiện tại.

## 2. File đã kiểm tra

Các file chính liên quan:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/controllers/ExamController.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/ExamHtmlTemplateRenderer.java`
- `src/main/resources/tse/practice-template.html`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/services/HtmlQuizDataParser.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/HtmlQuizImportService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/exam/PracticeQuestionViewDTO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/exam/PracticeOptionViewDTO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/exam/ParsedQuizQuestion.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ExamTab.java`
- `docs/quiz.html`
- `docs/vsl.html`

## 3. Hiện trạng tab Ôn tập

### 3.1. Giao diện hiện tại

`PracticeTab.java` hiện là một tab Swing đơn giản, gồm:

- Header với tiêu đề "Ôn tập nội bộ".
- Nút "Chọn file HTML đề thi".
- Vùng hiển thị nội dung bằng JavaFX `WebView` nhúng trong Swing qua `JFXPanel`.
- Fallback bằng `JEditorPane` nếu JavaFX không khả dụng.

Hiện tại tab này chủ yếu đóng vai trò trình mở file HTML local. Người dùng phải chọn một file `.html` từ máy để bắt đầu ôn tập.

### 3.2. Logic server đã có nhưng UI chưa dùng

Server đã có action:

```text
PRACTICE_START
```

Trong `ExamController`, API này:

1. Kiểm tra người dùng đã đăng nhập.
2. Nhận `paperId`.
3. Lấy `ExamPaper` từ DB.
4. Lấy danh sách câu hỏi trong đề.
5. Render HTML bằng `ExamHtmlTemplateRenderer.renderPractice(...)`.
6. Trả về `PRACTICE_START_SUCCESS` với `data.htmlContent`.

Tuy nhiên `PracticeTab.java` chưa gọi API này. Vì vậy đang tồn tại hai phần rời nhau:

- Client tab Ôn tập: mở file HTML local.
- Server practice API: render bài ôn tập từ đề trong DB.

Cần nối hai phần này lại để tab Ôn tập trở thành module thật.

### 3.3. Template ôn tập hiện tại

`practice-template.html` hiện có:

- `window.practiceData = __QUIZ_DATA_JSON__`
- Render toàn bộ câu hỏi lên một trang.
- Tìm kiếm câu hỏi.
- Shuffle đáp án.
- Chọn đáp án và check đúng/sai ngay lập tức.
- Highlight đáp án đúng/sai.
- Hiển thị giải thích nếu có.

Template này phù hợp làm prototype nhưng chưa đủ để thành trải nghiệm giống Quizizz/Kahoot/Azota.

### 3.4. Import HTML quiz

`HtmlQuizDataParser` có hướng làm khá tốt:

- Đọc file HTML dạng text.
- Không execute JavaScript.
- Tìm mảng `quizData`.
- Convert object literal sang JSON.
- Parse bằng Gson.
- Validate từng câu.
- Giới hạn file 10 MB.
- Giới hạn 500 câu hỏi.

`HtmlQuizImportService` có:

- Import vào `question_banks`.
- Insert câu hỏi và option.
- Có transaction DB.
- Có rollback khi lỗi.
- Có thể tạo `exam_papers` nếu được yêu cầu.

Nhược điểm hiện tại:

- Chủ yếu hỗ trợ MCQ.
- Chưa hỗ trợ Word/PDF/Excel.
- Chưa có mapping tag, môn học, lớp, độ khó, kỹ năng.
- Nhiều thông báo tiếng Việt đang bị lỗi encoding mojibake.

## 4. Ưu điểm và nhược điểm

| Nhóm | Ưu điểm | Nhược điểm |
|---|---|---|
| UI tab Ôn tập | Đơn giản, dễ thử nghiệm, có WebView để render HTML tương tác | Chưa có dashboard, chưa có danh sách bộ ôn tập, chưa có trạng thái học tập |
| Logic server | Đã có `PRACTICE_START`, có renderer riêng cho practice | UI chưa gọi API, chưa có lưu tiến độ, chưa có attempt/session |
| Template HTML | Có chọn đáp án, check đúng/sai, giải thích, search | Render tất cả câu một lúc, chưa có tiến độ, chưa có kết quả cuối bài, UI còn sơ khai |
| Import quiz | Parser không chạy JS, có validate, có transaction DB | Chỉ phù hợp format `quizData`, chưa hỗ trợ import phổ biến như Word/PDF/Excel |
| Bảo mật | Practice có thể cho xem đáp án đúng sau khi trả lời | `isCorrect` được gửi xuống client, cần tách rõ practice/test/exam để tránh rò đáp án |
| Khả năng mở rộng | Đã có model câu hỏi, đề, option, bank | Thiếu model riêng cho practice attempt, assignment, report, live room |
| Trải nghiệm người dùng | Có nền móng để luyện tập nhanh | Chưa có gamification, điểm số, streak, leaderboard, redo wrong, homework |
| Code quality | Có tách parser/service/renderer bước đầu | Chuỗi tiếng Việt bị mojibake, một số logic UI/server còn nối thủ công |

## 5. Bài học từ Quizizz, Kahoot và Azota

### 5.1. Quizizz / Wayground

Các điểm nên học:

- Có live mode và assigned/homework mode.
- Có session settings: shuffle, show answers, attempts, power-ups, timer, anti-cheating.
- Có report theo câu hỏi, học sinh, độ chính xác.
- Có mastery goal, cho phép học sinh luyện lại để đạt mức thành thạo.
- Có chế độ tự học và làm lại câu sai.

Nguồn tham khảo:

- Wayground session settings: `https://help.wayground.com/support/solutions/articles/158000404930-navigate-session-settings`
- Wayground Mastery Peak: `https://help.wayground.com/support/solutions/articles/158000404926-host-an-assessment-quiz-in-mastery-peak-mode`
- Wayground reports: `https://help.wayground.com/support/solutions/articles/158000404058-reports-on-wayground`

### 5.2. Kahoot

Các điểm nên học:

- Live game với mã PIN.
- Host control: giáo viên điều khiển câu hỏi.
- Leaderboard, podium, điểm theo tốc độ và độ chính xác.
- Assigned kahoot/self-paced mode cho học sinh làm không đồng bộ.
- Report sau phiên chơi.

Nguồn tham khảo:

- Assign Kahoot: `https://support.kahoot.com/hc/en-us/articles/360039411334-How-to-assign-a-kahoot-in-web-platform`
- Live game tips: `https://support.kahoot.com/hc/en-us/articles/360039900153-Tips-for-hosting-a-live-game`
- Kahoot reports: `https://support.kahoot.com/hc/en-us/articles/360035063054-Kahoot-quiz-reports`

### 5.3. Azota

Các điểm nên học:

- Tạo đề thi/giao bài nhanh cho giáo viên.
- Import đề từ file phổ biến.
- Chấm điểm tự động.
- Giao bài cho lớp/học sinh.
- Thống kê kết quả.
- Quản lý ngân hàng câu hỏi và đề.

Nguồn tham khảo:

- Hướng dẫn đề thi Azota: `https://docs.azota.vn/docs/huong-dan-su-dung/de-thi/`
- Tạo và giao bài tập Azota: `https://docs.azota.vn/docs/huong-dan-su-dung/bai-tap/tao-va-giao-bai-tap-chi-co-mo-ta/`

### 5.4. Lưu ý về mã nguồn

Quizizz, Kahoot và Azota là các nền tảng thương mại. Không có mã nguồn chính thức công khai đáng tin cậy để tham khảo trực tiếp. Vì vậy không nên sao chép UI, asset, icon, thuật toán hoặc tài nguyên nhận diện của họ.

Cách đúng là học luồng sản phẩm, mô hình tính năng và trải nghiệm người dùng, sau đó thiết kế lại theo phong cách TutorHub.

## 6. So sánh cách làm hiện tại và hướng giống các nền tảng lớn

| Khía cạnh | Hiện tại TutorHub | Hướng nên phát triển |
|---|---|---|
| Bắt đầu ôn tập | Chọn file HTML local | Chọn bộ ôn tập/đề từ server |
| Nguồn câu hỏi | File HTML hoặc đề DB chưa nối UI | Ngân hàng câu hỏi, đề, bài được giao, lớp học |
| Trải nghiệm làm bài | Tất cả câu trên một trang | Một câu/màn hình hoặc section rõ ràng |
| Feedback | Check đúng/sai ngay | Tùy mode: luyện tập hiển thị ngay, test chỉ hiện sau khi nộp |
| Lưu kết quả | Chưa có | Lưu attempt, answer, score, time, progress |
| Báo cáo | Chưa có | Report theo học sinh, câu hỏi, lớp, bài giao |
| Live mode | Chưa có | Host room, join code, realtime answer, leaderboard |
| Import | HTML `quizData` | HTML + Word + PDF + Excel + manual builder |
| Gamification | Chưa có | Điểm, streak, badge, leaderboard, mastery |
| Tìm kiếm | Search trong template | Search bộ đề, câu hỏi, tag, môn, lớp, độ khó |

## 7. Roadmap phát triển

### Phase 1: Biến tab Ôn tập thành module thật

Mục tiêu:

- Bỏ phụ thuộc vào việc người dùng tự chọn file HTML local.
- Hiển thị danh sách bộ đề/bộ ôn tập từ server.
- Cho phép bấm "Bắt đầu ôn tập" để gọi `PRACTICE_START`.

Việc cần làm:

1. Sửa `PracticeTab.java` thành layout có:
   - Header.
   - Search.
   - Bộ lọc: tất cả, của tôi, đã giao, gần đây.
   - Danh sách bộ ôn tập dạng card/table.
   - Vùng player.
2. Thêm hoặc dùng lại API:
   - `EXAM_PAPER_LIST`
   - hoặc tạo mới `PRACTICE_LIST`.
3. Khi chọn một bộ ôn tập:
   - Gửi packet `PRACTICE_START` với `paperId`.
   - Nhận `htmlContent`.
   - Load vào WebView bằng `webEngine.loadContent(htmlContent)`.
4. Sửa encoding tiếng Việt ở các file liên quan.

Kết quả mong muốn:

- Người dùng mở tab Ôn tập là thấy danh sách bài ôn.
- Không cần chọn file HTML thủ công.
- Có thể bắt đầu ôn tập từ dữ liệu thật trong DB.

### Phase 2: Practice Player giống Quizizz

Mục tiêu:

- Tạo trải nghiệm luyện tập có tiến độ, feedback và kết quả rõ ràng.

Chức năng:

- Một câu trên một màn hình.
- Nút câu trước/câu sau.
- Thanh tiến độ.
- Đánh dấu câu cần xem lại.
- Chế độ luyện nhanh.
- Chế độ luyện câu sai.
- Chế độ luyện theo chủ đề/tag.
- Chế độ random câu hỏi.
- Hiển thị giải thích sau khi trả lời.
- Màn kết quả cuối bài.

Điểm cần lưu ý:

- Nếu là practice mode, có thể gửi `isCorrect` xuống client.
- Nếu là test/assignment mode, không gửi đáp án đúng trước khi nộp.
- Cần tách DTO:
  - `PracticeQuestionViewDTO`
  - `TestQuestionViewDTO`
  - `ExamQuestionViewDTO`

### Phase 3: Lưu attempt, tiến độ và report

Mục tiêu:

- Biến hoạt động ôn tập thành dữ liệu có thể theo dõi.

Đề xuất bảng DB:

```text
practice_attempts
- id
- user_id
- paper_id
- assignment_id
- mode
- started_at
- submitted_at
- score
- correct_count
- wrong_count
- skipped_count
- duration_seconds
- status

practice_answers
- id
- attempt_id
- question_id
- selected_option_id
- answer_text
- is_correct
- answered_at
- time_spent_seconds

user_question_stats
- id
- user_id
- question_id
- total_attempts
- correct_attempts
- wrong_attempts
- last_attempt_at
- mastery_level
```

Chức năng:

- Lưu tự động sau mỗi câu.
- Resume bài đang làm.
- Xem lịch sử ôn tập.
- Xem câu hay sai.
- Giáo viên xem báo cáo theo lớp/học sinh.

### Phase 4: Giao bài kiểu Azota / assigned Kahoot

Mục tiêu:

- Giáo viên có thể giao bài ôn tập cho lớp.

Đề xuất bảng:

```text
practice_assignments
- id
- teacher_id
- class_id
- paper_id
- title
- due_at
- max_attempts
- show_answers_policy
- shuffle_questions
- shuffle_options
- status
```

Chức năng:

- Tạo bài ôn tập được giao.
- Giao cho lớp hoặc nhóm học sinh.
- Học sinh thấy bài được giao trong tab Ôn tập.
- Giáo viên theo dõi ai đã làm/chưa làm.
- Báo cáo điểm, thời gian, câu sai nhiều.

### Phase 5: Live Quiz kiểu Kahoot

Mục tiêu:

- Tạo chế độ quiz realtime cho lớp học.

Đề xuất bảng:

```text
practice_live_rooms
- id
- host_user_id
- paper_id
- room_code
- status
- current_question_index
- started_at
- ended_at

practice_live_players
- id
- room_id
- user_id
- display_name
- score
- joined_at

practice_live_events
- id
- room_id
- event_type
- payload_json
- created_at
```

Chức năng:

- Giáo viên tạo phòng live.
- Sinh mã tham gia.
- Học sinh nhập code để join.
- Realtime bằng WebSocket:
  - lobby.
  - start.
  - next question.
  - submit answer.
  - leaderboard.
  - finish.
- Có podium cuối phiên.

Không nên làm phase này trước khi Phase 1-3 ổn định.

### Phase 6: Nâng cao

Chức năng nâng cao:

- Spaced repetition.
- Mastery goal.
- Adaptive practice.
- AI tạo câu hỏi/giải thích.
- Import Word/PDF/Excel.
- Question quality workflow: draft, review, approved.
- Multimedia question: ảnh, audio, video.
- Chống gian lận nhẹ cho bài giao.

## 8. Kiến trúc đề xuất

### 8.1. Client Java desktop

Nên tách UI thành các lớp:

```text
PracticeTab
PracticeDashboardPanel
PracticeSetCard
PracticePlayerPanel
PracticeResultPanel
PracticeReportPanel
PracticeAssignmentPanel
```

Vai trò:

- `PracticeTab`: container chính.
- `PracticeDashboardPanel`: danh sách bộ ôn tập.
- `PracticePlayerPanel`: làm bài/luyện câu hỏi.
- `PracticeResultPanel`: kết quả cuối lượt.
- `PracticeReportPanel`: báo cáo.
- `PracticeAssignmentPanel`: bài được giao.

WebView vẫn có thể dùng cho nội dung giàu hình ảnh, nhưng không nên để toàn bộ logic sản phẩm nằm trong HTML.

### 8.2. Server API

Đề xuất action packet:

```text
PRACTICE_LIST
PRACTICE_START
PRACTICE_SAVE_PROGRESS
PRACTICE_SUBMIT_ATTEMPT
PRACTICE_ATTEMPT_HISTORY
PRACTICE_REPORT
PRACTICE_ASSIGNMENT_CREATE
PRACTICE_ASSIGNMENT_LIST
PRACTICE_LIVE_CREATE
PRACTICE_LIVE_JOIN
PRACTICE_LIVE_SUBMIT_ANSWER
PRACTICE_LIVE_LEADERBOARD
```

### 8.3. Service layer

Đề xuất service:

```text
PracticeService
PracticeAttemptService
PracticeAssignmentService
PracticeReportService
PracticeLiveService
QuestionImportService
QuestionAnalyticsService
```

### 8.4. DTO

Cần tách DTO theo mode:

```text
PracticeQuestionDTO
PracticeAnswerFeedbackDTO
PracticeAttemptDTO
PracticeResultDTO
PracticeAssignmentDTO
PracticeReportDTO
LiveRoomDTO
LiveLeaderboardDTO
```

Lưu ý quan trọng:

- Practice mode có thể gửi đáp án đúng sau khi trả lời.
- Test/assignment mode có thể chỉ gửi đáp án sau khi submit, tùy setting.
- Exam/secure mode tuyệt đối không gửi `isCorrect` xuống client trước khi nộp.

## 9. Rủi ro kỹ thuật

### 9.1. Rò đáp án

Hiện `PracticeOptionViewDTO` có field:

```java
public boolean isCorrect;
```

Điều này phù hợp với practice mode nhưng nguy hiểm nếu tái dùng nhầm cho test/exam mode.

Khuyến nghị:

- Tách DTO theo mode.
- Server quyết định mode.
- Không để client tự quyết định có được xem đáp án hay không.

### 9.2. WebView chạy HTML local

Việc cho người dùng mở file HTML local bằng WebView có thể có rủi ro nếu HTML chứa JavaScript không tin cậy.

Khuyến nghị:

- Giữ chức năng import HTML nhưng không trực tiếp chạy file tùy ý.
- Parse dữ liệu bằng `HtmlQuizDataParser`.
- Render lại bằng template nội bộ đã kiểm soát.

### 9.3. Encoding tiếng Việt

Nhiều chuỗi hiện bị mojibake như:

```text
Ã”n táº­p
Chá»n file
KhÃ´ng thá»ƒ
```

Khuyến nghị:

- Chuẩn hóa source encoding UTF-8.
- Kiểm tra Maven compiler/resource encoding.
- Sửa lại các chuỗi UI/server message trước khi mở rộng.

### 9.4. Hiệu năng khi render nhiều câu

Template hiện render toàn bộ câu hỏi một lần.

Rủi ro:

- Đề lớn 200-500 câu sẽ chậm.
- WebView có thể lag.

Khuyến nghị:

- Render từng câu hoặc paging.
- Virtualize danh sách câu.
- Tách dữ liệu và UI state.

## 10. Thứ tự ưu tiên triển khai

Nên làm theo thứ tự:

1. Sửa encoding tiếng Việt trong các file ôn tập/exam liên quan.
2. Tạo `PRACTICE_LIST` hoặc dùng `EXAM_PAPER_LIST` để hiển thị bộ ôn tập.
3. Sửa `PracticeTab` thành dashboard danh sách bộ ôn tập.
4. Gọi `PRACTICE_START` từ UI và load HTML server trả về.
5. Nâng cấp `practice-template.html` thành player có tiến độ và result summary.
6. Thêm DB `practice_attempts` và `practice_answers`.
7. Thêm lưu tiến độ và nộp kết quả ôn tập.
8. Thêm màn report cá nhân.
9. Thêm assignment cho giáo viên.
10. Thêm report lớp học.
11. Sau cùng mới làm live quiz realtime.

## 11. Kế hoạch MVP cụ thể

### MVP 1: Dashboard + Start Practice

File dự kiến sửa/tạo:

- `PracticeTab.java`
- `ExamController.java`
- `PracticeService.java` hoặc mở rộng service hiện có
- `practice-template.html`
- DTO cho danh sách bộ ôn tập

Chức năng:

- Load danh sách `ExamPaper`.
- Hiển thị card/table.
- Tìm kiếm theo tên.
- Bấm "Ôn tập".
- Gọi `PRACTICE_START`.
- Load HTML vào WebView.

### MVP 2: Result Summary

Chức năng:

- Đếm số câu đúng/sai.
- Hiển thị phần trăm.
- Danh sách câu sai.
- Nút làm lại câu sai.
- Nút quay về danh sách.

### MVP 3: Attempt Persistence

Chức năng:

- Tạo attempt khi bắt đầu.
- Lưu câu trả lời.
- Submit attempt.
- Xem lịch sử.

### MVP 4: Teacher Assignment

Chức năng:

- Giáo viên chọn bộ đề.
- Giao cho lớp.
- Học sinh thấy bài được giao.
- Giáo viên xem report.

## 12. Định hướng UI/UX

Tab Ôn tập nên có bố cục giống các nền tảng lớn nhưng giữ phong cách TutorHub:

### Dashboard

- Header: "Ôn tập"
- Subtitle: "Luyện tập, làm bài được giao và theo dõi tiến độ"
- Search bar.
- Filter chips:
  - Tất cả
  - Được giao
  - Của tôi
  - Gần đây
  - Câu sai
- Card bộ ôn tập:
  - Tên bộ đề.
  - Số câu.
  - Môn/lớp/tag.
  - Lần làm gần nhất.
  - Độ chính xác.
  - CTA: "Bắt đầu ôn tập".

### Player

- Một câu mỗi màn hình.
- Sidebar mini hoặc top progress.
- Nút:
  - Trước.
  - Tiếp.
  - Bỏ qua.
  - Đánh dấu.
  - Nộp/lưu kết quả.
- Feedback rõ ràng sau khi trả lời.
- Giải thích nằm trong card riêng.

### Result

- Điểm.
- Độ chính xác.
- Thời gian.
- Câu đúng/sai/bỏ qua.
- Câu cần ôn lại.
- CTA:
  - Làm lại câu sai.
  - Làm lại toàn bộ.
  - Quay về danh sách.

## 13. Kết luận

Tab Ôn tập hiện tại đã có nền móng kỹ thuật nhưng chưa phải một tính năng hoàn chỉnh. Điểm mạnh là dự án đã có:

- Model câu hỏi/đề.
- Import HTML quiz.
- Renderer practice.
- API `PRACTICE_START`.
- WebView để render nội dung tương tác.

Điểm yếu lớn nhất là:

- UI chưa kết nối server.
- Chưa có attempt/progress/report.
- Chưa có assignment.
- Chưa có live quiz.
- Chưa tách rõ practice/test/exam để bảo vệ đáp án.

Bước đúng nhất tiếp theo là làm **Phase 1: Dashboard + Start Practice từ dữ liệu server**, sau đó mới mở rộng sang result summary, lưu tiến độ, giao bài và live quiz.

