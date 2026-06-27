# Nghiên cứu tab Ôn tập v2 — Kế hoạch phát triển toàn diện

Ngày tạo: 2026-06-23  
Phiên bản: v2 (thay thế `practice_tab_research_and_roadmap.md` v1)  
Tác giả: Antigravity AI Agent  
Trạng thái: **CHỜ DUYỆT — CHƯA CODE**

---

## 1. Executive Summary

### Tab Ôn tập hiện tại đang ở mức nào?

Tab Ôn tập (`PracticeTab.java`) hiện là một **trình mở file HTML local** — người dùng phải tự chọn file `.html` từ máy tính, sau đó WebView load trực tiếp file đó. Không có kết nối server, không có danh sách bài ôn, không có lưu tiến độ, không có báo cáo.

Server đã có handler `PRACTICE_START` hoạt động được (nhận `paperId` → lấy câu hỏi từ DB → render HTML bằng `practice-template.html` → trả về `htmlContent`), nhưng **client chưa bao giờ gọi API này**.

Hai phần tồn tại rời rạc: client mở file local, server render từ DB. Cần nối lại.

### Mục tiêu cuối cùng của tab Ôn tập

Biến tab Ôn tập thành một **module luyện tập hoàn chỉnh** kiểu Quizizz/Kahoot/Azota nhưng phù hợp kiến trúc Java desktop:

- Học sinh tự luyện tập, làm lại câu sai, theo dõi tiến độ.
- Giáo viên giao bài, xem báo cáo, quản lý ngân hàng đề.
- Sau cùng: live quiz realtime cho lớp học.

### Nên triển khai theo hướng nào trước?

**Self-practice trước, assignment sau, live quiz cuối cùng.** Lý do:

1. Self-practice chỉ cần nối client với server API đã có — ROI cao nhất.
2. Assignment cần thêm bảng DB và luồng giao bài — phức tạp hơn.
3. Live quiz yêu cầu realtime state management trên WebSocket — phức tạp nhất, dễ gây bug nhất.

### Vì sao chưa nên làm live quiz ngay?

- Live quiz yêu cầu **đồng bộ trạng thái realtime** giữa host và N player qua WebSocket.
- Hệ thống WebSocket hiện tại (`TutorServer`) xử lý packet tuần tự trong thread pool, chưa có room/channel abstraction.
- Cần hoàn thiện practice player, attempt persistence, và result trước — đây là nền móng cho live quiz.
- Kahoot mất nhiều năm để polish live quiz. Quizizz cũng bắt đầu từ self-paced trước rồi mới thêm live.

---

## 2. Current State Audit

### 2.1. UI hiện tại của PracticeTab

| Thành phần | File | Mô tả | Trạng thái |
|---|---|---|---|
| Container | `PracticeTab.java` | JPanel với BorderLayout, header + browserContainer | Hoạt động |
| Header | Trong `PracticeTab` | Tiêu đề "Ôn tập nội bộ" + nút "Chọn file HTML đề thi" | Hoạt động |
| WebView | JFXPanel + WebEngine | Load file HTML local | Hoạt động |
| Fallback | JEditorPane | Khi JavaFX không khả dụng | Hoạt động |
| Dashboard | Không có | — | **Thiếu** |
| Search/Filter | Không có | — | **Thiếu** |
| Result summary | Không có | — | **Thiếu** |
| Kết nối server | Không có | PracticeTab không gọi bất kỳ action nào | **Thiếu** |

### 2.2. Server/API hiện có

| Action | File | Mô tả | Trạng thái |
|---|---|---|---|
| `PRACTICE_START` | `ExamController.java:656-710` | Nhận `paperId`, lấy câu hỏi từ `exam_paper_questions` → render HTML → trả `PRACTICE_START_SUCCESS` | **Hoạt động**, chưa có client gọi |
| `EXAM_PAPER_LIST` | `ExamController.java:105-111` | Liệt kê `exam_papers` theo `creator_id` | **Hoạt động** |
| `IMPORT_HTML_QUIZ` | `ExamController.java:622-653` | Parse HTML quiz → insert vào `question_banks` + `questions` + `question_options`, tùy chọn tạo `exam_papers` | **Hoạt động** |
| `PRACTICE_LIST` | Chưa có | Danh sách bộ đề cho practice (có thể dùng `EXAM_PAPER_LIST` hoặc tạo mới) | **Thiếu** |
| `PRACTICE_SAVE_PROGRESS` | Chưa có | Lưu tiến độ ôn tập | **Thiếu** |
| `PRACTICE_SUBMIT_ATTEMPT` | Chưa có | Nộp kết quả ôn tập | **Thiếu** |
| `PRACTICE_WRONG_QUESTIONS` | Chưa có | Lấy danh sách câu sai | **Thiếu** |

### 2.3. Template HTML hiện có

| File | Mô tả | Vấn đề |
|---|---|---|
| `practice-template.html` | Render tất cả câu trên 1 trang, search, shuffle, check đúng/sai ngay, hiển thị giải thích | Render cả đề 1 lúc (chậm với đề lớn), `isCorrect` gửi xuống client (chấp nhận được cho practice, nguy hiểm nếu tái dùng cho test/exam) |
| `exam-template.html` | Template cho bài thi chính thức, không gửi `isCorrect` | Dùng cho exam mode, không nên dùng cho practice |

### 2.4. Import HTML quiz hiện có

| Component | Mô tả | Trạng thái |
|---|---|---|
| `HtmlQuizDataParser` | Parse `quizData` từ HTML text, không execute JS, validate, giới hạn 10MB/500 câu | Hoạt động tốt |
| `HtmlQuizImportService` | Import vào `question_banks` → `questions` → `question_options`, có transaction/rollback, tùy chọn tạo `exam_papers` | Hoạt động tốt |

### 2.5. Những gì có thể tái sử dụng

- **DB tables**: `question_banks`, `questions`, `question_options`, `exam_papers`, `exam_paper_questions` — đã có, đầy đủ cho practice.
- **Server handler**: `PRACTICE_START` — chỉ cần client gọi.
- **Renderer**: `ExamHtmlTemplateRenderer.renderPractice()` — hoạt động được, cần cải tiến dần.
- **Template**: `practice-template.html` — dùng làm MVP, sau đó nâng cấp.
- **DTO**: `PracticeQuestionViewDTO`, `PracticeOptionViewDTO` — cần review field `isCorrect`.
- **Import pipeline**: `HtmlQuizDataParser` + `HtmlQuizImportService` — tái dùng nguyên.

### 2.6. Những gì đang thiếu

- Client PracticeTab không gọi server.
- Không có bảng `practice_attempts`, `practice_answers`, `user_question_stats`.
- Không có assignment workflow.
- Không có report.
- Không có live quiz infrastructure.
- Encoding tiếng Việt bị mojibake ở một số chuỗi server (dòng 78, 92 trong ExamController).
- Không có phân quyền practice: ai được xem đề nào chưa rõ.

---

## 3. Competitor Research

### 3.1. Bảng so sánh tổng quan

| Nền tảng | Tính năng chính | Điểm nên học | Không nên sao chép | Mức ưu tiên cho TutorHub |
|---|---|---|---|---|
| **Quizizz / Wayground** | Self-practice, mastery mode, adaptive question bank, session settings (shuffle/timer/show answer/attempts), redemption questions, compliance targets, focus mode, reports | Mastery mode (làm lại câu sai tự động), session settings linh hoạt, adaptive question bank theo hiệu suất | UI/brand assets, tên "Mastery Peak", hệ thống power-ups phức tạp | ⭐⭐⭐⭐⭐ — Rất cao |
| **Kahoot** | Live game + PIN code, leaderboard/podium, assigned challenge (self-paced), đa dạng question types (puzzle/poll/slider/type-answer), AI question generator, reports | Live game flow (PIN → lobby → play → podium), assigned challenge workflow, speed vs accuracy scoring | UI màu sắc đặc trưng, tên game mode (Color Kingdoms, Submarine Squad), âm nhạc/sound effects | ⭐⭐⭐⭐ — Cao (live quiz phase sau) |
| **Azota** | Tạo đề từ Word/PDF/Excel, trộn đề thông minh, giao bài qua link, chấm tự động (online + offline phiếu tô), AI chấm tự luận, ngân hàng câu hỏi, thống kê chi tiết, xuất Excel | Import đa định dạng, giao bài linh hoạt không cần đăng nhập, thống kê phổ điểm/tỷ lệ đúng sai, xuất báo cáo | UI web-only (không phù hợp desktop), flow phiếu tô offline, tên "Azota" | ⭐⭐⭐⭐ — Cao (import + report) |
| **Blooket** | Arcade-style mini-games (Gold Quest, Tower Defense, Café), đa dạng game loop, community question sets | Gamification nhẹ không cần phức tạp, community content sharing | Game modes cụ thể (Gold Quest, Tower Defense), visual assets | ⭐⭐ — Trung bình (gamification phase sau) |
| **Gimkit** | In-game economy, strategic gameplay, collaborative modes (Trust No One), student accounts + seasons | Long-term engagement qua progression system, collaborative learning | Economy system phức tạp, specific game modes | ⭐⭐ — Trung bình (phase sau) |
| **Moodle Quiz** | Question bank hierarchy, multiple attempt modes, 4-level review options, question behavior (deferred/interactive), user/group overrides, item analysis | Review options granular (khi nào hiện đáp án, điểm, feedback), question bank categories, grading method (highest/average/first/last) | UI phức tạp khó dùng, over-engineering settings | ⭐⭐⭐ — Tham khảo cho report + settings |
| **Google Classroom** | Assignment workflow (create → assign → collect → grade → return), stream notifications, rubrics | Assignment lifecycle rõ ràng, deadline + late submission handling | Google ecosystem dependency, UI/branding | ⭐⭐⭐ — Tham khảo cho assignment workflow |

### 3.2. Phân tích chi tiết

#### Quizizz / Wayground

**Điểm mạnh cần học:**
- **Session Settings**: Shuffle questions/options, show answers (after each question / after quiz / never), max attempts, timer per question, compliance/accuracy targets (60-100%).
- **Mastery Mode**: Adaptive question bank — mỗi lần làm lại nhận bộ câu khác nhau, ưu tiên câu đã sai. Redemption questions — làm lại ngay câu sai.
- **Self-Practice**: Học sinh tự tìm bài luyện từ dashboard, không cần code từ giáo viên. Reports riêng cho sessions do giáo viên tạo.
- **Focus Mode**: Full-screen lock + tab-switch alert (tương tự anti-cheating nhẹ).

**Áp dụng cho TutorHub:**
- Practice settings: shuffle, show answer policy, max attempts — đưa vào `practice_assignments.settings_json`.
- Wrong-question practice: tự động tạo bộ câu từ `user_question_stats` có `mastery_level` thấp.
- Focus mode cho assignment: có thể tái dùng logic từ TSE/Secure Exam (không cần lockdown nặng).

#### Kahoot

**Điểm mạnh cần học:**
- **Live Game Flow**: Host tạo room → sinh PIN → học sinh nhập PIN → lobby (thấy ai đã join) → host bấm Start → câu hỏi hiện trên màn host → học sinh chọn đáp án trên device → leaderboard sau mỗi câu → podium cuối.
- **Assigned Challenge**: Giáo viên giao → học sinh làm tự do → có deadline → report sau.
- **Scoring**: Điểm = f(accuracy, speed). Correct + fast = nhiều điểm hơn.
- **Question Types**: MCQ, True/False, Puzzle (sắp xếp), Poll, Type-answer, Slider, Open-ended.

**Áp dụng cho TutorHub:**
- Live quiz room model: host_user_id, room_code, current_question_index, player list, realtime leaderboard.
- Assigned challenge = practice assignment với deadline + max_attempts.
- Scoring by speed: tính `time_spent_seconds` cho mỗi câu, bonus nếu nhanh.

#### Azota

**Điểm mạnh cần học:**
- **Import đa định dạng**: Word (.docx), PDF, Excel — tự động nhận diện câu hỏi và đáp án.
- **Trộn đề thông minh**: Hàng trăm mã đề khác nhau từ 1 bộ gốc.
- **Giao bài linh hoạt**: Gửi link, không bắt buộc đăng nhập (cho bối cảnh phụ huynh gửi cho con).
- **Thống kê**: Phổ điểm, tỷ lệ đúng/sai theo câu, lọc theo trạng thái (đã nộp/chưa nộp/đã chấm), xuất Excel.
- **AI chấm tự luận**: Mới 2025, AI đọc + chấm điểm theo tiêu chí giáo viên mô tả.

**Áp dụng cho TutorHub:**
- Import Word/PDF/Excel: Phase 8, cần thêm parser (Apache POI cho Word/Excel, PDFBox cho PDF).
- Thống kê câu hỏi: `user_question_stats` table + aggregate query.
- Xuất báo cáo: Phase 6, xuất CSV/Excel từ server.

---

## 4. Product Vision cho TutorHub Practice

Tab Ôn tập của TutorHub nên trở thành **trung tâm luyện tập cá nhân hóa** cho hệ sinh thái gia sư:

### 4.1. Self-Practice (Phase 1-2)
Học sinh tự chọn bộ đề từ danh sách server, làm bài, nhận feedback ngay, xem kết quả cuối bài.

### 4.2. Assigned Practice (Phase 5)
Giáo viên/gia sư giao bài ôn tập cho học sinh cụ thể hoặc lớp, có deadline, max attempts, show answer policy.

### 4.3. Wrong-Question Practice (Phase 4)
Tự động tạo bộ ôn tập từ các câu học sinh hay sai. Spaced repetition đơn giản: ưu tiên câu lâu chưa ôn và câu mastery thấp.

### 4.4. Exam Preparation (Phase 2-3)
Ôn tập theo bộ đề gắn với kỳ thi. Simulated test mode: có timer, không hiện đáp án, chỉ hiện kết quả cuối.

### 4.5. Teacher Reports (Phase 6)
Giáo viên xem: completion rate, average score, câu nào sai nhiều nhất, học sinh nào cần hỗ trợ.

### 4.6. Live Quiz (Phase 7 — sau cùng)
Host room, join code, realtime, leaderboard, podium. Yêu cầu WebSocket room management.

### 4.7. AI/Question Generation (Phase 9 — tương lai)
AI sinh câu hỏi từ nội dung, AI giải thích câu sai, adaptive difficulty.

---

## 5. User Roles và Use Cases

### 5.1. Student (Học sinh)

| Use Case | Mô tả | Phase |
|---|---|---|
| Xem danh sách bộ ôn tập | Dashboard hiện tất cả đề available từ server | 1 |
| Tìm kiếm bộ đề | Search theo tên, filter theo môn/lớp/tag | 1 |
| Bắt đầu ôn tập | Chọn đề → gọi `PRACTICE_START` → load bài | 1 |
| Làm bài trong player | Một câu/màn hình, chọn đáp án, xem feedback | 2 |
| Xem kết quả cuối bài | Score, accuracy, time, câu đúng/sai, giải thích | 2 |
| Xem lịch sử ôn tập | Danh sách attempts trước đó | 3 |
| Resume bài đang làm | Mở lại bài chưa submit | 3 |
| Làm lại câu sai | Tự động tạo bộ từ câu sai | 4 |
| Xem tiến độ cá nhân | Mastery level, streak, số câu đã ôn | 4 |
| Xem bài được giao | Danh sách assignment từ giáo viên | 5 |
| Làm bài assignment | Có deadline, max attempts, tính điểm | 5 |
| Tham gia live quiz | Nhập room code, trả lời realtime | 7 |

### 5.2. Teacher / Tutor (Giáo viên / Gia sư)

| Use Case | Mô tả | Phase |
|---|---|---|
| Xem danh sách bộ đề | Tất cả đề mình tạo + đề được chia sẻ | 1 |
| Tạo bộ ôn tập | Chọn từ question bank, gán vào exam_paper | Đã có (ExamPaperTab) |
| Giao bài cho học sinh/lớp | Chọn đề + chọn người nhận + deadline + settings | 5 |
| Xem báo cáo lớp | Completion, average score, phổ điểm | 6 |
| Xem câu hỏi sai nhiều | Thống kê theo câu: % sai, thời gian trung bình | 6 |
| Xem chi tiết học sinh | Từng attempt, từng câu trả lời | 6 |
| Tạo live quiz session | Host room, sinh code, điều khiển câu hỏi | 7 |
| Import đề từ file | HTML (đã có), Word/PDF/Excel (tương lai) | 1 (HTML), 8 (khác) |

### 5.3. Center / Admin

| Use Case | Mô tả | Phase |
|---|---|---|
| Quản lý ngân hàng câu hỏi | CRUD question banks, categories | Đã có (TSE Question Bank UI) |
| Quản lý đề/bộ ôn tập | CRUD exam_papers | Đã có (ExamPaperTab) |
| Theo dõi chất lượng nội dung | Câu nào tỷ lệ sai bất thường, câu nào chưa dùng | 6 |
| Theo dõi thống kê trung tâm | Tổng lượt ôn, top bộ đề, top học sinh | 6 |

---

## 6. Feature Matrix

| Tính năng | Hiện có | Cần làm | Phase | Độ khó | Rủi ro | Ghi chú |
|---|---|---|---|---|---|---|
| **Dashboard** |  |  |  |  |  |  |
| Danh sách bộ đề từ server | ❌ | Gọi `EXAM_PAPER_LIST` hoặc `PRACTICE_LIST` | 1 | Thấp | Thấp | Tái dùng API đã có |
| Search theo tên | ❌ | Client-side filter | 1 | Thấp | Thấp | |
| Filter chips (tất cả/được giao/gần đây/câu sai) | ❌ | Client-side + server query | 1-5 | TB | Thấp | Filter "được giao" cần Phase 5 |
| Card bộ ôn tập (tên, số câu, lần làm gần nhất) | ❌ | Swing panel | 1 | TB | Thấp | |
| **Practice Player** |  |  |  |  |  |  |
| Load HTML từ server | ❌ | Gọi `PRACTICE_START`, load vào WebView | 1 | Thấp | Thấp | Server handler đã có |
| Một câu/màn hình | ❌ | Sửa `practice-template.html` hoặc tạo mới | 2 | TB | TB | Template hiện render tất cả 1 lúc |
| Tiến độ (progress bar) | ❌ | JS trong template | 2 | Thấp | Thấp | |
| Nút trước/sau/bỏ qua | ❌ | JS navigation | 2 | Thấp | Thấp | |
| Mark for review | ❌ | JS + visual indicator | 2 | Thấp | Thấp | |
| Timer tùy mode | ❌ | JS timer, setting từ server | 2 | TB | Thấp | |
| Feedback sau trả lời | ✅ (template hiện tại) | Giữ nguyên cho practice | 2 | — | Thấp | `isCorrect` được gửi |
| Giải thích (explanation) | ✅ (template hiện tại) | Giữ nguyên | 2 | — | Thấp | |
| **Result Summary** |  |  |  |  |  |  |
| Score/accuracy/time | ❌ | JS tổng kết cuối bài | 2 | TB | Thấp | |
| Câu đúng/sai/bỏ qua | ❌ | JS đếm | 2 | Thấp | Thấp | |
| CTA: làm lại câu sai/toàn bộ/quay về | ❌ | JS + bridge về Java | 2 | TB | TB | |
| **Attempt Persistence** |  |  |  |  |  |  |
| Tạo attempt khi bắt đầu | ❌ | Server: tạo `practice_attempts` row | 3 | TB | TB | Cần tạo bảng mới |
| Lưu câu trả lời | ❌ | Server: insert `practice_answers` | 3 | TB | TB | |
| Submit attempt | ❌ | Server: cập nhật status + tính score | 3 | TB | TB | |
| Resume bài đang làm | ❌ | Server: trả lại state + client restore | 3 | Cao | TB | |
| Xem lịch sử attempts | ❌ | Server query + client UI | 3 | TB | Thấp | |
| **Wrong-Question Mode** |  |  |  |  |  |  |
| Thống kê theo câu (`user_question_stats`) | ❌ | Server aggregate | 4 | TB | Thấp | |
| Tạo bộ ôn từ câu sai | ❌ | Server query + render | 4 | TB | Thấp | |
| Mastery level đơn giản | ❌ | Tính từ correct_rate | 4 | Thấp | Thấp | |
| **Assignment** |  |  |  |  |  |  |
| Giáo viên giao bài | ❌ | Server + UI: `practice_assignments` | 5 | Cao | TB | |
| Học sinh thấy bài được giao | ❌ | Server query + client filter | 5 | TB | Thấp | |
| Deadline / max attempts | ❌ | Server enforcement | 5 | TB | TB | |
| Show answer policy | ❌ | Server quyết định có gửi `isCorrect` không | 5 | TB | Cao | Rủi ro rò đáp án |
| **Teacher Report** |  |  |  |  |  |  |
| Completion rate | ❌ | Server aggregate query | 6 | TB | Thấp | |
| Average score | ❌ | Server aggregate | 6 | Thấp | Thấp | |
| Question analysis (% sai) | ❌ | Server aggregate | 6 | TB | Thấp | |
| Student list + detail | ❌ | Server query + UI | 6 | TB | Thấp | |
| Export CSV/Excel | ❌ | Server generate file | 6 | TB | Thấp | |
| **Live Room** |  |  |  |  |  |  |
| Room code generation | ❌ | Server random code | 7 | TB | TB | |
| Host control | ❌ | WebSocket state machine | 7 | Cao | Cao | |
| Realtime answer | ❌ | WebSocket broadcast | 7 | Cao | Cao | |
| Leaderboard | ❌ | Server-side scoring + broadcast | 7 | Cao | TB | |
| **Import Pipeline** |  |  |  |  |  |  |
| Import HTML quiz | ✅ | Giữ nguyên | — | — | Thấp | |
| Import Word/PDF/Excel | ❌ | Apache POI / PDFBox | 8 | Cao | Cao | |
| Manual question builder | Đã có (TSE UI) | Tái dùng | — | — | Thấp | |
| Tags/difficulty/subject/grade | Một phần (difficulty, category) | Mở rộng | 8 | TB | Thấp | |
| **Question Bank / Tags** |  |  |  |  |  |  |
| CRUD question banks | ✅ | Đã có | — | — | — | |
| Question bank categories | ✅ | Đã có | — | — | — | |
| Tags/subject/grade mapping | ❌ | Thêm columns hoặc bảng junction | 8 | TB | Thấp | |
| **Gamification** |  |  |  |  |  |  |
| Badge/streak | ❌ | Client + server | 9 | TB | Thấp | |
| Spaced repetition | ❌ | Algorithm + scheduler | 9 | Cao | TB | |
| **Anti-cheating nhẹ** |  |  |  |  |  |  |
| Tab-switch detection cho assignment | ❌ | JS trong WebView | 5 | TB | TB | |
| Show answer policy enforcement | ❌ | Server-side DTO selection | 5 | TB | Cao | |
| **Accessibility** |  |  |  |  |  |  |
| Keyboard navigation | ❌ | HTML template | 2 | TB | Thấp | |
| Font size adjustment | ❌ | CSS variable | 2 | Thấp | Thấp | |
| **Security/Privacy** |  |  |  |  |  |  |
| DTO tách biệt practice/test/exam | Một phần | Hoàn thiện tách | 0 | TB | Cao | |
| Server quyết định show answer | ❌ | Logic trong handler | 0-5 | TB | Cao | |
| Input sanitization HTML import | ✅ | Đã có parse, không execute JS | — | — | Thấp | |

---

## 7. Recommended Architecture

### 7.1. Client — Cấu trúc lớp đề xuất

```
PracticeTab                        ← Container chính, quản lý navigation giữa các panel
├── PracticeDashboardPanel         ← Danh sách bộ ôn tập, search, filter
│   └── PracticeSetCard            ← Card hiển thị 1 bộ đề (tên, số câu, accuracy, CTA)
├── PracticePlayerPanel            ← Vùng chứa WebView, load HTML từ server
├── PracticeResultPanel            ← Kết quả cuối bài (score, accuracy, time, câu sai)
├── PracticeAssignmentPanel        ← Danh sách bài được giao (Phase 5)
└── PracticeReportPanel            ← Báo cáo cho giáo viên (Phase 6)
```

**Nên dùng Swing hay JavaFX WebView hay hybrid?**

| Thành phần | Công nghệ | Lý do |
|---|---|---|
| Dashboard, search, filter, cards | **Swing thuần** | Đơn giản, consistent với toàn bộ app, không cần WebView |
| Practice Player (làm bài) | **JavaFX WebView** | HTML template render câu hỏi giàu format, đã có cơ sở hạ tầng |
| Result summary | **Swing thuần** hoặc **WebView** | Swing nếu đơn giản, WebView nếu cần chart/animation |
| Reports | **JavaFX WebView** | Charts, phổ điểm, visualization → HTML/JS dễ hơn Swing |

### 7.2. Server — Service layer

```
PracticeService                    ← Quản lý practice flow: list, start, settings
PracticeAttemptService             ← Attempt CRUD: create, save progress, submit, history
PracticeAssignmentService          ← Assignment CRUD: create, list, recipients (Phase 5)
PracticeReportService              ← Aggregate queries: completion, scores, question stats (Phase 6)
PracticeLiveService                ← Live room management: create, join, state (Phase 7)
QuestionImportService              ← Đã có: HtmlQuizImportService (mở rộng Word/PDF Phase 8)
QuestionAnalyticsService           ← Question stats: user_question_stats aggregation (Phase 4)
```

### 7.3. Protocol Actions — Thứ tự triển khai

#### Phase 1 (MVP — làm ngay)
```
PRACTICE_LIST                      ← Danh sách bộ đề cho practice
PRACTICE_START                     ← ĐÃ CÓ — chỉ cần client gọi
```

#### Phase 2-3 (Player + Persistence)
```
PRACTICE_SAVE_PROGRESS             ← Lưu từng câu trả lời
PRACTICE_SUBMIT_ATTEMPT            ← Nộp kết quả cuối bài
PRACTICE_ATTEMPT_HISTORY           ← Lịch sử attempts
```

#### Phase 4 (Wrong Questions)
```
PRACTICE_WRONG_QUESTIONS           ← Lấy danh sách câu sai
PRACTICE_MASTERY_STATS             ← Thống kê mastery level
```

#### Phase 5 (Assignment)
```
PRACTICE_ASSIGNMENT_CREATE         ← Giáo viên tạo assignment
PRACTICE_ASSIGNMENT_LIST           ← Danh sách assignment cho user
PRACTICE_ASSIGNMENT_DETAIL         ← Chi tiết 1 assignment
```

#### Phase 6 (Report)
```
PRACTICE_REPORT_CLASS              ← Report theo lớp
PRACTICE_REPORT_STUDENT            ← Report theo học sinh
PRACTICE_REPORT_QUESTION           ← Report theo câu hỏi
```

#### Phase 7 (Live Quiz — sau cùng)
```
PRACTICE_LIVE_CREATE               ← Host tạo room
PRACTICE_LIVE_JOIN                 ← Player join
PRACTICE_LIVE_NEXT_QUESTION        ← Host chuyển câu
PRACTICE_LIVE_SUBMIT_ANSWER        ← Player trả lời
PRACTICE_LIVE_LEADERBOARD          ← Broadcast leaderboard
PRACTICE_LIVE_END                  ← Kết thúc session
```

---

## 8. Data Model Proposal

### 8.1. `practice_attempts` — Phase 3

**Mục đích**: Lưu mỗi lượt ôn tập của người dùng.

```sql
CREATE TABLE IF NOT EXISTS practice_attempts (
    id VARCHAR(36) PRIMARY KEY,         -- UUID
    user_id INT NOT NULL,               -- FK users
    paper_id INT NOT NULL,              -- FK exam_papers
    assignment_id INT,                  -- FK practice_assignments (null nếu tự luyện)
    mode VARCHAR(30) DEFAULT 'PRACTICE',-- PRACTICE | ASSIGNMENT | WRONG_ONLY | SIMULATED_TEST
    total_questions INT DEFAULT 0,      -- Tổng số câu
    correct_count INT DEFAULT 0,
    wrong_count INT DEFAULT 0,
    skipped_count INT DEFAULT 0,
    score FLOAT DEFAULT 0,
    max_score FLOAT DEFAULT 0,
    duration_seconds INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS', -- IN_PROGRESS | COMPLETED | ABANDONED
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pa_user ON practice_attempts(user_id);
CREATE INDEX idx_pa_paper ON practice_attempts(paper_id);
CREATE INDEX idx_pa_assignment ON practice_attempts(assignment_id);
CREATE INDEX idx_pa_status ON practice_attempts(status);
```

**Quan hệ**: `user_id` → users, `paper_id` → exam_papers, `assignment_id` → practice_assignments.  
**Phase**: 3.

### 8.2. `practice_answers` — Phase 3

**Mục đích**: Lưu từng câu trả lời trong mỗi attempt.

```sql
CREATE TABLE IF NOT EXISTS practice_answers (
    id SERIAL PRIMARY KEY,
    attempt_id VARCHAR(36) NOT NULL,    -- FK practice_attempts
    question_id INT NOT NULL,           -- FK questions
    selected_option_id INT,             -- FK question_options (null nếu essay/skipped)
    answer_text TEXT,                   -- Cho essay/free-text
    is_correct BOOLEAN,                 -- Server tính sau khi trả lời
    time_spent_seconds INT DEFAULT 0,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(attempt_id, question_id)
);
CREATE INDEX idx_pans_attempt ON practice_answers(attempt_id);
CREATE INDEX idx_pans_question ON practice_answers(question_id);
```

**Quan hệ**: `attempt_id` → practice_attempts, `question_id` → questions, `selected_option_id` → question_options.  
**Phase**: 3.

### 8.3. `user_question_stats` — Phase 4

**Mục đích**: Thống kê cá nhân theo từng câu hỏi — nền cho wrong-question mode và mastery.

```sql
CREATE TABLE IF NOT EXISTS user_question_stats (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    question_id INT NOT NULL,
    total_attempts INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    wrong_count INT DEFAULT 0,
    last_attempt_at TIMESTAMP,
    mastery_level INT DEFAULT 0,        -- 0=not started, 1=learning, 2=familiar, 3=mastered
    UNIQUE(user_id, question_id)
);
CREATE INDEX idx_uqs_user ON user_question_stats(user_id);
CREATE INDEX idx_uqs_mastery ON user_question_stats(user_id, mastery_level);
```

**Quan hệ**: `user_id` → users, `question_id` → questions.  
**Phase**: 4. Cập nhật bằng trigger hoặc application logic mỗi khi `practice_answers` được insert.

### 8.4. `practice_assignments` — Phase 5

**Mục đích**: Giáo viên giao bài ôn tập cho lớp hoặc nhóm học sinh.

```sql
CREATE TABLE IF NOT EXISTS practice_assignments (
    id SERIAL PRIMARY KEY,
    teacher_id INT NOT NULL,            -- FK users (giáo viên tạo)
    paper_id INT NOT NULL,              -- FK exam_papers
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_at TIMESTAMP,                   -- Hạn nộp
    max_attempts INT DEFAULT 0,         -- 0 = unlimited
    show_answers_policy VARCHAR(30) DEFAULT 'AFTER_SUBMIT',
        -- IMMEDIATELY | AFTER_SUBMIT | AFTER_DUE | NEVER
    shuffle_questions BOOLEAN DEFAULT FALSE,
    shuffle_options BOOLEAN DEFAULT TRUE,
    time_limit_minutes INT DEFAULT 0,   -- 0 = no limit
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE | CLOSED | DRAFT
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Phase**: 5.

### 8.5. `practice_assignment_recipients` — Phase 5

**Mục đích**: Mapping assignment → học sinh/lớp.

```sql
CREATE TABLE IF NOT EXISTS practice_assignment_recipients (
    assignment_id INT NOT NULL,         -- FK practice_assignments
    user_id INT NOT NULL,               -- FK users
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING | IN_PROGRESS | COMPLETED
    last_attempt_id VARCHAR(36),        -- FK practice_attempts
    PRIMARY KEY (assignment_id, user_id)
);
```

**Phase**: 5.

### 8.6. `practice_live_rooms` — Phase 7

**Mục đích**: Live quiz room.

```sql
CREATE TABLE IF NOT EXISTS practice_live_rooms (
    id SERIAL PRIMARY KEY,
    host_user_id INT NOT NULL,
    paper_id INT NOT NULL,
    room_code VARCHAR(8) NOT NULL UNIQUE, -- 6-8 ký tự, dễ nhập
    status VARCHAR(20) DEFAULT 'LOBBY',   -- LOBBY | IN_PROGRESS | FINISHED
    current_question_index INT DEFAULT -1,
    total_questions INT DEFAULT 0,
    time_per_question_seconds INT DEFAULT 30,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Phase**: 7.

### 8.7. `practice_live_players` — Phase 7

```sql
CREATE TABLE IF NOT EXISTS practice_live_players (
    id SERIAL PRIMARY KEY,
    room_id INT NOT NULL,               -- FK practice_live_rooms
    user_id INT NOT NULL,
    display_name VARCHAR(100),
    score INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    streak INT DEFAULT 0,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(room_id, user_id)
);
```

**Phase**: 7.

### 8.8. `practice_live_answers` — Phase 7

```sql
CREATE TABLE IF NOT EXISTS practice_live_answers (
    room_id INT NOT NULL,
    user_id INT NOT NULL,
    question_index INT NOT NULL,
    selected_option_id INT,
    is_correct BOOLEAN,
    time_ms INT,                        -- Thời gian trả lời (ms) — dùng tính điểm
    score_earned INT DEFAULT 0,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id, user_id, question_index)
);
```

**Phase**: 7.

---

## 9. Security & Answer Exposure

### 9.1. Phân tích rủi ro theo mode

| Mode | Gửi `isCorrect` xuống client? | Gửi `correctAnswer`? | Gửi `explanation`? | Kiểm soát bởi |
|---|---|---|---|---|
| **Practice (tự luyện)** | ✅ Có — feedback ngay | ❌ Không trực tiếp, nhưng highlight đáp án đúng | ✅ Có | Template JS |
| **Assignment (show_answers=IMMEDIATELY)** | ✅ Có | ❌ | ✅ Có | Server setting |
| **Assignment (show_answers=AFTER_SUBMIT)** | ❌ Không — chỉ gửi sau khi submit toàn bài | ❌ | ✅ Sau submit | Server enforcement |
| **Assignment (show_answers=NEVER)** | ❌ Tuyệt đối không | ❌ | ❌ | Server enforcement |
| **Test / Simulated Exam** | ❌ Không — chỉ hiện kết quả cuối | ❌ | ❌ hoặc sau submit | Server enforcement |
| **Secure Exam / TSE** | ❌ **TUYỆT ĐỐI KHÔNG** | ❌ | ❌ | Encrypted package, server-only grading |

### 9.2. DTO tách biệt đề xuất

```
PracticeQuestionDTO                ← Cho practice: CÓ isCorrect trên options
PracticeQuestionFeedbackDTO        ← Feedback sau khi trả lời: explanation, correctAnswer
TestQuestionDTO                    ← Cho assignment/test: KHÔNG CÓ isCorrect, KHÔNG CÓ explanation
ExamQuestionDTO                    ← Cho TSE/Secure Exam: KHÔNG CÓ gì nhạy cảm (đã có ExamQuestionViewDTO)
PracticeResultDTO                  ← Kết quả cuối bài: score, accuracy, danh sách câu + feedback
```

### 9.3. Nguyên tắc bảo mật bắt buộc

1. **Server quyết định mode** — client gửi `paperId` hoặc `assignmentId`, server tra cứu `show_answers_policy` và chọn DTO phù hợp.
2. **Không để client tự quyết định quyền xem đáp án** — client không gửi flag "showAnswers=true".
3. **Secure Exam/TSE tuyệt đối không dùng DTO chứa `isCorrect`** — đã tách rõ qua `ExamQuestionViewDTO` (không có `isCorrect`).
4. **Không chạy HTML local tùy ý trong WebView production** — import HTML → parse bằng `HtmlQuizDataParser` → render lại bằng template nội bộ.
5. **Import HTML chỉ dùng cho nhập liệu/debug** — không phải main flow cho practice.

### 9.4. Rủi ro hiện tại cần sửa ngay (Phase 0)

> [!CAUTION]
> `PracticeOptionViewDTO.isCorrect` hiện là field `public boolean` — đang gửi xuống client qua template. Phù hợp cho practice mode, nhưng nếu bất kỳ code path nào tái dùng DTO này cho test/exam mode → **rò đáp án**.

**Giải pháp**: Đảm bảo `renderPractice()` chỉ được gọi cho practice mode. Tạo DTO riêng cho test/assignment không có `isCorrect`.

---

## 10. UX Plan

### 10.1. Dashboard

```
┌─────────────────────────────────────────────────────┐
│  📖 Ôn tập                                         │
│  Luyện tập, làm bài được giao và theo dõi tiến độ  │
├─────────────────────────────────────────────────────┤
│  🔍 [Tìm kiếm bộ đề...]                            │
│                                                     │
│  [Tất cả] [Được giao ⓷] [Của tôi] [Gần đây] [Câu sai] │
├─────────────────────────────────────────────────────┤
│ ┌──────────────────────┐ ┌──────────────────────┐   │
│ │ Toán 10 - Chương 3   │ │ Vật lý - Động lực học│   │
│ │ 25 câu │ Lần cuối: 2h│ │ 40 câu │ Chưa làm    │   │
│ │ ▓▓▓▓░░ 72%           │ │ ░░░░░░ —             │   │
│ │ [Ôn tập]             │ │ [Bắt đầu]            │   │
│ └──────────────────────┘ └──────────────────────┘   │
│ ┌──────────────────────┐ ┌──────────────────────┐   │
│ │ 📌 Bài tập tuần 12   │ │ Anh văn - Grammar    │   │
│ │ 30 câu │ Hạn: 25/06  │ │ 50 câu │ 3 lần đã làm│   │
│ │ ▓▓▓▓▓▓ 90%           │ │ ▓▓▓░░░ 58%           │   │
│ │ [Làm tiếp]           │ │ [Ôn lại câu sai]     │   │
│ └──────────────────────┘ └──────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### 10.2. Player

```
┌─────────────────────────────────────────────────────┐
│  Toán 10 - Chương 3    Câu 5/25    ⏱ 02:30         │
│  ▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░ 20%                    │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Câu 5: Cho hàm số y = x² - 4x + 3. Tìm giá trị  │
│  nhỏ nhất của hàm số.                               │
│                                                     │
│  ○ A. -1                                            │
│  ● B. -2        ← [đã chọn, chưa confirm]          │
│  ○ C. 0                                             │
│  ○ D. 1                                             │
│                                                     │
│  ┌─ Feedback (sau khi confirm) ─────────────────┐   │
│  │ ✅ Chính xác!                                │   │
│  │ Giải thích: y = (x-2)² - 1 → min = -1       │   │
│  │ tại x = 2.                                   │   │
│  └──────────────────────────────────────────────┘   │
│                                                     │
│  [⬅ Trước]  [🔖 Đánh dấu]  [⏭ Bỏ qua]  [Tiếp ➡]  │
│                                            [Nộp bài]│
└─────────────────────────────────────────────────────┘
```

### 10.3. Result

```
┌─────────────────────────────────────────────────────┐
│  🎉 Kết quả ôn tập                                  │
│                                                     │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │  18/25  │ │  72%    │ │ 15:23   │ │ 5 câu   │   │
│  │  Điểm   │ │ Chính xác│ │ Thời gian│ │ Cần ôn  │   │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘   │
│                                                     │
│  📊 Chi tiết:                                       │
│  ✅ Đúng: 18  │  ❌ Sai: 5  │  ⏭ Bỏ qua: 2        │
│                                                     │
│  Chủ đề yếu: Hàm số bậc 2 (2/5 sai)               │
│                                                     │
│  [🔄 Làm lại câu sai]  [♻ Làm lại toàn bộ]         │
│  [📋 Quay về danh sách]                             │
└─────────────────────────────────────────────────────┘
```

### 10.4. Teacher Report

```
┌─────────────────────────────────────────────────────┐
│  📊 Báo cáo: Toán 10 - Chương 3                    │
│                                                     │
│  Hoàn thành: 28/35 (80%)  │  Điểm TB: 7.2/10       │
│                                                     │
│  📈 Phổ điểm:                                       │
│  9-10: ▓▓▓▓ 4            │  Câu sai nhiều nhất:    │
│  7-8:  ▓▓▓▓▓▓▓▓ 12       │  #15: 72% sai           │
│  5-6:  ▓▓▓▓▓ 8           │  #22: 65% sai           │
│  <5:   ▓▓ 4              │  #8:  58% sai            │
│                                                     │
│  Học sinh  │ Lần làm │ Điểm │ Thời gian │ Trạng thái│
│  Nguyễn A  │ 2       │ 8.5  │ 12:30     │ ✅        │
│  Trần B    │ 1       │ 6.0  │ 25:00     │ ✅        │
│  Lê C      │ 0       │ —    │ —         │ ⏳ Chưa làm│
│                                                     │
│  [📥 Xuất Excel]                                    │
└─────────────────────────────────────────────────────┘
```

---

## 11. Roadmap v2

### Phase 0 — Cleanup & Safety ⏱ 1-2 ngày

**Mục tiêu**: Dọn dẹp nền móng trước khi xây dựng.

- [ ] Sửa encoding tiếng Việt mojibake trong `ExamController.java` (dòng 78, 92 và các chỗ tương tự).
- [ ] Chuẩn hóa Maven compiler/resource encoding = UTF-8.
- [ ] Review `PracticeOptionViewDTO.isCorrect` — đảm bảo chỉ dùng cho practice, không leak sang test/exam.
- [ ] Tạo `TestQuestionDTO` (không có `isCorrect`) nếu chưa có, sẵn sàng cho Phase 5.
- [ ] Giữ `selectHtmlFile()` trong PracticeTab dưới dạng debug/import, không phải main flow.
- [ ] Xác nhận `PRACTICE_START` handler hoạt động đúng bằng cách test manual (gửi packet thủ công hoặc viết test script).

**Acceptance Criteria**:
- Không còn chuỗi mojibake trong server log và response.
- `PracticeOptionViewDTO` chỉ được import/dùng trong practice flow.
- `PRACTICE_START` trả về `PRACTICE_START_SUCCESS` với `htmlContent` hợp lệ khi gửi `paperId` hợp lệ.

---

### Phase 1 — Practice Dashboard + Start ⏱ 2-3 ngày

**Mục tiêu**: Người dùng mở tab Ôn tập → thấy danh sách bộ đề → bấm "Ôn tập" → bài load từ server.

- [ ] Sửa `PracticeTab.java`: thay layout thành CardLayout (dashboard ↔ player).
- [ ] Tạo `PracticeDashboardPanel.java`: header + search + danh sách card bộ đề.
- [ ] Tạo action `PRACTICE_LIST` trên server (hoặc tái dùng `EXAM_PAPER_LIST` với filter).
- [ ] Client gọi `PRACTICE_LIST` khi mở tab → hiển thị danh sách.
- [ ] Search client-side (filter theo tên).
- [ ] Khi bấm "Ôn tập" → gọi `PRACTICE_START` với `paperId` → nhận `htmlContent` → load vào WebView.
- [ ] Nút "Quay lại danh sách" từ player.

**Acceptance Criteria**:
- Mở tab Ôn tập → thấy danh sách bộ đề từ DB (không cần chọn file HTML).
- Search hoạt động.
- Bấm "Ôn tập" → WebView hiển thị bài ôn tập từ server.
- Nút quay lại hoạt động.
- Login, Schedule email, ExamTab, TSE không bị ảnh hưởng.

---

### Phase 2 — Practice Player v1 ⏱ 3-4 ngày

**Mục tiêu**: Trải nghiệm làm bài có tiến độ, feedback và kết quả.

- [ ] Nâng cấp `practice-template.html` hoặc tạo `practice-player-template.html`:
  - Một câu trên một màn hình.
  - Nút trước/sau/bỏ qua.
  - Progress bar.
  - Mark for review.
  - Timer (tùy chọn).
- [ ] Feedback: highlight đáp án đúng/sai, hiển thị giải thích.
- [ ] Màn result summary cuối bài: score, accuracy, time, danh sách câu đúng/sai.
- [ ] CTA trong result: làm lại câu sai, làm lại toàn bộ, quay về danh sách.
- [ ] JS bridge: khi bấm "Quay về" → gọi Java qua `cefQuery` hoặc URL scheme.

**Acceptance Criteria**:
- Hiển thị 1 câu/màn hình, chuyển câu bằng nút.
- Progress bar phản ánh đúng tiến độ.
- Feedback hiển thị sau khi chọn đáp án.
- Màn result cuối bài hiển thị đầy đủ.
- Đề 100+ câu không lag.

---

### Phase 3 — Attempt Persistence ⏱ 3-4 ngày

**Mục tiêu**: Lưu mọi hoạt động ôn tập vào DB.

- [ ] Tạo bảng `practice_attempts` và `practice_answers` (schema ở Section 8).
- [ ] Tạo `PracticeAttemptDAO.java` và `PracticeAttemptService.java`.
- [ ] Server: khi `PRACTICE_START` → tạo `practice_attempts` row, trả `attemptId`.
- [ ] Action `PRACTICE_SAVE_PROGRESS`: client gửi `attemptId + questionId + selectedOptionId` → server insert `practice_answers`.
- [ ] Action `PRACTICE_SUBMIT_ATTEMPT`: client gửi `attemptId` → server tính score, cập nhật status.
- [ ] Action `PRACTICE_ATTEMPT_HISTORY`: server trả danh sách attempts của user.
- [ ] Resume: nếu có attempt `IN_PROGRESS` cho paper này → trả lại state.
- [ ] Dashboard card hiện: lần làm gần nhất, accuracy, số lần đã làm.

**Acceptance Criteria**:
- Sau khi ôn tập, xem lại lịch sử → thấy attempt với score/time.
- Đóng app giữa chừng → mở lại → resume được.
- Dashboard card hiện đúng accuracy và số lần đã làm.

---

### Phase 4 — Wrong Question & Mastery ⏱ 2-3 ngày

**Mục tiêu**: Học sinh luyện lại câu sai, theo dõi mastery.

- [ ] Tạo bảng `user_question_stats` (schema ở Section 8).
- [ ] Cập nhật `user_question_stats` mỗi khi `practice_answers` được insert (trong `PracticeAttemptService`).
- [ ] Action `PRACTICE_WRONG_QUESTIONS`: server query câu có `mastery_level < 2` hoặc `wrong_count > correct_count` → render bộ ôn riêng.
- [ ] Dashboard: filter chip "Câu sai" → hiển thị bộ đề tự tạo từ câu sai.
- [ ] Mastery level đơn giản: 0=chưa làm, 1=đang học (correct < 50%), 2=quen (50-80%), 3=thành thạo (>80%).
- [ ] Card bộ đề: hiện mastery progress.

**Acceptance Criteria**:
- Bấm "Câu sai" → thấy danh sách câu đã sai.
- Ôn lại câu sai → mastery level tăng khi trả lời đúng.
- Dashboard card hiện mastery progress.

---

### Phase 5 — Assignment ⏱ 4-5 ngày

**Mục tiêu**: Giáo viên giao bài, học sinh làm bài được giao.

- [ ] Tạo bảng `practice_assignments` và `practice_assignment_recipients` (schema ở Section 8).
- [ ] `PracticeAssignmentService.java`: CRUD assignment.
- [ ] Action `PRACTICE_ASSIGNMENT_CREATE`: giáo viên chọn paper + recipients + settings.
- [ ] Action `PRACTICE_ASSIGNMENT_LIST`: học sinh/giáo viên xem danh sách.
- [ ] UI giáo viên: form tạo assignment (chọn đề, chọn học sinh/lớp, deadline, max attempts, show answer policy).
- [ ] UI học sinh: dashboard filter "Được giao" → hiện bài assignment.
- [ ] `show_answers_policy` enforcement: server chọn DTO phù hợp (có/không `isCorrect`).
- [ ] Deadline enforcement: server từ chối submit sau deadline.
- [ ] Max attempts enforcement: server đếm attempts và từ chối nếu vượt.

**Acceptance Criteria**:
- Giáo viên tạo assignment → học sinh thấy trong danh sách "Được giao".
- Học sinh làm bài → tuân thủ deadline + max attempts.
- `show_answers_policy=AFTER_SUBMIT` → không thấy đáp án khi đang làm, chỉ thấy sau submit.
- `show_answers_policy=NEVER` → không bao giờ thấy đáp án.

---

### Phase 6 — Reports ⏱ 3-4 ngày

**Mục tiêu**: Giáo viên xem báo cáo.

- [ ] `PracticeReportService.java`: aggregate queries.
- [ ] Action `PRACTICE_REPORT_CLASS`: completion rate, average score, phổ điểm.
- [ ] Action `PRACTICE_REPORT_STUDENT`: chi tiết từng học sinh.
- [ ] Action `PRACTICE_REPORT_QUESTION`: câu nào sai nhiều, time trung bình.
- [ ] UI: `PracticeReportPanel.java` — WebView load HTML report (chart.js hoặc đơn giản bằng HTML table).
- [ ] Export CSV (basic).

**Acceptance Criteria**:
- Giáo viên xem báo cáo lớp → thấy completion, average score, phổ điểm.
- Giáo viên xem câu hỏi → thấy % sai, thời gian trung bình.
- Export CSV hoạt động.

---

### Phase 7 — Live Quiz ⏱ 5-7 ngày

**Mục tiêu**: Quiz realtime cho lớp.

- [ ] Tạo bảng `practice_live_rooms`, `practice_live_players`, `practice_live_answers`.
- [ ] `PracticeLiveService.java`: room state machine (LOBBY → IN_PROGRESS → FINISHED).
- [ ] Room code generation (6 ký tự, alphanumeric, dễ nhập).
- [ ] Actions: `CREATE`, `JOIN`, `NEXT_QUESTION`, `SUBMIT_ANSWER`, `LEADERBOARD`, `END`.
- [ ] WebSocket broadcast: host → all players, player → host.
- [ ] UI Host: tạo room → hiện room code → điều khiển câu hỏi → xem leaderboard.
- [ ] UI Player: nhập code → join → trả lời → xem ranking.
- [ ] Scoring: base_score × speed_bonus.
- [ ] Podium cuối phiên.

**Acceptance Criteria**:
- Host tạo room → sinh code.
- Player nhập code → join lobby.
- Host bấm Start → câu hỏi hiện lên tất cả players.
- Player trả lời → leaderboard cập nhật.
- Host bấm End → podium hiện.

---

### Phase 8 — Import & Authoring nâng cao ⏱ 4-5 ngày

- [ ] Import Word (.docx): Apache POI parser.
- [ ] Import PDF: PDFBox parser.
- [ ] Import Excel: Apache POI parser.
- [ ] Tags/difficulty/subject/grade: thêm columns hoặc bảng junction.
- [ ] Review workflow: question status = DRAFT → REVIEW → APPROVED.

---

### Phase 9 — Gamification / AI nâng cao ⏱ tương lai

- [ ] Badge/streak system.
- [ ] Spaced repetition algorithm (SM-2 hoặc đơn giản hơn).
- [ ] AI explanation: gọi API AI để giải thích câu sai.
- [ ] AI question generation: sinh câu hỏi từ nội dung.

---

## 12. MVP Definition

### MVP nên có (Phase 0 + 1):

| Có | Không có |
|---|---|
| Dashboard list bộ đề từ server | Live quiz |
| Search theo tên | Assignment lớn |
| Bấm "Ôn tập" → gọi `PRACTICE_START` | AI |
| WebView load content từ server | Import Word/PDF/Excel |
| Template hiện tại (tất cả câu 1 trang, check đúng/sai ngay) | One-question-per-page (Phase 2) |
| Nút quay về danh sách | Attempt persistence (Phase 3) |
| Encoding tiếng Việt sửa xong | Reports (Phase 6) |

### MVP mở rộng (Phase 1 + 2):

Thêm: one-question-per-page player, progress bar, result summary local (JS-only, chưa lưu server).

---

## 13. Acceptance Criteria chi tiết

### Phase 0

- [ ] Grep toàn project: không còn chuỗi `Ã` hoặc `Ã„` trong các file practice/exam server response.
- [ ] `PracticeOptionViewDTO` không được import trong bất kỳ exam/test code path nào.
- [ ] Test manual gửi packet `PRACTICE_START` với `paperId` hợp lệ → nhận `PRACTICE_START_SUCCESS`.

### Phase 1

- [ ] Mở tab Ôn tập → thấy danh sách ≥1 bộ đề (nếu DB có dữ liệu).
- [ ] Search "Toán" → chỉ hiện bộ đề có chứa "Toán".
- [ ] Bấm "Ôn tập" trên card → WebView hiển thị bài ôn tập.
- [ ] Bấm "Quay lại" → về dashboard.
- [ ] Forgot Password OTP, Schedule email, ExamTab, TSE — tất cả vẫn hoạt động bình thường.
- [ ] Không còn nút "Chọn file HTML" làm main flow (có thể giữ dưới dạng menu debug).

### Phase 2

- [ ] Player hiện 1 câu/màn hình.
- [ ] Nút Trước/Sau hoạt động.
- [ ] Progress bar chính xác.
- [ ] Sau khi chọn đáp án: feedback hiện ngay (practice mode).
- [ ] Cuối bài: result summary hiện score, accuracy, time.
- [ ] Đề 200 câu không crash WebView.

### Phase 3

- [ ] Sau khi ôn tập xong → `practice_attempts` có record mới.
- [ ] `practice_answers` có đủ câu trả lời.
- [ ] Dashboard card hiện "Lần cuối: 2 giờ trước, 72%".
- [ ] Đóng app giữa chừng → mở lại → attempt `IN_PROGRESS` được resume.

---

## 14. Risk Register

| # | Rủi ro | Mức độ | Nguyên nhân | Cách giảm rủi ro |
|---|---|---|---|---|
| R1 | **Rò đáp án** trong test/assignment mode | 🔴 Cao | `PracticeOptionViewDTO.isCorrect` bị tái dùng nhầm; client tự quyết show answer | Tách DTO triệt để (Phase 0); server quyết định mode; code review khi thêm mode mới |
| R2 | **WebView chạy HTML không tin cậy** | 🟡 TB | Người dùng import file HTML chứa JS malicious, WebView execute | Parse dữ liệu bằng `HtmlQuizDataParser` → render lại bằng template nội bộ; không `webEngine.load(localFile)` cho production flow |
| R3 | **Encoding mojibake** | 🟡 TB | Source file hoặc Maven resource không UTF-8 | Sửa `pom.xml` encoding; sửa chuỗi thủ công; grep toàn bộ chuỗi `Ã` |
| R4 | **DB migration thiếu version** | 🟡 TB | Thêm bảng practice_* không qua migration script → inconsistent state | Dùng pattern `ExamDatabaseManager` (CREATE IF NOT EXISTS + safeAddColumn); log migration vào `schema_migrations` |
| R5 | **Performance đề lớn (500+ câu)** | 🟡 TB | Template render tất cả 1 lúc → DOM quá lớn → WebView lag | Phase 2 chuyển sang one-question-per-page; virtualize navigation |
| R6 | **UI Swing/JavaFX phức tạp** | 🟡 TB | Mix Swing + JavaFX WebView gây focus/paint issues (đã gặp với ScheduleTab PopOver) | Dùng pattern JFXPanel đã ổn định; test kỹ focus behavior; tránh PopOver |
| R7 | **Realtime live quiz khó** | 🔴 Cao | WebSocket state management cho N players, race conditions, reconnect handling | Để Phase 7 sau cùng; thiết kế state machine đơn giản; test với ≤30 players trước |
| R8 | **Client thao tác DB trực tiếp** | 🟡 TB | Nếu có code client gọi DB (SQLite local) mà không qua server → inconsistent | Practice data phải luôn qua server; local chỉ dùng cho cache/draft nếu cần |
| R9 | **Thiếu session enforcement** | 🟡 TB | Client gửi `PRACTICE_SUBMIT_ATTEMPT` cho attempt không phải của mình | Server validate `attempt.user_id == authenticated_user_id` trước khi xử lý |
| R10 | **Template JS XSS** | 🟡 TB | Question content chứa `<script>` → execute trong WebView | Escape HTML trong template (đã có `.replace(/</, "&lt;")` nhưng cần review toàn diện) |

---

## 15. Final Recommendation

### Có nên code ngay Phase 1 không?

**Có, nhưng Phase 0 trước.** Phase 0 mất 1-2 ngày, sửa encoding + đảm bảo DTO safety. Sau đó Phase 1 mất 2-3 ngày — ROI rất cao vì chỉ cần nối client với server API đã có.

### Trước khi code nên chốt những quyết định nào?

1. **`PRACTICE_LIST` riêng hay dùng `EXAM_PAPER_LIST`?** — Khuyến nghị tạo `PRACTICE_LIST` riêng để sau này có thể thêm filter/metadata mà không ảnh hưởng exam flow.
2. **Nút "Chọn file HTML" giữ hay bỏ?** — Giữ dưới dạng debug menu (Ctrl+Shift+I hoặc tương tự), không hiện default.
3. **Dashboard dùng Swing hay WebView?** — Khuyến nghị Swing (consistent với app, không cần WebView cho layout đơn giản).
4. **Template practice giữ nguyên hay tạo mới?** — Phase 1 giữ nguyên template hiện tại (tất cả câu 1 trang). Phase 2 mới tạo template one-question-per-page.

### Nên ưu tiên gì trong 1-2 ngày đầu?

1. **Ngày 1**: Phase 0 — sửa encoding, review DTO, test `PRACTICE_START` handler.
2. **Ngày 2**: Phase 1 — sửa `PracticeTab.java` layout, tạo `PracticeDashboardPanel`, nối với `PRACTICE_LIST` + `PRACTICE_START`.

### Nên tránh gì để không làm vỡ module Exam/TSE hiện có?

- **KHÔNG sửa** `ExamController` handler cho exam actions (`EXAM_START_REQUEST`, `EXAM_SUBMIT`, `EXAM_START_REQUEST_V2`).
- **KHÔNG sửa** `ExamHtmlTemplateRenderer.renderExam()`.
- **KHÔNG sửa** `ExamQuestionViewDTO` hoặc `ExamOptionViewDTO`.
- **KHÔNG import** `PracticeOptionViewDTO` trong bất kỳ exam/TSE file nào.
- **KHÔNG thay đổi** bảng `exam_sessions`, `exam_answers`, `exam_attempts`.
- Tất cả bảng practice mới dùng prefix `practice_` để tách biệt namespace.

---

## References

### Quizizz / Wayground
- Wayground Session Settings: https://help.wayground.com/support/solutions/articles/158000404930-navigate-session-settings
- Wayground Mastery Peak Mode: https://help.wayground.com/support/solutions/articles/158000404926-host-an-assessment-quiz-in-mastery-peak-mode
- Wayground Reports: https://help.wayground.com/support/solutions/articles/158000404058-reports-on-wayground
- Wayground Self-Practice: https://help.wayground.com/support/solutions/articles/ (student dashboard self-practice feature)
- Wayground Adaptive Question Bank & Redemption Questions (video): https://www.youtube.com/watch?v=wayground-mastery

### Kahoot
- Assign a Kahoot: https://support.kahoot.com/hc/en-us/articles/360039411334-How-to-assign-a-kahoot-in-web-platform
- Tips for hosting a live game: https://support.kahoot.com/hc/en-us/articles/360039900153-Tips-for-hosting-a-live-game
- Kahoot quiz reports: https://support.kahoot.com/hc/en-us/articles/360035063054-Kahoot-quiz-reports
- Kahoot question types: https://kahoot.com/blog/question-types/
- Kahoot AI question generator: https://kahoot.com/blog/ai-question-generator/

### Azota
- Hướng dẫn đề thi Azota: https://docs.azota.vn/docs/huong-dan-su-dung/de-thi/
- Tạo và giao bài tập: https://docs.azota.vn/docs/huong-dan-su-dung/bai-tap/tao-va-giao-bai-tap-chi-co-mo-ta/
- Tính năng thống kê Azota: https://azota.vn/features (trang chính thức)
- AI chấm tự luận Azota: https://azota.vn/ai-grading (2025 feature)

### Blooket / Gimkit
- Blooket game modes overview (community knowledge base)
- Gimkit collaborative modes and in-game economy (official docs)

### Moodle
- Moodle Quiz Activity: https://docs.moodle.org/en/Quiz_activity
- Moodle Question Bank: https://docs.moodle.org/en/Question_bank
- Moodle Quiz Review Options: https://docs.moodle.org/en/Quiz_settings#Review_options

### Google Classroom
- Google Classroom Assignment Workflow: https://support.google.com/edu/classroom/

### Tài liệu nội bộ TutorHub
- `docs/practice_tab_research_and_roadmap.md` — v1 roadmap (bản trước)
- `docs/tse_exam_operation_architecture_research.md` — kiến trúc exam
- `docs/tse_exam_operation_master_plan_deep_research.md` — master plan exam
- `docs/auth_session_architecture_plan.md` — kiến trúc session
- `docs/tse_question_bank_backend_phase_2_3.md` — question bank backend
- `docs/tse_question_bank_ui_phase_4e_1.md` — question bank UI
- `docs/tse_exam_paper_builder_ui_phase_4e_2.md` — exam paper builder
- `docs/google_apps_script_email_relay_setup.md` — email setup
- `src/main/java/.../PracticeTab.java` — UI hiện tại
- `src/main/java/.../ExamController.java` — Server handlers
- `src/main/java/.../ExamHtmlTemplateRenderer.java` — Renderer
- `src/main/resources/tse/practice-template.html` — Practice HTML template
- `src/main/java/.../PracticeQuestionViewDTO.java` — Practice DTO
- `src/main/java/.../PracticeOptionViewDTO.java` — Practice option DTO
- `src/main/java/.../HtmlQuizDataParser.java` — HTML quiz parser
- `src/main/java/.../HtmlQuizImportService.java` — Import service
- `src/main/java/.../db/ExamDatabaseManager.java` — DB schema

---

## So sánh v2 với v1

| Khía cạnh | v1 | v2 |
|---|---|---|
| **Số phase** | 6 (lớn, gộp nhiều) | 10 (nhỏ hơn, rõ hơn, mỗi phase có acceptance criteria) |
| **Phase 0 cleanup** | Không có | Có — sửa encoding, review DTO safety, test handler |
| **Competitor research** | 3 nền tảng, mô tả sơ lược | 7 nền tảng, bảng so sánh chi tiết, phân tích áp dụng cụ thể |
| **User roles** | Không phân tích | Student / Teacher / Admin với use cases chi tiết |
| **Feature matrix** | Bảng so sánh đơn giản | Bảng 40+ tính năng với hiện trạng, phase, độ khó, rủi ro |
| **Security analysis** | Nêu rủi ro `isCorrect` | Phân tích theo 6 mode, đề xuất 5 DTO tách biệt, 5 nguyên tắc bắt buộc |
| **Data model** | 3 bảng sơ lược | 8 bảng chi tiết với SQL, index, quan hệ, phase |
| **UX plan** | Mô tả text | ASCII wireframe cho Dashboard, Player, Result, Teacher Report |
| **Architecture** | Liệt kê class names | Phân tích Swing vs WebView cho từng component, protocol actions chia theo phase |
| **MVP definition** | 4 MVP gộp | MVP rõ ràng: có gì / không có gì, acceptance criteria |
| **Risk register** | 4 rủi ro | 10 rủi ro với mức độ, nguyên nhân, cách giảm |
| **Acceptance criteria** | Không có | Có cho mỗi phase |
| **Final recommendation** | "Làm Phase 1" | Chốt 4 quyết định trước khi code, kế hoạch ngày 1-2, danh sách "không được sửa" |
