# TutorHub Practice Tab - Big Platform Research And Professional Roadmap

Ngay nghien cuu: 2026-06-24  
Pham vi: tab On tap / Practice cua TutorHub Enterprise / TutorHub_Maven  
Ket qua: tai lieu audit, benchmark san pham lon va roadmap phat trien. Khong sua code trong phase nay.

## Nguon cong khai da tham khao

- Quizizz / Wayground Help Center: https://help.wayground.com/support/home
- Azota: https://azota.vn/
- baitaptracnghiem.com mau lam bai: https://baitaptracnghiem.com/lam-bai/de-thi-bai-tap-trac-nghiem-quan-tri-co-so-du-lieu-online-de-3
- Quizlet Help: https://help.quizlet.com/
- Quizlet Flashcards: https://quizlet.com/features/flashcards
- Quizlet Learn: https://quizlet.com/features/learn
- Quizlet Test: https://quizlet.com/features/test
- Kahoot Support: https://support.kahoot.com/
- Moodle Quiz activity: https://docs.moodle.org/en/Quiz_activity
- Moodle Quiz settings: https://docs.moodle.org/en/Quiz_settings
- H5P Question Set: https://h5p.org/question-set
- Google Forms quiz: https://support.google.com/docs/answer/7032287
- Microsoft Forms quiz: https://support.microsoft.com/en-us/office/create-a-quiz-with-microsoft-forms-a082a018-24a1-48c1-b176-4b3616cdc83d

## File noi bo da doc

- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java`
- `src/main/resources/tse/quiz-practice-template.html`
- `src/main/resources/tse/practice-template.html`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/AuthClient.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/controllers/ExamController.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/ExamHtmlTemplateRenderer.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/PracticeAttemptService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/PracticeAssignmentService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/QuestionAnalyticsService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/PracticeAttemptDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/PracticeAssignmentDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/UserQuestionStatsDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/auth/AuthProtocol.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/Packet.java`
- `docs/practice_tab_research_and_roadmap_v2.md`
- `docs/practice_full_flow_audit_after_phase_5.md`
- `docs/practice_hotfix_after_phase_5_report.md`
- `docs/practice_tab_load_and_assignment_filter_fix_report.md`
- `docs/practice_tab_rewrite_with_quiz_html_report.md`
- `walkthrough.md`

---

## 1. Executive Summary

Tab On tap hien da co huong di dung: dung JavaFX WebView de render trai nghiem quiz HTML hien dai, co backend tach service/DAO, co attempt, assignment, autosave theo tung cau, submit server-side, thong ke cau sai va mastery stats. Day la nen mong tot de dua TutorHub toi mot tab on tap giong san pham that.

Tuy nhien trang thai hien tai chua nen polish UI tiep ngay. Van co cac loi contract/blocker o lop client-server:

- `PracticeTab` dang gui request bang `new Packet(false, "...")` va `new Packet(false, "...", req)`. Theo `Packet.java`, day la constructor response, khong set `action`, nen nhieu request co nguy co den server voi `action=null`.
- `PracticeTab` dang dung mot so action cu/khong khop server nhu `PRACTICE_SAVE`, `PRACTICE_SUBMIT`, `PRACTICE_ASSIGN`; trong khi server va `AuthProtocol` dang dung `PRACTICE_SAVE_PROGRESS`, `PRACTICE_SUBMIT_ATTEMPT`, `PRACTICE_ASSIGNMENT_CREATE`.
- `quiz-practice-template.html` cap nhat dap an vao state local `selections`, nhung khong thay luong goi `window.saveAnswer(...)` khi hoc sinh chon dap an. Khi submit, JS chi goi `submitQuiz(durationSeconds)`, nen server co the khong nhan du answer snapshot va danh dau skipped.
- `PRACTICE_WRONG_QUESTIONS` co constant va handler rieng, nhung switch chinh trong `ExamController.handlePacket` chua co case cho action nay; dong thoi `QuestionAnalyticsService` tra `htmlContent` kieu cu, khong phai `quizData` ma UI moi can.
- Template moi con tham chieu `DECKS` / `TITLES` cu, nen cac chuc nang flashcard, retry wrong, capture co nguy co loi runtime.
- Policy hien dap an dang chua that su chac. `PracticeAssignmentService` moi cho `IMMEDIATELY`, nhung `resumeAttempt` trong `PracticeAttemptService` lai mac dinh `includeAnswers=true`, de lai rui ro lo dap an neu sau nay mo `AFTER_SUBMIT` hoac `NEVER`.

Ket luan: phase tiep theo nen la "Practice Core Contract Stabilization", khong phai them tinh nang moi. Sau khi core list/start/save/submit/resume/wrong-questions chay dung, moi nen chuyen sang UI polish kieu Quizizz/Quizlet/Azota.

---

## 2. Current State

### 2.1 UI hien tai

`PracticeTab.java` la Swing panel nhung noi dung chinh duoc render bang JavaFX `JFXPanel` + `WebView`. HTML duoc load tu:

```text
src/main/resources/tse/quiz-practice-template.html
```

Template nay da di theo huong SPA nho:

- menu danh sach de
- mode on tap / assignment
- man hinh lam quiz
- navigator cau hoi
- flag cau hoi
- ket qua
- flashcard prototype
- instant feedback
- search/filter

Day la huong tot hon Swing thu tien vi de tao trai nghiem giong cac ung dung lon.

### 2.2 Luong du lieu client-server

Luong mong muon:

```text
PracticeTab
  -> NetworkManager
  -> ClientHandler
  -> ExamController
  -> PracticeAttemptService / PracticeAssignmentService / DAO
  -> response ve MainDashboard
  -> PracticeTab callback
  -> WebView JS loadMenu/loadQuiz
```

Trong `MainDashboard`, `PRACTICE_LIST_SUCCESS`, `PRACTICE_START_SUCCESS`, `PRACTICE_ASSIGNMENT_LIST_SUCCESS`, `PRACTICE_ASSIGNMENT_START_SUCCESS`, `PRACTICE_SUBMIT_ATTEMPT_SUCCESS` da co route ve `PracticeTab`.

### 2.3 Backend hien tai

Backend da co nhieu lop dung:

- `ExamController`: route danh sach de, start practice, save progress, submit attempt, assignment.
- `PracticeAttemptService`: tao attempt, luu tung cau, submit, resume.
- `PracticeAssignmentService`: giao bai, list assignment, start assignment.
- `PracticeAttemptDAO`, `PracticeAssignmentDAO`, `UserQuestionStatsDAO`: luu attempt/assignment/mastery.
- `ExamHtmlTemplateRenderer`: render `quizData` va co co che include/hide dap an.

### 2.4 Tai lieu cu

Trong `docs` da co nhieu bao cao phase truoc. Cac tai lieu nay cho thay module on tap da di qua nhieu lan sua:

- phase dashboard/start
- phase player
- full flow audit
- hotfix assignment filter
- rewrite HTML quiz

Nhung code hien tai khong hoan toan khop voi mot so ket luan "da fix" trong report cu. Can coi report nay la audit moi tai thoi diem 2026-06-24.

---

## 3. Strengths

| Nhom | Diem manh | Y nghia |
|---|---|---|
| Kien truc | Da tach client UI, controller, service, DAO | Co nen tang de mo rong thanh module on tap nghiem tuc |
| UI | Dung WebView/HTML thay vi Swing thu tien | De lam layout hien dai, animation, quiz player, flashcard |
| Attempt | Co bang/DAO attempt va answer | Co the autosave, resume, thong ke tien do |
| Cham diem | Server-side grading trong `PracticeAttemptService` | Dung huong, tranh tin vao client |
| Validation | Co kiem tra attempt owner/status, question thuoc paper, option thuoc question | Giam rui ro gian lan va du lieu rac |
| Submit | Khi submit co fill cau bo qua/skipped | Ket qua day du hon |
| Assignment | Co role guard teacher/admin, ownership paper, deadline, max attempts | Nen tang gan voi Azota/Classroom |
| Analytics | Co `UserQuestionStatsDAO`, `QuestionAnalyticsService` | Nen tang cho cau sai, mastery, learning loop |
| Dashboard integration | `MainDashboard` da route nhieu response ve `PracticeTab` | Co duong tich hop san voi app |
| Bao mat cau tra loi | Renderer co `includeAnswers` | Co the dieu khien hien dap an theo policy neu contract duoc chuan hoa |

---

## 4. Weaknesses

| Muc do | Van de | Bang chung trong code | Tac dong |
|---|---|---|---|
| P0 | `PracticeTab` dung sai constructor `Packet` | `new Packet(false, "PRACTICE_LIST")`, `new Packet(false, "PRACTICE_START", req)` | Request co the khong co `action`; server khong route duoc |
| P0 | Action client-server khong khop | Client gui `PRACTICE_SAVE`, `PRACTICE_SUBMIT`, `PRACTICE_ASSIGN`; server cho `PRACTICE_SAVE_PROGRESS`, `PRACTICE_SUBMIT_ATTEMPT`, `PRACTICE_ASSIGNMENT_CREATE` | Save/submit/giao bai khong on dinh |
| P0 | Chon dap an khong autosave len server | Template chi update `selections`, khong thay call `window.saveAnswer` tu UI event | Submit co nguy co toan skipped |
| P1 | Wrong questions bi dut route | `ClientHandler` route action vao `ExamController.handlePacket`, nhung switch chua co `PRACTICE_WRONG_QUESTIONS` | Luyen cau sai co the khong chay |
| P1 | Wrong questions response sai shape | `QuestionAnalyticsService` tra `htmlContent`; UI moi can `quizData` | `loadQuiz` khong nhan du lieu dung |
| P1 | Stale JS global | `DECKS`, `TITLES` con duoc dung trong template moi | Flashcard/retry/capture co nguy co crash |
| P1 | Resume co nguy co lo dap an | `resumeAttempt` dang `includeAnswers=true` | Neu assignment policy khong phai immediate se lo answer key |
| P2 | HTML template qua lon | Mot file tren 1000 dong gom CSS, state, router, quiz logic | Kho maintain, de regression |
| P2 | UI chua co loading/error/empty state chuan | Con dung `alert`, `confirm`, JOptionPane assign | Cam giac prototype |
| P2 | Multi-answer chua dong bo server | JS co multi-selection Set, service save mot `selectedOptionId` | Cau nhieu dap an chua chuan |

---

## 5. Technical Risks

### 5.1 Protocol drift

Rui ro lon nhat la protocol drift: client, dashboard, server va docs moi class dung mot bo action khac nhau. Khi module phat trien tiep ma khong dong bang contract, loi se lap lai.

De khac phuc:

- tat ca action phai dung `AuthProtocol`
- cam hardcode string moi trong UI neu da co constant
- moi request/response can DTO ro rang
- them smoke test cho contract list/start/save/submit/resume

### 5.2 State split brain

Hien co 2 state song song:

- state local trong JS: `selections`, `marked`, `currentQuiz`
- state server: `practice_attempt_answers`

Neu UI chi update local ma khong save server, ket qua submit se sai. Neu save server ma local khong dong bo khi resume, nguoi dung thay dap an khac voi ket qua backend.

Can quy dinh:

- UI la state hien thi tam thoi
- server la source of truth cho attempt
- submit phai gui answer snapshot hoac dam bao toan bo cau da save
- resume phai nap `existingAnswers`

### 5.3 Data contract mong manh

`quizData`, `htmlContent`, `attemptId`, `mode`, `showAnswersPolicy` dang bi tron giua renderer cu va SPA moi. Nen tao contract duy nhat:

```text
PracticeStartResponse
PracticeQuestionView
PracticeSaveProgressRequest
PracticeSubmitAttemptRequest
PracticeResultResponse
```

### 5.4 WebView bridge risk

JavaFX `WebView` bridge tien loi nhung can can than:

- bridge methods nen it, typed ro
- moi payload JSON phai validate server-side
- khong expose method nguy hiem
- JS error phai log ra Java console de debug

### 5.5 Large quiz performance

Template monolithic render tat ca cau co the chap khi de lon. Nen co nguong:

- duoi 100 cau: render full ok
- 100-300 cau: virtualize cau hoi/nav
- tren 300 cau: chunk/pagination

---

## 6. UX Risks

| Rui ro | Hien trang | Huong sua |
|---|---|---|
| Nguoi dung khong biet da luu hay chua | Chua co save status noi bat | Them "Da luu luc..." + pending/saving/error |
| Ket qua submit sai lam mat niem tin | Save/submit contract dang loi | Fix core truoc UI |
| Chua ro mode | Practice/assignment/wrong/flashcard bi tron | Dung header mode + badge policy |
| Giao bai bang JOptionPane | Cam giac tool noi bo | Doi sang slide-over/modal HTML trong tab |
| Alert/confirm | Trai nghiem tho | Inline toast, confirm sheet |
| Cau sai/flashcard co the crash | `DECKS/TITLES` stale | Tam an tinh nang chua ready hoac rewrite dung data dynamic |
| UI chua co empty state | Khi khong co de/bai giao de trong | Them getting-started cards |

---

## 7. Security / Answer Leakage Risks

### 7.1 Nguyen tac can giu

On tap khong phai Secure Exam, nhung van can bao ve answer key vi:

- giao vien co the dung ngan hang cau hoi that
- bai duoc giao co the tinh diem
- hoc sinh khong nen xem dap an truoc thoi diem policy cho phep

### 7.2 Rui ro hien tai

- `ExamHtmlTemplateRenderer.getPracticeQuizDataJson(..., includeAnswers)` da co co che an/hien dap an, nhung client va resume chua dong bo policy that chat.
- `PracticeAttemptService.resumeAttempt` mac dinh include answers true. Neu sau nay mo `AFTER_SUBMIT`/`NEVER`, day la diem lo dap an.
- `quiz-practice-template.html` luu correct indices trong client khi `includeAnswers=true`; dieu nay chap nhan cho free practice immediate feedback, nhung khong chap nhan cho assignment policy hidden.
- `renderPractice` cu luon include answers. Neu route cu con duoc dung ngoai Practice, can chan.

### 7.3 Chuan bao mat de xuat

| Policy | Truoc submit | Sau submit | Ghi chu |
|---|---|---|---|
| `IMMEDIATELY` | Co the tra correctness/explanation | Co review day du | Free practice |
| `AFTER_SUBMIT` | Khong tra `isCorrect`, khong tra explanation | Tra review sau submit | Bai giao co diem |
| `AFTER_DEADLINE` | Khong tra answer key | Chi tra sau deadline | Phu hop lop hoc |
| `NEVER` | Khong tra answer key | Chi tra diem/tong quan | De kiem tra nghiem tuc |

Bat buoc:

- DTO cau hoi khong co `isCorrect` khi policy khong cho phep
- submit grading server-side
- client khong co answer key an trong DOM
- log khong in full payload co answer key

---

## 8. Research Quizizz / Wayground

### 8.1 Dieu dang hoc

Quizizz/Wayground manh o trai nghiem quiz vui, nhanh, co feedback, co report va co nhieu che do giao bai/host. Triet ly san pham:

- hoc sinh vao bai nhanh
- cau hoi ro, dap an cham duoc ngay
- phan thuong/diem/feedback tao dong luc
- giao vien thay report de can thiep

### 8.2 Ap dung cho TutorHub

Nen hoc:

- player tap trung mot cau hoi/mot man hinh
- feedback sau moi cau trong free practice
- result summary de hieu ngay: diem, do chinh xac, thoi gian, cau sai
- retry wrong questions
- teacher report: ai lam, diem, cau nao sai nhieu

Khong nen lam ngay:

- live game room
- leaderboard realtime
- meme/power-up
- timer/canh tranh qua manh

Ly do: core save/submit chua on dinh. Them game hoa luc nay se lam loi kho debug hon.

---

## 9. Research Azota

### 9.1 Dieu dang hoc

Azota phu hop voi ngu canh Viet Nam: giao bai, lam bai, cham bai, quan ly lop, xem ket qua. Diem manh la no tap trung vao workflow giao vien:

- tao de/giao bai nhanh
- gan hoc sinh/lop
- thiet lap deadline/so lan lam
- hoc sinh lam bai don gian
- giao vien xem danh sach nop, diem, thong ke

### 9.2 Ap dung cho TutorHub

Day la nen tang TutorHub nen hoc nhieu nhat cho giai doan gan:

- `Bai duoc giao` cho hoc sinh
- `Giao bai on tap` cho giao vien
- deadline, max attempts, show answer policy
- report theo hoc sinh/lop/de
- nhac hoc sinh chua lam

Ly do: TutorHub la app hoc online co lop hoc, khong phai chi game quiz. Workflow giao-bai-theo-doi phu hop hon leaderboard.

---

## 10. Research baitaptracnghiem.com

### 10.1 Dieu dang hoc

baitaptracnghiem.com la mo hinh on tap nhanh, it trang tri:

- vao de nhanh
- danh sach cau hoi/dap an ro
- lam bai theo cau
- nop/cham diem don gian
- kho de theo mon/chu de

### 10.2 Ap dung cho TutorHub

Nen hoc:

- kho de de tim va bat dau
- cau hoi/dap an nhanh, it rao can
- navigator cau hoi ro rang
- result co danh sach cau dung/sai

Khong nen sao chep:

- giao dien qua cu
- thieu trai nghiem assignment/report hien dai

---

## 11. Research Quizlet

### 11.1 Dieu dang hoc

Quizlet khong chi la quiz. Diem manh la learning loop:

- flashcards
- learn mode
- test mode
- repeat/spaced repetition
- tap trung vao ghi nho va on lai cau sai

### 11.2 Ap dung cho TutorHub

Nen hoc:

- flashcard tu cau hoi
- cau sai thanh deck on lai
- mastery theo tung cau/chu de
- retry until mastered
- gom cau "hay sai" thanh session ngan

Day la huong tao khac biet cho TutorHub: khong chi cham diem, ma giup hoc sinh tien bo.

---

## 12. Feature Comparison Table

| Feature | TutorHub hien tai | Quizizz | Azota | baitaptracnghiem.com | Quizlet | De xuat TutorHub |
|---|---|---|---|---|---|---|
| Kho de | Co paper list nhung contract loi | Co quiz library | Co tao/giao de | Manh ve kho de | Co study sets | Lam paper library co filter/search |
| Lam bai | Co SPA quiz | Rat manh | Don gian | Don gian/nhanh | Test mode | Fix player core truoc |
| Autosave | Backend co service, UI chua goi dung | Co luu tien do tuy mode | Co nop/luu bai | Thuong don gian | Co tien do hoc | Save tung cau + snapshot submit |
| Cham diem | Server-side | Co report | Co cham diem | Co diem ngay | Co test result | Giu server-side |
| Giao bai | Co service/DAO | Co assign | Rat manh | Yeu | Khong phai trong tam | Hoc Azota truoc |
| Câu sai | Co service nhung route/data loi | Co review | Co report | Co review | Rat manh learning loop | Fix wrong-questions mode |
| Flashcard | Prototype loi do `DECKS/TITLES` | Khong phai trong tam | Khong phai trong tam | Khong | Manh nhat | Phase rieng sau core |
| Report giao vien | Co nen tang stats | Manh | Manh | Yeu | Trung binh | Phase report sau assignment |
| Live game | Chua co | Manh | Khong trong tam | Khong | Khong | De dai han |
| Bao mat answer key | Co `includeAnswers` nhung chua chat | Tuy mode | Tuy thiet lap | Thap | Study-oriented | Chuan hoa policy |

---

## 13. Product Positioning

TutorHub khong nen co gang tro thanh ban sao Quizizz thu hai. Vi TutorHub da co lop hoc online, lich hoc, tin nhan, bang ve, tai lieu va TSE, vi tri tot nhat cua tab On tap la:

```text
On tap ca nhan + Bai tap duoc giao + Bao cao tien bo trong he sinh thai lop hoc TutorHub
```

Thu tu nen hoc:

1. Azota: workflow giao bai, deadline, lop, report.
2. baitaptracnghiem.com: vao de nhanh, lam bai nhanh, de hieu.
3. Quizlet: flashcard, cau sai, mastery.
4. Quizizz/Kahoot: game hoa, leaderboard, live mode sau.
5. Moodle/H5P/Forms: contract, attempt policy, grading settings.

---

## 14. Phase Roadmap

### Phase P0 - Practice Core Contract Stabilization (1-2 ngay)

Muc tieu: list/start/save/submit/resume chay dung, khong mat dap an.

Cong viec:

- Sua `PracticeTab` dung constructor `new Packet(action, payload)` thay vi response constructor.
- Dung constant tu `AuthProtocol`.
- Doi `PRACTICE_SAVE` -> `PRACTICE_SAVE_PROGRESS`.
- Doi `PRACTICE_SUBMIT` -> `PRACTICE_SUBMIT_ATTEMPT`.
- Doi `PRACTICE_ASSIGN` -> `PRACTICE_ASSIGNMENT_CREATE`.
- Khi chon dap an trong JS, goi save answer dung contract.
- Submit gui snapshot hoac dam bao save truoc submit.
- Them case `PRACTICE_WRONG_QUESTIONS` vao `ExamController.handlePacket` hoac route rieng dung.

Acceptance:

- Dang nhap -> vao On tap -> load list.
- Start paper -> chon dap an -> server save success.
- Submit -> ket qua khong bi skipped neu da chon.
- Wrong questions khong crash.

### Phase P1 - Response Contract And Answer Policy (2-3 ngay)

Muc tieu: chuan hoa DTO va chong lo dap an.

Cong viec:

- Tao/chuẩn hoa DTO:
  - `PracticeStartResponse`
  - `PracticeQuestionView`
  - `PracticeOptionView`
  - `PracticeSaveProgressRequest`
  - `PracticeSubmitAttemptRequest`
  - `PracticeResultResponse`
- `showAnswersPolicy` ap dung ca start/resume/submit/review.
- `resumeAttempt` khong mac dinh include answers.
- Assignment `AFTER_SUBMIT`, `AFTER_DEADLINE`, `NEVER` co design ro.

Acceptance:

- Free practice immediate feedback van co dap an.
- Assignment hidden policy khong co `isCorrect` trong DOM truoc submit.

### Phase P2 - Player UX Like Big Apps (3-5 ngay)

Muc tieu: player giong san pham that, khong con cam giac prototype.

Cong viec:

- Loading skeleton, empty state, inline error.
- Save status: `Dang luu`, `Da luu`, `Loi luu`.
- Navigator ro trang thai: unanswered/answered/marked/wrong/right.
- Confirm submit dep, khong dung `alert/confirm`.
- Responsive trong WebView.
- Keyboard navigation co gioi han.

### Phase P3 - Assignment Flow Like Azota (1-2 tuan)

Muc tieu: giao vien giao bai that su dung duoc.

Cong viec:

- Modal/slide-over giao bai trong HTML UI.
- Chon lop/hoc sinh, deadline, max attempts, policy hien dap an.
- Hoc sinh co tab `Bai duoc giao`.
- Giao vien co danh sach bai da giao.
- Report nop bai: da lam/chua lam/diem/thoi gian.

### Phase P4 - Wrong Questions And Mastery Loop (1 tuan)

Muc tieu: TutorHub bat dau co diem khac biet so voi app chi cham diem.

Cong viec:

- Deck cau sai theo hoc sinh.
- Practice session tu cau sai.
- Mastery per question/topic.
- "On lai 10 cau yeu nhat".
- De xuat hoc tiep dua tren stats.

### Phase P5 - Flashcard / Learn Mode Like Quizlet (1-2 tuan)

Muc tieu: bien ngan hang cau hoi thanh cong cu hoc.

Cong viec:

- Flashcard dynamic tu `quizData`, khong dung `DECKS/TITLES`.
- Flip card, mark known/unknown.
- Learn mode theo cau sai.
- Session ngan 5/10/20 cau.

### Phase P6 - Teacher Analytics And Reports (2 tuan)

Muc tieu: giao vien thay duoc lop dang yeu o dau.

Cong viec:

- Report theo bai/lop/hoc sinh.
- Question difficulty, common wrong answer.
- Export CSV/PDF sau.
- Heatmap cau hoi.

### Phase P7 - Gamification And Live Practice (dai han)

Muc tieu: them tinh than Quizizz/Kahoot khi core da chat.

Cong viec:

- leaderboard tuy chon
- live practice room
- challenge theo lop
- badge/streak
- nhac hoc bang notification

---

## 15. UI Redesign Proposal

### 15.1 Dashboard On tap

Bo cuc de xuat:

- Header: `On tap` + search + filter role/mode.
- Quick action cards:
  - `Lam de on tap`
  - `Bai duoc giao`
  - `Cau sai cua toi`
  - `Flashcard`
- Section `Gan day`
- Section `De cua toi / De duoc giao / Kho de`

Phong cach:

- Sạch, sang, desktop app.
- Card dung border nhe, shadow rat nhe.
- Khong dung qua nhieu gradient.
- Trang thai ro: active, overdue, completed, draft.

### 15.2 Quiz Player

Bo cuc:

- Top bar: ten de, mode, save status, timer neu co.
- Main: cau hoi + dap an.
- Right rail hoac bottom rail: navigator cau hoi.
- Footer: quay lai, danh dau, bo qua, cau tiep, nop bai.

Trang thai:

- Cau chua lam
- Da lam
- Da danh dau
- Dang luu
- Loi luu
- Dung/sai neu policy cho phep

### 15.3 Result Screen

Can co:

- score, accuracy, time
- cau dung/sai/skipped
- retry wrong
- review answers
- goi y hoc tiep

### 15.4 Teacher Assignment UI

Khong nen dung `JOptionPane`. Nen la modal/slide-over trong WebView:

- Chon de
- Chon lop/hoc sinh
- Deadline
- Attempts
- Show answers policy
- Nut giao bai

---

## 16. Backend Architecture

### 16.1 Module de xuat

```text
PracticeController / ExamController practice branch
  -> PracticeQueryService
  -> PracticeAttemptService
  -> PracticeAssignmentService
  -> QuestionAnalyticsService
  -> PracticeReportService
  -> DAO layer
```

Hien co `ExamController` dang gom nhieu viec. Ngan han co the giu, nhung nen tach dan khi module on tap lon len.

### 16.2 Nguyen tac

- Controller chi parse request/return response.
- Service xu ly policy/validation/business logic.
- DAO chi data access.
- Renderer/DTO mapper tach khoi service.
- Auth/role context ro rang.

### 16.3 Data can co

- users
- exam_papers
- questions
- question_options
- practice_attempts
- practice_attempt_answers
- practice_assignments
- practice_assignment_recipients
- user_question_stats
- practice_events/audit optional

---

## 17. Frontend Architecture

### 17.1 Hien tai

`quiz-practice-template.html` gom HTML/CSS/JS trong mot file lon. Day la chap nhan cho prototype, nhung bat dau kho mo rong.

### 17.2 De xuat ngan han

Chua can dua React vao ngay. Trong Java desktop hien tai, nen:

- tach JS thanh cac module nho neu WebView cho phep load resource noi bo
- tach CSS vao file rieng
- tao `PracticeBridge` contract ro
- viet cac function duy nhat:
  - `loadMenu(data, mode)`
  - `loadQuiz(response)`
  - `applySaveAck(response)`
  - `applySubmitResult(response)`
  - `showError(message)`

### 17.3 De xuat dai han

Neu tab On tap lon nhu mot san pham rieng, co the can:

- WebView + bundled frontend build (van trong Java app)
- TypeScript cho contract
- component-based UI
- test bang Playwright tren HTML template

Khong nen lam truoc khi P0/P1 on dinh.

---

## 18. Data Contract

### 18.1 PracticeStartResponse

```json
{
  "requestId": "uuid",
  "mode": "FREE_PRACTICE",
  "attemptId": 123,
  "paperId": 10,
  "title": "De on tap",
  "questionCount": 20,
  "showAnswersPolicy": "IMMEDIATELY",
  "durationLimitSeconds": null,
  "existingAnswers": [],
  "questions": []
}
```

### 18.2 PracticeQuestionView

```json
{
  "id": 1,
  "content": "Noi dung cau hoi",
  "type": "SINGLE_CHOICE",
  "points": 1,
  "options": [
    {
      "id": 11,
      "text": "A",
      "isCorrect": null
    }
  ],
  "explanation": null
}
```

Quy tac:

- `isCorrect` chi co gia tri khi policy cho phep.
- `explanation` chi tra khi duoc phep review.

### 18.3 PracticeSaveProgressRequest

```json
{
  "attemptId": 123,
  "questionId": 1,
  "selectedOptionIds": [11],
  "isSkipped": false,
  "isMarked": false,
  "timeSpentSeconds": 12,
  "clientVersion": "practice-webview-v1"
}
```

### 18.4 PracticeSubmitAttemptRequest

```json
{
  "attemptId": 123,
  "durationSeconds": 420,
  "answersSnapshot": [
    {
      "questionId": 1,
      "selectedOptionIds": [11],
      "isSkipped": false
    }
  ],
  "submittedAtClient": "2026-06-24T10:00:00+07:00"
}
```

### 18.5 PracticeResultResponse

```json
{
  "attemptId": 123,
  "status": "SUBMITTED",
  "score": 8.5,
  "totalQuestions": 20,
  "correctCount": 17,
  "wrongCount": 2,
  "skippedCount": 1,
  "reviewAllowed": true,
  "reviewQuestions": []
}
```

---

## 19. Quick Wins 1-2 Days

1. Sua tat ca request trong `PracticeTab` dung `new Packet(action, payload)`.
2. Dung `AuthProtocol` constants cho cac action Practice.
3. Doi action cu:
   - `PRACTICE_SAVE` -> `PRACTICE_SAVE_PROGRESS`
   - `PRACTICE_SUBMIT` -> `PRACTICE_SUBMIT_ATTEMPT`
   - `PRACTICE_ASSIGN` -> `PRACTICE_ASSIGNMENT_CREATE`
4. Trong `quiz-practice-template.html`, khi chon dap an thi goi save.
5. Submit gui snapshot hoac flush pending saves truoc submit.
6. Them case `PRACTICE_WRONG_QUESTIONS` vao switch hoac route dung handler.
7. Tam an flashcard/retry/capture neu con phu thuoc `DECKS/TITLES`, hoac map lai data dynamic.
8. Them console log co prefix cho bridge request/response.
9. Them empty state khi khong co de.
10. Them save status nho tren player.

---

## 20. Medium-Term 1-2 Weeks

1. Chuan hoa DTO request/response.
2. Chuan hoa answer policy.
3. Hoan thien assignment UI theo Azota.
4. Hoan thien wrong questions mode.
5. Hoan thien result/review screen.
6. Them report giao vien co ban.
7. Them test smoke:
   - list
   - start
   - save
   - submit
   - resume
   - assignment start
   - wrong questions
8. Tach CSS/JS cua template.
9. Them inline error/loading state.

---

## 21. Long-Term

1. Flashcard/Learn mode nhu Quizlet.
2. Spaced repetition dua tren `UserQuestionStatsDAO`.
3. Report lop hoc nang cao.
4. Import de tu HTML/PDF/Word co validation.
5. Question tagging theo mon/chu de/do kho.
6. Live practice room / challenge.
7. Gamification tuy chon.
8. AI goi y cau can on lai.
9. Dong bo tien do da thiet bi neu co mobile/web sau nay.
10. Observability: event logs, client error report, slow query report.

---

## 22. Don't Do Now

Khong nen lam trong phase tiep theo:

- Khong them leaderboard/live game khi save/submit chua chuan.
- Khong them AI tao cau hoi luc contract Practice chua on dinh.
- Khong migrate sang React/TypeScript ngay neu chua co ly do thuc chien.
- Khong polish visual lon truoc khi submit dung.
- Khong mo `AFTER_SUBMIT`/`NEVER` assignment policy neu `resumeAttempt` va DTO chua chong lo dap an.
- Khong dua Flashcard ra production khi `DECKS/TITLES` con la state cu.
- Khong dung `alert/confirm` cho UX moi.
- Khong copy logo, icon, noi dung hay trade dress cua cac nen tang tham khao.

---

## 23. Conclusion

Tab On tap cua TutorHub da co nen mong dung de phat trien thanh mot module manh: co WebView player, backend attempt, assignment, analytics va role filtering. Nhung hien tai core contract giua `PracticeTab`, `quiz-practice-template.html`, `MainDashboard`, `ClientHandler`, `ExamController` va `AuthProtocol` dang la diem gay loi lon nhat.

Huong phat trien dung la:

1. On dinh core protocol va luong save/submit.
2. Chuan hoa answer policy de khong lo dap an.
3. Lam UI player sach, co save status, empty/error state.
4. Xay assignment/report theo Azota.
5. Xay learning loop theo Quizlet.
6. Sau cung moi them gamification theo Quizizz/Kahoot.

Neu lam theo thu tu nay, TutorHub se khong chi co mot tab "lam trac nghiem", ma co the thanh mot he thong on tap gan chat voi lop hoc online, giao vien, hoc sinh va du lieu tien bo hoc tap.

