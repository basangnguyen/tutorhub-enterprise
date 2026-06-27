# Hướng Dẫn Cấu Hình Facebook OAuth Login

Để tính năng Đăng nhập Facebook bằng OAuth hoạt động trên TutorHub, bạn cần tạo ứng dụng Facebook và lấy thông tin xác thực.

## 1. Tạo ứng dụng trên Meta for Developers
1. Truy cập [Meta for Developers](https://developers.facebook.com/).
2. Đăng nhập và chọn **My Apps** > **Create App**.
3. Chọn loại ứng dụng: **Authenticate and request data from users with Facebook Login** (hoặc **Consumer** tùy giao diện hiện tại).
4. Đặt tên App (vd: `TutorHub Enterprise`) và cung cấp email liên hệ. Nhấn **Create App**.

## 2. Cấu hình Facebook Login
1. Trong màn hình Dashboard của App, kéo xuống tìm thẻ **Facebook Login** và chọn **Set Up**.
2. Tại menu bên trái, chọn **Facebook Login** > **Settings**.
3. Ở phần **Valid OAuth Redirect URIs**, bạn nhập chính xác URL sau:
   - **Môi trường Dev Local:** `http://127.0.0.1:7861/oauth/facebook/callback`
   - **Môi trường Production:** URL thực tế, ví dụ `https://api.tutorhub.com/oauth/facebook/callback`
   *(Lưu ý: Không thể dùng `localhost`, Facebook yêu cầu `127.0.0.1` hoặc domain có HTTPS).*
4. Nhấn **Save Changes**.

## 3. Lấy App ID và App Secret
1. Quay lại menu bên trái, chọn **App Settings** > **Basic**.
2. Lưu lại **App ID**.
3. Nhấn nút "Show" bên cạnh **App Secret**, nhập mật khẩu Facebook để xem và copy chuỗi bí mật.

## 4. Bật yêu cầu App Secret Proof (Bảo mật bắt buộc)
Để bảo vệ tối đa API, hệ thống TutorHub đã cấu hình dùng `appsecret_proof`.
1. Trong trang Meta Developers, chọn **App Settings** > **Advanced**.
2. Tìm tùy chọn **Require App Secret** (Yêu cầu App Secret) -> Bật ON.
3. Lưu lại thay đổi.

## 5. Cấu hình vào hệ thống TutorHub
Mở file `config/local-oauth.properties` và cập nhật thông tin:
```properties
tutorhub.facebook.app.id=APP_ID_CUA_BAN
tutorhub.facebook.app.secret=APP_SECRET_CUA_BAN
tutorhub.facebook.redirect.uri=http://127.0.0.1:7861/oauth/facebook/callback
tutorhub.facebook.oauth.http.port=7861
```

## 6. Lưu ý khi Test
- Bản dev test hiện tại chỉ xin quyền `public_profile` (tránh lỗi Invalid Scopes).
- Quyền `email` sẽ được bật sau khi bạn thêm permission trong Meta Dashboard (có thể yêu cầu App Review hoặc thiết lập bổ sung tuỳ thuộc vào Facebook).
- App mặc định ở trạng thái **Development**, chỉ có những tài khoản Facebook được thêm vào mục **App Roles** (với quyền Admin, Developer hoặc Tester) mới đăng nhập được.
- Để người khác đăng nhập, cần chuyển App sang chế độ **Live**.
