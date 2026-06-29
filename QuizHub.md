Chào Claude, tôi đang phát triển **Chế độ Trò Chơi / Game Mode** cho tab **QuizHub** trong app desktop Java **TutorHub Enterprise**.

QuizHub hiện đang dùng:

```text id="ird883"
Java desktop app
JCEF / Chromium Embedded Framework
Frontend HTML/CSS/JavaScript
Render trong tab QuizHub bằng JCEF
JS giao tiếp với Java bằng window.cefQuery
```

Tôi muốn nâng cấp QuizHub thành chế độ game trắc nghiệm sống động giống cảm giác của:

```text id="d7h04q"
Quizizz / Wayground
Kahoot
Blooket
Gimkit
Quizlet game modes
Duolingo lesson game UI
```

Mục tiêu là có giao diện, hiệu ứng, âm thanh, điểm số, streak, timer, feedback đúng/sai, huy hiệu và màn tổng kết chuyên nghiệp như một game quiz thật.

---

# 1. File/tài liệu tôi sẽ gửi cho bạn

Bạn là Claude chat thường, không phải AI Agent, nên bạn không thể tự đọc project. Tôi sẽ gửi kèm:

```text id="nmjaqt"
1. docs/QuizHub/QuizHub_Architecture_Report.md
2. src/main/resources/tse/quiz.html
3. JSON deck mẫu trong AppData/TutorHub/quizhub/decks
4. src/main/java/com/mycompany/tutorhub_enterprise/client/quizhub/bridge/QuizHubBridge.java nếu cần lưu điểm/lịch sử
```

Bạn phải đọc kỹ các file tôi gửi. Không được tự bịa class, function, biến hoặc file không tồn tại.

---

# 2. Việc đầu tiên: đọc kiến trúc JCEF trước

Trước khi đề xuất thư viện hoặc code, hãy đọc kỹ:

```text id="jy291r"
docs/QuizHub/QuizHub_Architecture_Report.md
```

Bạn cần xác định rõ:

```text id="ifb35a"
- QuizHub đang chạy bằng JCEF.
- quiz.html được render như thế nào.
- JS gọi Java bằng window.cefQuery ra sao.
- Hiện đã có helper wrapper nào quanh cefQuery chưa.
- quiz.html hiện đang load deck/question như thế nào.
- Có đang dùng local file/resource path nào cần giữ không.
```

Quy tắc bắt buộc:

```text id="wx9p27"
Không dùng fetch().
Không dùng XMLHttpRequest.
Không dùng JavaFX JSObject bridge.
Không dùng window.quizBridge nếu kiến trúc hiện tại dùng cefQuery.
Không tự tạo backend HTTP.
Không gọi API server mới.
Không sửa backend Đề thi/Câu hỏi/TSE.
```

Nếu cần lưu điểm hoặc lịch sử game, phải dùng đúng pattern `window.cefQuery` hiện có.

---

# 3. Nhiệm vụ nghiên cứu thư viện

Trước khi code, hãy nghiên cứu các nền tảng lớn và thư viện phổ biến dùng cho game quiz/web game hiện đại.

Hãy nghiên cứu và so sánh:

```text id="gdxud8"
1. Animation engine:
   - GSAP
   - Anime.js
   - CSS animation thuần
   - Web Animations API

2. Âm thanh:
   - Howler.js
   - Web Audio API thuần
   - HTMLAudioElement

3. Confetti/celebration:
   - canvas-confetti
   - party.js
   - custom Canvas API

4. Character/motion asset:
   - Lottie-web
   - CSS sprite
   - SVG animation

5. Particle/background:
   - tsParticles
   - particles.js
   - custom Canvas particle

6. Game engine:
   - Phaser.js
   - Canvas API thuần
```

Hãy đánh giá theo tiêu chí:

```text id="su5ai7"
- Có phù hợp JCEF không
- Có chạy tốt trong app desktop offline không
- Có nhẹ không
- Có dễ tích hợp vào quiz.html hiện tại không
- Có cần framework như React không
- Có quá nặng so với nhu cầu QuizHub không
- Có giấy phép sử dụng phù hợp không
- Có thể vendored local vào src/main/resources không
```

Không được chọn thư viện chỉ vì đẹp. Phải chọn theo độ ổn định, hiệu năng, khả năng chạy offline trong JCEF và độ phù hợp với quiz.html hiện tại.

---

# 4. Quyết định thư viện mong muốn

Tôi muốn bạn đưa ra quyết định rõ ràng:

```text id="mo86lx"
Nên dùng thư viện nào.
Không nên dùng thư viện nào.
Thư viện nào để Phase sau.
Thư viện nào là bắt buộc.
Thư viện nào là optional.
```

Gợi ý hướng tôi đang cân nhắc:

```text id="l3w64c"
Core stack có thể là:
- GSAP: animation câu hỏi, đáp án, score, streak, transition
- Howler.js: âm thanh đúng/sai/tick/level up
- canvas-confetti: pháo giấy khi đúng và màn tổng kết

Optional:
- Lottie-web: mascot/badge animation nếu có asset local
- tsParticles: background/particle nếu không quá nặng
- Animate.css: chỉ dùng nếu thật sự cần class animation nhanh

Không ưu tiên:
- Phaser.js cho Phase này, vì QuizHub chỉ cần quiz game UI, chưa phải game engine phức tạp.
```

Nhưng bạn phải tự nghiên cứu và ra quyết định cuối cùng, không làm theo máy móc.

---

# 5. Quy tắc tích hợp thư viện trong app desktop

Vì đây là app desktop JCEF, không được phụ thuộc CDN ở runtime.

Không làm production theo kiểu:

```html id="8bfmes"
<script src="https://cdnjs.cloudflare.com/..."></script>
```

Thay vào đó, nếu chọn thư viện, hãy đề xuất vendored local:

```text id="ng9a4n"
src/main/resources/tse/vendor/gsap.min.js
src/main/resources/tse/vendor/howler.min.js
src/main/resources/tse/vendor/confetti.browser.min.js
src/main/resources/tse/vendor/lottie.min.js nếu cần
src/main/resources/tse/vendor/animate.min.css nếu cần
```

Trong `quiz.html`, load bằng path local tương thích với resource hiện tại.

Bạn không cần paste toàn bộ mã nguồn minified của thư viện vào câu trả lời. Chỉ cần:

```text id="dhlfu1"
- Ghi tên thư viện
- Version khuyến nghị
- Link nguồn chính thức/npm/cdn để tôi tự tải
- Đường dẫn local nên đặt trong project
- Đoạn HTML script/link để nhúng local
- Code tích hợp sử dụng thư viện đó
```

Nếu không chắc project load resource tương đối thế nào trong JCEF, hãy ghi rõ cần kiểm tra path hiện tại trong `quiz.html`.

---

# 6. Quyết định kiến trúc QuizHub

QuizHub là module độc lập:

```text id="5i1hh5"
QuizHub không dùng chung backend với Đề thi.
QuizHub không dùng chung backend với Ngân hàng câu hỏi.
QuizHub không dùng chung backend với Thi/TSE.
QuizHub không ghi vào bảng question_banks/questions/question_options/exam_papers.
QuizHub không gọi ExamController, QuestionBankTab, TSE hoặc backend thi bảo mật.
```

QuizHub là module:

```text id="vts6yq"
- Ôn tập cá nhân
- Local-first
- Dùng JCEF để render HTML/CSS/JS
- Deck được lưu riêng trong AppData/TutorHub/quizhub/decks
- Có thể import Excel riêng
- Có thể lưu điểm/progress riêng thông qua QuizHubBridge
```

---

# 7. File frontend chính cần sửa

File quan trọng nhất:

```text id="c9qznm"
src/main/resources/tse/quiz.html
```

Hiện file này có thể đang chứa toàn bộ:

```text id="1e75lx"
HTML
CSS
JavaScript
Menu chọn đề
Chế độ Flashcard
Chế độ Ôn tập
Chế độ Thi thử
Logic hiển thị câu hỏi
Logic chấm điểm
Logic giải thích đáp án
```

Yêu cầu:

```text id="kw9x1t"
- Đọc kỹ quiz.html trước khi sửa.
- Tái sử dụng class CSS hiện có nếu phù hợp.
- Không viết lại toàn bộ file nếu chỉ cần patch.
- Không xóa các mode cũ.
- Không phá Flashcard.
- Không phá Ôn tập.
- Không phá Thi thử.
- Không đổi schema deck/question nếu không cần.
```

---

# 8. Dữ liệu deck/question mẫu

Đây là cấu trúc deck QuizHub hiện đang dùng:

```json id="zdfqpa"
{
  "id": "de-10-mang-may-tinh-1782670948594",
  "title": "Đề 10 - Mạng máy tính",
  "description": "Cơ bản - Theo chương 1-3",
  "subject": "Mạng máy tính",
  "color": "#1A4F8B",
  "source": "excel_import",
  "createdAt": "2026-06-28T18:22:28.594Z",
  "updatedAt": "2026-06-28T18:22:28.594Z",
  "defaultOptions": {
    "shuffleQuestions": true,
    "showExplanationImmediately": true
  },
  "questions": [
    {
      "id": "de-10-mang-may-tinh-1782670948594#row-2",
      "deckId": "de-10-mang-may-tinh-1782670948594",
      "text": "Giá trị ngẫu nhiên thêm vào trước khi băm mật khẩu được gọi là gì?",
      "options": [
        "IV",
        "Hash",
        "Salt",
        "DIP"
      ],
      "correct": [
        2
      ],
      "explanation": "Muối (Salt) làm cho cùng một mật khẩu ở 2 tài khoản sinh ra mã băm khác nhau.",
      "wrongExplanations": {
        "0": "IV dùng trong mã hoá khối...",
        "1": "Hash là hàm băm..."
      }
    }
  ]
}
```

Cách dùng:

```text id="yntys9"
deck.questions -> danh sách câu hỏi
question.text -> nội dung câu hỏi
question.options -> các lựa chọn đáp án
question.correct -> mảng index đáp án đúng
question.correct = [2] nghĩa là options[2] là đáp án đúng
question.explanation -> giải thích đáp án đúng
question.wrongExplanations -> giải thích đáp án sai theo index
```

Game Mode phải dùng đúng cấu trúc này.

---

# 9. Chế độ mới cần thêm

Thêm mode mới:

```text id="b4yyud"
Chế độ Trò Chơi
Game Mode
```

Game Mode cần có:

```text id="xjy55m"
- Câu hỏi theo từng vòng
- Mỗi câu có timer riêng
- Trả lời càng nhanh càng được nhiều điểm
- Streak đúng liên tiếp
- Combo/multiplier nhẹ
- Hiệu ứng đúng/sai rõ ràng
- Confetti/particle
- Âm thanh đúng/sai/tick/level up
- Tổng kết cuối game
- Huy hiệu/badge
```

Không thay thế các mode cũ. Game Mode là một lựa chọn mới.

---

# 10. Game logic cần thêm

## 10.1 Game state

Tạo state riêng, ví dụ:

```javascript id="o0ntei"
const gameState = {
  deckId: null,
  deckTitle: "",
  questions: [],
  currentIndex: 0,
  score: 0,
  streak: 0,
  bestStreak: 0,
  correctCount: 0,
  wrongCount: 0,
  totalQuestions: 0,
  questionStartTime: 0,
  timeLimitPerQuestion: 20,
  timerId: null,
  answers: [],
  badges: [],
  soundEnabled: true
};
```

Không làm rối state cũ.

## 10.2 Scoring

Cơ chế điểm gợi ý:

```text id="tkz7yk"
Đúng: 100 điểm cơ bản
Speed bonus: tối đa +50 điểm nếu trả lời nhanh
Streak bonus: +10 hoặc multiplier nhẹ khi streak cao
Sai: 0 điểm câu đó
Hết giờ: 0 điểm câu đó
```

## 10.3 Timer theo câu

```text id="drrfnx"
- Mặc định 20 giây/câu.
- Thanh thời gian giảm dần.
- Khi còn 5 giây, timer pulse nhẹ.
- Hết giờ thì hiện timeout, đánh dấu sai, rồi chuyển tiếp.
```

## 10.4 Feedback đúng/sai

Nếu đúng:

```text id="3f6wtn"
- Đáp án được chọn chuyển xanh.
- Có glow.
- Có particle/confetti nhỏ.
- Hiện “Chính xác!”
- Hiện + điểm.
- Tăng streak.
```

Nếu sai:

```text id="eej8l3"
- Đáp án chọn sai chuyển đỏ.
- Đáp án đúng hiện xanh.
- Card/câu hỏi shake nhẹ.
- Hiện “Chưa đúng”.
- Reset streak.
- Hiện explanation hoặc wrongExplanations nếu có.
```

## 10.5 Badge/huy hiệu

Cuối game hiển thị:

```text id="v98fmo"
- Không sai câu nào
- Nhanh như chớp
- Chuỗi đúng 5+
- Chuỗi đúng 10+
- Về đích
- Cải thiện điểm nếu có dữ liệu điểm cũ
```

Nếu chưa có dữ liệu điểm cũ thì không tự bịa.

---

# 11. UI/UX Game Mode

## 11.1 Nút vào Game Mode

Trong menu chọn đề hoặc màn chọn mode, thêm:

```text id="xzbm9d"
Chế độ Trò Chơi
```

Mô tả:

```text id="3hwcax"
Trả lời nhanh, giữ streak, nhận điểm thưởng và huy hiệu.
```

## 11.2 Game HUD

Trong Game Mode, phía trên có HUD:

```text id="5z3zw0"
- Điểm
- Streak
- Câu hiện tại / tổng câu
- Timer
- Progress bar
```

## 11.3 Question screen

Câu hỏi:

```text id="lm53wz"
- Card lớn, rõ
- Typography mạnh
- Có số câu
- Có progress
```

Đáp án:

```text id="tbdpda"
- Dạng 2x2 grid ở desktop
- Mỗi đáp án là card lớn
- Có nhãn A/B/C/D
- Màu sắc khác nhau nhẹ
- Hover rõ
- Click có ripple
- Đúng/sai có trạng thái cực rõ
```

## 11.4 Result screen

Cuối game:

```text id="wa904z"
- Điểm tổng
- Số câu đúng/sai
- Tỷ lệ chính xác
- Best streak
- Thời gian hoàn thành
- Badge đạt được
- Nút chơi lại
- Nút làm lại câu sai nếu code cũ có
- Nút về menu
```

---

# 12. Hiệu ứng đồ họa cần thêm

Nếu chọn thư viện, hãy tận dụng đúng vai trò:

```text id="yjue7s"
GSAP:
- transition câu hỏi
- animation đáp án
- score count-up
- streak pop
- timer bar
- badge unlock

Howler.js:
- correct sound
- wrong sound
- tick sound
- level up/badge sound
- optional background loop nhẹ

canvas-confetti:
- confetti khi đúng
- confetti màn tổng kết
- side cannon confetti nếu đạt điểm cao

CSS/custom:
- ripple click
- glow correct
- shake wrong
- reduced motion fallback
```

Nếu không dùng thư viện nào, hãy giải thích tại sao và dùng Canvas/Web Audio/CSS thuần thay thế.

---

# 13. Âm thanh

Âm thanh phải optional:

```text id="mnzzw5"
- Có nút bật/tắt sound.
- Không tự phát nhạc nền quá khó chịu.
- Không dùng file online.
- Có thể dùng Web Audio API tạo beep nếu chưa có asset.
- Nếu dùng Howler.js, có thể dùng sound sprite local sau này.
```

Nếu chưa có file âm thanh local, hãy code fallback bằng Web Audio API hoặc để sound wrapper trống nhưng không crash.

---

# 14. JCEF compatibility

Vì chạy trong JCEF:

```text id="b4anrl"
- Không dùng framework.
- Không dùng CDN trong production.
- Không dùng import/export module.
- Không dùng server API mới.
- Ưu tiên vanilla JS + thư viện local vendored.
- Có thể dùng CSS animation/canvas/Web Audio.
- Không dùng tính năng quá mới nếu không cần.
```

Hỗ trợ:

```css id="xo28hl"
@media (prefers-reduced-motion: reduce)
```

Nếu reduced motion:

```text id="0cg4lb"
- Giảm confetti.
- Giảm shake.
- Giảm particle.
- Giữ UI ổn định.
```

---

# 15. Không được làm

```text id="23gqy6"
Không sửa backend Đề thi/Câu hỏi/TSE.
Không dùng fetch().
Không dùng XMLHttpRequest.
Không dùng JavaFX JSObject bridge.
Không dùng API server mới.
Không thêm thư viện mà không giải thích lý do.
Không load CDN trong production.
Không paste toàn bộ mã nguồn minified của thư viện vào câu trả lời.
Không dùng ảnh online runtime.
Không copy source/asset/logo Quizizz.
Không phá chế độ Ôn tập.
Không phá chế độ Thi thử.
Không phá Flashcard.
Không đổi schema deck/question nếu không bắt buộc.
Không làm mất window.cefQuery bridge hiện tại.
```

---

# 16. Cách làm bắt buộc

Trước khi code, hãy trả lời ngắn:

```text id="tnfwjj"
1. Bạn hiểu gì về kiến trúc QuizHub JCEF + cefQuery hiện tại.
2. Bạn nghiên cứu được gì từ Quizizz/Kahoot/Blooket/Gimkit/Duolingo.
3. Bảng so sánh thư viện: GSAP, Howler, canvas-confetti, Lottie, tsParticles, Phaser, Animate.css.
4. Quyết định cuối cùng: chọn thư viện nào, bỏ thư viện nào, để Phase sau thư viện nào.
5. Game Mode của TutorHub nên gồm những cơ chế gì.
6. File/function hiện tại nào cần sửa.
7. Rủi ro dễ làm hỏng logic cũ là gì.
```

Sau đó mới code.

Nếu `quiz.html` là file lớn chứa cả HTML/CSS/JS, hãy chia patch theo nhóm:

```text id="i353bq"
Patch 1: Vendor/library loading local
Patch 2: HTML markup cho Game Mode
Patch 3: CSS theme/effects
Patch 4: JS game state + scoring engine
Patch 5: JS render game question + answer feedback
Patch 6: Result screen + badges + confetti
Patch 7: Optional cefQuery save score nếu cần
```

Nếu HTML/CSS/JS đã tách file, hãy trả code theo file.

---

# 17. Output tôi cần

Hãy trả về:

```text id="sk8ba9"
1. Phân tích ngắn về kiến trúc hiện tại
2. Nghiên cứu thư viện và nền tảng quiz game lớn
3. Stack thư viện đề xuất
4. Danh sách file vendor cần tải về và đặt ở đâu
5. Patch code cụ thể
6. Những logic cũ đã giữ nguyên
7. Cách test sau khi copy code
```

Acceptance criteria:

```text id="euz8kv"
1. Tab QuizHub vẫn mở được.
2. Các mode cũ vẫn chạy.
3. Có thêm Game Mode / Chế độ Trò Chơi.
4. Chọn deck rồi vào Game Mode được.
5. Có timer theo câu.
6. Có score, speed bonus, streak, badge.
7. Đáp án có animation đúng/sai.
8. Có confetti hoặc fallback.
9. Có âm thanh hoặc fallback không crash.
10. Không dùng fetch/XMLHttpRequest.
11. Không dùng CDN trong production.
12. Không sửa backend Đề thi/Câu hỏi/TSE.
13. Không phá window.cefQuery bridge.
```

Sau đây là tài liệu/code tôi gửi:

```text id="u7djdp"
1. docs/QuizHub/QuizHub_Architecture_Report.md
2. src/main/resources/tse/quiz.html
3. JSON deck mẫu
4. QuizHubBridge.java nếu cần
```
