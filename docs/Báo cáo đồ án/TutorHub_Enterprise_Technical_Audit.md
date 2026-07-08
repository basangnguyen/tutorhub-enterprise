# 🏛️ Tổng Hợp Đánh Giá Kỹ Thuật Chuyên Sâu (Master Technical Audit Report) - TutorHub Enterprise

> **Phiên bản:** 3.0 (Tổng hợp từ tất cả Blueprint và Phân tích hệ thống)
> **Phạm vi:** Phân tích cấu trúc thư mục, Frontend, Backend, UI/UX, Module thi bảo mật (TSE V2), Whiteboard/Classroom, QuizHub, AI, Locket.

## 1. Kiến trúc Tổng thể (Hybrid Architecture)
Dự án không phải là một phần mềm Desktop thông thường mà là sự kết hợp (Hybrid) giữa **Native Desktop App (Java)** và **Web Technologies (Node.js/React/HTML5)** thông qua **JCEF (Java Chromium Embedded Framework)**.
- **Tầng Native (Host Process - JVM)**: Chịu trách nhiệm quản lý cửa sổ (Swing/FlatLaf), Socket Server cục bộ, gọi API hệ điều hành (JNA), I/O Ổ cứng, Database.
- **Tầng Web (Embedded)**: Xử lý các UI phức tạp, Real-time Whiteboard (Tldraw), Video Call (LiveKit Cloud WebRTC), QuizHub.
- **Giao thức liên lạc**: Sử dụng cơ chế IPC, Named Pipes cho giao tiếp giữa Java và Rust; WebSocket (Hybrid JSON/Binary) cho liên lạc Java Client - Java Server.

## 2. Phân tích Các Module Cốt Lõi (Core Modules Analysis)

### 2.1. Module Phòng Học Trực Tuyến (TutorHub Classroom)
Thay vì sử dụng Zoom/Teams, TutorHub tự xây dựng phòng học trực tuyến với triết lý: **Whiteboard-First (Bảng trắng là trung tâm)**.
- **Ưu điểm**:
  - Dùng **Tldraw V2** (React-based) tạo không gian canvas vô hạn.
  - Video streaming sử dụng **LiveKit Cloud (SFU WebRTC)** với Adaptive Stream chất lượng cao, hạ tầng chuyên nghiệp.
  - Tích hợp phong phú: MathLive (Toán), Mermaid (Sơ đồ), PhET (Thí nghiệm ảo), YouTube Co-watch, Code Arena.
- **Điểm thắt cổ chai (Bottlenecks) cần lưu ý**:
  - Toàn bộ logic frontend dồn vào một file khổng lồ `tldraw_board_v2.html` (~8MB, 20.000+ dòng code). Gây khó khăn lớn cho bảo trì, parse time cao.
  - Board Sync đang dùng **WebSocket Broadcast** thay vì thuật toán đồng bộ trạng thái CRDT (Yjs). Dễ gây ra race condition khi 2 người vẽ cùng lúc.
  - JCEF + Java Swing + LiveKit tiêu tốn khá nhiều RAM (800MB - 1.2GB).
- **Lộ trình tối ưu**: Tái cấu trúc Toolbar (mô hình Zoom More Menu để tránh quá tải tính năng), tách file HTML thành các module nhỏ, chuyển đổi đồng bộ bảng vẽ sang Yjs CRDT.

### 2.2. Module Ôn Thi Trắc Nghiệm (QuizHub)
Kế hoạch nâng cấp từ trang `quiz.html` tĩnh thành một hệ thống ôn luyện chuyên nghiệp (tương tự Azota, Quizizz, Quizlet).
- **Hiện trạng**: UI Flashcard, trộn đề, chấm điểm đã hoàn thiện khá tốt nhưng dữ liệu đang bị hardcode (Đề 1, 2, 3).
- **Định hướng phát triển**:
  - **Nhập đề từ Excel**: Đưa file Excel vào, hệ thống tự động bóc tách câu hỏi, đáp án, giải thích.
  - **JS ↔ Java Bridge (`JSObject`)**: Java sẽ đọc file Excel/SQLite và truyền dữ liệu JSON xuống `WebView` thông qua `window.quizBridge`. Tách biệt hoàn toàn UI và Logic truy xuất dữ liệu.
  - Hỗ trợ chế độ Flashcard theo thuật toán **Spaced Repetition (Leitner system)**.
  - Tích hợp game hoá (Gamification): streak, điểm số, luyện tập sửa sai (Mastery Peak).

### 2.3. Module Thi Bảo Mật - Chống Gian Lận (TutorHub Secure Exam - TSE V2)
Đây là "trái tim" kỹ thuật của hệ thống, được thiết kế vô cùng tinh vi qua hàng chục giai đoạn (phases).
- **Kiến trúc 2 Tiến trình độc lập**: 
  - Host Process (Java JVM).
  - OS Security Layer (Rust Core - `TutorHub_LockdownCore.exe`): Nhẹ (~3MB), giao tiếp với JVM qua Named Pipe (`\\.\pipe\TutorHubExam`) bằng các lệnh PING/PONG.
- **Cơ chế Khóa Môi trường (Lockdown Mechanism)**:
  - Sử dụng API Windows `CreateDesktopW` để mở một màn hình ảo (Virtual Desktop), cách ly bài thi khỏi toàn bộ hệ điều hành chính (chặn tuyệt đối OBS, cheat engine, phần mềm thứ 3).
  - JNA Hooks (LowLevelKeyboardProc): Khóa phím Windows, Alt+Tab, Task Manager.
  - Hash-based Process Scanner (`process_scanner.rs`): Quét mã băm phát hiện và tự động diệt các tiến trình đen.
  - VM Detection (`vm_detection.rs`): Phát hiện gian lận qua máy ảo (CPUID/MAC check).
- **Kiến trúc Cứu hộ và Toàn vẹn (Fault-Tolerant & Integrity)**:
  - **Watchdog & Fail-safe**: Liên tục gửi nhịp tim (Heartbeat). Nếu JVM sập hoặc treo, Rust Core tự động nhận diện và nhả khóa (SwitchDesktop về lại môi trường cũ) để tránh khóa chết máy học sinh.
  - **V2 State Machine Ledger**: Mọi thao tác thi đều được lưu vào Sổ cái (Ledger - SQLite) qua `V2AttemptStatusExecutionLedgerDAO`, `V2ScoreDraftDAO`... Đảm bảo bài thi được phục hồi nguyên vẹn dù rớt mạng hay cúp điện. Cấu trúc Handoff Bundle (đề thi, kết quả) được mã hóa bảo mật trước khi đẩy xuống Client. Có tích hợp kiểm tra mã băm TEK Hash.

### 2.4. Module AI & Tìm Kiếm & Mạng Xã Hội
- **Scenic Searchbar**: Thanh tìm kiếm trung tâm mô phỏng Spotlight, tích hợp dropdown command.
- **AI Agent (LaVie/Gemini)**: Tích hợp API qua `AiAgentServiceFactory` (Langchain4j/HuggingFace). Agent hỗ trợ học sinh học tập tự động, phân tích tài liệu, chạy ngầm trên Background Thread mà không làm nghẽn UI (EDT).
- **Locket (Mạng xã hội nội bộ)**: Feed chia sẻ ảnh/video trực tiếp bên trong ứng dụng, có tính năng bình luận, thả tương tác. Quản lý bởi bộ 3 `LocketPostDAO`, `LocketReactionDAO`, `LocketCommentDAO` và lưu trữ hình ảnh trên Backblaze B2 đám mây.

## 3. Khuyến nghị Kỹ thuật Tối ưu Hóa (Technical Recommendations)
1. **Kiến trúc Frontend**:
   - Tách ngay `tldraw_board_v2.html` thành dự án React/Vite độc lập.
   - Biên dịch tĩnh và nạp thông qua local server hoặc giao thức file `file://` chuẩn để cải thiện tốc độ nạp (Parse Time).
2. **Kiến trúc Mạng**:
   - Triển khai thuật toán CRDT (Conflict-free Replicated Data Type) như **Yjs** cho module Whiteboard thay cho Raw WebSocket.
   - Bổ sung bảo mật Token/Auth (JWT) cho các API LiveKit để ngăn chặn truy cập trái phép vào phòng họp.
3. **Quản lý Bộ nhớ (RAM)**:
   - Chủ động gọi hàm hủy (Dispose) các instance JCEF ngay khi người dùng đóng Tab (thay vì Cache toàn bộ) để lấy lại RAM cho hệ thống, tránh vượt ngưỡng 1GB.

---
*Tài liệu này tổng hợp toàn bộ triết lý thiết kế, lộ trình nâng cấp từ các Master Blueprint trong dự án. TutorHub Enterprise không chỉ là một ứng dụng quản lý, mà là một hệ sinh thái học trực tuyến mạnh mẽ với khả năng bảo mật (TSE V2) và tương tác sâu (Tldraw/LiveKit) ở đẳng cấp doanh nghiệp.*
