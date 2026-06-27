# Locket Popup Web Full Rework Report

Ngày thực hiện: 2026-06-25

## 1. Đã đọc những file nào

- `src/main/java/com/mycompany/tutorhub_enterprise/client/HomeTab.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/home/HomeSocialWebPanel.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/home/LocketWebPopupDialog.java`
- `src/main/resources/home-social/home-social.html`
- `src/main/resources/home-social/home-social.css`
- `src/main/resources/home-social/home-social.js`
- `src/main/resources/locket-web/locket-popup.html`
- `src/main/resources/locket-web/locket-popup.css`
- `src/main/resources/locket-web/locket-popup.js`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/B2Helper.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/services/LocketService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/LocketPostDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/LocketReactionDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/LocketCommentDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/auth/AuthProtocol.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/locket/LocketPostModel.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/models/locket/LocketPostViewDTO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/NativeReelPlayer.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/VideoReelSection.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/ReelsTabPanel.java`
- `pom.xml`

## 2. Popup Locket cũ nằm ở đâu

Popup Locket cũ nằm trong `src/main/java/com/mycompany/tutorhub_enterprise/client/home/LocketWebPopupDialog.java` và bộ WebView asset `src/main/resources/locket-web/locket-popup.html`, `locket-popup.css`, `locket-popup.js`.

Ngoài ra còn legacy Swing upload dialog `src/main/java/com/mycompany/tutorhub_enterprise/client/UploadLocketDialog.java` được gọi từ `NativeReelPlayer`. Đường này hiện không còn nằm trên luồng HomeTab mới vì `HomeTab` đã dùng `HomeSocialWebPanel` và `LocketWebPopupDialog` mới. `src/main/java/com/mycompany/tutorhub_enterprise/client/home/LocketPostUploadDialog.java` cũng là legacy/upload dialog, không được gọi bởi HomeTab mới.

## 3. Logic camera cũ nằm ở đâu

Logic camera liên quan popup nằm trong `LocketWebPopupDialog.java`. Một số luồng xem media/camera cũ khác có thể tham khảo ở `NativeReelPlayer.java` và `VideoReelSection.java`, nhưng rework này chỉ dùng `LocketWebPopupDialog.java` cho popup Web mới.

## 4. Đã sửa/tạo file nào

- Sửa `src/main/resources/locket-web/locket-popup.html`
- Sửa `src/main/resources/locket-web/locket-popup.css`
- Sửa `src/main/resources/locket-web/locket-popup.js`
- Sửa `src/main/java/com/mycompany/tutorhub_enterprise/client/home/LocketWebPopupDialog.java`
- Sửa `src/main/java/com/mycompany/tutorhub_enterprise/client/HomeTab.java`
- Sửa `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`
- Sửa `src/main/java/com/mycompany/tutorhub_enterprise/models/locket/LocketPostViewDTO.java`
- Sửa `src/main/java/com/mycompany/tutorhub_enterprise/models/locket/LocketPostModel.java`
- Sửa `src/main/resources/home-social/home-social.html`
- Sửa `src/main/resources/home-social/home-social.js`
- Tạo `docs/locket_popup_web_full_rework_report.md`

## 5. Đã bỏ phụ thuộc classId/lớp học chưa

Đã bỏ phụ thuộc lớp học trong runtime UI và payload Locket:

- `HomeTab` không còn chặn đăng Locket bằng `currentClassId`.
- `LocketWebPopupDialog` gửi `LOCKET_POST_CREATE` không có `classId`.
- `LOCKET_POST_LIST`, `LOCKET_POST_CREATE`, `LOCKET_POST_REACT`, `LOCKET_COMMENT_*` trong `ClientHandler` không đọc `classId`.
- `LocketService` không kiểm tra `classroom_members` và không gọi `isUserMemberOfClass`.

Ghi chú: DB vẫn còn cột `locket_posts.class_id` để tương thích schema cũ, nhưng tạo post mới dùng `NULL`. Field `classId` trong `LocketPostModel` và `LocketPostViewDTO` đã để `transient` để không xuất hiện trong Gson payload.

## 6. Backend global feed còn ổn không

Backend hiện là global feed:

- `LocketService.listPosts(...)` gọi `LocketPostDAO.listGlobal(...)`.
- `LocketService.createPost(...)` gọi `LocketPostDAO.createPostGlobal(...)`.
- `createPostGlobal(...)` insert `class_id = NULL`.
- Reaction/comment chỉ dựa trên `postId` và user hiện tại.

## 7. Popup Web mới nằm ở đâu

Popup Web mới nằm ở:

- `src/main/resources/locket-web/locket-popup.html`
- `src/main/resources/locket-web/locket-popup.css`
- `src/main/resources/locket-web/locket-popup.js`
- Java host/bridge: `src/main/java/com/mycompany/tutorhub_enterprise/client/home/LocketWebPopupDialog.java`

## 8. Layout popup đã bám mockup như thế nào

Popup mới dùng WebView layout 3 vùng:

- Cột trái: tạo bài mới và danh sách gần đây.
- Khu giữa: viewer ảnh lớn, avatar/tên/thời gian/caption, nút prev/next, reaction strip và nút nhắn tin.
- Cột phải: các nút dọc `Phát`, `Ảnh`, `OK`, `Tùy chọn`, `Thoát`.

Tone giao diện là sáng/pastel, card bo góc lớn, shadow nhẹ, không dùng table.

## 9. Nút Phát hoạt động thế nào

Nút `Phát` bật/tắt slideshow trong `locket-popup.js`. Khi bật, timer tự chuyển ảnh mỗi 5 giây và quay vòng về ảnh đầu nếu đang ở ảnh cuối. Nếu người dùng bấm prev/next thủ công khi slideshow đang chạy, state vẫn được cập nhật bình thường.

## 10. Nút Ảnh hoạt động thế nào

Nút `Ảnh` gửi event `LOCKET_PICK_IMAGE` sang Java. Java mở `JFileChooser`, validate:

- Định dạng `jpg`, `jpeg`, `png`, `webp`.
- Dung lượng không quá 8MB.
- File phải đọc được bằng `ImageIO`.

Ảnh hợp lệ được chuyển thành preview base64 để hiển thị trong popup. File path local không được đưa vào HTML/JS.

## 11. Nút OK/camera hoạt động thế nào

Ở chế độ xem ảnh, `OK` gửi `LOCKET_CAMERA_START` để mở camera. Khi đang ở camera mode, `OK` gửi `LOCKET_CAMERA_CAPTURE` để chụp frame hiện tại, đóng webcam, convert frame sang ảnh JPG và đưa về preview. Sau đó người dùng submit ảnh qua `LOCKET_POST_SUBMIT`.

## 12. Chế độ camera có ẩn nhắn tin/cảm xúc không

Có. Khi vào camera mode, JS thêm class `camera-mode`; CSS ẩn phần viewer meta, reaction strip, caption/message và vùng nhập caption của post upload chỉ hiện khi đã có ảnh preview.

## 13. Nút Nhắn tin chuyển tab thế nào

Nút `Nhắn tin` gửi event `LOCKET_MESSAGE_OPEN`. `LocketWebPopupDialog` gọi callback về `HomeTab`, sau đó `MainDashboard` chuyển sang card `Chat`. Nếu chưa có người nhận cụ thể, chỉ mở tab Tin nhắn chung.

## 14. Nút Thoát cleanup thế nào

Nút `Thoát` gửi `LOCKET_CLOSE`. Java gọi `dispose()`, dừng slideshow phía JS theo lifecycle, cleanup webcam thread, đóng webcam nếu đang mở và reset active instance.

## 15. Slideshow 5 giây hoạt động chưa

Đã triển khai ở `locket-popup.js` bằng `setInterval(nextPhoto, 5000)`. Đã kiểm tra cú pháp JS bằng `node --check`. Chưa test thủ công GUI trong runtime thật ở bước này.

## 16. Feed ảnh lướt ngang hoạt động chưa

HomeSocial feed vẫn dùng `HomeSocialWebPanel` và `home-social.js` để hiển thị danh sách ảnh lướt ngang. Bấm dấu `+` mở popup, bấm ảnh mở popup tại ảnh tương ứng. Đã giữ cơ chế refresh feed sau `LOCKET_POST_CREATE_SUCCESS`.

## 17. Có còn thông báo chọn lớp không

Không còn trong runtime Locket đã rà bằng `rg` trên các file Locket/HomeSocial liên quan. Không còn thông báo kiểu `Vui lòng chọn lớp trước khi đăng ảnh` hay `Bạn chưa có lớp để đăng ảnh` trong scope này.

## 18. Có classId trong payload không

Không có `classId` trong payload tạo/list post runtime:

- `LocketWebPopupDialog` log `LOCKET_POST_CREATE sent without classId`.
- `ClientHandler` không parse `classId` cho `LOCKET_POST_LIST` hoặc `LOCKET_POST_CREATE`.

Chỉ còn `class_id` trong DAO để đọc/insert `NULL` vào DB legacy schema và field Java `classId` đã là `transient`.

## 19. Có hardcode secret không

Không thêm hardcoded secret. Upload dùng `B2Helper`, đọc cấu hình từ system properties/environment/file config hiện có. HTML/JS không nhận secret.

## 20. Có DROP/TRUNCATE/DELETE nguy hiểm không

Đã chạy kiểm tra chính xác:

```powershell
rg -n -i "\bDROP\s+TABLE\b|\bTRUNCATE\s+(TABLE\s+)?(locket_posts|locket_comments)\b|\bDELETE\s+FROM\s+(locket_posts|locket_comments)\b" src/main/java/com/mycompany/tutorhub_enterprise src/main/resources/locket-web src/main/resources/home-social
```

Kết quả: không tìm thấy SQL nguy hiểm với `locket_posts` hoặc `locket_comments`. `DELETE FROM locket_reactions` nếu có cho un-heart vẫn là hành vi được phép khi có điều kiện phù hợp.

## 21. Build/test result

Đã chạy:

```powershell
node --check ".\src\main\resources\home-social\home-social.js"
node --check ".\src\main\resources\locket-web\locket-popup.js"
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean compile assembly:single -DskipTests
```

Kết quả:

- `node --check` pass cho cả hai file JS.
- Maven `BUILD SUCCESS`.
- Có warning JavaFX effective model và một số warning cũ ở `ScheduleTab`, không chặn build.

## 22. update.jar đã copy chưa

Đã copy:

```text
D:\Ban_sao_du_an\HF_UPLOAD\update.jar
```

Dung lượng sau build: `222159230` bytes. Jar đã chứa:

- `locket-web/locket-popup.html`
- `locket-web/locket-popup.css`
- `locket-web/locket-popup.js`
- `home-social/home-social.html`
- `home-social/home-social.css`
- `home-social/home-social.js`
- `LocketWebPopupDialog.class`

## 23. Rủi ro còn lại

- Chưa chạy runtime GUI thủ công để xác nhận camera/webcam, upload B2 và chuyển tab Chat trên máy thật.
- `webp` chỉ pass nếu runtime `ImageIO` hỗ trợ đọc WebP; nếu không, file WebP sẽ bị reject đúng theo rule validate hiện tại.
- `B2Helper` phải được cấu hình đúng ở môi trường chạy; nếu thiếu cấu hình B2, popup sẽ báo lỗi thân thiện thay vì crash.
- DB vẫn còn cột `class_id` legacy. Hiện runtime không phụ thuộc cột này, nhưng migration schema dài hạn nên cân nhắc nullable/loại bỏ khi chắc chắn không còn client cũ.
- Một số class legacy như `UploadLocketDialog`, `NativeReelPlayer`, `VideoReelSection`, `LocketPostUploadDialog` vẫn còn trong source để tránh xóa nhầm. Chúng không nằm trên luồng HomeTab Locket mới nhưng nên được dọn ở một phase riêng nếu muốn giảm nợ kỹ thuật.
