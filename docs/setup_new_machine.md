# Hướng dẫn Cài đặt Môi trường (Setup New Machine)

Tài liệu này hướng dẫn chi tiết cách thiết lập môi trường để có thể clone, biên dịch (build) và chạy dự án TutorHub Enterprise & Secure Exam trên máy tính mới.

## 1. Yêu cầu Cài đặt Hệ thống
Bạn cần cài đặt các phần mềm sau và đảm bảo chúng có sẵn trong biến môi trường `PATH`:
- **Git:** Tải từ [https://git-scm.com/](https://git-scm.com/).
- **Java JDK 24:** Tải bộ JDK phù hợp với dự án và thiết lập biến môi trường `JAVA_HOME`.
- **Apache Maven:** Tải từ [https://maven.apache.org/](https://maven.apache.org/) (hoặc sử dụng bản đi kèm với IDE NetBeans qua `mvn.cmd`).
- **Rust Toolchain:** Tải từ [https://rustup.rs/](https://rustup.rs/). Yêu cầu bắt buộc nếu bạn cần thay đổi/build lại module bảo mật `TutorHub_LockdownCore`.

## 2. Sao chép (Clone) Dự án
Mở terminal/cmd và chạy:
```bash
git clone <repo-url>
cd TutorHub
```
*(Nếu sử dụng Antigravity IDE, hãy mở thư mục vừa clone bằng Antigravity).*

## 3. Cấu hình Môi trường Ứng dụng
Tạo file cấu hình local cho máy của bạn:
1. Copy file `src/main/resources/application.example.properties`.
2. Đổi tên thành `src/main/resources/application.properties` (hoặc `application.local.properties` tuỳ vào thiết lập Spring/Java properties của dự án).
3. Cập nhật các thông tin nhạy cảm vào file mới tạo (Database Neon, B2 keys, v.v.).

*Chú ý:* File này đã được thêm vào `.gitignore` để tránh rủi ro bảo mật.

## 4. Biên dịch dự án (Build)

### Biên dịch Java (Main App)
```bash
mvn clean install
```
*(Nếu máy không nhận lệnh `mvn`, trỏ đường dẫn tới `mvn.cmd` của NetBeans).*

### Biên dịch Rust (TutorHub_LockdownCore)
Nên thực hiện nếu có update mã nguồn bảo mật màn hình:
```bash
cd tutorhub_lockdown
cargo build --release
```
Sau khi build xong, copy file nhị phân vào thư mục gốc của Java:
```bash
copy target\release\TutorHub_LockdownCore.exe ..\src\main\resources\tools\
```
(Nếu có file `.sha256`, hãy cập nhật hash tương ứng để vượt qua các lớp kiểm tra toàn vẹn).

### Tải Chromium Embedded Framework (JCEF) và FFmpeg
- Thư mục `jcef_core_v2` và `ffmpeg.zip` bị bỏ qua trên Git do dung lượng lớn.
- **Cách xử lý:** Sao chép các tệp này từ ổ đĩa lưu trữ ngoài, từ bản Release trên GitHub (nếu có đính kèm ở Tags) hoặc cấu hình script tự động tải.

## 5. Chạy Thử Ứng dụng
Chạy đoạn script tự động để kiểm tra môi trường:
```powershell
.\verify_environment.ps1
```

Nếu báo `[OK]`, bạn có thể chạy lần lượt các thành phần sau để nghiệm thu:
1. Khởi động Server: Chạy `TutorServer`.
2. Khởi động Giao diện: Chạy `LoginFrame` (từ lớp main).
3. Đăng nhập và Mở tab **Exam**.
4. Test luồng thi bảo mật bằng tệp `run.bat` hoặc `run_gui.bat`.
5. Sinh bản portable: `powershell -ExecutionPolicy Bypass -File .\build_portable.ps1`.

## 6. Xử lý Lỗi Thường Gặp
- **Thiếu `TutorHub_LockdownCore.exe`:** Hãy chắc chắn bạn đã cài Rust và build lại project `tutorhub_lockdown` thành công.
- **Cửa sổ Exam trắng / Thiếu JCEF:** Tải thư mục `jcef_core_v2` đầy đủ và đặt ở thư mục tương ứng theo cấu hình.
- **Không tìm thấy lệnh mvn:** Thêm thư mục `bin/` của Maven vào PATH của hệ điều hành.
