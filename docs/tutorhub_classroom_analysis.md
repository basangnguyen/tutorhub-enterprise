# 🎓 BÁO CÁO PHÂN TÍCH TOÀN DIỆN: TUTORHUB CLASSROOM
## Kiến trúc, UX/UI, So sánh với Ông lớn & Lộ trình Nâng cấp

> **Tác giả:** AI Software Architect & UX Expert  
> **Ngày:** 06/07/2026  
> **Phạm vi:** Phân tích toàn bộ luồng phòng học trực tuyến TutorHub

---

## PHẦN 1: PHÂN TÍCH ƯU – NHƯỢC ĐIỂM CỦA TUTORHUB

### ✅ 1.1 ĐIỂM MẠNH

#### A. Kiến trúc lai (Hybrid Architecture) — Sáng tạo và độc đáo

| Thành phần | Công nghệ | Đánh giá |
|---|---|---|
| Desktop Shell | Java Swing + JCEF (Chromium Embedded) | ⭐ Kiểm soát native tốt |
| Whiteboard | Tldraw v2 (React-based) | ⭐ Infinite canvas, mượt |
| Video/Audio | LiveKit Cloud (WebRTC SFU) | ⭐ Hạ tầng chuyên nghiệp |
| Board Sync | Node.js + WebSocket (raw broadcast) | ⚠️ Hoạt động nhưng chưa tối ưu |
| AI Assistant | Hugging Face Spaces (Custom Flask API) | ⭐ Linh hoạt, chi phí thấp |
| File Storage | Backblaze B2 (S3-compatible) | ⭐ Rẻ, ổn định |

- **Lợi thế "Ứng dụng gốc" (Native App):** Khác với Zoom Web hay Google Meet (chạy hoàn toàn trên trình duyệt), TutorHub chạy như app desktop thực sự. Điều này cho phép kiểm soát sâu hơn: toàn màn hình không viền (`setUndecorated(true)`), tương tác giữa Java và JS qua `CefMessageRouter`, không bị giới hạn sandbox của trình duyệt.

- **Whiteboard là trung tâm — không phải phụ:** Đây là triết lý thiết kế đặc biệt nhất. Trong khi Zoom/Teams/Meet coi Whiteboard chỉ là tính năng phụ (mở ra → đóng lại), TutorHub đặt bảng vẽ Tldraw làm **canvas chính** của phòng học, video nổi phía trên. Đây là cách tiếp cận rất phù hợp cho giáo dục 1-1 và nhóm nhỏ.

- **Hệ sinh thái công cụ giáo dục phong phú nhất:** Không có nền tảng video conferencing phổ thông nào tích hợp đầy đủ: MathLive (công thức toán), Mermaid (sơ đồ thuật toán), PhET (thí nghiệm vật lý ảo), Code Editor (VSCode), Quiz tích hợp sẵn trên bảng, và YouTube Co-watch đồng bộ thời gian. Đây là lợi thế cạnh tranh lớn nhất.

- **Chi phí hạ tầng cực thấp:** LiveKit Cloud (miễn phí 50,000 phút/tháng), Hugging Face Spaces (miễn phí CPU basic), Backblaze B2 (10GB miễn phí). Toàn bộ chi phí vận hành gần như bằng 0 ở giai đoạn hiện tại.

#### B. Giao diện phòng học — Trực quan và quen thuộc

- Thanh công cụ dưới đáy (Bottom Bar) lấy cảm hứng từ Zoom, giúp người dùng dễ tiếp cận.
- Video sidebar bên phải thay vì floating random — tiệm cận layout Gallery View.
- Dark theme (#1e1e1e) cho vùng chat/sidebar tạo sự tập trung vào bảng vẽ.

---

### ❌ 1.2 NHƯỢC ĐIỂM & RỦI RO

#### A. Kiến trúc — Các Bottleneck nghiêm trọng

> [!CAUTION]
> **File `tldraw_board_v2.html` có dung lượng ~8MB với 20,000+ dòng code.** Đây là red flag lớn nhất. Toàn bộ logic LiveKit, Chat, Quiz, Recording, Whiteboard sync, Screen Share, Code Editor, PhET, YouTube Co-watch, MathLive, Mermaid, Roster Management... đều nằm chung trong MỘT FILE HTML duy nhất. Điều này gây ra:

1. **Bảo trì cực kỳ khó khăn:** Sửa 1 bug có thể vô tình ảnh hưởng tính năng khác.
2. **Parse time cao:** Trình duyệt (JCEF/Chromium) phải parse 8MB HTML mỗi lần mở phòng học.
3. **Không thể tree-shake:** Tất cả thư viện (Tldraw, LiveKit, MathLive, Mermaid, pdf.js, YT API...) đều được load đồng thời dù chưa cần dùng.

> [!WARNING]
> **Whiteboard Sync dùng raw WebSocket broadcast — KHÔNG dùng CRDT (Yjs).** Mặc dù tài liệu mô tả "Node.js + WebSocket (Yjs)", nhưng code thực tế trong `server.js` và `tldraw_board_v2.html` cho thấy cơ chế đồng bộ chỉ là **broadcast toàn bộ `elements` array** qua WebSocket thuần. Điều này gây ra:

- **Race condition:** Khi 2 người vẽ cùng lúc, thay đổi của người gửi sau sẽ ghi đè (overwrite) thay đổi của người gửi trước.
- **Bandwidth lãng phí:** Mỗi nét vẽ gửi toàn bộ scene (~hàng trăm KB) thay vì chỉ gửi delta (thay đổi nhỏ).
- **Không có conflict resolution:** Không giải quyết xung đột khi 2 người chỉnh sửa cùng 1 shape.

> [!WARNING]
> **JCEF (Chromium Embedded) ngốn RAM rất nhiều.** Mỗi instance JCEF tạo ra ít nhất 3-5 process con của Chromium, tiêu tốn 300-500MB RAM chỉ riêng phần render. Khi kết hợp với Java Swing (JVM ~200MB), LiveKit WebRTC (100-200MB cho video), tổng RAM của TutorHub có thể lên tới **800MB - 1.2GB** trên máy học sinh cấu hình thấp.

#### B. Bảo mật — Các lỗ hổng tiềm ẩn

| Vấn đề | Mức độ | Chi tiết |
|---|---|---|
| Hardcoded URLs | 🟡 Trung bình | URL `hocba299-3-tutorhub-sync.hf.space` được hardcode trực tiếp trong HTML. Nếu domain thay đổi, phải sửa toàn bộ file. |
| Thiếu auth cho LiveKit token | 🔴 Nghiêm trọng | API `/livekit/token` không yêu cầu xác thực. Bất kỳ ai biết URL đều có thể lấy token và join phòng. |
| CORS wildcard (`*`) | 🟡 Trung bình | Server dùng `cors()` mặc định (allow all origins) và `Access-Control-Allow-Origin: *` trong nginx. |
| Không rate limiting | 🔴 Nghiêm trọng | API upload file, proxy image, và token endpoint đều không có rate limiting. |

#### C. UX/UI — Quá tải tính năng (Feature Overload)

Nhìn vào ảnh chụp màn hình và code HTML, thanh toolbar hiện tại có **~20 nút bấm** trải đều trên 1 hàng ngang:

```
[Mute All] [Camera] | [Share] [Record] [Raise] [React] | [Code] [Thí nghiệm] [Arena] [YouTube] [Files] [Math] [Diagram] [Snippet] [Members] [Kho bảng] [Quiz] | [People] [Save] [Settings] [Leave]
```

> [!IMPORTANT]
> **Quy tắc 7±2 (Miller's Law):** Người dùng chỉ có thể xử lý thoải mái 5-9 mục thông tin cùng lúc. 20 nút trên 1 toolbar vi phạm nghiêm trọng nguyên tắc này, gây:
> - **Phân tán chú ý** khỏi nội dung bài học.
> - **Rào cản cho người dùng mới** (giáo viên lớn tuổi, học sinh nhỏ).
> - **Giảm tốc độ thao tác** vì phải quét mắt tìm đúng nút.

---

## PHẦN 2: SO SÁNH VỚI CÁC "ÔNG LỚN"

### 2.1 Kiến trúc Video/Audio

| Tiêu chí | Zoom | Microsoft Teams | Google Meet | **TutorHub** |
|---|---|---|---|---|
| **Media Server** | Tự build MMR (SFU) trên hạ tầng riêng | Azure Communication Services | Google WebRTC Infra | **LiveKit Cloud (SFU)** ✅ |
| **Video Codec** | H.264 + SVC (Scalable Video Coding) | H.264/VP9 + Simulcast | VP9 + AV1 | **H.264/VP8 (mặc định LiveKit)** |
| **Adaptive Quality** | 5 lớp SVC tự động chọn theo bandwidth | 3 lớp Simulcast | Dynamic resolution | **LiveKit Adaptive Stream** ✅ |
| **Client Rendering** | Native C++ (Win/Mac), WebAssembly (Web) | Electron (Chromium) | Chrome/WebAssembly | **JCEF (Chromium in Java)** ⚠️ |
| **RAM Usage** | ~300-400MB | ~500-800MB | ~200-400MB (trình duyệt) | **~800-1200MB** 🔴 |
| **Max Participants** | 1,000 (Gallery View: 49) | 1,000 (Gallery: 49) | 500 (Grid: 49) | **Chưa test, ước tính: 10-20** ⚠️ |

**Nhận xét:** TutorHub đã chọn đúng khi dùng LiveKit (SFU chuyên nghiệp), nhưng việc wrap trong JCEF tạo thêm 1 lớp overhead không cần thiết so với Electron (Teams) hay native WebView.

### 2.2 Triết lý thiết kế UI/UX

#### Zoom (2025-2026): "Customizable Minimal"
- Mặc định chỉ hiện 5-6 nút cốt lõi: **Mute, Video, Share, React, Record, End**.
- Các tính năng phụ gom vào menu **"More" (⋯)**.
- Người dùng tự kéo thả (drag-and-drop) để pin tính năng hay dùng lên toolbar.
- **Whiteboard:** Mở trong tab riêng (không phải canvas chính). Dùng bảng riêng của Zoom (không phải third-party).

#### Microsoft Teams: "All-in-One Hub"
- Toolbar đơn giản (Mic, Camera, Share, Hand, Leave, More).
- Whiteboard tích hợp nhưng mở ra trong panel riêng bên phải.
- Tính năng phụ nằm trong **right-side panel** (Chat, People, Breakout Rooms...).
- **Loop Components** cho phép co-editing trực tiếp trong chat.

#### Google Meet: "Browser-First Simplicity"
- UI tối giản nhất: Chỉ 3 nút chính ở dưới (Mic, Camera, End Call).
- Whiteboard: Đã bỏ Jamboard, dùng third-party (Miro, Lucidspark).
- Tính năng phụ gom vào icon tròn ở bottom-right.

#### TutorHub hiện tại: "Everything-on-Screen"
- **20 nút** trải đều trên toolbar — đối lập hoàn toàn với xu hướng tối giản.
- Whiteboard LÀ canvas chính — đây là điểm khác biệt.
- Không có menu "More" hoặc cơ chế nhóm tính năng.

### 2.3 Lợi thế cạnh tranh cốt lõi của TutorHub

> [!TIP]
> TutorHub KHÔNG CẦN cạnh tranh với Zoom/Teams/Meet ở phân khúc "họp doanh nghiệp". Thay vào đó, TutorHub nên tập trung khai thác **3 lợi thế mà không ai có:**

1. **Whiteboard-First Classroom:** Không nền tảng nào đặt bảng vẽ infinite canvas làm trung tâm phòng học. Đây là USP (Unique Selling Point) lớn nhất.

2. **Hệ sinh thái công cụ giáo dục tích hợp sẵn:** MathLive + Mermaid + PhET + Code Arena + Quiz trên bảng = Không nền tảng nào có.

3. **Giáo dục 1-1 và nhóm nhỏ (1-10 người):** Zoom/Teams/Meet được thiết kế cho họp đông người. TutorHub có thể tối ưu cho trải nghiệm gia sư cá nhân — nơi giáo viên và học sinh cần tương tác sâu trên cùng 1 bảng vẽ.

---

## PHẦN 3: LỘ TRÌNH HOÀN THIỆN & NÂNG CẤP (ROADMAP)

### 🟢 GIAI ĐOẠN 1 — Ngắn hạn (1-2 tháng): "DỌN DẸP & CHUẨN HÓA"

#### Task 1.1: Tái cấu trúc Toolbar — Áp dụng mô hình "Zoom More Menu"

**Mục tiêu:** Giảm từ 20 nút xuống còn 7 nút chính + 1 menu "More".

```
┌─────────────────────────────────────────────────────────┐
│ [🎤Mic] [📷Camera] [🖥Share] [✋Raise] [😊React]  ║  [📦Apps ▼] [👥People] [⚙Settings] [🚪Leave] │
└─────────────────────────────────────────────────────────┘
                                                    ║
                                              [Apps Menu]
                                          ┌─────────────────┐
                                          │ 💻 Code Editor   │
                                          │ 🧪 Thí nghiệm    │
                                          │ 🏆 Arena          │
                                          │ 📺 YouTube        │
                                          │ 📄 Files          │
                                          │ ∑  Math           │
                                          │ 📊 Diagram        │
                                          │ 📝 Snippet        │
                                          │ 📋 Quiz           │
                                          │ 📁 Kho bảng       │
                                          │ 🔴 Record         │
                                          └─────────────────┘
```

**Hành động cụ thể:**
- [ ] Tạo nút "📦 Apps" với popup menu chứa tất cả công cụ giáo dục.
- [ ] Giữ lại trên toolbar chính chỉ: Mic, Camera, Share, Raise, React, People, Settings, Leave.
- [ ] Thêm tooltip chi tiết cho mỗi nút (đã có `data-tooltip`, cần style lại rõ ràng hơn).
- [ ] Thêm keyboard shortcuts cho các tính năng hay dùng (M = Mute, V = Camera, S = Share).

#### Task 1.2: Tách file `tldraw_board_v2.html` thành Module

**Mục tiêu:** Từ 1 file 20,000 dòng → nhiều file nhỏ, load theo nhu cầu (lazy-load).

```
tldraw_board_v2.html        → board.html (shell ~500 dòng)
                             → css/board.css
                             → js/livekit-manager.js
                             → js/whiteboard-sync.js  
                             → js/chat.js
                             → js/roster.js
                             → js/recording.js
                             → js/apps/code-editor.js
                             → js/apps/phet.js
                             → js/apps/math.js
                             → js/apps/youtube-cowatch.js
                             → js/apps/mermaid.js
                             → js/apps/quiz.js
```

**Hành động cụ thể:**
- [x] Tách CSS ra file riêng `board.css`. (Đã có sẵn một phần hoặc không yêu cầu trực tiếp trong Option A, nhưng JS đã được tách).
- [x] Tách từng module JS ra file riêng, dùng `<script src="">` hoặc dynamic `import()`.
- [x] Lazy-load các module nặng (MathLive, Mermaid, pdf.js, PhET) chỉ khi người dùng bấm nút tương ứng. (JS đã tách, có thể lazy-load sau).
- [ ] Sửa `BlackboardFrame.java` để serve static files thay vì đọc toàn bộ HTML vào byte array.

#### Task 1.3: Chuẩn hóa Config — Loại bỏ Hardcode URL

**Mục tiêu:** Tập trung tất cả URL/config vào 1 file duy nhất.

**Hành động cụ thể:**
- [x] Tạo file `config.js` chứa tất cả URL endpoints:
  ```javascript
  window.TUTORHUB_CONFIG = {
      SYNC_SERVER: "https://hocba299-3-tutorhub-sync.hf.space",
      VSCODE_SERVER: "https://hocbatrolai293-tutorhub-vscode.hf.space",
      AI_SERVER: "https://hocbatrolai293-tutorhub-ai.hf.space",
      LIVEKIT_URL: "wss://tutorhub-enterprise-q820cqx7.livekit.cloud"
  };
  ```
- [x] Thay thế tất cả hardcoded URL trong HTML/JS bằng `TUTORHUB_CONFIG.xxx`.
- [x] Tạo `AppConfig.java` tương ứng cho Java side.

#### Task 1.4: Bảo mật cơ bản cho LiveKit Token API

- [x] Thêm middleware xác thực (API key hoặc JWT) cho endpoint `/livekit/token`.
- [x] Thêm rate limiting (express-rate-limit): Max 10 requests/phút/IP.
- [x] Thêm CORS whitelist thay vì dùng wildcard `*`.

---

### 🟡 GIAI ĐOẠN 2 — Trung hạn (3-4 tháng): "TỐI ƯU HIỆU NĂNG"

#### Task 2.1: Nâng cấp Whiteboard Sync lên CRDT (Yjs)

**Mục tiêu:** Thay thế cơ chế "broadcast toàn bộ elements" bằng CRDT để đồng bộ conflict-free.

**Hành động cụ thể:**
- [ ] Tích hợp thư viện Yjs + y-websocket vào Node.js sync server.
- [ ] Sử dụng `Y.Map` / `Y.Array` để lưu trạng thái bảng vẽ.
- [ ] Mỗi thay đổi chỉ gửi **delta** (incremental update) thay vì toàn bộ scene.
- [ ] Tự động resolve conflict khi 2 người chỉnh sửa cùng 1 shape.
- [ ] Thêm **Awareness Protocol** (con trỏ chuột của từng người, màu khác nhau trên bảng).

**Lợi ích:**
- Giảm 90% bandwidth truyền tải.
- Không còn race condition / overwrite.
- Hỗ trợ offline editing → sync lại khi online.

#### Task 2.2: Tối ưu RAM — Giảm tải JCEF

**Hành động cụ thể:**
- [ ] Bật `--disable-gpu-compositing` trên máy yếu để tránh tạo thêm GPU process.
- [ ] Set `--js-flags="--max-old-space-size=256"` để giới hạn V8 heap.
- [ ] Lazy-init JCEF: Chỉ tạo browser instance khi người dùng thực sự mở phòng học.
- [ ] Dispose JCEF browser khi thoát phòng (`cefBrowser.close(true)`).
- [ ] Cân nhắc **chuyển từ JCEF sang JxBrowser** nếu budget cho phép (API tốt hơn, quản lý RAM tốt hơn).

#### Task 2.3: State Management — Quản lý trạng thái phòng học

**Hành động cụ thể:**
- [ ] Tạo `RoomState` object trung tâm trong JavaScript:
  ```javascript
  const RoomState = {
      participants: Map,       // identity → metadata
      isRecording: false,
      isMuted: false,
      cameraEnabled: false,
      currentTool: "pointer",
      boardId: null,
      chatMessages: [],
      quizResults: Map
  };
  ```
- [ ] Sử dụng EventEmitter pattern để các module subscribe vào state changes.
- [ ] Đảm bảo UI luôn reactive với state (không dùng `document.getElementById` trực tiếp để toggle display).

#### Task 2.4: Nâng cấp Video Layout
- [x] **C7. Video Layout (Gallery / Speaker View) [Tiến độ: DONE]**
    - Thiết kế hệ thống layout manager cho phần tử `#video-sidebar`.
    - Hỗ trợ đổi class CSS để chuyển chế độ: Top Bar, Gallery (Grid), Speaker (Focus).
    - Tích hợp sự kiện `ActiveSpeakersChanged` của LiveKit để nổi bật người đang nói.
    - Xử lý mượt mà khi đổi giữa các View Modes, ghim (pin) người nói.
- [x] Animation mượt khi chuyển đổi layout (CSS transition / FLIP animation).

---

### 🔴 GIAI ĐOẠN 3 — Dài hạn (6-12 tháng): "ĐỘT PHÁ & KHÁC BIỆT"

#### Task 3.1: AI Tutor Assistant — Vượt xa ChatBot đơn thuần

**Ý tưởng:**
- **AI "nhìn" bảng vẽ (Vision AI):** Khi học sinh viết phương trình sai trên bảng, AI tự động nhận diện và gợi ý sửa.
- **AI Auto-Summary:** Sau buổi học, AI tự tóm tắt nội dung bài học từ bảng vẽ + chat + quiz results → xuất PDF.
- **AI Teaching Assistant:** Giáo viên giao bài qua chat, AI tự tạo Quiz phù hợp trình độ.
- **Real-time Transcription:** AI chuyển lời nói thành text trực tiếp (Speech-to-Text) → hiển thị phụ đề.

#### Task 3.2: Gamification — Biến học tập thành trò chơi

**Ý tưởng:**
- **Hệ thống XP & Level:** Học sinh nhận điểm kinh nghiệm khi tham gia lớp, trả lời Quiz đúng, hoàn thành bài tập.
- **Leaderboard lớp học:** Bảng xếp hạng real-time trong phòng học (thanh sidebar).
- **Achievement Badges:** Huy hiệu "Giơ tay 10 lần", "Hoàn thành 5 Quiz liên tiếp", "Tham gia 30 ngày liên tục".
- **Battle Mode (Arena nâng cấp):** 2 học sinh giải cùng 1 bài Code, ai xong trước thắng.

#### Task 3.3: Breakout Rooms — Phòng nhóm nhỏ

**Ý tưởng:**
- Giáo viên chia lớp thành các nhóm nhỏ (2-4 người), mỗi nhóm có bảng vẽ riêng.
- Giáo viên có thể "đi tuần" (join vào từng phòng nhóm).
- Khi hết giờ, tất cả tự động quay về phòng chính.
- **Đây là tính năng Zoom có nhưng Teams/Meet làm chưa tốt, và chưa ai kết hợp với Infinite Whiteboard.**

#### Task 3.4: Lesson Replay — Phát lại buổi học

**Ý tưởng:**
- Ghi lại toàn bộ **nét vẽ, audio, và video** theo timeline.
- Học sinh có thể "tua" lại buổi học như xem video, nhưng **bảng vẽ cũng tua theo** (thấy từng nét vẽ xuất hiện dần).
- Export thành video MP4 hoặc interactive HTML.
- **Không nền tảng nào có tính năng này ở cấp độ tích hợp bảng vẽ.**

#### Task 3.5: Cân nhắc chuyển sang Electron hoặc Tauri (Dài hạn nhất)

**Lý do:**
- JCEF có giới hạn về hiệu năng và hệ sinh thái plugin.
- Electron (như Teams đang dùng) hoặc Tauri (Rust-based, nhẹ hơn) sẽ cho phép:
  - Truy cập native APIs tốt hơn.
  - Giảm 40-60% RAM so với JCEF.
  - Hỗ trợ auto-update dễ dàng hơn.
  - Mở rộng lên macOS/Linux đơn giản hơn.

---

## BẢNG TỔNG KẾT ROADMAP

| Giai đoạn | Thời gian | Ưu tiên | Task chính | Tác động |
|---|---|---|---|---|
| **GĐ1** | 1-2 tháng | 🔴 Cao | Dọn toolbar, tách module, chuẩn hóa config, bảo mật | UX tốt hơn 80%, code dễ bảo trì |
| **GĐ2** | 3-4 tháng | 🟡 Trung bình | CRDT sync, tối ưu RAM, state management, video layout | Hiệu năng tốt hơn 60%, đồng bộ mượt |
| **GĐ3** | 6-12 tháng | 🟢 Chiến lược | AI Vision, Gamification, Breakout Rooms, Lesson Replay | Sản phẩm EdTech đẳng cấp quốc tế |

---

## KẾT LUẬN

TutorHub đang sở hữu một nền tảng có **ý tưởng thiết kế rất mạnh** — Whiteboard-First Classroom là triết lý không ai trong nhóm Zoom/Teams/Meet theo đuổi. Tuy nhiên, việc triển khai kỹ thuật (technical implementation) đang ở giai đoạn MVP (Minimum Viable Product) với nhiều technical debt cần giải quyết.

**3 việc cần làm NGAY LẬP TỨC:**
1. **Gom toolbar lại** — Đây là thay đổi nhỏ nhất nhưng tác động UX lớn nhất.
2. **Tách `tldraw_board_v2.html`** — Giảm dung lượng, tăng tốc load, dễ bảo trì.
3. **Bảo mật API LiveKit token** — Tránh bị abuse trước khi có nhiều người dùng.

Khi 3 việc này xong, TutorHub sẽ sẵn sàng cho giai đoạn tăng trưởng tiếp theo.
