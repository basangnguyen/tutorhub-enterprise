---
title: TutorHub Sync
emoji: 🎓
colorFrom: blue
colorTo: indigo
sdk: docker
app_port: 7860
---

# TutorHub Enterprise

TutorHub Enterprise là ứng dụng quản lý học tập và thi cử trực tuyến dành riêng cho trung tâm gia sư. 

Dự án ban đầu được build để hỗ trợ học trực tuyến, sau phình to ra thêm phần chống gian lận thi cử (Secure Exam) và mấy tính năng giải trí (như Locket). 

## Tính năng chính

- **Chống gian lận thi cử (TSE - TutorHub Secure Exam):** 
  - Khóa màn hình (Lockdown) dùng Rust để tạo một Desktop riêng biệt, cách ly hoàn toàn khỏi Windows.
  - Chặn phím (Alt+Tab, Win, Ctrl+Alt+Del) và các tool capture màn hình.
  - Giao diện làm bài thi chạy bằng JCEF (nhúng Chromium vào Swing). Hỗ trợ auto-save phòng khi rớt mạng.
- **Locket Class:** Chức năng chia sẻ video/ảnh giống Locket. Dùng JavaFX để phát video, backend tự nén bằng FFmpeg rồi đẩy lên Backblaze B2.
- **Lớp học Live & Bảng vẽ:** Tích hợp Excalidraw, đồng bộ nét vẽ real-time qua WebSockets.

## Tech Stack

- **Desktop App:** Java Swing + FlatLaf + MigLayout.
- **Web Integration:** JCEF (Java Chromium Embedded Framework).
- **Media:** JavaFX (JFXPanel).
- **Core Security:** Rust (Win32 API cho Secure Desktop).
- **Server:** Java WebSockets.
- **Database:** PostgreSQL (NeonDB).
- **Cloud:** Backblaze B2.

## Cài đặt & Build

Ae nhớ setup môi trường trước khi chạy, chi tiết xem ở [docs/setup_new_machine.md](docs/setup_new_machine.md).

### 1. Build App (Java)
```bash
mvn clean install
```

### 2. Build Lockdown Module (Rust)
Nếu có sửa code phần chặn màn hình trong folder `tutorhub_lockdown` thì ae cần build lại con exe:
```bash
cd tutorhub_lockdown
cargo build --release
copy target\release\TutorHub_LockdownCore.exe ..\src\main\resources\tools\
```

---
*Lưu ý: Các icon dùng trong dự án thuộc bộ Microsoft Fluent UI System Icons (MIT License).*
