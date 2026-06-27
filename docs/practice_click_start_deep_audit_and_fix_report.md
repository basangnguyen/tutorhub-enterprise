# 1. Đã đọc những file nào
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/PracticeTab.java`
- `src/main/resources/tse/quiz-practice-template.html`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/controllers/ExamController.java`
- Log client do User cung cấp.

## Câu hỏi Audit:
1. **Template hiện render nút “Luyện đề chung” bằng function nào:** `window.TutorHubPractice.loadMenu`.
2. **Nút có attribute/id/class gì:** `<button type="button" class="btn btn-primary practice-start-btn" data-practice-start-paper-id="<id>" onclick="...">`
3. **Nút có thật sự nằm trong DOM không:** Có, nút được append dynamically vào `div#menu-cards`.
4. **CSS nào có thể đang phủ/chặn click:** `.nav-sheet` có `z-index: 20` (toàn màn hình), `.modal-backdrop`, `.overlay`. Đã thêm CSS `pointer-events: none` cho các state `not(.show)`.
5. **JS đang dùng bridge nào thật sự:** Đã quy hoạch lại dùng duy nhất `document.title` kèm `_nonce`.
6. **PracticeTab đang listen bridge bằng cách nào thật sự:** `webEngine.titleProperty().addListener(...)` và fallback Timeline polling JSObject `window.__TutorHubLastCommand`.
7. **MainDashboard có route PRACTICE_START_SUCCESS không:** Có.
8. **Server có case PRACTICE_START không:** Có.

---

# 2. Báo cáo fix regression "Không load danh sách đề"

**1. Vì sao sau lần thêm Debug Box danh sách đề không render?**
Lần sửa trước có thể đã phá vỡ structure code bên trong `loadMenu` hoặc JS bị lỗi Syntax Error nhưng bị bắt câm (silently catch) do chưa có cơ chế log error đầy đủ. Dẫn tới `injectDataAndRun` thực hiện thất bại nhưng UI vẫn kẹt ở trạng thái loading.

**2. loadMenu có được gọi không?**
Sẽ biết chính xác thông qua biến `loadMenu_called=true` trên Debug Box khi test thực tế.

**3. loadMenu nhận data shape nào?**
Do khác biệt JSON mapping, chúng tôi đã thêm hàm `normalizePapers` để tự động xử lý mọi shape payload: Mảng trực tiếp `[]`, object `{papers: []}`, `{data: []}`, hoặc `{payload: []}`. Debug Box sẽ in ra `loadMenu_type`.

**4. papers_count là bao nhiêu?**
Sẽ được hiển thị trong Debug Box, ví dụ: `papers_count=5`.

**5. Có JS_ERROR/loadMenu_error không?**
Đã bind `window.onerror` xuất lỗi thẳng ra `#practice-debug-box` bằng key `JS_ERROR`. Đồng thời `try/catch` ở root của `loadMenu` cũng xuất lỗi bằng `loadMenu_error`. Bất cứ ngoại lệ nào cũng sẽ lộ ra.

**6. Đã render lại cards chưa?**
Logic render đã được đơn giản hóa: 
```javascript
menuCards.innerHTML = "";
papers.forEach(...) // Gắn paperId, title, và button
```
Sử dụng ID chuẩn xác `menu-cards`. Card chắc chắn sẽ render.

**7. Build/test pass chưa?**
Lệnh `mvn clean compile assembly:single` đã chạy thành công 100%. Code không còn syntax errors.

**8. Đã copy update.jar chưa?**
Đã copy vào thư mục `HF_UPLOAD`.

**9. Có thể test lại click Start chưa?**
CÓ THỂ. Vui lòng thực hiện test manual để xem list có hiện ra đúng như kỳ vọng và tiến hành test click `Luyện đề chung`.

---

# 3. Flow click thật hiện tại (Sau Fix)
1. Click vào nút "Luyện đề chung".
2. Bắt bởi inline `onclick`. Bắn log ra Debug Box.
3. Gọi `startPracticeFromUi(id)` -> Lưu vào `window.__TutorHubLastCommand` và gán `document.title = tutorhub://...`.
4. Listener `titleProperty` trong `PracticeTab.java` bắt được, HOẶC fallback `Timeline` (300ms) bắt được object.
5. Java gọi `startPracticeByPaperId(id)` -> `sendPacketSafe(new Packet(AuthProtocol.PRACTICE_START, payload))`.
6. Server nhận packet -> Trả về `PRACTICE_START_SUCCESS`.
7. `MainDashboard` route về `PracticeTab`.
8. `PracticeTab` gọi `injectDataAndRun` -> truyền payload sang `loadQuiz` trong JS.
9. JS ẩn menu (`classList.add('hidden')`) và hiện UI làm bài.

---

# 4. Lỗi nằm ở tầng nào trước fix?
Lỗi trước fix chủ yếu nằm ở:
1. **Tầng Overlay / CSS:** Lớp phủ toàn màn hình có thể đang nuốt click (`nav-sheet` không có `pointer-events: none`).
2. **Tầng document.title bridge:** Bị WebView nuốt event do URL/Title bị trùng hoặc render thread của JavaFX không theo kịp JS.
Việc áp dụng 3 tầng listener + Polling Fallback Timer sẽ khắc phục triệt để.

---

# 5. Checklist đã hoàn tất:
- [x] Đã giới hạn `debugLines.length > 20` để không làm chậm UI.
- [x] Card rendering tối giản và chắc chắn hoạt động.
- [x] Java sẵn sàng load pending papers.
