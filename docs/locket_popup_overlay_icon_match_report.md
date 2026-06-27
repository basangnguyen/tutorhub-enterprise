# Locket Popup Overlay & Icon Match Report

## 1. Đã đọc lại file/report nào
- Đã đọc lại LocketWebPopupDialog.java, locket-popup.html, locket-popup.css, locket-popup.js.
- Đã tham khảo UploadLocketDialog.java và NativeReelPlayer.java để xem logic hiển thị overlay cũ và cách map icon local cho các nút ở cột phải.

## 2. Đã sửa file nào
- LocketWebPopupDialog.java: Đổi logic set size thành phủ toàn màn hình cha (setSize(parent.getSize())) và đặt background mờ (setBackground(new Color(0,0,0,140))).
- locket-popup.css & locket-popup.html: Tạo class .locket-wrapper phủ toàn bộ 100vh/100vw và render card chính giữa. Thay thế ảnh reaction và action panel sang tài nguyên tĩnh local (.gif và .svg / .png).

## 3. Overlay đen trong suốt triển khai thế nào
Sử dụng JDialog với setUndecorated(true) và setBackground(new Color(0, 0, 0, 140)) bao trùm toàn bộ kích thước parent. WebView bên trong (thông qua JFXPanel) được cấu hình Scene và CSS body hoàn toàn transparent.

## 4. Đã chặn click nền phía sau thế nào
Bằng cách sử dụng JDialog mode true (application modal) và mở rộng kích thước dialog bằng với cửa sổ cha, mọi thao tác chuột ra bên ngoài vùng card sẽ rơi vào background của JDialog thay vì các frame phía sau. Background này không cài đặt listener bắt click tự động đóng nên người dùng buộc phải dùng nút Thoát.

## 5. Đã xóa khung chữ nhật thừa thế nào
Bỏ màu nền tĩnh ở JFXPanel bằng fxPanel.setOpaque(false). Đồng thời ở HTML/CSS đổi body sang background: transparent;. Class gốc .locket-shell trước kia bung tràn màn hình thì giờ được đưa vào trong khối .locket-wrapper có background trong suốt và căn giữa .locket-shell thành dạng card nổi.

## 6. Thanh phải đã giảm kích thước thế nào
- Thu hẹp cột phải trong grid: grid-template-columns: 256px minmax(0, 1fr) 100px; (giảm thành 100px).
- Thu hẹp round-action (nút lớn): min-height: 64px, padding và gap nhỏ lại. Nút OK nhỏ lại một chút thành 76px.

## 7. Icon bên phải lấy từ local resource nào
Lấy đúng icon từ source local: 
- Phát: images/icon/phat.png
- Ảnh: images/icon/camera.svg
- Tùy chọn: images/icon/tuychon.svg
- Nhắn tin: images/icon/message.svg
- Thoát: images/icon_svg/x.svg

## 8. Reaction/emoji lấy từ local resource nào hoặc popup Swing cũ nào
Thay vì dùng emoji text, tôi đã đổi lại theo đúng popup Swing cũ, sử dụng ảnh động (GIF) từ local project:
- 👍: images/reactions/like.gif
- ❤️: images/reactions/love.gif
- 🥰: images/reactions/care.gif
- 😁: images/reactions/haha.gif
- 😮: images/reactions/wow.gif
- 😢: images/reactions/sad.gif
- 😡: images/reactions/angry.gif

## 9. Đã đổi Xóa thành Thoát chưa
Chính xác, nhãn hiển thị là "Thoát" thay vì "Xóa", nút dùng icon x.svg local và chỉ phát tín hiệu LOCKET_CLOSE.

## 10. Có đụng backend không
Không có bất kỳ sự thay đổi nào tới Backend, file Handler, Entity hay DAO.

## 11. Camera/slideshow/upload còn hoạt động không
Vẫn hoạt động bình thường qua event JS emit. Java Bridge kết nối camera, show ảnh và đẩy dữ liệu hoàn toàn độc lập với CSS interface vừa làm. Nút Thoát cũng đã call cleanupCamera() trên backend (trong disposeDialog()).

## 12. Build/test result
Kết quả node --check hợp lệ (pass JS check). Build Maven thành công BUILD SUCCESS. 

## 13. update.jar đã copy chưa
Đã copy vào HF_UPLOAD\update.jar thành công.
