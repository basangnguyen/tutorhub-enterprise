# KẾ HOẠCH PHÁT TRIỂN TAB QUIZHUB
### Từ trang quiz.html tĩnh → Module ôn thi trắc nghiệm chuyên nghiệp cho TutorHub Enterprise

---

## 0. Tóm tắt nhanh

QuizHub hiện tại là **1 file HTML/CSS/JS tĩnh** (`quiz.html`, ~1130 dòng) được JavaFX `WebView` load lên, với 3 đề trắc nghiệm được **hard-code thẳng trong code** (`QUIZ_1`, `QUIZ_2`, `QUIZ_3`). Phần logic làm bài, chấm điểm, flashcard, đánh dấu câu, xuất ảnh câu đã đánh dấu… đã được viết khá tốt và đầy đủ cho một bản v1.

Mục tiêu của kế hoạch này là nâng cấp QuizHub thành một module có thể:
1. **Nhập đề từ file Excel** theo một format chuẩn → tự động tạo đề, không cần sửa code.
2. Có **chế độ Flashcard kiểu Quizlet** (học theo cơ chế lặp lại ngắt quãng, không chỉ lật thẻ tuyến tính).
3. Có **yếu tố game hoá kiểu Quizizz** (điểm số, streak, tốc độ, huy hiệu...) để tăng động lực ôn tập.
4. Có **quản lý nhiều đề / nhiều môn**, thống kê tiến độ, thay vì 3 đề cố định.
5. Giữ được toàn bộ UI/UX hiện có (đã khá ổn) — nâng cấp **kiến trúc và dữ liệu**, không vẽ lại từ đầu.

Hai sản phẩm đính kèm theo kế hoạch này:
- **Tài liệu này** (kế hoạch + kiến trúc + đặc tả format dữ liệu).
- **File Excel mẫu** (`Mau_Import_Cau_Hoi_QuizHub.xlsx`) — đúng format đề xuất ở Mục 6, có hướng dẫn + ví dụ sẵn, dùng để vừa giao cho người soạn đề, vừa làm input test cho bộ parser.

---

## 1. Đánh giá hiện trạng mã nguồn `quiz.html`

### Điểm mạnh (giữ nguyên, tận dụng lại)
- Cấu trúc dữ liệu câu hỏi đã khá tốt: mỗi câu là `[câu_hỏi, [đáp_án...], [chỉ_số_đúng...], giải_thích, {giải_thích_đáp_án_sai}]` — hỗ trợ **cả câu 1 đáp án và nhiều đáp án đúng** ngay từ đầu. Đây chính là nền tảng để thiết kế format Excel ở Mục 6.
- Đã có: trộn câu hỏi, trộn đáp án, giới hạn số câu, 2 chế độ Ôn tập/Thi thử, timer, đánh dấu (flag) câu, ô tìm kiếm full-text trong đề, lưới điều hướng câu (nav grid), "làm lại câu sai", flashcard lật thẻ, và một tính năng khá hay là **xuất câu đã đánh dấu thành ảnh PNG để copy/lưu**.
- UI mobile-first, gọn, không phụ thuộc framework ngoài — dễ nhúng vào WebView.

### Hạn chế / vấn đề cần xử lý ngay
| # | Vấn đề | Mức độ |
|---|---|---|
| 1 | **3 đề bị hard-code trong code** (`QUIZ_1/2/3`, `TITLES`, 3 thẻ HTML cố định trong `#menu`) → muốn thêm đề phải sửa code, build lại app. | 🔴 Chặn hoàn toàn mục tiêu "nhập đề từ Excel" |
| 2 | `loadBests()`/`saveBest()` gọi `window.storage.get/set` — đây là API lưu trữ riêng của môi trường Claude.ai Artifacts, **không tồn tại trong JavaFX WebView thật**. Lỗi bị `try/catch` nuốt im lặng nên không crash, nhưng **điểm cao nhất hiện không được lưu lại giữa các lần chạy app**. | 🔴 Bug ẩn cần vá khi làm cầu nối Java↔JS |
| 3 | ID câu hỏi = **chỉ số trong mảng** (`origIdx`), không có ID ổn định → nếu sau này theo dõi tiến độ học/flashcard mastery theo từng câu thì việc trộn câu, sửa đề, import lại sẽ làm lệch ID. | 🟡 Cần xử lý trước khi làm tính năng "tiến độ ghi nhớ" |
| 4 | Không có khái niệm "ngân hàng câu hỏi" tách khỏi "đề" — mỗi đề là một khối câu hỏi cố định, không lọc theo chủ đề/độ khó, không tạo "đề con" ngẫu nhiên từ ngân hàng lớn. | 🟡 Cần cho tính năng nâng cao (Phase 3) |
| 5 | Không có quản lý nhiều môn học, không có nơi nhập liệu trong app (toàn bộ phải soạn ở code). | 🔴 Chặn mục tiêu chính |

**Kết luận:** Không cần viết lại UI làm bài/flashcard — phần đó giữ và mở rộng. Việc cần làm là **tách dữ liệu ra khỏi code, đưa vào lớp service Java + định dạng import Excel + cầu nối thay cho `window.storage` giả**.

---

## 2. Bài học rút ra từ nghiên cứu 4 nền tảng tham khảo

### 2.1 baitaptracnghiem.com
Mô hình: ngân hàng đề được phân loại theo **cấp học / môn học / lớp** (ví dụ "Lớp 12", "Đại học – Triết học Mác-Lênin"), mỗi danh mục hiển thị số đề có sẵn, vào trong là danh sách đề để chọn làm; có chấm điểm kèm đáp án và lời giải chi tiết cho từng câu, hoàn toàn miễn phí, không cần tài khoản để làm bài thử.
→ **Áp dụng:** giữ mô hình thẻ chọn đề như UI hiện tại của QuizHub, nhưng thêm 1 lớp phân loại **Môn học → Đề** (hiện tại đang phẳng, chỉ có "Đề 1/2/3") để mở rộng tốt khi có hàng chục đề.

### 2.2 Azota
Đây là nền tảng có tính năng **gần nhất với yêu cầu "tải Excel lên, hệ thống tự tạo đề"**: giáo viên tải file Word/Excel có sẵn câu hỏi, hệ thống **tự nhận diện câu hỏi và đáp án trả lời, không cần làm thủ công**. Cấu hình đề khá chi tiết: cấu hình thời gian làm bài, **cố định câu hỏi đã bốc ngẫu nhiên cho mỗi đề con** (mỗi học sinh có thể nhận một đề con khác nhau từ cùng một đề gốc), và **kiểm soát thời điểm cho xem điểm/đáp án** (không bao giờ / khi nộp xong / khi tất cả đã thi xong). Một bài viết hướng dẫn của Azota mô tả tùy chọn cho xem điểm có ba mức: ẩn hoàn toàn, hiện ngay khi làm bài xong, hoặc chỉ hiện sau khi tất cả người thi đã hoàn thành — đi kèm tùy chọn tương tự cho việc xem đề và đáp án.
→ **Áp dụng:**
- Tính năng "tải Excel → tự tạo đề" chính là yêu cầu cốt lõi của bạn — Azota xác nhận đây là mô hình khả thi và đã được người dùng thực tế chấp nhận tốt.
- Bổ sung 2 cấu hình rất đáng học: (a) **đề con random theo ma trận** từ ngân hàng câu hỏi lớn, (b) **kiểm soát thời điểm hiện điểm/giải thích** — hữu ích khi QuizHub được dùng cho mục đích kiểm tra nghiêm túc, không chỉ tự ôn.

### 2.3 Quizizz (đã đổi tên thương hiệu thành **Wayground**)
Các điểm đáng học:
- **Đa chế độ chơi**: "Thông thường" cho học sinh chơi cá nhân theo tốc độ riêng, "Theo nhóm" để thi đấu theo nhóm, "Đỉnh cao tinh thông" (Mastery Peak) để luyện tập sâu, và "Kiểm tra" cho đánh giá nghiêm túc — đúng tinh thần "Ôn tập vs Thi thử" mà QuizHub đã có, chỉ cần đặt tên/UX rõ hơn.
- **Mastery Peak**: chế độ giúp học sinh luyện tập và cải thiện bằng cách trả lời lại chính các câu đã làm sai, kết hợp yếu tố trò chơi để tăng hứng thú học tập — về bản chất là "ôn câu sai" mà QuizHub đã có (`retryWrong()`), chỉ cần thêm lớp game hoá lên trên.
- **Phản hồi tức thì & báo cáo chi tiết theo từng câu** sau khi làm bài là tiêu chuẩn ngành, QuizHub đã làm tốt phần này.
- Quizizz/Wayground giờ còn có công cụ AI tạo câu hỏi từ tài liệu có sẵn (Word, PDF...) — gợi ý cho roadmap dài hơi (Phase 4) nếu muốn tự sinh đề bằng AI thay vì chỉ nhập Excel thủ công.
→ **Áp dụng:** lớp "game hoá" (điểm, streak, tốc độ trả lời, huy hiệu) nên xây trên nền các chế độ đã có sẵn (Ôn tập = self-paced, Thi thử = nghiêm túc có giờ), không cần làm multiplayer thời gian thực ngay (yêu cầu hạ tầng mạng/server, nên để Phase 4 — xem Mục 8).

### 2.4 Quizlet
Đây là chuẩn tham khảo tốt nhất cho **chế độ Flashcard**, vì hiện tại flashcard của QuizHub chỉ là lật thẻ tuyến tính (next/prev/shuffle), còn Quizlet có cả một hệ thống học chủ động:
Quizlet cung cấp 7 chế độ học chính: Flashcards (lật thẻ), Learn (cá nhân hoá nội dung theo tiến độ ghi nhớ của người học), Write (viết chính xác thông tin còn thiếu), Spell (nghe và gõ đúng từ), Test (kiểm tra tổng hợp dưới dạng trắc nghiệm/tự luận/điền khuyết), Match (đối chiếu thuật ngữ–định nghĩa trong thời gian giới hạn) và Gravity (thử thách tốc độ-độ chính xác). Điểm mạnh nhất theo các bài đánh giá là **chế độ Learn**: hệ thống ghi nhận những thẻ người dùng làm sai và lặp lại chúng thường xuyên hơn để giúp ghi nhớ kỹ hơn — đây chính là cơ chế **lặp lại ngắt quãng (spaced repetition)**.
→ **Áp dụng:** nâng cấp Flashcard của QuizHub theo hướng **Leitner system** (phiên bản đơn giản hoá của spaced repetition, dễ cài đặt offline, không cần thuật toán SM-2 phức tạp) — chi tiết ở Mục 7.2. Không cần làm đủ cả 7 chế độ của Quizlet ngay, ưu tiên: Flashcards (đã có) → Learn/spaced repetition (Phase 2) → Test tổng hợp tự sinh (đã có dưới dạng "Ôn tập/Thi thử") → Match (mini-game, Phase 2/3, nice-to-have).

### 2.5 Bảng tổng hợp: lấy gì, bỏ gì, làm tới đâu

| Nguồn cảm hứng | Tính năng | Mức ưu tiên cho QuizHub |
|---|---|---|
| Azota | Nhập đề từ Excel, tự nhận diện câu hỏi/đáp án | **Bắt buộc — Phase 1** |
| Azota | Kiểm soát thời điểm hiện điểm/đáp án | Nên có — Phase 1/2 |
| Azota | Đề con random từ ngân hàng theo ma trận chủ đề/độ khó | Nâng cao — Phase 3 |
| Quizizz | Chế độ tự ôn theo tốc độ riêng (self-paced) | Đã có — chỉ cần đặt tên rõ |
| Quizizz | Ôn lại câu sai có game hoá (Mastery Peak) | Nâng cấp từ `retryWrong()` — Phase 2 |
| Quizizz | Multiplayer thời gian thực, leaderboard lớp | Tuỳ chọn, cần server — Phase 4 |
| Quizlet | Flashcard lật thẻ | Đã có |
| Quizlet | Học theo spaced repetition (Learn) | **Phase 2** |
| Quizlet | Mini-game ghép cặp (Match) | Tuỳ chọn — Phase 2/3 |
| baitaptracnghiem.com | Phân loại đề theo môn/cấp học | Phase 1 (cùng lúc với refactor đa đề) |

---

## 3. Quyết định kiến trúc cần chốt **trước khi viết code**

Trước khi vào chi tiết kỹ thuật, có một câu hỏi quan trọng ảnh hưởng tới toàn bộ thiết kế:

> **TutorHub Enterprise đã có sẵn các tab "Đề thi", "Câu hỏi", "Thi" ở sidebar — những tab này có dùng chung một backend/ngân hàng câu hỏi với máy chủ trung tâm không?**

- Nếu **có** một backend trung tâm cho "Đề thi"/"Câu hỏi": QuizHub nên **gọi API của backend đó** để lấy/lưu đề, thay vì tự lưu trữ local — tránh trùng lặp 2 ngân hàng câu hỏi khác nhau trong cùng 1 app.
- Nếu **QuizHub là module tự ôn tập độc lập** (cá nhân hoá, lưu local trên máy), không nhất thiết phải tích hợp ngay với backend đó.

**Giả định mặc định của kế hoạch này** (sẽ nêu rõ để bạn xác nhận lại với team): QuizHub được thiết kế là **module độc lập, lưu trữ local trước**, nhưng tách lớp Service rõ ràng (Mục 4.2) để **dễ dàng thay nguồn dữ liệu local bằng API backend sau này mà không phải sửa UI**. Đây là lựa chọn an toàn nhất khi chưa chắc về hạ tầng backend hiện có.

---

## 4. Kiến trúc tổng thể đề xuất

### 4.1 Sơ đồ tổng thể

```
┌─────────────────────────────── JavaFX Application ───────────────────────────────┐
│                                                                                    │
│   ┌───────────────┐        ┌───────────────────────────────────────────────┐     │
│   │  Toolbar/Menu │        │                  WebView                      │     │
│   │  (JavaFX FX)  │        │   (load quiz.html — UI giữ nguyên, mở rộng)   │     │
│   │ "Nhập đề Excel"│  ───▶ │                                                │     │
│   │ "Quản lý đề"  │        │   JS: renderQuestions(), grade(), flashcards…  │     │
│   └──────┬────────┘        └───────────────┬────────────────────────────────┘     │
│          │ FileChooser (.xlsx)             │  window.quizBridge.xxx(...)          │
│          ▼                                  ▼                                     │
│   ┌─────────────────────────── QuizBridge (JS↔Java) ─────────────────────────┐    │
│   │  importExcel(path) · listDecks() · getDeck(id) · saveAttempt(json) ·     │    │
│   │  getBestScore(deckId) · getCardProgress(deckId) · saveCardProgress(...)  │    │
│   └───────────────────────────────────┬───────────────────────────────────────┘    │
│                                        ▼                                           │
│              ┌─────────────────────── Service Layer (Java) ───────────────────┐    │
│              │ ExcelImportService │ DeckService │ AttemptService │ ProgressService│ │
│              └─────────────────────────────┬───────────────────────────────────┘    │
│                                             ▼                                       │
│                       ┌───────────────── Storage ─────────────────┐                 │
│                       │  SQLite (sqlite-jdbc) hoặc JSON file       │                 │
│                       │  trong thư mục dữ liệu app (AppData/...)   │                 │
│                       └─────────────────────────────────────────────┘                │
│                                             ▲ (tuỳ chọn, Phase 4)                    │
│                       ┌────────────────────┴────────────────────┐                   │
│                       │  REST API của TutorHub backend (nếu có)  │                   │
│                       └───────────────────────────────────────────┘                  │
└────────────────────────────────────────────────────────────────────────────────────┘
```

**Vì sao chọn cách này (Option A — JS↔Java Bridge) làm hướng đi chính, thay vì Option B (chạy local web server + `fetch()`)?**

| | **Option A: JSObject Bridge** (đề xuất chính) | **Option B: Local HTTP server nội bộ + fetch()** |
|---|---|---|
| Effort ban đầu | Thấp — tận dụng ngay `WebEngine.executeScript`/`JSObject` có sẵn trong JavaFX | Trung bình — cần thêm thư viện server nhúng (Javalin/Jetty) |
| Debug | Khó hơn — lỗi JS↔Java đôi khi khó trace | Dễ — có thể mở thẳng `localhost:port` trên Chrome để debug như web bình thường |
| Khả năng tái sử dụng UI cho web thật sau này | Thấp — code JS gắn chặt vào API `window.quizBridge` riêng của JavaFX | **Cao** — JS chỉ gọi `fetch('/api/...')`, sau này có thể trỏ sang server thật mà không sửa JS |
| Phù hợp giai đoạn | MVP nhanh (Phase 1–2) | Khi QuizHub đã ổn định, muốn mở rộng đa người dùng/đồng bộ (Phase 3–4) |

→ **Khuyến nghị:** làm Phase 1–2 theo **Option A** để ra MVP nhanh, nhưng **thiết kế lớp Service (Mục 4.2) độc lập với cách giao tiếp** (không để Service biết gì về JSObject hay HTTP) — để sang Phase 3/4 có thể bọc thêm một REST layer mỏng (Option B) tái dùng đúng các Service đó, không phải viết lại logic nghiệp vụ.

### 4.2 Lớp Service (Java) — trái tim của hệ thống, độc lập với UI

```java
public interface ExcelImportService {
    ImportResult importFile(Path excelFile);          // đọc & validate, trả về preview + lỗi
    Deck commitImport(ImportResult validatedResult);   // người dùng xác nhận → lưu thật
}

public interface DeckService {
    List<DeckSummary> listDecks();                     // cho màn hình "chọn đề" và "quản lý đề"
    Deck getDeck(String deckId);
    void deleteDeck(String deckId);
    void renameDeck(String deckId, String newTitle);
    Deck buildSubDeck(String parentDeckId, SubDeckRule rule); // Phase 3: đề con random theo ma trận
}

public interface AttemptService {
    String startAttempt(String deckId, RunOptions opts);
    void submitAttempt(String attemptId, List<AnswerRecord> answers);
    AttemptSummary getBestAttempt(String deckId);
    List<AttemptSummary> getHistory(String deckId);
}

public interface ProgressService {                      // cho Flashcard Leitner — Phase 2
    CardProgress getProgress(String questionId);
    void recordReview(String questionId, boolean correct);
    List<String> getDueQuestionIds(String deckId, int limit);
}
```

`QuizBridge` (lớp duy nhất biết về `JSObject`/WebView) chỉ là một **adapter mỏng** gọi xuống các Service này và convert qua/lại JSON. Toàn bộ logic nghiệp vụ nằm ở Service, **không nằm trong bridge và không nằm trong JS** — nhờ vậy có thể unit-test Service độc lập, không cần mở WebView mới test được logic chấm điểm/import.

### 4.3 Lưu trữ dữ liệu

| | **JSON file** (mỗi đề 1 file trong thư mục data) | **SQLite (sqlite-jdbc)** |
|---|---|---|
| Phù hợp khi | Số đề ít, chủ yếu đọc nguyên cả đề ra để chạy | Cần lọc/thống kê: theo chủ đề, độ khó, lịch sử nhiều lần làm, nhiều người dùng |
| Effort | Rất thấp (Jackson/Gson serialize trực tiếp) | Cần thiết kế bảng + migration đơn giản |
| Khuyến nghị | **Dùng cho `Deck`/câu hỏi** (đọc nhiều, sửa ít, mỗi đề là 1 đơn vị) | **Dùng cho `Attempt`, `CardProgress`** (ghi liên tục, cần query thống kê: điểm theo thời gian, độ chính xác theo chủ đề...) |

→ **Đề xuất hỗn hợp**: đề thi lưu file JSON (`/data/decks/<deckId>.json`, dễ backup/copy/chia sẻ thủ công giữa máy), còn kết quả làm bài + tiến độ flashcard lưu SQLite (truy vấn thống kê nhanh, không phải tự viết logic lọc/group trong Java).

### 4.4 Cầu nối Java ↔ WebView (vá lỗi `window.storage` hiện tại)

```java
WebEngine engine = webView.getEngine();
engine.getLoadWorker().stateProperty().addListener((obs, old, st) -> {
    if (st == Worker.State.SUCCEEDED) {
        JSObject window = (JSObject) engine.executeScript("window");
        window.setMember("quizBridge", new QuizBridge(deckService, attemptService, progressService));
    }
});
```

Phía JS (`quiz.html`), thay toàn bộ `window.storage.get/set` (đang gọi vào API không tồn tại) bằng:

```javascript
// Thay cho saveBest() cũ
function saveBest(deckId, correct, total, score){
  const json = JSON.stringify({correct, total, score});
  window.quizBridge.saveBestScore(deckId, json); // gọi đồng bộ sang Java, không cần async/await nữa
}
function loadBest(deckId){
  const raw = window.quizBridge.getBestScore(deckId); // trả về chuỗi JSON hoặc null
  return raw ? JSON.parse(raw) : null;
}
```

Việc chọn file Excel **nên làm bằng `FileChooser` của JavaFX** (native, hỗ trợ lọc `*.xlsx` tốt) thay vì `<input type="file">` trong WebView — sau khi người dùng chọn file, Java gọi `engine.executeScript("onExcelSelected('" + jsonPreview + "')")` để đẩy kết quả preview vào UI có sẵn.

---

## 5. Mô hình dữ liệu

```json
// Question
{
  "id": "deck-cnatt-01#row-12",
  "deckId": "deck-cnatt-01",
  "text": "Giá trị ngẫu nhiên thêm vào trước khi băm mật khẩu được gọi là?",
  "options": ["IV", "Hash", "Salt", "DIP"],
  "correct": [2],
  "explanation": "Muối (Salt) làm cho cùng một mật khẩu ở hai tài khoản sinh ra mã băm khác nhau.",
  "wrongExplanations": {"0": "IV dùng trong mã hoá khối...", "1": "Hash là kết quả băm...", "3": "..."},
  "topic": "Mật mã học",
  "difficulty": "medium",
  "imageUrl": null
}

// Deck
{
  "id": "deck-cnatt-01",
  "title": "Đề 4 — Mạng máy tính căn bản",
  "description": "Cơ bản · Theo chương 1-3",
  "subject": "Mạng máy tính",
  "color": "#1A4F8B",
  "source": "excel_import",          // "builtin" | "excel_import"
  "defaultOptions": {"shuffleQuestions": true, "showExplanationImmediately": true},
  "questionIds": ["deck-cnatt-01#row-1", "..."]
}

// Attempt (1 lần làm bài)
{
  "id": "attempt-uuid",
  "deckId": "deck-cnatt-01",
  "mode": "exam",                     // "study" | "exam"
  "startedAt": "2026-06-28T10:00:00",
  "finishedAt": "2026-06-28T10:22:00",
  "answers": [{"questionId": "...", "selected": [2], "correct": true, "timeMs": 4210}],
  "correctCount": 42, "totalCount": 50, "score": 8.4
}

// CardProgress (trạng thái Leitner — Phase 2)
{
  "questionId": "deck-cnatt-01#row-12",
  "box": 3,                           // 1 (mới/sai) → 5 (đã thuộc)
  "lastReviewedAt": "...",
  "nextDueAt": "...",
  "correctStreak": 2
}
```

> **Lưu ý kỹ thuật quan trọng:** `id` câu hỏi nên **ổn định theo `deckId + số dòng Excel lúc import`**, không phải theo vị trí sau khi trộn. Nếu sau này import lại cùng file Excel đã sửa nội dung, hệ thống **nên tạo phiên bản đề mới** (`deck-cnatt-01-v2`) thay vì ghi đè đề cũ, để không làm mất lịch sử `Attempt`/`CardProgress` đã gắn với ID câu hỏi cũ. Đây là điểm dễ bị bỏ qua nhưng ảnh hưởng trực tiếp đến tính đúng của tính năng "tiến độ ghi nhớ" ở Mục 7.2.

---

## 6. Định dạng file Excel chuẩn để import đề

### 6.1 Cấu trúc — đã đóng gói sẵn trong file `Mau_Import_Cau_Hoi_QuizHub.xlsx` đính kèm

File mẫu gồm 3 sheet:

1. **`Huong_dan`** — hướng dẫn nhập liệu, quy tắc, lỗi thường gặp (tiếng Việt, dùng được luôn để gửi cho người soạn đề không biết kỹ thuật).
2. **`Thong_tin_de`** — thông tin chung của đề (map trực tiếp vào object `Deck` ở Mục 5):

   | Trường | Ý nghĩa |
   |---|---|
   | `Ten_de` | Tiêu đề thẻ đề (giống "Đề 1", "Đề 2" hiện tại) |
   | `Mo_ta_ngan` | Dòng phụ đề dưới tiêu đề thẻ |
   | `Mon_hoc` | Dùng để gom nhóm nhiều đề theo môn |
   | `Mau_chu_de` | Mã màu HEX cho thẻ (tuỳ chọn) |
   | `Tron_cau_hoi_mac_dinh`, `Hien_giai_thich_ngay` | Map vào `defaultOptions` của Deck — đúng 2 công tắc đã có sẵn trong panel "Tuỳ chọn làm bài" hiện tại |

3. **`Cau_hoi`** — sheet chính, **mỗi dòng = 1 câu hỏi**:

   | Cột | Bắt buộc? | Ý nghĩa |
   |---|---|---|
   | `STT` | Không | Chỉ để người soạn dễ theo dõi, không dùng để parse |
   | `Chu_de` | Không | Dùng để lọc/thống kê (map vào `topic`) |
   | `Do_kho` | Không | `Dễ` / `Trung bình` / `Khó` (có dropdown sẵn trong file mẫu) |
   | `Cau_hoi` | **Có** | Nội dung câu hỏi |
   | `Dap_an_A` … `Dap_an_F` | **A, B bắt buộc**, C–F tuỳ chọn | Tối đa 6 lựa chọn; cột nào trống thì bỏ qua, không tính vào số lựa chọn |
   | `Dap_an_dung` | **Có** | Chữ cái viết HOA. 1 đáp án: `A`. Nhiều đáp án: `A,C` (ngăn cách bằng dấu phẩy) — **hệ thống tự suy ra single/multi-select từ số chữ cái ở đây**, không cần cột riêng khai báo loại câu |
   | `Giai_thich` | Khuyến nghị | Giải thích vì sao đáp án đúng — hiển thị ở Ôn tập + Flashcard |
   | `Giai_thich_dap_an_sai` | Không | Định dạng `B: lý do | C: lý do` — mỗi cặp `Chữ cái: nội dung` ngăn bằng `|` |
   | `Hinh_anh` | Không (Phase 2) | URL ảnh minh hoạ |

File mẫu đã điền sẵn **5 dòng ví dụ thật** (gồm cả câu 1 đáp án và câu nhiều đáp án đúng, có cả `Giai_thich_dap_an_sai`) để bạn/đội dev import thử ngay, không cần tự bịa dữ liệu test.

### 6.2 Logic parse phía Java (Apache POI) — minh hoạ

```java
public ImportResult parse(Path file) {
    List<Question> ok = new ArrayList<>();
    List<RowError> errors = new ArrayList<>();
    try (Workbook wb = new XSSFWorkbook(Files.newInputStream(file))) {
        Sheet sheet = wb.getSheet("Cau_hoi");
        Map<String,Integer> col = mapHeaderToColumnIndex(sheet.getRow(0)); // đọc header, không phụ thuộc thứ tự cột

        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isBlank(row, col.get("Cau_hoi"))) continue; // bỏ qua dòng trống

            String text = str(row, col.get("Cau_hoi"));
            List<String> options = readOptions(row, col, "Dap_an_A", "Dap_an_F"); // bỏ cột trống
            String correctRaw = str(row, col.get("Dap_an_dung"));

            if (text.isBlank() || options.size() < 2 || correctRaw.isBlank()) {
                errors.add(new RowError(r + 1, "Thiếu Cau_hoi/Dap_an/Dap_an_dung"));
                continue;
            }
            List<Integer> correctIdx = parseLetters(correctRaw, options.size()); // "A,C" -> [0,2]
            if (correctIdx.isEmpty() || correctIdx.contains(-1)) {
                errors.add(new RowError(r + 1, "Dap_an_dung không khớp với các lựa chọn hiện có"));
                continue;
            }
            ok.add(Question.builder()
                .text(text).options(options).correct(correctIdx)
                .explanation(str(row, col.get("Giai_thich")))
                .wrongExplanations(parseWrongExpl(str(row, col.get("Giai_thich_dap_an_sai"))))
                .topic(str(row, col.get("Chu_de")))
                .difficulty(str(row, col.get("Do_kho")))
                .build());
        }
    } catch (IOException e) { throw new ImportException("Không đọc được file Excel", e); }
    return new ImportResult(ok, errors);
}
```

Vài quyết định thiết kế đáng chú ý:
- **Đọc header theo tên cột, không theo vị trí cột cố định** (`mapHeaderToColumnIndex`) — để nếu người dùng vô tình đảo cột hoặc thêm cột mới, parser vẫn chạy đúng, miễn tên header không đổi.
- **Dòng lỗi không làm hỏng cả file** — chỉ dòng đó bị loại, các dòng hợp lệ khác vẫn import được, kèm báo cáo lỗi rõ số dòng + lý do (đúng tinh thần "preview trước khi commit" ở Mục 6.3).

### 6.3 Luồng xử lý & UX báo lỗi

```
Chọn file .xlsx
      │
      ▼
ExcelImportService.parse()  →  đọc toàn bộ, KHÔNG lưu gì cả
      │
      ▼
Màn hình Preview:
  ✓ 48 câu hợp lệ sẽ được tạo
  ✗ 2 câu lỗi:
      - Dòng 15: Thiếu Dap_an_dung
      - Dòng 31: Dap_an_dung ghi "E" nhưng chỉ có A–D
  [ Huỷ ]                         [ Tạo đề với 48 câu hợp lệ ]
      │
      ▼ (người dùng xác nhận)
DeckService lưu Deck mới → xuất hiện thẻ đề mới trong màn hình chọn đề
```

Nguyên tắc: **không bao giờ import "ngầm" mà không cho xem trước** — đúng theo cách Azota hiển thị bước "nhận diện & xem giải thích chi tiết" trước khi đề được đưa vào dùng thật, tránh trường hợp đề bị sai mà người dùng không biết.

---

## 7. Tính năng chi tiết theo module

### 7.1 Trắc nghiệm (Ôn tập & Thi thử) — nâng cấp từ cái đã có

Giữ nguyên toàn bộ UI/logic hiện tại (`renderQuestions`, `grade`, `buildExplHtml`, nav grid, search...). Bổ sung:

- **Đa đề động**: màn hình `#menu` sinh thẻ đề bằng JS từ `DECKS` (object động, nạp từ `quizBridge.listDecks()`) thay vì 3 thẻ HTML cứng.
- **Kiểm soát thời điểm hiện điểm/đáp án** (học theo Azota): thêm option `revealPolicy` trong `RunOptions`: `always` (như hiện tại) / `after_submit` / `never_during_session`.
- **Phân loại theo Chủ đề/Độ khó**: dùng để lọc trước khi bắt đầu ("chỉ làm câu Khó", "chỉ chương 2") — tận dụng cột `Chu_de`/`Do_kho` đã có trong Excel.

### 7.2 Flashcard — nâng cấp kiểu Quizlet (Leitner spaced repetition)

Thay flashcard tuyến tính hiện tại bằng **hệ thống 5 hộp Leitner** (bản đơn giản hoá của spaced repetition, dễ cài đặt offline, không cần thuật toán SM-2 phức tạp):

| Hộp | Khi nào được ôn lại | Quy tắc chuyển hộp |
|---|---|---|
| 1 (mới/hay sai) | Mỗi buổi học | Trả lời đúng → lên hộp 2; sai → giữ hộp 1 |
| 2 | Sau 1 ngày | Đúng → hộp 3; sai → về hộp 1 |
| 3 | Sau 3 ngày | Đúng → hộp 4; sai → về hộp 1 |
| 4 | Sau 7 ngày | Đúng → hộp 5; sai → về hộp 1 |
| 5 (đã thuộc) | Sau 14 ngày (ôn xác nhận) | Đúng → giữ hộp 5 (thuộc); sai → về hộp 1 |

Màn Flashcard có thêm 2 chế độ chọn bộ thẻ để học (giống cách Quizlet tách "Flashcards" và "Learn"):
- **"Lật thẻ tự do"** (giữ y nguyên UX hiện tại — `flipCard`, `fcNext/fcPrev/fcShuffle`).
- **"Học thông minh" (mới)**: chỉ lấy các thẻ **đến hạn ôn** (`ProgressService.getDueQuestionIds`), sau mỗi thẻ hỏi "Bạn nhớ đúng không?" (Có/Không) để cập nhật hộp — đây là phần tạo cảm giác "app biết tôi học tới đâu" giống Quizlet Learn.

### 7.3 Game hoá kiểu Quizizz

Vì QuizHub hiện là ứng dụng **chạy local trên máy người dùng** (không có sẵn hạ tầng multiplayer thời gian thực), nên ưu tiên các yếu tố game hoá **không cần server**:

- **Điểm theo tốc độ + độ chính xác**: trả lời đúng trong < 10s được điểm thưởng (giống cơ chế tính điểm theo thời gian phản hồi của Quizizz), hiển thị ngay dưới mỗi câu ở chế độ Thi thử.
- **Streak (chuỗi đúng liên tiếp)**: hiển thị ở thanh trên cùng, có hiệu ứng nhỏ khi đạt mốc 5/10/20 câu đúng liên tiếp.
- **Huy hiệu cuối bài** (badge): "Không sai câu nào", "Về đích trong giờ", "Cải thiện so với lần trước" — tính từ so sánh `Attempt` hiện tại với `getBestAttempt()`.
- **Mini-game "Ghép cặp"** (lấy cảm hứng từ Match của Quizlet): kéo-thả nối câu hỏi với đáp án đúng trong thời gian giới hạn — dùng chính dữ liệu `Question` đã có, không cần thêm loại dữ liệu mới, phù hợp làm trong Phase 2/3 vì là tính năng độc lập, không phá vỡ luồng chính.
- *Để Phase 4 (cần backend)*: bảng xếp hạng theo lớp, thi đua thời gian thực nhiều người cùng lúc — chỉ làm khi đã có server, vì đây là tính năng tốn hạ tầng nhất trong toàn bộ kế hoạch.

### 7.4 Quản lý đề & ngân hàng câu hỏi

- Màn hình **"Quản lý đề"** mới (JavaFX, ngoài WebView hoặc 1 trang riêng trong WebView): danh sách đề (built-in + đã import), nút Nhập đề từ Excel / Xoá / Đổi tên / Xem nhanh số câu theo chủ đề.
- *Phase 3*: **"Đề con random theo ma trận"** — chọn ví dụ "20 câu Dễ + 15 câu Trung bình + 15 câu Khó, ưu tiên chủ đề X" từ một ngân hàng lớn → tạo `Deck` tạm để làm bài, không sửa ngân hàng gốc (đúng tinh thần đề cha/đề con của Azota).

### 7.5 Thống kê & tiến độ học tập

- Lịch sử điểm theo thời gian cho mỗi đề (biểu đồ đơn giản: điểm các lần làm gần nhất).
- Tỷ lệ đúng/sai theo `Chu_de` → chỉ ra "đang yếu chương nào" (truy vấn SQLite theo `Attempt.answers` join `Question.topic`).
- Số thẻ đã "thuộc" (hộp 5) / tổng số thẻ trong đề, cho riêng chế độ Flashcard.

---

## 8. Lộ trình triển khai theo giai đoạn

> Ước lượng effort chỉ mang tính tham khảo cho 1 dev full-time/quen JavaFX; điều chỉnh theo năng lực thực tế của team.

### Phase 1 — Nền tảng nhập đề (≈ 2–3 tuần) — **MVP phải có**
- [ ] Refactor `DECKS` từ hard-code sang registry động trong JS (`registerDeck`).
- [ ] Viết `ExcelImportService` (Apache POI) + validate + preview lỗi theo Mục 6.
- [ ] Viết `QuizBridge` + vá lỗi `window.storage` giả bằng bridge thật (Mục 4.4).
- [ ] Lưu `Deck` bằng JSON file, `Attempt`/best score bằng SQLite.
- [ ] Màn hình "Quản lý đề" cơ bản (liệt kê, nhập, xoá).
- [ ] **Kết quả:** người dùng tải file Excel mẫu lên → có đề mới chạy được ngay trong giao diện làm bài hiện tại.

### Phase 2 — Flashcard thông minh + Game hoá cơ bản (≈ 2–3 tuần)
- [ ] `ProgressService` + Leitner system (Mục 7.2), thêm chế độ "Học thông minh".
- [ ] Điểm theo tốc độ, streak, huy hiệu cuối bài (Mục 7.3).
- [ ] Nâng cấp `retryWrong()` hiện có thành luồng "ôn câu sai" có ghi nhận vào `CardProgress`.

### Phase 3 — Ngân hàng câu hỏi & thống kê (≈ 2–3 tuần)
- [ ] Lọc theo Chủ đề/Độ khó trước khi làm bài.
- [ ] "Đề con" random theo ma trận từ ngân hàng lớn.
- [ ] Trang thống kê: biểu đồ điểm theo thời gian, tỷ lệ đúng theo chủ đề, % thẻ đã thuộc.
- [ ] Mini-game "Ghép cặp" (tuỳ chọn, có thể làm song song nếu dư thời gian).

### Phase 4 — Mở rộng nâng cao (tuỳ nhu cầu thực tế, ưu tiên sau)
- [ ] Nếu có backend TutorHub cho "Đề thi"/"Câu hỏi": bọc thêm REST layer mỏng theo Option B (Mục 4.1) để đồng bộ 2 chiều, tránh 2 ngân hàng câu hỏi tách biệt.
- [ ] Thi đua/leaderboard nhiều người thời gian thực — cần server, nên đặt cuối vì là phần tốn hạ tầng nhất.
- [ ] Tạo câu hỏi tự động từ tài liệu (Word/PDF) bằng AI — tương tự hướng Azota/Quizizz đang phát triển, cần tích hợp thêm dịch vụ AI bên ngoài.
- [ ] Chế độ "Thi thử" có giám sát nhẹ (cảnh báo chuyển tab/mất focus cửa sổ) cho mục đích thi nghiêm túc hơn.

---

## 9. Công nghệ & thư viện đề xuất

| Nhu cầu | Lựa chọn | Lý do |
|---|---|---|
| Đọc/viết Excel | **Apache POI** (`poi`, `poi-ooxml`) | Chuẩn de-facto cho Java, đọc `.xlsx` ổn định, đọc theo tên cột dễ dàng |
| Serialize JSON (Java↔JS) | **Jackson** hoặc **Gson** | Map trực tiếp các class ở Mục 5 sang/từ JSON |
| Lưu kết quả/tiến độ | **sqlite-jdbc** (SQLite nhúng) | Không cần cài server DB riêng, vẫn truy vấn SQL được cho thống kê |
| Cầu nối JS↔Java | `javafx.scene.web.WebEngine` + `netscape.javascript.JSObject` (có sẵn trong JavaFX) | Không cần thư viện ngoài |
| (Tuỳ chọn Phase 4) Local REST server | **Javalin** (nhẹ, ít phụ thuộc) hoặc `com.sun.net.httpserver` (có sẵn trong JDK, không cần thêm dependency) | Nếu sau này muốn JS gọi `fetch()` thay cho bridge, hoặc muốn tái dùng UI cho 1 bản web thật |
| Chọn file Excel | `javafx.stage.FileChooser` (native) | Trải nghiệm chọn file chuẩn OS, lọc đuôi `*.xlsx` |

---

## 10. Rủi ro, lưu ý kỹ thuật & câu hỏi mở

1. **Câu hỏi kiến trúc cần chốt sớm nhất** (đã nêu ở Mục 3): QuizHub độc lập hay tích hợp với backend "Đề thi"/"Câu hỏi" hiện có của TutorHub? Câu trả lời này ảnh hưởng tới việc có cần làm Option B (REST) sớm hơn dự kiến hay không.
2. **ID câu hỏi ổn định** (Mục 5) — nếu bỏ qua bước này ngay từ Phase 1, các tính năng Leitner/thống kê ở Phase 2-3 sẽ phải làm lại từ đầu vì không có gì để gắn tiến độ vào.
3. **Người soạn đề không phải dev** — vì vậy sheet `Huong_dan` trong file Excel mẫu và bước "Preview trước khi commit" (Mục 6.3) quan trọng hơn là tối ưu kỹ thuật của parser: ưu tiên thông báo lỗi rõ ràng, dễ tự sửa, hơn là cố "đoán" ý người nhập khi dữ liệu mơ hồ.
4. **Đề rất lớn (≥ 500 câu)**: cân nhắc lazy-load câu hỏi theo trang trong JS thay vì render hết DOM một lần (hiện tại `renderQuestions()` render toàn bộ — với 86 câu vẫn ổn, nhưng nên để ý nếu ngân hàng câu hỏi phát triển lớn ở Phase 3).
5. **Ảnh minh hoạ trong câu hỏi** (cột `Hinh_anh`): để Phase 2, cần quyết định ảnh lưu local (copy file vào thư mục data) hay chỉ chấp nhận URL — ảnh hưởng tới việc đề có "mang đi" được sang máy khác hay không.

---

## 11. Phụ lục: File mẫu đính kèm

- **`Mau_Import_Cau_Hoi_QuizHub.xlsx`** — đúng theo đặc tả Mục 6, gồm 3 sheet (`Huong_dan`, `Thong_tin_de`, `Cau_hoi`), có 5 dòng câu hỏi ví dụ thật (đủ dạng 1 đáp án + nhiều đáp án đúng) để import thử ngay với bộ parser khi bắt đầu Phase 1.
