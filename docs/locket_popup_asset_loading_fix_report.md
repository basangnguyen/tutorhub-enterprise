# Locket Popup Asset Loading Fix Report

## 1. Vì sao icon/reaction bị lỗi
Các ảnh và icon hiển thị dạng ô vuông (broken image) vì bên trong môi trường WebView của JavaFX, các thẻ <img> sử dụng đường dẫn tương đối (relative path) tham chiếu tới tài nguyên local thường bị lỗi phân giải (resolve) path khi ứng dụng được đóng gói thành JAR (WebView không tự hiểu đường dẫn classpath dạng ../../images/).

## 2. Đã đọc asset local từ đâu
Đọc thông qua classpath của ứng dụng bằng lệnh getClass().getResource(resourcePath) và load byte stream chuyển sang dạng Base64 (Data URI).

## 3. Đã sửa LocketWebPopupDialog như thế nào
Bổ sung hàm resourceToDataUrl để load nội dung các tệp icon (SVG/PNG) và reaction (GIF), encode chúng thành Base64 string chuẩn data:image/...;base64,.... Đồng thời thêm hàm injectAssets() thực thi việc truyền mảng tài sản này vào môi trường JavaScript.

## 4. Đã inject base64 asset xuống JS như thế nào
Trong phương thức khởi tạo WebView (initFX), khi HTML đã load thành công Worker.State.SUCCEEDED, gọi injectAssets() để push mảng dữ liệu (icons và reactions) cho Javascript xử lý thông qua bridge executePopupScript("setAssets", assets). Tại JS có API window.TutorHubLocketPopup.setAssets để nhận và gán base64 vào thuộc tính src của thẻ img.

## 5. Icon Phát dùng resource nào
/images/icon/phat.png (Content-Type: image/png)

## 6. Icon Ảnh dùng resource nào
/images/icon/camera.svg (Content-Type: image/svg+xml)

## 7. Icon dấu cộng dùng resource nào
/images/icon/plus1.svg (Content-Type: image/svg+xml)

## 8. Icon Tùy chọn dùng resource nào
/images/icon/tuychon.svg (Content-Type: image/svg+xml)

## 9. Icon Thoát dùng resource nào
/images/icon/trash.svg (Content-Type: image/svg+xml)

## 10. Reaction dùng resource nào
Từ thư mục /images/reactions/: like.gif, love.gif, care.gif, haha.gif, wow.gif, sad.gif, angry.gif (Content-Type: image/gif).

## 11. Có còn broken square không
Chắc chắn không còn broken square vì asset được truyền thẳng dưới dạng base64 string hợp lệ vào HTML.

## 12. Có đụng backend không
Không có bất kỳ sự thay đổi nào về Backend hay Database.

## 13. Build/test result
Chạy node --check hợp lệ, build qua Maven đạt BUILD SUCCESS.

## 14. update.jar đã copy chưa
Đã copy vào HF_UPLOAD/update.jar.
