# TÀI LIỆU KIẾN TRÚC VÀ TRẠNG THÁI HỆ THỐNG QUIZHUB (TUTORHUB)

*Cập nhật lần cuối: Tháng 6/2026*

Tài liệu này đóng vai trò như một bản thiết kế (blueprint) và báo cáo trạng thái hiện tại của module **QuizHub**. Các AI Agent sau đọc tài liệu này để hiểu ngữ cảnh, cấu trúc và cách thức hoạt động của hệ thống trước khi thực hiện các thay đổi mã nguồn.

---

## 1. TỔNG QUAN KIẾN TRÚC (HIGH-LEVEL ARCHITECTURE)
QuizHub là tính năng Thi trắc nghiệm và Ôn tập bằng Flashcard của ứng dụng TutorHub.
Kiến trúc cốt lõi sử dụng:
- **Frontend (UI/UX)**: HTML, CSS, JavaScript thuần, giao diện hiện đại mang phong cách tối giản. Nằm trong file `quiz.html`.
- **Backend (Core Logic)**: Java 21 (Swing/Maven).
- **Embedded Browser**: Ứng dụng Desktop sử dụng **JCEF (Java Chromium Embedded Framework)** để nhúng trình duyệt, thay thế hoàn toàn cho JavaFX WebView cũ (nhằm khắc phục lỗi tương thích và tăng hiệu suất render).
- **Cầu nối giao tiếp (Bridge)**: Giao tiếp 2 chiều giữa Java và JS thông qua `CefMessageRouter` (cụ thể là hàm `window.cefQuery` ở phía JS).
- **Lưu trữ (Storage)**: Kiến trúc **Cloud-First** sử dụng Backblaze B2 làm Master Data (nguồn chính). Thư mục Local `%APPDATA%\TutorHub\quizhub\` đóng vai trò dự phòng (Cache/Offline mode).

---

## 2. CƠ CHẾ LƯU TRỮ CLOUD-FIRST (BACKBLAZE B2)
Toàn bộ dữ liệu đề thi được định dạng bằng JSON và lưu trữ trên Backblaze B2 (thư mục: `quizhub/decks/`).

### Chiến lược 1 File Index + N File Nội dung:
1. **`quizhub/decks_index.json`**: Chứa một mảng `QuizHubDeckSummary`. Mỗi khi mở QuizHub, Java sẽ gọi API B2 tải file này xuống, phân tích cú pháp (parse) và trả về JS để hiển thị menu. (Giúp tải hàng trăm đề thi chỉ trong 1 request).
2. **`quizhub/decks/{deck_id}.json`**: Chứa toàn bộ câu hỏi và thông số chi tiết của một đề thi cụ thể. Chỉ được tải khi người dùng click vào một đề cụ thể.

### Cơ chế Fallback Cache:
- Mọi dữ liệu JSON tải từ B2 sẽ được lưu đè xuống ổ cứng (`%APPDATA%\TutorHub\quizhub\decks\`).
- Khi người dùng mất mạng hoặc B2 không phản hồi, `QuizHubDeckService` tự động đọc dữ liệu từ Local Cache để đảm bảo trải nghiệm không bị gián đoạn.

---

## 3. CÁC TỆP QUAN TRỌNG (KEY COMPONENTS)

### Lớp UI / Giao diện Java (Swing)
- `QuizHubTab.java`: Giao diện JPanel chính chứa `CefBrowser`. Có thanh Toolbar phía trên chứa nút **"🔄 Làm mới từ Cloud"** (gọi `browser.reload()`) và nút **"Nhập đề từ Excel"**.
- `QuizExcelImportDialog.java`: Dialog JDialog chứa một JCEF browser khác nạp file `quiz_excel.html` (chứa thư viện LuckySheet) để Preview file Excel trước khi import.

### Lớp Xử lý và Cầu nối (Bridge)
- `QuizHubCefRouterHandler.java`: Router chính lắng nghe mọi request `window.cefQuery` từ `quiz.html`. Parse chuỗi request (ví dụ: `LIST_DECKS`, `GET_DECK:123`, `IMPORT_EXCEL:...`) và điều hướng tới các hàm tương ứng trong Bridge.
- `QuizHubBridge.java`: Xử lý logic trung gian, gọi sang `QuizHubDeckService` và bọc dữ liệu trả về theo format an toàn: `{"success": true, "data": ...}`.

### Lớp Dịch vụ và Đám mây (Service & Cloud)
- `QuizHubDeckService.java`: Nơi chứa toàn bộ logic xử lý dữ liệu của Đề thi (List, Get, Save, Delete). 
  - Đã được nâng cấp để làm việc trực tiếp với Cloud (`B2Helper.downloadJsonData` / `uploadJsonData`). 
  - Khi `saveDeck()` được gọi, nó sẽ upload file json mới lên B2, sau đó tự động kéo `decks_index.json` về, thêm đề mới vào mảng và push ngược Index lại B2.
- `B2Helper.java`: Lớp tiện ích giao tiếp với AWS S3 SDK (sử dụng Endpoint của Backblaze B2).
- `QuizHubExcelImportService.java`: Sử dụng Apache POI để đọc file Excel (`.xlsx`), phân tích dòng cột và chuyển đổi thành đối tượng Java `QuizHubDeck`.

### Lớp Frontend HTML/JS
- `quiz.html`: Nằm trong `src/main/resources/tse/`. Đã được refactor bỏ dữ liệu cứng (hardcode). 
  - Khởi tạo bằng cách gọi `cefQuery({request: "LIST_DECKS", onSuccess: ...})`.
  - Toàn bộ kết quả từ Java trả về là chuỗi JSON. JS phải xử lý lấy `.data`.
- `quiz_excel.html`: Tích hợp thư viện LuckySheet qua CDN để hiển thị lưới Excel tương tác.

---

## 4. QUY TRÌNH LUỒNG DỮ LIỆU (DATA FLOW)

**Ví dụ Luồng: Thêm 1 đề thi từ Excel**
1. Người dùng ấn "Nhập đề từ Excel" -> Mở `QuizExcelImportDialog`.
2. Giao diện CefBrowser nạp `quiz_excel.html`.
3. Java dùng Apache POI parse Excel và đẩy dữ liệu JSON qua JS (LuckySheet) để Preview.
4. Người dùng bấm "Xác nhận Nhập" (JS). Lệnh `IMPORT_EXCEL_ROWS: <json_data>` được bắn về Java.
5. `QuizHubCefRouterHandler` bắt lệnh, đưa cho `QuizHubExcelImportService` build ra object `QuizHubDeck`.
6. Chuyển sang `QuizHubDeckService.saveDeck(deck)`. 
7. Ghi nội dung xuống Local Cache.
8. Gọi `B2Helper` đẩy tệp `{deck_id}.json` lên B2.
9. Gọi `B2Helper` tải `decks_index.json` từ B2, cập nhật và đẩy lại lên B2.
10. `quiz.html` Reload (bằng cách ấn nút Làm mới hoặc tự động) -> `LIST_DECKS` -> Giao diện hiện đề mới.

---

## 5. NHỮNG LƯU Ý CHO CÁC AI AGENT ĐỜI SAU
1. **Tuyệt đối KHÔNG DÙNG `unwrap()` hoặc `expect()`** tuỳ tiện khi code Rust hoặc các tác vụ tương lai có liên quan tới hệ thống này.
2. **Giao tiếp JCEF**: Nếu bạn viết logic mới phía JS (trong `quiz.html`), bắt buộc phải dùng `window.cefQuery` để đẩy lệnh xuống Java. Backend Java sẽ trả về cấu trúc bọc `{"success": true/false, "data": ...}`, do vậy bên JS luôn phải parse cẩn thận: `const result = JSON.parse(response); const data = result.data;`.
3. **Mã hóa (Encoding)**: Khi dùng tool/script chỉnh sửa file `quiz.html`, phải LUÔN ép mã UTF-8. Không dùng PowerShell `Out-File` mặc định (vì nó sinh ra BOM hoặc mã ANSI gây lỗi Font chữ hiển thị Tiếng Việt).
4. **Cloud First**: Không bao giờ lấy Local Folder làm gốc. Bất cứ tính năng đồng bộ nào cũng phải gọi lên API của Cloud (`B2Helper`) trước tiên.

*Kết thúc tài liệu.*
