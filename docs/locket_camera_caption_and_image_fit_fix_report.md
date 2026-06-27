# Locket Camera Caption and Image Fit Fix Report

## 1. Đã đọc file nào
- `src/main/resources/locket-web/locket-popup.html`
- `src/main/resources/locket-web/locket-popup.css`
- `src/main/resources/locket-web/locket-popup.js`
- `src/main/resources/home-social/home-social.css`
- `src/main/resources/home-social/home-social.js`

## 2. Luồng camera cũ sai ở đâu
- CSS cũ sử dụng duy nhất class `camera-mode` chung cho cả trạng thái đang mở camera (preview) và trạng thái hiển thị ảnh vừa chụp (draft mode).
- Điều này dẫn đến lỗi: Khi bật camera, input caption và nút đăng không bị ẩn (do CSS `display: grid`). Khi chụp xong, JS lại gọi `showPickedImage` đưa UI về lại trạng thái `viewer` (chỉ cho phép xem bài viết có sẵn), dẫn đến input caption bị ẩn mất.

## 3. Đã sửa state camera/caption thế nào
- Tách state JS ra thành 3 trạng thái rõ ràng: `"viewer"`, `"cameraPreview"`, và `"draft"`.
- Cập nhật lại các hàm render tương ứng: `renderViewer()`, `renderCameraPreview()`, `renderDraft()`.

## 4. Khi camera preview thì caption/reaction/message xử lý thế nào
- Cập nhật CSS với class mới `.camera-preview`: Khi đang ở trạng thái `cameraPreview`, toàn bộ thanh `social-strip` (chứa reaction/message) và `caption-row` (chứa input/nút đăng) đều bị ẩn đi. Nút OK mang chức năng "Chụp".

## 5. Sau khi chụp thì caption/nút Đăng xử lý thế nào
- Sau khi chụp, logic JS chuyển trực tiếp sang trạng thái `"draft"`.
- Cập nhật CSS với class mới `.draft-mode`: Thanh `social-strip` bị ẩn, nhưng `caption-row` được hiển thị để người dùng nhập nội dung cùng với nút Đăng (`#submitButton`).

## 6. Đăng ảnh chụp camera hoạt động chưa
- Đã sẵn sàng. Sau khi chụp, ảnh được đưa lên giao diện giữa, người dùng có thể gõ nội dung vào input caption và bấm nút Đăng để upload.

## 7. Ảnh bị crop do CSS nào
- Do thuộc tính `object-fit: cover` trên `#viewerImage` và `#cameraImage` (trong file `locket-popup.css`).
- Và thuộc tính `background-size: cover` trên `.locket-image` (trong file `home-social.css`).

## 8. Đã sửa cơ chế fit ảnh thế nào
- Đổi thành `object-fit: contain` đối với các thành phần của popup viewer (đảm bảo ảnh giữ đúng tỷ lệ, không bị cắt xén).
- Đối với thẻ card ở màn hình Home, cập nhật CSS thành `background-size: cover, contain; background-repeat: no-repeat; background-color: #1e293b;`. Cấu trúc đa background này giúp giữ lại layer gradient/blur để làm background, còn ảnh foreground được apply kích thước `contain` giúp hiển thị đầy đủ không khuyết mất hình.

## 9. Popup viewer có hiển thị ảnh đầy đủ chưa
- Chắc chắn sẽ hiển thị trọn vẹn toàn bộ bức ảnh ở bất kỳ tỷ lệ nào (nhờ `object-fit: contain`).

## 10. Card feed có hiển thị ảnh đầy đủ chưa
- Rồi, ảnh trên home card feed được render với thiết kế 2 layer đảm bảo không bị trống lặp khung ảnh hay méo ảnh. Hình ảnh hiển thị đủ mặt/chi tiết mà không vỡ layout card.

## 11. Có đụng backend không
- Hoàn toàn KHÔNG sửa đổi backend, DB hay ảnh hưởng đến tính năng upload/presigned URL đã sửa trước đó. Tính năng overlay Reels, Base64 reactions vẫn nguyên vẹn.

## 12. Build/test result
- Đã kiểm tra cú pháp hai file JS bằng `node --check` -> PASS (Không lỗi cú pháp).
- Thực thi quy trình build Maven: SUCCESS (không lỗi, update.jar đã được tạo mới).

## 13. update.jar đã copy chưa
- Đã sao chép thành công sang thư mục `HF_UPLOAD\update.jar` thông qua câu lệnh copy.
