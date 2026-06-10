# Bảng Kiểm Tra (Fresh Clone Test)

Dưới đây là Checklist bắt buộc phải thử nghiệm mỗi khi clone mã nguồn TutorHub về một thiết bị/máy chủ hoàn toàn mới để đảm bảo tính sẵn sàng của dự án.

Hãy tích dấu `[x]` vào các hạng mục sau khi hoàn thành.

## 1. Môi trường Cơ sở
- [ ] Clone repo thành công qua lệnh `git clone`.
- [ ] Mở thành công toàn bộ thư mục bằng Antigravity IDE (hoặc VS Code / IntelliJ).
- [ ] Chạy lệnh `.\verify_environment.ps1` báo PASS (Xanh) toàn bộ các mục (Java, Git, Maven, Rust).

## 2. Biên dịch & Vận hành Core
- [ ] Chạy thành công lệnh `mvn clean install` không báo lỗi (BUILD SUCCESS).
- [ ] Nếu cần, biên dịch lại Rust với `cargo build --release` thành công.
- [ ] Không cần copy các thư mục phụ trợ như `target/`, `dist/`, `runtime/` từ máy cũ sang máy mới.

## 3. Kiểm tra Flow Ứng dụng
- [ ] `TutorServer` khởi động thành công và lắng nghe trên port 7860.
- [ ] Chạy `LoginFrame` thành công, giao diện ứng dụng hiển thị tốt.
- [ ] Đăng nhập thành công với tài khoản Dev/Admin cục bộ.
- [ ] Tab **Exam** load được thông tin các kỳ thi giả lập (Active Exam).

## 4. Kiểm tra Secure Exam
- [ ] Module `Secure Exam Launcher` mở lên bình thường.
- [ ] Giao diện thi đi đúng trình tự 3 bước: **Login** -> **TSE Config** -> **Exam Desktop**.
- [ ] JCEF Render được giao diện làm bài thi (đòi hỏi phải tự chép tay `jcef_core_v2` vào đúng vị trí).
- [ ] Bấm Nộp bài hệ thống thoát sạch sẽ (không treo đen).

## 5. Đóng gói Bản Portable
- [ ] Chạy thử kịch bản `build_portable.ps1`.
- [ ] Xác nhận thư mục `dist\TutorHubSecureExam` được tạo thành công và chứa đầy đủ app nhị phân.
