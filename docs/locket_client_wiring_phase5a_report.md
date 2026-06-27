# Báo cáo Triển khai Phase 5A: Upload Ảnh Locket Từ Máy

## 1. Phạm vi thực hiện
Đã hoàn thành tính năng upload ảnh trực tiếp từ máy tính lên Locket feed, đảm bảo tuân thủ toàn bộ các yêu cầu an toàn, không phá vỡ tính nguyên vẹn của các module cũ.

### Các file đã chỉnh sửa / tạo mới
- **`LocketPostUploadDialog.java` (Tạo mới):** Kế thừa UI/UX của hệ thống hiện tại với các cải tiến về preview ảnh. Validation, async upload đều nằm ở lớp tương tác gọi từ HomeTab.
- **`HomeTab.java` (Sửa đổi):** Xử lý sự kiện `LOCKET_CREATE_OPEN` để hiển thị `LocketPostUploadDialog` và khởi chạy luồng background upload.
- **`MainDashboard.java` (Sửa đổi):** Bắt packet `LOCKET_POST_CREATE_SUCCESS` và `LOCKET_ERROR` để đóng/cập nhật dialog hiển thị trạng thái cho user.
- **`LocketService.java` (Sửa đổi):** Bổ sung logic sanitize caption, loại bỏ khoảng trắng thừa, xử lý caption quá dài (cắt bớt về 150 ký tự) tại backend.

---

## 2. Giải đáp các yêu cầu an toàn

### 2.1. B2Helper lấy credential từ đâu
**Trả lời:** `B2Helper` hoàn toàn an toàn và **không chứa hardcoded secret**.
Nó đang lấy credentials thông qua `System.getProperty` hoặc `System.getenv`. Các biến môi trường bắt buộc là:
- `TUTORHUB_B2_BUCKET`
- `TUTORHUB_B2_ENDPOINT`
- `TUTORHUB_B2_REGION`
- `TUTORHUB_B2_ACCESS_KEY`
- `TUTORHUB_B2_SECRET_KEY`

Trong trường hợp rỗng, `B2Helper` ném `IllegalStateException`. `HomeTab` bắt exception này và thông báo lỗi rõ ràng lên UI mà không làm sập (crash) hệ thống. Không có log secret ra màn hình.

### 2.2. Upload có chạy background không
**Trả lời:** Có.
Trong `HomeTab.java`, thao tác upload qua `B2Helper` và mã hoá Base64 được bao bọc hoàn toàn bên trong một luồng `SwingWorker`. Giao diện (EDT) không bị đơ giật trong quá trình đọc ảnh, convert và đẩy qua network. 

### 2.3. Đã disable nút Đăng khi upload chưa
**Trả lời:** Có.
Hàm `dialog.setLoading(true)` được gọi ngay khi quá trình doInBackground bắt đầu:
- Thay đổi nút Đăng thành: `"Đang upload..."`
- Set màu nút sang dạng xám (Light_Gray) thay vì gradient.
- Đổi chuột thành hình đồng hồ cát (WAIT_CURSOR).
- Khóa ô nhập Caption (`txtCaption.setEnabled(false)`).
- Chặn các hành vi kéo-thả thả file mới, chọn file mới, click Đăng lại, và click đóng Dialog (chống phá vỡ luồng upload đang thực thi).

### 2.4. LOCKET_ERROR xử lý thế nào
**Trả lời:**
`MainDashboard` đã có logic bắt `LOCKET_ERROR`. 
Nó được cập nhật để kiểm tra nếu user đang mở `LocketPostUploadDialog`:
- Gửi thông điệp lỗi trực tiếp vào hàm `homeTab.handleCreateError(packet.payload)`.
- Dialog lúc này tự động tắt hiệu ứng loading (`setLoading(false)`).
- Hiện lại trạng thái ban đầu để người dùng thử lại và báo lỗi bằng `JOptionPane.showMessageDialog`.
Dialog không bị đóng lại, user có thể đổi ảnh hoặc rút gọn caption rồi nhấn "Đăng" lại.

### 2.5. WebP có hỗ trợ preview thật không
**Trả lời:**
Java tĩnh (`ImageIO`) phiên bản mặc định không có bộ giải mã tích hợp sẵn cho định dạng WebP. 
Để an toàn, `LocketPostUploadDialog` đã được chặn riêng trường hợp `.webp`. Nếu user thả file `.webp` vào và `ImageIO.read` trả về `null`, dialog sẽ bung cảnh báo: 
*“Định dạng WebP chưa được hỗ trợ trên máy này, vui lòng chọn JPG/PNG.”*
Ứng dụng sẽ không sập vì đọc ảnh lỗi. Các đuôi `.jpg`, `.jpeg`, `.png` sẽ preview ảnh bình thường, được scale mượt (nhỏ hơn khung hình) trước khi đặt lên giao diện.

---

## 3. Hoàn thành kiểm tra (Audit)
- Bắt được giới hạn size `<= 8MB`.
- Validate định dạng: `JPG, PNG, WEBP`.
- Audit SQL Danger Commands: Kết quả quét mã nguồn hoàn toàn KHÔNG CÓ `DROP`, `TRUNCATE` hay `DELETE` nguy hiểm.
- Project đã được build thành công thành `update.jar` thông qua lệnh Maven.

Tất cả đã sẵn sàng để tích hợp trên production test.
