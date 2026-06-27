# TutorHub Auth Production Audit

Ngay tao: 2026-06-23  
Pham vi: Login, Sign Up, Forgot Password, OTP, SMS, Google OAuth, Facebook OAuth, session/logout, cau hinh auth server/client trong du an Java Swing `TutorHub Enterprise / TutorHub_Maven`.  
Trang thai: Audit va ke hoach san pham. Khong sua code trong phase nay.

> Ghi chu bao mat: Tai lieu nay khong in gia tri secret, token, password, database URL that, app secret, B2 key, SMTP password hay OAuth secret. Cac key cau hinh chi duoc nhac o muc "co ton tai / can quan ly bang secret manager".

## 1. Ket Luan Dieu Hanh

He thong auth hien tai da co nen tang tot hon ban thu nghiem ban dau: password duoc hash bang BCrypt, da co `AuthProtocol`/`AuthClient` rieng, OTP moi dung `SecureRandom`, OTP hash, TTL, cooldown, gioi han so lan verify, Google OAuth da co PKCE/state/nonce, Facebook OAuth da co session state va HMAC worker callback.

Tuy nhien, neu muc tieu la dua app vao moi truong production/co kinh doanh, ket luan hien tai la **NO-GO cho production auth** cho den khi xu ly xong cac loi P0/P1 ben duoi. Ba diem chan lon nhat:

1. Van con duong reset password legacy trong `ClientHandler` dung `Random`, static memory map, khong TTL/rate limit ro rang.
2. Dang nhap bang password chua co login throttling/rate limit/chong credential stuffing.
3. Sau khi login thanh cong, client chi nhan `DASHBOARD_GO|userId|role|avatar`, chua co server-side session/access token/refresh token/revocation dung nghia.

Day la cac loi kien truc, khong phai loi giao dien. Can sua truoc khi toi uu UI login tiep.

## 2. Tai Lieu Doi Chieu

Nguon chinh dung de doi chieu:

- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html
- OWASP Forgot Password Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html
- OWASP Session Management Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html
- OWASP Password Storage Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- NIST SP 800-63B, ban hien hanh SP 800-63-4: https://pages.nist.gov/800-63-4/sp800-63b.html
- RFC 8252 OAuth 2.0 for Native Apps: https://www.rfc-editor.org/rfc/rfc8252
- RFC 7636 PKCE: https://www.rfc-editor.org/rfc/rfc7636

Nguyen tac rut ra:

- App desktop/native khong duoc giu client secret quan trong.
- OAuth native nen dung Authorization Code + PKCE, state, nonce va loopback redirect tren localhost.
- Forgot password nen tra response dong nhat, tranh tiet lo email co ton tai hay khong.
- Login can co throttling/rate limit, audit, monitoring va co che chong brute-force/credential stuffing.
- Password phai duoc hash bang thuat toan cham, co salt, cost phu hop; khong tu viet crypto.
- Session phai co dinh danh du doan kho, expiry, revoke, rotation/re-auth cho thao tac nhay cam.

## 3. Cac File/Luong Da Kiem Tra

### Client

- `src/main/java/com/mycompany/tutorhub_enterprise/client/LoginFrame.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/SignUpFrame.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/AuthClient.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/NetworkManager.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/FacebookLoginDialog.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/PhoneOtpDialog.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/HeaderPanel.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/oauth/OAuthPKCE.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/oauth/OAuthLoginFlow.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/oauth/OAuthCallbackResult.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/oauth/LocalCallbackServer.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/oauth/FacebookLoginFlow.java`

### Server

- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/AuthService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/SocialAuthService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/SocialAuthConfig.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/oauth/FacebookOAuthCallbackServer.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/oauth/FacebookPendingSession.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/DatabaseManager.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/OtpService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/SmsService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/EmailService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ServerConfig.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/TutorServer.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/UserDAO.java`

### Models/Protocol

- `src/main/java/com/mycompany/tutorhub_enterprise/models/auth/AuthProtocol.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/auth/AuthRequest.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/auth/AuthResponse.java`

### Config/Docs

- `config/local-oauth.properties`
- `src/main/resources/application.properties`
- `src/main/resources/application.example.properties`
- `src/main/resources/client-public.properties`
- `docs/google-oauth-netbeans-run-guide.md`
- `docs/facebook-oauth-setup-guide.md`
- `docs/huggingface-facebook-oauth-deploy-guide.md`
- `docs/tse_login_captcha_password_about_phase.md`

## 4. Ban Do Luong Auth Hien Tai

### Password Login

1. `LoginFrame` nhan email/password tu UI.
2. `AuthClient.login()` gui `AUTH_LOGIN`.
3. `NetworkManager` gui packet qua WebSocket.
4. `ClientHandler.handleAuthRequest()` goi `AuthService.authenticateWithPassword()`.
5. `AuthService` goi `DatabaseManager.authenticateByEmail()`.
6. `DatabaseManager` tim user theo email va check `BCrypt.checkpw`.
7. Neu thanh cong, server tra `DASHBOARD_GO|userId|role|avatarBase64`.
8. Client parse chuoi va mo `MainDashboard`.

Diem tot: BCrypt, SQL parameterized, giao tiep qua WebSocket cloud.  
Diem yeu: khong co session token, khong co rate limit, payload string brittle, log co PII.

### Sign Up

1. `SignUpFrame` validate email co `@` va `.`.
2. Gui `AUTH_REQUEST_REGISTRATION_OTP`.
3. `AuthService.requestRegistrationOtp()` tao OTP qua `OtpService`.
4. `EmailService` gui OTP hoac log OTP neu dev mode.
5. Gui `AUTH_REGISTER` de verify OTP va tao user.
6. `DatabaseManager.registerUser()` hash password bang BCrypt cost 12.

Diem tot: OTP moi co hash/TTL/cooldown/max attempts.  
Diem yeu: password policy gan nhu chua co, response co the tiet lo email ton tai, OTP memory-only.

### Forgot Password

Hien co hai duong:

- Duong moi: `AUTH_REQUEST_PASSWORD_RESET_OTP` va `AUTH_RESET_PASSWORD`, di qua `AuthService` + `OtpService`.
- Duong legacy: `REQUEST_OTP_RESET` va `VERIFY_AND_RESET` trong `ClientHandler`, dung static map va `java.util.Random`.

Duong moi tot hon nhieu, nhung duong legacy la rui ro P0 vi con ton tai trong server dispatcher.

### Google OAuth

1. Client tao `code_verifier`, `state`, `nonce`.
2. Mo browser he thong.
3. `LocalCallbackServer` lang nghe loopback `127.0.0.1`.
4. Client gui `code`, `codeVerifier`, `redirectUri`, `nonce` ve server.
5. Server doi token, verify ID token, kiem issuer/audience/nonce/email_verified.
6. Server tao/link user va tra dashboard payload.

Diem tot: dung PKCE/state/nonce; client khong giu Google client secret trong public config.  
Diem can chot: mo hinh server/confidential client phai ro rang; khong duoc dua client secret vao desktop package.

### Facebook OAuth

1. Client yeu cau server `AUTH_FACEBOOK_START`.
2. Server tao pending session/state, tra URL.
3. Client mo browser va poll session.
4. Worker callback ve server bang HMAC signature.
5. Server match state, tao/link user, tra dashboard payload.

Diem tot: co state, pending expiry, HMAC worker callback.  
Diem yeu: `isFacebookConfigured()` chua yeu cau worker shared secret, compare HMAC chua constant-time, scope `public_profile` co the tao placeholder email, auto-link bang email can policy chat hon.

### SMS Login / Phone OTP

- `AuthClient` co `requestSmsLoginOtp`, `verifySmsLogin`, `requestPhoneVerificationOtp`, `verifyPhoneOtp`.
- `AuthProtocol` co action phone verification.
- `ClientHandler.handleAuthRequest()` co SMS login, nhung chua thay switch case cho phone verification action.

Ket luan: SMS login co khung logic, phone verification chua noi day du.

### Remember Me / Auto Login

- `Remember Me` hien chi luu email `saved_email`.
- `Keep me signed in` dang disable, tooltip noi cho secure OS storage.

Day la quyet dinh dung. Khong nen luu password trong client. Neu lam keep-signed-in, phai dung refresh token rotate + OS secure storage.

## 5. Uu Diem Hien Tai

| Nhom | Uu diem | Y nghia |
|---|---|---|
| Password storage | Dung BCrypt cost 12 khi register/reset password | Tot hon luu plain text/SHA thuan; phu hop huong OWASP Password Storage |
| SQL | Nhieu truy van dung `PreparedStatement` | Giam rui ro SQL injection |
| OTP moi | `OtpService` dung `SecureRandom`, hash OTP, TTL 10 phut, cooldown 60s, max 5 lan verify | Nen tang OTP dung huong production |
| OTP test | Co `OtpServiceTest` cho single-use/rate-limit/invalid attempts | Co diem bam de them regression test |
| OAuth Google | Co PKCE S256, state, nonce, loopback localhost | Phu hop tu duy RFC 8252/RFC 7636 cho native app |
| OAuth Facebook | Co pending session/state va HMAC worker callback | Tot hon redirect truc tiep ve client |
| Config | `ServerConfig` uu tien env/system property/local properties | Co the dua secret len Hugging Face Secrets |
| Client UX | Remember Me chi luu email, Keep signed-in disable | Tranh luu password sai cach |
| Protocol moi | Co `AuthRequest`/`AuthResponse` requestId | Tot hon packet string tu do |

## 6. Nhuoc Diem Va Loi Can Sua

| ID | Muc | Khu vuc | Van de | Bang chung code | Tac dong | De xuat sua |
|---|---:|---|---|---|---|---|
| AUTH-01 | P0 | Forgot password | Con duong reset legacy dung `java.util.Random`, static `otpStorage`, khong TTL/rate limit ro rang | `ClientHandler` xu ly `REQUEST_OTP_RESET`, `VERIFY_AND_RESET` | Co the bi brute-force/reset trai phep | Xoa/disable legacy actions, route to `AuthService` + `OtpService`, them test dam bao legacy bi reject |
| AUTH-02 | P0 | Password login | Chua co login throttling/rate limit/account lock/cooldown | `AuthService.authenticateWithPassword`, `DatabaseManager.authenticateByEmail` | Credential stuffing va brute-force | Them `AuthRateLimitService`, bang `login_attempts`, limit theo email + IP + device, backoff theo rui ro |
| AUTH-03 | P0 | Session | Login thanh cong chi tra `DASHBOARD_GO|id|role|avatar`, khong co access token/session token/refresh token/revoke | `AuthResponse.dashboardPayload`, `LoginFrame`, `ClientHandler` | Client state de gia mao, reconnect/logout khong quan ly session that | Thiet ke server-side session, access token ngan han, refresh token rotate luu OS secure storage, logout revoke server-side |
| AUTH-04 | P1 | Forgot password / Sign up | Response tiet lo email co ton tai hay khong | `AuthService.requestPasswordResetOtp`, registration duplicate message | User enumeration | Doi response public thanh dong nhat; van log/audit noi bo |
| AUTH-05 | P1 | Password policy | Chua co policy do dai, banned password, password giong email, strength meter | `SignUpFrame`, `AuthService.verifyAndRegister`, `verifyAndResetPassword` | Mat khau yeu, support cost cao ve sau | Them `PasswordPolicyService` theo NIST/OWASP; UI hien strength va loi ro rang |
| AUTH-06 | P1 | Protocol | Legacy auth packet va auth protocol moi dang song song | `ClientHandler`, `LoginFrame.xuLyDangNhapOAuth`, legacy OTP blocks | De sua nham, bug security khong bi test | Lap migration map, disable endpoint cu, viet compatibility test |
| AUTH-07 | P1 | Logging | Log co email day du, social state, OTP neu dev mode | `DatabaseManager`, `SocialAuthService`, `EmailService`, `SmsService` | Lo PII/OTP tren log/server console | Mask email, khong log state/token/OTP o prod, add `app.env=prod` guard |
| AUTH-08 | P1 | OAuth Google | Mo hinh secret can chot ro: native client khong duoc co client secret trong package | `SocialAuthService`, `SocialAuthConfig`, `config/local-oauth.properties` | Lo client secret neu dong goi sai | Server-side BFF giu secret tren backend; client chi giu public client ID hoac dung server-start flow |
| AUTH-09 | P1 | Facebook OAuth | `isFacebookConfigured()` khong check worker secret; HMAC compare chua constant-time; placeholder email/auto-link can policy | `SocialAuthConfig`, `FacebookOAuthCallbackServer`, `SocialAuthService` | Callback worker yeu hoac link nham account | Require worker secret, constant-time compare, one-time state, link account chi sau re-auth/xac minh email |
| AUTH-10 | P1 | Secret hygiene | `application.properties` va `local-oauth.properties` co key nhay cam can dam bao khong vao repo/package | `src/main/resources/application.properties`, `config/local-oauth.properties` | Lo DB/OAuth/B2 secret | Chuyen secret sang env/HF secrets, giu `application.example.properties`, scan CI truoc build |
| AUTH-11 | P2 | Database migration | `DatabaseManager` auto alter/create table trong static initializer | `DatabaseManager` | Kho rollback, rui ro prod schema drift | Dung Flyway/Liquibase hoac migration runner rieng |
| AUTH-12 | P2 | Payload | `DASHBOARD_GO|...` la chuoi pipe-delimited brittle | `AuthResponse.dashboardPayload` | Loi parse, kho mo rong, kho sign payload | Dung DTO JSON co schema/version/session metadata |
| AUTH-13 | P2 | UX | Main login con thieu password visibility, password policy feedback, i18n, help text | `LoginFrame`, `SignUpFrame` | UX thua app lon | Lam sau khi P0/P1 xong |
| AUTH-14 | P2 | Network dispatcher | `NetworkManager` dung global packet queue/deferred packet | `NetworkManager.receivePacket` | De treo timeout/nhan nham trong multi-flow | Request-id dispatcher map/future per request |
| AUTH-15 | P2 | Audit | Login audit IP dang `"0.0.0.0"`, thieu device/app version/session id | `DatabaseManager.insertLoginAuditLog` | Kho dieu tra su co | Thu thap connection metadata va luu audit chuan |
| AUTH-16 | P2 | Social account | Social users co `password_hash NULL`, can policy add password/unlink | `DatabaseManager.createSocialUser` | Kho quan ly account lifecycle | UI link/unlink provider, add password flow co re-auth |
| AUTH-17 | P2 | Dead code | `FacebookLoginDialog` mock con ton tai | `FacebookLoginDialog.java` | De bi dung lai nham | Xoa hoac danh dau debug-only, block trong prod build |

## 7. So Sanh Hien Tai Voi Cach Lam Cua Cac Nen Tang Lon

| Nang luc | Hien tai TutorHub | Cach app lon thuong lam | Gap can lap |
|---|---|---|---|
| Password login | Email/password + BCrypt | Rate limit, risk scoring, device/session audit, generic error | Them rate limit + audit + session |
| Forgot password | Duong moi OTP tot, nhung legacy con nguy hiem | Token/OTP one-time, expiry, generic response, alert user | Xoa legacy, dong nhat response, audit |
| Session | Mo dashboard bang userId/role payload | Session token/server session, expiry, refresh, revoke, device management | Thiet ke `auth_sessions` va token lifecycle |
| Remember me | Chi luu email | Refresh token rotate trong OS keychain/credential vault | Sau khi co session token moi lam |
| OAuth Google | PKCE/state/nonce kha tot | Native PKCE hoac BFF, khong secret trong client | Chot mo hinh secret deployment |
| OAuth Facebook | Server-start/poll + HMAC worker | State one-time, signed callback, strict linking, audit | Constant-time HMAC, worker secret required |
| SMS | Co khung OTP | Rate limit, SIM-swap/risk signal, fallback, delivery audit | Hoan thien rate limit va phone verification |
| Logs | Con PII | Structured logs, masked PII, correlation ID | Log policy va sanitizer |
| Database | Auto migration trong code | Versioned migrations, rollback, environment separation | Dua schema ra migration tool |
| UX login | Co social buttons, reset/signup | Password visibility, strength meter, clear states, security notices | Lam sau backend hardening |

## 8. Roadmap De Dua Auth Len Chuan Production

### Phase 0 - Dong Bang Va Bao Ve Hien Trang

Muc tieu: khong cho loi auth cu tiep tuc song song.

Viec can lam:

- Tao test danh sach endpoint auth hop le.
- Mark legacy actions `REQUEST_OTP_RESET`, `VERIFY_AND_RESET`, `LOGIN_OAUTH` la deprecated.
- Them guard prod: neu `app.env=prod`, reject legacy reset path.
- Tao checklist secret scan cho `application.properties`, `local-oauth.properties`, build portable va Hugging Face package.

Ket qua mong muon:

- Khong con reset password qua duong legacy trong production.
- Khong co secret that bi dong goi vao client.

### Phase 1 - Password/Forgot Password Hardening

Muc tieu: dat nen tang password/OTP dung OWASP/NIST.

Viec can lam:

- Tao `PasswordPolicyService`.
- Policy toi thieu:
  - Do dai toi thieu 8, khuyen nghi 12+.
  - Cho phep password dai toi thieu 64 ky tu.
  - Chan password qua pho bien/banned list.
  - Chan password trung email/username/phone.
  - Khong bat quy tac ky tu qua may moc neu da co strength check.
- Ap dung policy cho signup, reset password, change password.
- Forgot password tra message public dong nhat: "Neu tai khoan ton tai, ma xac thuc se duoc gui".
- Gui email thong bao sau reset thanh cong.
- Test OTP:
  - OTP het han.
  - OTP single-use.
  - Sai OTP qua gioi han.
  - Resend cooldown.

### Phase 2 - Login Rate Limit Va Audit

Muc tieu: chong brute-force/credential stuffing.

Viec can lam:

- Tao `AuthRateLimitService`.
- Them bang de xuat:

```sql
auth_login_attempts(
  id bigserial primary key,
  normalized_identifier text not null,
  ip_hash text,
  device_hash text,
  success boolean not null,
  reason text,
  created_at timestamptz not null default now()
);
```

- Rate limit theo:
  - Email/identifier.
  - IP hoac connection fingerprint.
  - Device/app instance.
  - Global burst.
- Them lock/cooldown mem + DB.
- Audit masked:
  - khong log password, OTP, token, state.
  - mask email: `ba***@gmail.com`.
  - log correlation/request id.

### Phase 3 - Session Architecture

Muc tieu: thay `DASHBOARD_GO|...` bang session that.

De xuat bang:

```sql
auth_sessions(
  id uuid primary key,
  user_id int not null,
  refresh_token_hash text not null,
  device_id text,
  device_name text,
  app_version text,
  ip_hash text,
  created_at timestamptz not null,
  last_seen_at timestamptz,
  expires_at timestamptz not null,
  revoked_at timestamptz
);
```

Luong de xuat:

1. Login thanh cong -> server tao session.
2. Server tra:
   - `accessToken` ngan han hoac signed session ticket.
   - `refreshToken` mot lan/rotate.
   - user profile DTO.
3. Client luu refresh token bang OS secure storage, khong luu password.
4. Moi request sau do kem access token/session id.
5. Logout -> revoke refresh token tren server.
6. Reconnect -> refresh token rotate.

Neu chua muon JWT, co the dung opaque random token hash trong DB. Day la cach de revoke de hon va phu hop monolith hien tai.

### Phase 4 - OAuth/Social Login Chuan Hoa

Muc tieu: mot luong social login thong nhat, khong leak secret.

Viec can lam:

- Google:
  - Giu PKCE/state/nonce.
  - Chot mo hinh: server-side BFF giu secret hoac native public client khong secret.
  - Khong dong goi client secret vao desktop app.
- Facebook:
  - `isFacebookConfigured()` phai require `workerSharedSecret`.
  - HMAC compare constant-time.
  - State one-time va expire ro.
  - Khong auto-link theo email neu chua co policy re-auth/xac minh.
  - Neu email Facebook khong co, tao account social-only voi identifier rieng, khong tao email placeholder de nguoi dung thay.
- Social account management:
  - Link/unlink provider.
  - Add password for social account.
  - Re-auth before unlink/reset.

### Phase 5 - UI/UX Login Ngang Tam App Lon

Chi lam sau khi P0/P1/P2 backend da xong.

Viec can lam:

- Password visibility toggle tren Login/Sign Up/Reset.
- Password strength indicator trong Sign Up/Reset.
- Loi dang nhap public dong nhat nhung co huong dan: "Sai thong tin hoac tai khoan chua san sang".
- Trang thai network ro rang: dang ket noi, mat ket noi, retry.
- Social login state ro: dang mo trinh duyet, dang cho xac thuc, het han.
- Phone/SMS flow ro: nhap phone, gui OTP, cooldown, resend, verify.
- Link dieu khoan/bao mat.
- I18n Viet/Anh dong nhat.

## 9. Kien Truc De Xuat Cho Auth Module

### Client

```text
client/auth/
  AuthClient.java
  AuthSessionStore.java
  AuthViewModel.java
  OAuthBrowserFlow.java
  SecureCredentialStore.java
```

Nguyen tac:

- UI khong goi truc tiep `NetworkManager`; di qua `AuthClient`.
- `AuthClient` tra DTO typed, khong tra chuoi pipe-delimited.
- `SecureCredentialStore` dung Windows Credential Manager/DPAPI neu lam "Keep signed in".
- Khong luu password, khong luu OAuth secret trong client.

### Server

```text
server/auth/
  AuthService.java
  PasswordPolicyService.java
  AuthRateLimitService.java
  SessionService.java
  OtpService.java
  SocialAuthService.java
  AuthAuditService.java
```

Nguyen tac:

- `ClientHandler` chi la transport dispatcher mong.
- Business logic nam trong service.
- Database access qua DAO/repository.
- Moi auth flow ghi audit event.

### Database De Xuat

Bang can co:

- `users`
- `auth_identities`
- `auth_sessions`
- `auth_login_attempts`
- `auth_otp_challenges` neu can multi-instance server
- `auth_password_reset_events`
- `auth_audit_logs`

`OtpService` memory-only chap nhan cho single server/dev, nhung production multi-instance nen dua challenge hash vao DB/Redis voi TTL.

## 10. Checklist Truoc Khi Dua Len Ban Kinh Doanh

### Bat buoc truoc production

- [ ] Legacy reset password endpoint bi xoa hoac reject trong production.
- [ ] Password login co rate limit theo identifier + IP/device.
- [ ] Forgot password khong tiet lo email ton tai.
- [ ] Password policy dung o signup/reset/change password.
- [ ] Session token/server session da thay `DASHBOARD_GO|...` cho cac request nhay cam.
- [ ] Logout revoke session tren server.
- [ ] Secret khong nam trong repo, jar, portable zip, Hugging Face files public.
- [ ] OAuth client secret chi nam o backend/secret manager.
- [ ] Logs khong in OTP, token, state, secret, email day du.
- [ ] Database migration co version, khong auto alter bang trong static initializer o production.
- [ ] Auth regression tests pass.

### Nen lam som sau production hardening

- [ ] Password visibility toggle.
- [ ] Password strength meter.
- [ ] Device/session management UI.
- [ ] Social link/unlink UI.
- [ ] SMS OTP resend cooldown UI.
- [ ] Full audit dashboard cho admin.

## 11. Thu Tu Trien Khai Nen Lam Tiep Theo

1. **Auth hardening gate**: disable/remove legacy reset password path.
2. **Rate limit login**: them service + bang DB + test.
3. **Forgot password uniform response**: sua message + test user enumeration.
4. **Password policy**: server first, client UI sau.
5. **Session service**: thiet ke DTO/token, thay payload pipe-delimited.
6. **OAuth cleanup**: Google/Facebook secret policy, HMAC constant-time, social linking policy.
7. **Log sanitizer**: masked PII va guard dev mode.
8. **UI polish**: password visibility, strength, network/social states.

## 12. Goi Y Prompt Cho AI Agent Khi Trien Khai

### Prompt buoc 1

```text
Hay lam Auth Phase 1A: vo hieu hoa duong reset password legacy trong ClientHandler.
Chi sua ClientHandler va test lien quan.
Khong doi UI.
Acceptance:
- AUTH_REQUEST_PASSWORD_RESET_OTP/AUTH_RESET_PASSWORD van hoat dong.
- REQUEST_OTP_RESET/VERIFY_AND_RESET bi reject trong prod hoac xoa dispatcher.
- Them test/ghi log ro legacy path khong con duoc dung.
```

### Prompt buoc 2

```text
Hay lam Auth Phase 1B: them AuthRateLimitService cho password login.
Can thiet ke truoc:
- key theo normalized email + remote address/device hash
- threshold/cooldown
- audit event
Khong lam UI.
Khong hardcode secret.
```

### Prompt buoc 3

```text
Hay lam Auth Phase 1C: password policy server-side cho signup/reset password.
Dung PasswordPolicyService rieng.
Khong yeu cau ky tu phuc tap may moc; uu tien length, banned list, khong trung email.
Cap nhat UI message sau khi backend pass.
```

### Prompt buoc 4

```text
Hay lam Auth Phase 2A: thiet ke SessionService opaque token.
Khong dung JWT neu chua can.
Access token ngan han, refresh token hash trong DB, logout revoke.
Khong luu password trong client.
De xuat cach luu refresh token bang Windows Credential Manager/DPAPI.
```

## 13. Go / No-Go

Trang thai hien tai: **NO-GO cho production auth**.

Ly do:

- Con legacy reset password P0.
- Chua co rate limit login P0.
- Chua co session/token lifecycle P0.
- Con user enumeration va secret hygiene can chot P1.

Co the tiep tuc dev/demo noi bo neu:

- Dung tai khoan test.
- Khong mo public registration/reset password cho nguoi dung that.
- Khong dong goi secret vao client.
- Ghi ro `app.env=dev` va khong dung du lieu that.

## 14. Ket Luan

TutorHub da co huong di dung o nhieu diem quan trong: BCrypt, OTP service moi, OAuth PKCE, server-side social flow va config qua env. De dat muc "ung dung co the ban/van hanh nghiem tuc", can tam dung polish UI login va sua cac lop nen tang: legacy reset, rate limiting, session token, response uniform, secret hygiene, log masking.

Khi ba loi P0 duoc xu ly, he thong auth moi nen duoc xem la du nen de tiep tuc xay social login, SMS login va giao dien dang nhap theo chuan cac app lon.
