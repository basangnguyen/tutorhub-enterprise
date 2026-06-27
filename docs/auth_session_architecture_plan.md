# Kế hoạch Kiến trúc Session / Token (AUTH-03)

Tài liệu này mô tả thiết kế kiến trúc quản lý phiên đăng nhập (Session/Token Management) cho hệ thống TutorHub Enterprise, nhằm thay thế cơ chế payload `DASHBOARD_GO` thô sơ bằng cơ chế Opaque Token bảo mật, hỗ trợ Device Management và Auto Login an toàn.

## 1. Đánh giá hiện trạng (As-is)

Hiện tại, cơ chế đăng nhập và xác thực của hệ thống diễn ra như sau:
*   **Tạo Payload ở Server:**
    Sau khi đăng nhập thành công qua Email/Password (`ClientHandler.java:1851`, `ClientHandler.java:1501`) hoặc Social Auth (`SocialAuthService.java:301`), server đóng gói thông tin session thành chuỗi string: 
    `"DASHBOARD_GO|" + userId + "|" + role + "|" + avatarBase64`
    Chuỗi này được trả về qua class `AuthResponse` trong biến `dashboardPayload`.
*   **Parse Payload ở Client:**
    *   Trong `LoginFrame.java` (Dòng 319-325): Chuỗi `dashboardPayload` được tách theo ký tự `|` để lấy `userId` (`uid`). Sau đó client gọi trực tiếp `new MainDashboard(finalUid, username, "", "").setVisible(true);`. Các thông tin như `role`, `avatar` hiện đang bị bỏ qua hoặc truyền chuỗi rỗng.
    *   Trong `OAuthLoginFlow.java` & `FacebookLoginFlow.java`: Client cũng tách chuỗi để lấy `uid` và `roleStr`, sau đó gọi `new MainDashboard(uid, "SocialUser", roleStr)`.
*   **Logout:** 
    Khi nhấn Logout trong `HeaderPanel.java`, client chỉ thực hiện ngắt kết nối WebSocket thông qua `NetworkManager.resetInstance()`, đóng `MainDashboard` và mở lại `LoginFrame`. Server chỉ dọn dẹp các tài nguyên socket trong hàm `onClose()`. **Hoàn toàn chưa có cơ chế Revoke session ở Server-side.**

## 2. Đề xuất kiến trúc Session an toàn

### 2.1 Loại Token
Sử dụng **Opaque Token** thay vì JWT (JSON Web Token) để dễ dàng thu hồi (revoke) session khi logout, giới hạn thiết bị, hoặc phát hiện đăng nhập bất thường. Token chỉ là chuỗi ngẫu nhiên dài (Ví dụ: sinh bằng `SecureRandom`), mọi thông tin session được tra cứu từ Database.

### 2.2 Cấu trúc Database (`auth_sessions`)
```sql
CREATE TABLE IF NOT EXISTS auth_sessions (
  id VARCHAR(64) PRIMARY KEY,                  -- Session ID định danh
  user_id INT NOT NULL,                        -- ID người dùng
  access_token_hash TEXT NOT NULL,             -- SHA-256 của Access Token
  refresh_token_hash TEXT,                     -- SHA-256 của Refresh Token (nếu bật Keep me signed in)
  device_id TEXT,                              -- HWID hoặc Hardware Fingerprint của thiết bị
  device_name TEXT,                            -- Tên máy tính (VD: DESKTOP-ABC)
  app_version TEXT,                            -- Phiên bản ứng dụng Client
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP NOT NULL,               -- Thời điểm hết hạn Access Token
  revoked_at TIMESTAMP                         -- Thời điểm user chủ động Logout hoặc bị Server đá
);
```

## 3. Flow Đăng nhập mới (To-be)

1. Client gửi request đăng nhập (Email/Password hoặc OAuth).
2. Server xác thực thành công.
3. Server sinh `accessToken` (Ngắn/trung hạn, ví dụ 24h) và `refreshToken` (Dài hạn, ví dụ 30 ngày).
4. Server lưu **hash SHA-256** của các token này vào bảng `auth_sessions`.
5. Server trả về `AuthResponse` gồm:
   ```json
   {
      "success": true,
      "message": "Đăng nhập thành công",
      "sessionInfo": {
          "accessToken": "sk_1234567890abcdef...",
          "refreshToken": "rf_1234567890abcdef...",
          "expiresAt": 1718000000,
          "userProfile": { "userId": 42, "role": "TUTOR", "avatar": "..." }
      },
      "dashboardPayload": "DASHBOARD_GO|42|TUTOR|..." // Dùng để Backward Compatibility
   }
   ```
6. Client lưu `accessToken` vào Runtime Memory (`SessionManager`) để gắn vào Header/Packet trong các request gọi API/WS tiếp theo.

## 4. Cơ chế Logout

1. Khi User bấm Logout, Client gửi một Packet `AUTH_LOGOUT` đính kèm `accessToken` hiện tại.
2. Server nhận Packet, băm (hash) `accessToken`, tìm trong bảng `auth_sessions` và set `revoked_at = CURRENT_TIMESTAMP`.
3. Client tự động xóa `SessionManager` ở local memory.
4. Client tiến hành dọn dẹp `NetworkManager`, đóng Dashboard và chuyển về `LoginFrame` như cũ.

## 5. Thiết kế "Keep me signed in" (Auto Login)

Tính năng này yêu cầu OS Secure Storage vì Token không được để lọt ra dạng raw:
*   Client sử dụng JNA/DPAPI (Windows Data Protection API) hoặc Windows Credential Manager để lưu trữ `refreshToken`. Không dùng `java.util.prefs.Preferences`.
*   Khi mở App, Client gọi DPAPI lấy ra `refreshToken`. Gửi lên Server qua action `AUTH_REFRESH_SESSION`.
*   Server kiểm tra Hash của `refreshToken`, check `revoked_at` và `expires_at`. Nếu hợp lệ, cấp `accessToken` và `refreshToken` mới (Token Rotation).

## 6. Backward Compatibility (Bảo vệ code cũ)

Để quá trình chuyển giao mượt mà và không gây gián đoạn:
*   **Giai đoạn đầu:** Trong object `AuthResponse.java`, ta sẽ bổ sung thêm trường `SessionInfo sessionInfo`. Vẫn giữ nguyên việc tạo chuỗi `dashboardPayload` và thuộc tính `dashboardPayload`.
*   **Client fallback:** Trong các Frame như `LoginFrame`, `OAuthLoginFlow`, Client sẽ thử kiểm tra `if (response.getSessionInfo() != null) { ... }`. Nếu trả về `null` hoặc server chưa update, Client tự động đọc `dashboardPayload` như hiện tại để sử dụng.
*   Khi chắc chắn tất cả luồng đã sử dụng `sessionInfo` ổn định (qua các bản release), tiến hành đánh dấu `@Deprecated` và xóa bỏ `DASHBOARD_GO` hoàn toàn ở Server.

## 7. Roadmap Triển khai (Implementation Phases)

*   **Phase S1 — Chuẩn bị Cơ sở dữ liệu:** Tạo bảng `auth_sessions`, viết DAO `SessionDAO.java` và Service `SessionManagementService.java`. Không thay đổi UI/Client.
*   **Phase S2 — Cập nhật Core Login:** Sinh `sessionInfo` sau khi login bằng Email/Password (vẫn giữ payload cũ).
*   **Phase S3 — Cập nhật Social Login:** Sinh `sessionInfo` trong `SocialAuthService.java` cho Google/Facebook.
*   **Phase S4 — Client Runtime Session:** Thêm `SessionManager` ở Client để lưu trữ `accessToken` trong RAM khi ứng dụng đang chạy. Cập nhật `LoginFrame` và `OAuthLoginFlow` ưu tiên đọc `sessionInfo`.
*   **Phase S5 — Server-side Revocation:** Thêm action `AUTH_LOGOUT` vào giao thức `AuthProtocol`. Triển khai revoke ở Server và gắn logic này vào nút Logout ở `HeaderPanel.java`.
*   **Phase S6 — Loại bỏ Legacy:** Xóa hoàn toàn mã liên quan đến chuỗi `DASHBOARD_GO` ở cả Server và Client.
*   **Phase S7 — Secure Storage (Tương lai):** Thư viện JNA/DPAPI được tích hợp vào Client để hỗ trợ tính năng Keep me signed in bằng Refresh Token.
