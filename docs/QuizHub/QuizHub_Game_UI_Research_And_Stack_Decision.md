# QuizHub Game UI Research And Stack Decision

Ngay: 2026-07-02

## 1. Kien truc hien tai

QuizHub la UI web nhung chay trong desktop app Java qua JCEF/Chromium. File render chinh la `src/main/resources/tse/quiz.html`, gom HTML/CSS/JS inline. Java khong mo HTTP backend rieng cho QuizHub; JS goi Java bang `window.cefQuery`.

Luong hien tai:

- `LIST_DECKS` tai danh sach de qua `QuizHubBridge.listDecks()`.
- `GET_DECK:<deckId>` tai noi dung de.
- `GET_BEST_SCORE:<deckId>` va `SAVE_BEST_SCORE:<deckId>|<json>` luu diem cao nhat.
- Du lieu de dung schema deck/question hien co: `deck.questions`, `question.text`, `question.options`, `question.correct`, `question.explanation`, `question.wrongExplanations`.

Gioi han bat buoc:

- Khong doi sang `fetch`/`XMLHttpRequest`.
- Khong doi sang JavaFX WebView bridge.
- Khong tao backend HTTP moi.
- Khong sua backend de thi/TSE.

## 2. Pattern hoc tu quiz game lon

Nguon tham khao chinh: Kahoot, Blooket, Gimkit, Wayground/Quizizz, Duolingo lesson UI va Quizlet game/test mode. Khong copy logo, mau nhan dien, asset hay source.

Nhung pattern nen chuyen hoa vao TutorHub:

- Man chon de can co card ro rang hon: title, subject/source, so cau, diem cao nhat, CTA on tap va choi.
- Man chon che do can gan voi card de, dung segmented controls/toggles de nguoi hoc scan nhanh.
- Man lam tung cau can co "question stage" noi bat hon, dap an co mau/hover/selected/correct/wrong ro.
- HUD game can co diem, timer ring, tien do, streak va feedback tuc thi.
- Dung/sai nen co micro-interaction: correct glow, wrong shake, score fly-up, confetti nhe.
- Man tong ket can co count-up score, accuracy, best streak, thoi gian, badge.
- Empty/loading/error state can duoc thiet ke nhu mot state san pham, khong de text tran.

## 3. So sanh thu vien do hoa/am thanh

| Cong nghe | Phu hop JCEF/offline | Nhe/doi framework | De vendor local | License/ghi chu | Quyet dinh |
|---|---|---:|---:|---|---|
| Anime.js | Tot, dang co `vendor/anime.min.js` v3.2.2 | Nhe, khong can framework | Co | MIT, local file da co | Dung ngay |
| canvas-confetti | Tot, dang co `vendor/confetti.browser.min.js` v1.9.3 | Rat nhe | Co | ISC, local file da co | Dung ngay |
| Web Audio API | Built-in trong Chromium/JCEF | Khong them lib | Khong can | Web API | Dung ngay fallback am thanh |
| CSS animation | Built-in | Nhe nhat | Khong can | CSS | Dung ngay fallback animation |
| Web Animations API | Built-in | Nhe | Khong can | Web API | Dung co chon loc |
| Canvas API | Built-in | Nhe | Khong can | Web API | Dung cho capture/particle don gian neu can |
| GSAP | Tot, manh hon Anime.js | Them vendor moi | Co | GSAP hien free, can kiem terms truoc release | De phase sau |
| Howler.js | Tot cho sound sprite | Them lib khi co asset am thanh | Co | MIT | De phase sau |
| Lottie-web | Tot neu co JSON animation dep | Co the nang neu asset lon | Co | MIT | De phase sau |
| tsParticles | Manh nhung de nang/qua tay voi quiz | Co the nang | Co | MIT | Chua dung |
| Animate.css | De dung nhung it can thiet | CSS extra | Co | Hippocratic-2.1, can review | Khong dung |
| Phaser.js | Game engine day du, qua nang cho quiz UI hien tai | Nang, doi kien truc | Co | MIT | Khong dung phase nay |

## 4. Stack de xuat

Stack cho patch hien tai:

- `Anime.js v3.2.2`: dung file da co `src/main/resources/tse/vendor/anime.min.js`.
- `canvas-confetti v1.9.3`: dung file da co `src/main/resources/tse/vendor/confetti.browser.min.js`.
- CSS transitions/keyframes: fallback khi Anime.js khong load.
- Web Audio API: sound fallback khong can asset ngoai.

Khong them GSAP/Howler/Lottie trong patch nay vi se tang vendor va duplicate voi Anime.js/Web Audio da co. Khi QuizHub co motion system rieng hon hoac sound sprite chat luong cao, can can nhac phase sau.

## 5. Vendor local can dat o dau

Da co:

```html
<script src="vendor/anime.min.js"></script>
<script src="vendor/confetti.browser.min.js"></script>
```

Neu phase sau them:

- `src/main/resources/tse/vendor/gsap.min.js` tu https://gsap.com/
- `src/main/resources/tse/vendor/howler.min.js` tu https://howlerjs.com/
- `src/main/resources/tse/vendor/lottie.min.js` tu https://github.com/airbnb/lottie-web

Ban production khong duoc load CDN.

## 6. Rui ro khi chay trong JCEF

- File vendor thieu: phai fallback khong crash (`typeof anime`, `typeof confetti`).
- Autoplay audio bi chan den khi co user gesture: Web Audio chi khoi tao khi bat dau game/click.
- JCEF offline: CDN/font remote co the fail, nen font stack can co local/system fallback.
- HTML lon inline: patch can nho, tranh rewrite toan bo file de khong vo logic cu.
- Schema cu/moi: JS can doc ca `text/correct` va alias cu neu du lieu cache cu con ton tai.

## 7. Ke hoach patch `quiz.html`

1. Doi vendor loading tu CDN sang local `vendor/*`.
2. Polish menu/deck cards, loading/empty/error state, CTA Bat dau/Choi.
3. Polish panel tuy chon lam bai.
4. Polish Game Mode intro/HUD/question/answer/result/badge.
5. Sua mapping deck/question tu Java theo schema hien tai, giu alias cu de tuong thich.
6. Giu `window.cefQuery`, khong dung `fetch`/`XMLHttpRequest`.

## Nguon tham khao

- Kahoot official: https://kahoot.com/
- Blooket official: https://www.blooket.com/
- Wayground/Quizizz overview: https://www.techlearning.com/how-to/what-is-wayground-and-how-can-it-be-used-for-teaching
- Gimkit overview: https://www.techlearning.com/how-to/what-is-gimkit-and-how-can-it-be-used-for-teaching-tips-and-tricks
- Anime.js: https://animejs.com/
- canvas-confetti: https://github.com/catdad/canvas-confetti
- GSAP: https://gsap.com/
- Howler.js: https://howlerjs.com/
- Lottie-web: https://github.com/airbnb/lottie-web
- tsParticles: https://particles.js.org/
- Animate.css: https://animate.style/
- Phaser: https://phaser.io/
- Web Animations API: https://developer.mozilla.org/en-US/docs/Web/API/Web_Animations_API
- Canvas API: https://developer.mozilla.org/en-US/docs/Web/API/Canvas_API
- Web Audio API: https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API
