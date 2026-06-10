# TutorHub Enterprise 🎓

TutorHub Enterprise là một nền tảng giáo dục trực tuyến (EdTech) toàn diện, kết hợp sức mạnh của ứng dụng Desktop truyền thống (Java) và các công nghệ Web/Cloud hiện đại. Dự án được thiết kế hướng tới trải nghiệm "Đẳng cấp thế giới" (World-Class) tương tự như ClassIn, Microsoft Teams, nhưng với những tính năng độc đáo dành riêng cho giới trẻ.

---

## 🚀 Các Tính Năng Nổi Bật (Cập nhật mới nhất)

### 1. Hệ thống Thi Bảo Mật (Secure Exam Module)
Module **TutorHub Secure Exam (TSE)** là hệ thống kiểm soát môi trường thi trên máy tính, chống gian lận và khóa màn hình chuyên nghiệp:
- **Lockdown Core (Rust):** Module bảo mật cấp thấp viết bằng Rust, tạo ra một Secure Desktop (Màn hình cách ly hoàn toàn khỏi Windows Desktop mặc định).
- **Process & Keyboard Hooking:** Chặn phím tắt (Alt+Tab, Win, Ctrl+Alt+Del) và giám sát tiến trình lạ.
- **Java Child Process:** Trình làm bài JCEF chạy trên Secure Desktop với UX/UI 3 bước: Đăng nhập -> Chọn cấu hình thi -> Bắt đầu làm bài.
- **Tự động lưu & Thu hồi:** Cơ chế Autosave, gửi Submit payload an toàn. JVM exit cực nhanh bằng `Runtime.halt(0)`.

### 2. Locket Class (Chia sẻ Khoảnh khắc)
Một tính năng độc quyền mang phong cách mạng xã hội (giống Instagram Stories / Locket) ngay trong lớp học.
- **Phát Video/Ảnh mượt mà:** Tích hợp JavaFX `MediaPlayer` trong Swing để phát video HLS/MP4.
- **Cloud Storage:** Tự động nén video bằng FFmpeg và đẩy lên đám mây **Backblaze B2**.

### 3. Không Gian Học Trực Tuyến (Live Classroom & Blackboard)
- **Kiến trúc Web-in-Desktop:** Nhúng trực tiếp trình duyệt Chromium (JCEF) chạy Excalidraw vào Java Swing để tận dụng sức mạnh WebGL.
- **Ultra-low Latency:** Đồng bộ nét vẽ thời gian thực.

---

## 🏗️ Kiến Trúc Hệ Thống (Tech Stack)

### Frontend (Desktop Client)
- **UI Framework:** Java Swing kết hợp **FlatLaf**.
- **Media/Web Integration:** JavaFX (`JFXPanel`) xử lý Video, JCEF xử lý Web/Bảng vẽ.

### Module Bảo Mật Cấp Thấp
- **Ngôn ngữ:** Rust (Sử dụng Win32 APIs).
- **Chức năng:** Tạo Secure Desktop riêng, chặn màn hình capture. `TutorHub_LockdownCore.exe` là tệp nhị phân chịu trách nhiệm vận hành.

### Backend & Cơ Sở Dữ Liệu
- **Core Server:** Java Server xử lý các luồng packet TCP/Sockets truyền thống.
- **Real-time Sync:** Node.js WebSockets xử lý luồng dữ liệu nét vẽ cường độ cao.
- **Database:** PostgreSQL (NeonDB).
- **Cloud Storage:** Backblaze B2 (S3-compatible API).

---

## 🛠 Hướng dẫn Cài đặt & Build (Dành cho Lập trình viên)

Dự án này sử dụng kiến trúc kết hợp **Java Maven** và **Rust Cargo**. 
Xin vui lòng đọc tệp [docs/setup_new_machine.md](docs/setup_new_machine.md) để biết cách:
1. Thiết lập biến môi trường và tải về (`git clone`).
2. Tái tạo cấu hình cục bộ từ `application.example.properties`.
3. Kiểm thử với kịch bản `verify_environment.ps1`.

### Build Nhanh Ứng dụng Java
```bash
mvn clean install
```

### Build Lại Module Rust (TutorHub Secure Exam)
Nếu bạn thay đổi mã nguồn trong thư mục `tutorhub_lockdown`, bạn phải biên dịch lại mã nguồn Rust và sao chép nó vào thư mục tài nguyên của Java:
```bash
cd tutorhub_lockdown
cargo build --release
copy target\release\TutorHub_LockdownCore.exe ..\src\main\resources\tools\
```

---
*Tài liệu này (README) được cập nhật liên tục để các thành viên dự án và người dùng có thể nhanh chóng nắm bắt bức tranh toàn cảnh của nền tảng TutorHub Enterprise.*
