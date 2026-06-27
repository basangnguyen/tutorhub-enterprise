# Báo cáo Audit luồng tải ảnh Locket & Backblaze B2

## 1. Các file đã audit
- `B2Helper.java`
- `LocketWebPopupDialog.java`
- `ClientHandler.java`
- `LocketService.java`
- `LocketPostDAO.java`
- `LocketPostViewDTO.java`
- `HomeLocketItem.java`
- `MainDashboard.java`
- `HomeTab.java`
- `home-social.js`, `locket-popup.js`
- `locket-popup.css`

## 2. Luồng upload/list/render end-to-end
1. Người dùng chọn ảnh, `LocketWebPopupDialog` gọi `B2Helper.uploadBase64Image`.
2. `B2Helper` tải file lên Backblaze B2 S3 và trả về `publicUrl` (định dạng `https://<bucket>.s3.<region>.backblazeb2.com/<filename>`).
3. Payload tạo bài viết (`imageUrl`, `thumbnailUrl`, `caption`) được Client gửi qua lệnh `LOCKET_POST_CREATE` tới `ClientHandler`.
4. `ClientHandler` gọi `LocketService`, truyền dữ liệu xuống `LocketPostDAO`.
5. `LocketPostDAO` ghi chính xác các URL này vào bảng `locket_posts`.
6. Khi lấy danh sách, `LocketPostDAO` ánh xạ dữ liệu DB sang `LocketPostViewDTO`. Gson trên server chuyển đổi danh sách này thành JSON (với định dạng camelCase tự động `imageUrl`, `thumbnailUrl`).
7. `MainDashboard` nhận JSON, dùng Gson ánh xạ ngược lại thành danh sách `HomeLocketItem` (các trường mapping hoàn toàn khớp).
8. Dữ liệu truyền xuống `HomeTab` -> `HomeSocialWebPanel` -> WebEngine push tới JS.
9. JS nhận `imageUrl` và áp dụng hiển thị qua `<img>` (trong Popup) và `background-image` (trong Feed).

## 3. Cấu hình B2Helper
- Biến môi trường/config được cấp qua các khoá: `TUTORHUB_B2_BUCKET`, `TUTORHUB_B2_ENDPOINT`, `TUTORHUB_B2_REGION`, `TUTORHUB_B2_ACCESS_KEY`, `TUTORHUB_B2_SECRET_KEY`.
- Nếu thông số thiếu, việc upload sẽ báo lỗi ném ra `IllegalStateException` và in log đỏ. Việc Client có thể gửi `LOCKET_POST_CREATE` thành công chứng tỏ **upload đã thành công** và trả về URL, config trên máy đang chạy không bị thiếu.

## 4. Nguyên nhân khiến ảnh không hiển thị
**Nguyên nhân:** Lỗi 403 Forbidden do truy cập Public URL vào Private Bucket.
Cụ thể, Backblaze B2 Bucket `tutorhub-videos-123` của hệ thống hiện tại là một bucket bảo mật (Private).
Trong quá trình up file (`uploadBase64Image`), URL trả về là một URL thô không kèm mã xác thực (Signature) AWS.
Vì vậy, mặc dù URL đã được truyền đi hoàn hảo từ đầu cuối: Client -> Server -> DB -> Client -> JS, nhưng khi thẻ `<img>` của JavaFX WebView gửi GET request để tải ảnh, server B2 sẽ từ chối truy cập (403). Khi ảnh không load được, giao diện chỉ hiển thị `fallback-gradient` màu xanh/tím.

*Bằng chứng:* Các Module xem Reel (`NativeReelPlayer`, `VideoReelSection`) của hệ thống hiện tại đều đang phải gọi `B2Helper.getPresignedUrl(url)` để sinh Temporary Signed URL 7 ngày trước khi đưa cho Player.

## 5. Kết quả Fix
Đã triển khai bản vá lỗi (Không còn là bản nháp):

**Sửa file `HomeTab.java`:** Can thiệp tại phương thức `handleLocketPostListSuccess`. Vòng lặp duyệt danh sách `HomeLocketItem` nhận từ server sẽ tiến hành bọc `imageUrl`, `thumbnailUrl` qua hàm `B2Helper.getPresignedUrl()`. 
Đã triển khai hàm `presignIfB2Url(url)` để đảm bảo:
- Không presign data:image/base64.
- Không presign local resource / file path.
- Không presign URL đã có X-Amz-Signature.
- Tránh double-presign hoặc làm rách URL.
- Bắt lỗi Try-Catch và fallback về URL thô (không làm crash app).

**Sửa Javascript Render (`home-social.js`, `locket-popup.js`):**
- Popup: Thêm `onerror` vào thẻ `viewerImage`. Nếu lỗi, chuyển sang trạng thái "Không tải được ảnh từ storage" và bắn Event log `[LOCKET_JS][IMAGE_ERROR]` qua Bridge.
- Feed: JS tự động preload Image trong nền, nếu gặp lỗi sẽ inject div thông báo lỗi nổi bật màu đỏ "Không tải được ảnh từ storage" đè lên khung ảnh.

**Nhờ vậy:**
- Giao diện feed và popup đều nhận URL đúng và hiển thị ảnh bình thường.
- Dữ liệu thô trong DB không bị tác động.
- DTO/Backend không bị sửa.
- Không hardcode secret (sử dụng 100% tài nguyên có sẵn).
- Nếu URL lỗi, UI thông báo rõ ràng thay vì im lặng.
