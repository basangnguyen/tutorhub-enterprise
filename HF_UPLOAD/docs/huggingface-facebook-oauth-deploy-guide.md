# Hướng Dẫn Deploy Hugging Face Space (TutorHub Sync) với Facebook OAuth

Tài liệu này hướng dẫn cách đưa phiên bản mới nhất của TutorServer với các cấu hình OAuth 2.0 (Google, Facebook) lên Hugging Face Space một cách an toàn và đúng chuẩn.

## 1. Cấu trúc Kiến trúc (Nginx Reverse Proxy)
Hugging Face chỉ expose một port duy nhất ra ngoài public (thường là 7860). Do đó, chúng ta cần:
- **Nginx (Public Port 7860):** Router nhận kết nối từ ngoài.
- **TutorServer WebSocket (Internal Port 9000):** Xử lý Realtime, chat, bảng vẽ.
- **Facebook Callback Server (Internal Port 7861):** Xử lý nhận Code trả về từ Facebook.

**Cách hoạt động:**
- Mọi truy cập vào đường dẫn `/oauth/facebook/callback` sẽ được đẩy vào port `7861`.
- Các truy cập khác (`/`, `/ws`) sẽ được Upgrade lên WebSocket và đẩy vào port `9000`.

## 2. Quản lý Mật Khẩu (Secrets) trên Hugging Face
> [!CAUTION]
> **Tuyệt đối không commit file `config/local-oauth.properties` lên repo Git public của Hugging Face.**

Thay vào đó, bạn sử dụng Settings của Space:
1. Truy cập Space của bạn: `https://huggingface.co/spaces/hoca299/hoca299-3-tutorhub-sync/settings`
2. Kéo xuống phần **Variables and secrets**.
3. Thêm các **Secrets** (Bảo mật, không hiển thị lại):
   - `TUTORHUB_FACEBOOK_APP_ID`: App ID từ Meta Developer.
   - `TUTORHUB_FACEBOOK_APP_SECRET`: App Secret từ Meta Developer.
   - `TUTORHUB_GOOGLE_CLIENT_ID`: Google Client ID.
   - `TUTORHUB_GOOGLE_CLIENT_SECRET`: Google Client Secret.
4. Thêm các **Variables** (Cấu hình môi trường):
   - `TUTORHUB_FACEBOOK_REDIRECT_URI`: `https://hoca299-3-tutorhub-sync.hf.space/oauth/facebook/callback`
   - `TUTORHUB_FACEBOOK_OAUTH_HTTP_PORT`: `7861`

## 3. Cấu hình bên Meta Developer
- **App > Facebook Login > Settings > Valid OAuth Redirect URIs:**
- Bắt buộc phải thêm URL: `https://hoca299-3-tutorhub-sync.hf.space/oauth/facebook/callback`
- Lưu lại cấu hình (Save Changes).

## 4. Hướng dẫn Deploy (Đẩy Code lên Hugging Face)
1. Build file JAR Java Server mới nhất:
   ```bash
   mvn clean install -DskipTests
   ```
2. Copy hoặc di chuyển file `target/TutorServer-1.0-SNAPSHOT-jar-with-dependencies.jar` thành `TutorServer.jar` ở thư mục gốc (Nơi có `Dockerfile` của bạn).
3. Push toàn bộ code lên nhánh chính của thư mục Hugging Face Repo (hoặc sync Github).
4. Đợi Docker Container trên Hugging Face build (bạn có thể xem Log trong mục Build Logs).

## 5. Hướng dẫn Chạy Client (TutorHub Desktop)
Để ứng dụng Desktop kết nối được tới Hugging Face Cloud Server, khi chạy ứng dụng Desktop, bạn cần thêm biến System Property cho WebSocket URL:
```bash
java -Dtutorhub.websocket.url=wss://hoca299-3-tutorhub-sync.hf.space -jar TutorHubClient.jar
```
*Ghi chú: Nếu chạy từ NetBeans (máy Dev), bạn có thể vào Project Properties > Run > VM Options và thêm cấu hình trên, hoặc để trống thì ứng dụng sẽ mặc định nối vào `localhost:7860`.*

## 6. Xử lý Lỗi thường gặp
- **Space Error: Address already in use:** Hãy đảm bảo `Dockerfile` đang set biến môi trường `ENV PORT=9000` trước khi chạy server Java để tránh tranh chấp với Nginx (7860).
- **Callback không hoạt động (HTTP 404):** Kiểm tra xem Nginx proxy có đúng path `/oauth/facebook/callback` không.
- **Client Desktop không bắt được Facebook:** Hãy đảm bảo bạn đã khởi động Client bằng `wss://hoca299-3-tutorhub-sync.hf.space` thay vì `ws://` hay localhost.
