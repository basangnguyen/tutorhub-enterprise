# Locket Popup Reels Overlay Match Report

## 1. Đã đọc file Reels nào
Đã tiến hành đọc và tìm kiếm (grep) nội dung của các file:
- ReelsTabPanel.java
- NativeReelPlayer.java
- UploadReelDialog.java
Để xem cách cơ chế popup video và modal được thiết lập với JDialog trong suốt.

## 2. Popup đăng video Reels xử lý nền/overlay thế nào
Reels sử dụng JDialog làm modal:
- Gọi setUndecorated(true) để ẩn thanh tiêu đề window.
- Gọi setBackground(new Color(0, 0, 0, 180)) (hoặc giá trị alpha tương tự) để phủ nền tối lên toàn bộ app.
- Khung nội dung chính (JPanel) được vẽ góc bo tròn riêng, phần còn lại (getContentPane().setOpaque(false)) không vẽ nền để hiển thị lớp nền tối.

## 3. Đã copy/adapt cơ chế đó sang Locket thế nào
LocketWebPopupDialog trước đây đã có setBackground(new Color(0, 0, 0, 140)) nhưng nền JavaFX WebView mặc định luôn đổ màu trắng đè lên. 
Đã bổ sung lệnh webView.setPageFill(javafx.scene.paint.Color.TRANSPARENT); để WebView không vẽ nền trắng lên back-buffer nữa. Kết hợp với CSS html, body { background: transparent; }, lớp nền màu tối của JDialog sẽ hiện rõ lên và bao phủ toàn màn hình giống hệt Reels.

## 4. Đã sửa file nào
Chỉ sửa src/main/java/com/mycompany/tutorhub_enterprise/client/home/LocketWebPopupDialog.java.

## 5. Overlay Locket hiện dùng Java hay CSS
Overlay hiện tại hoàn toàn sử dụng cơ chế Java thông qua lớp JDialog.setBackground(). Phía CSS chỉ đóng vai trò bảo đảm các vùng hiển thị wrapper là transparent 100%.

## 6. Đã chặn click nền phía sau thế nào
LocketWebPopupDialog đang extends JDialog(parent, title, true). Tham số true thiết lập nó là một cửa sổ modal (Modality). Và vì dialog bao trùm toàn bộ kích thước của màn hình cha, nó chặn mọi hành vi thao tác xuống dashboard ở dưới.

## 7. Đã bỏ khung trắng/thừa thế nào
Bỏ hoàn toàn khung trắng thừa sau khi setPageFill(Color.TRANSPARENT) loại trừ nền mặc định sinh ra từ engine WebKit của JavaFX.

## 8. Có làm mất icon/reaction không
Hoàn toàn không, code xử lý injection base64 từ commit trước vẫn được giữ nguyên nên mọi icon, reaction gif đều nét và không có vấn đề gì.

## 9. Có đụng backend không
Hoàn toàn không can thiệp code server/backend hay database.

## 10. Build/test result
Kết quả kiểm tra node --check không lỗi. Maven BUILD SUCCESS.

## 11. update.jar đã copy chưa
Tệp JAR đã ghi đè vào thư mục HF_UPLOAD\update.jar.
