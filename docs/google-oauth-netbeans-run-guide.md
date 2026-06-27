# Hướng dẫn chạy Google OAuth với NetBeans

Khi chạy ứng dụng qua **Apache NetBeans** bằng Maven (`exec-maven-plugin`), có 2 cách để đảm bảo TutorServer nhận được cấu hình bảo mật Google OAuth:

## Cách A — Cấu hình qua VM Options (Ưu tiên cho Môi trường chạy chính)

Khi chạy Server qua NetBeans bằng một Profile riêng, bạn phải nhập VM Options cho Profile đó.

1. Nhấp chuột phải vào dự án **TutorHub_Enterprise** > Chọn **Properties**.
2. Chọn **Run** trong danh sách bên trái.
3. Trong phần Configuration, đảm bảo chọn đúng Profile dùng để chạy Server (ví dụ: `<Server Profile>`).
4. Tại ô **VM Options**, nhập:
```text
-Dtutorhub.google.client.id=CLIENT_ID
-Dtutorhub.google.client.secret=CLIENT_SECRET
-Dtutorhub.google.redirect.port=8889
```
*(Thay thế CLIENT_ID và CLIENT_SECRET bằng thông tin thật của bạn từ Google Cloud Console)*

**Lưu ý:** Cách này đôi khi không hoạt động nếu bạn dùng tính năng "Run File" (Shift+F6) trực tiếp lên file `TutorServer.java` do NetBeans sử dụng action `run.single.main` không chứa VM Options.

## Cách B — Sử dụng Local Properties File (Ưu tiên cho Developer)

Nếu bạn không muốn thiết lập VM Options hoặc bị lỗi NetBeans không nhận cấu hình, hãy tạo file cấu hình cục bộ.

1. Tại thư mục gốc của dự án (cùng cấp với `pom.xml`), tạo một thư mục tên là `config`.
2. Bên trong `config`, tạo file `local-oauth.properties`.
3. Điền nội dung sau vào file `local-oauth.properties`:
```properties
tutorhub.google.client.id=CLIENT_ID
tutorhub.google.client.secret=CLIENT_SECRET
tutorhub.google.redirect.port=8889
```

**An toàn bảo mật:** 
Hệ thống đã tự động đưa thư mục `config/local-oauth.properties` vào file `.gitignore` để tránh vô tình commit secret lên GitHub.

## Cách Kiểm Tra
Sau khi chạy `TutorServer`, hãy xem cửa sổ Output Console. Nếu cấu hình thành công, hệ thống sẽ in ra dòng trạng thái:
```text
[SERVER] Đang khởi động...
[OAUTH CONFIG] Google Client ID loaded: true
[OAUTH CONFIG] Google Client Secret loaded: true
[OAUTH CONFIG] Redirect Port: 8889
```
Lúc này bạn có thể chạy `LoginFrame` và test tính năng Google Login bình thường.
