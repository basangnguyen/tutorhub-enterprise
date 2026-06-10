# Quy trình Làm việc (Development Workflow)

Để đảm bảo source code trên GitHub luôn được quản lý đồng bộ, an toàn và không gây xung đột (conflict) hoặc lộ thông tin nhạy cảm (secret leaks), đội ngũ lập trình cần tuân thủ nghiêm ngặt Workflow dưới đây.

## 1. Đồng bộ mã nguồn (Trước khi code)
Luôn luôn lấy phiên bản mới nhất từ remote server trước khi chỉnh sửa bất kỳ thứ gì:
```bash
git pull
```

## 2. Kiểm tra trạng thái và Nhánh hiện tại
Đảm bảo bạn đang làm việc trên nhánh đúng và thư mục làm việc đã "sạch" (clean):
```bash
git status
```

## 3. Tạo nhánh tính năng (Branching)
Không nên code trực tiếp trên `main`. Hãy tạo một nhánh tính năng (feature branch) theo định dạng: `feature/<tên-task>` hoặc `bugfix/<tên-bug>`.
```bash
git checkout -b feature/tse-login-ui
```

## 4. Biên dịch và Sửa lỗi cục bộ
Trong quá trình code, thường xuyên chạy lệnh biên dịch để đảm bảo code không phá vỡ (break) ứng dụng:
```bash
mvn clean install
```
*(Nếu sửa module Rust, hãy chạy `cargo build --release` trong thư mục `tutorhub_lockdown`).*

## 5. Rà soát file thay đổi
Kiểm tra xem bạn đã thay đổi những gì để tránh add thừa các file rác hoặc cấu hình cá nhân:
```bash
git status
git diff
```

## 6. Đóng gói (Commit)
Chỉ add các file mã nguồn hợp lệ. Lưu ý đặt thông điệp (message) rõ ràng mô tả công việc:
```bash
git add .
git commit -m "Thêm tính năng màn hình tải cho Secure Desktop"
```

## 7. Đẩy mã nguồn (Push)
Sau khi hoàn tất, đẩy nhánh của bạn lên hệ thống:
```bash
git push -u origin <tên-nhánh>
```
*(Nếu làm trực tiếp trên main: `git push`)*

---

## 8. CÁC TỆP TUYỆT ĐỐI KHÔNG COMMIT

Bất kỳ khi nào thực hiện `git add .`, hãy xem lại `git status` và chắc chắn các thư mục/tệp sau **KHÔNG** bị đưa vào commit:
- Thư mục Build: `dist/`, `target/`, `runtime/`, `build/`.
- File có dung lượng siêu lớn không nằm trong luồng source (như `jcef_core_v2`, `ffmpeg.zip`).
- **SECRETS**: Không commit bất cứ file `.env`, tệp chứng chỉ (`.pfx`, `.key`, `.pem`), hay các tệp Properties lưu password thật (`application.properties`, `secrets.properties`).
- Nếu vô tình thêm secret, hãy gỡ nó ra ngay bằng lệnh `git restore --staged <file>` và chuyển thông tin nhạy cảm vào `application.example.properties`.
