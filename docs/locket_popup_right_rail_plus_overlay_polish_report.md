# Locket Popup Right Rail & Overlay Polish Report

## 1. Đã đọc lại file/report nào
- LocketWebPopupDialog.java
- locket-popup.html, locket-popup.css, locket-popup.js
- Đã xem qua docs/locket_popup_asset_loading_fix_report.md
- Đã đọc cấu trúc nền của tab Reel.

## 2. Đã tham khảo cơ chế nền trong suốt của tab Reel ở file nào
Đã kiểm tra qua lệnh grep_search và xem trực tiếp class NativeReelPlayer.java để xác định cơ chế nền JDialog trong suốt setUndecorated(true) cùng setBackground(new Color(0,0,0,x)) và việc getContentPane().setOpaque(false).

## 3. Đã sửa file nào
- LocketWebPopupDialog.java (thêm ((JComponent) getContentPane()).setOpaque(false))
- locket-popup.css (chỉnh layout cột phải và nút bấm)

## 4. Thanh phải đã giảm kích thước thế nào
- grid-template-columns chuyển từ 100px xuống 86px.
- .action-panel thêm align-self: center để không bị kéo giãn cao hết toàn màn hình mà chỉ cao vừa đủ bọc lấy nội dung bên trong.

## 5. Các nút phải đã xích lại gần nhau thế nào
- Trong .action-panel, đổi justify-content: space-between thành justify-content: center với khoảng cách gap: 14px.
- Kích thước .round-action giảm xuống còn chiều ngang 66px, chiều cao min-height: 60px, gap bên trong giữa icon và text là 4px.
- .action-icon img chỉnh về 24px x 24px.

## 6. Nút OK đã chỉnh thế nào
Nút .ok-action được thu hẹp về width: 70px, bo góc border-radius: 22px.

## 7. Icon dấu cộng đã chỉnh thế nào
- Vòng tròn pastel .create-plus-circle mở rộng lên kích thước 64px x 64px.
- Icon bên trong set cứng 30px x 30px với object-fit: contain để vừa khít đẹp mắt, chữ "Tạo bài mới" không bị đè lấn.

## 8. Có dùng plus1.svg không, nếu không thì vì sao
Có dùng trực tiếp icon local images/icon/plus1.svg qua DataURL base64. Vì icon này đã được set lại kích thước trong CSS nên sẽ mịn, không thô và không cần CSS trick dạng chuỗi text.

## 9. Overlay/nền trong suốt đã chỉnh theo cơ chế nào
Đã tái sử dụng cơ chế Overlay của NativeReelPlayer:
- setUndecorated(true)
- setBackground(new Color(0, 0, 0, 140))
- Bổ sung ((JComponent) getContentPane()).setOpaque(false) để đảm bảo Content Pane của JDialog không vô tình phủ nền màu xám/trắng mặc định của Look & Feel lên phía sau WebView.

## 10. Có còn khung chữ nhật thừa không
Hoàn toàn không, JFXPanel và ContentPane đều set opaque(false), HTML body transparent. Nền mờ (0,0,0,140) sẽ che toàn bộ màn hình.

## 11. Có đụng backend không
Hoàn toàn không đụng cấu trúc logic backend.

## 12. Build/test result
Kết quả kiểm tra node --check vượt qua, build Maven BUILD SUCCESS.

## 13. update.jar đã copy chưa
Đã ghi đè thành công update.jar vào thư mục HF_UPLOAD.
