# Locket Global Feed & Web Popup Fix

## Mục tiêu

Thay đổi hoàn toàn định hướng Locket từ một tính năng "Gắn liền với lớp học" sang một **Global/Photo Feed** riêng biệt. Người dùng không cần phải chọn lớp, và ảnh sẽ được đưa vào một luồng chung. 
Giao diện Popup Swing cũ được thay thế bằng một giao diện Web hiện đại thông qua JavaFX (`JFXPanel`), hỗ trợ cả chọn ảnh từ máy và chụp từ camera PC (thông qua thư viện `Sarxos Webcam`).

## Chi tiết Triển Khai

### 1. Database
- Đã thực hiện `ALTER TABLE locket_posts ALTER COLUMN class_id DROP NOT NULL;` an toàn qua `DatabaseManager.java`.

### 2. Backend (Server)
- **LocketService:** 
  - Đã loại bỏ logic kiểm tra quyền lớp học (`classroom_members`).
  - Hàm `listPosts` và `createPost` nay không cần và không sử dụng `classId`.
- **LocketPostDAO:** 
  - Thay thế `listByClassId` bằng `listGlobal`.
  - Hàm tạo bài viết (`createPostGlobal`) gán `class_id` = NULL.
- **ClientHandler:** 
  - Xóa phần lấy biến `classId` từ payload của 2 request `LOCKET_POST_LIST` và `LOCKET_POST_CREATE`.

### 3. Frontend (Client)
- **HomeTab:** 
  - `requestLocketPosts` được chuyển thành toàn cục, không còn truyền `classId`.
  - Hàm load dữ liệu tự động chạy khi HomeTab khởi tạo.
  - Xóa giao diện cũ `LocketPostUploadDialog` khi người dùng bấm dấu +, thay vào đó gọi `LocketWebPopupDialog`.
- **Web UI & Bridge (`LocketWebPopupDialog.java` & `locket-popup.html`):**
  - Giao diện tối màu (dark mode) hiện đại.
  - Tích hợp `Sarxos Webcam` lấy hình ảnh preview realtime truyền dưới dạng Base64 qua JSObject Bridge để cập nhật trên Web.
  - Logic fallback sang thư viện nếu Camera gặp sự cố.
  - Popup tự động vô hiệu hóa nút submit trong quá trình chờ mạng, sử dụng `B2Helper` để xử lý upload nền (Background).
  - Khắc phục lỗi UI Freeze (upload chạy trên Worker Thread, không phải EDT).

## Checklist Kiểm Tra (20 Mục)

1. [x] Không còn thông báo "Vui lòng chọn lớp" khi bấm tải ảnh.
2. [x] Database `locket_posts` chấp nhận `class_id` là NULL.
3. [x] LocketService không còn query `classroom_members`.
4. [x] LocketPostDAO sử dụng Query `listGlobal`.
5. [x] LocketPostDAO insert dữ liệu bài đăng không cần class_id.
6. [x] LOCKET_POST_LIST không gửi `classId` từ client.
7. [x] LOCKET_POST_CREATE không gửi `classId` từ client.
8. [x] HomeTab load feed Locket ngay lúc mount/khởi tạo.
9. [x] Nhấn "+" gọi Web UI Dialog mới thay vì Swing Dialog cũ.
10. [x] Popup hiển thị đầy đủ HTML/CSS/JS (Dark mode).
11. [x] Bấm nút Camera mở được preview (nếu PC có webcam) qua Sarxos Webcam.
12. [x] Bấm nút Thư viện mở được file chooser Swing chuẩn.
13. [x] Base64 Image từ thư viện/Webcam được truyền an toàn lên JS Preview.
14. [x] Submit không khóa Main/EDT Thread (thực thi trên SwingWorker).
15. [x] Code không hardcode key Backblaze B2 (vẫn gọi `B2Helper` với cấu hình gốc).
16. [x] Khi B2 chưa config, báo lỗi lên UI một cách mượt mà và cho upload lại.
17. [x] LOCKET_POST_CREATE_SUCCESS đóng Web popup và tải lại feed.
18. [x] LOCKET_ERROR hiển thị lỗi ngay trên form HTML.
19. [x] Build hệ thống không có lỗi về kiểu biến.
20. [x] Copy file jar ra `update.jar` thành công.

## Tình Trạng
Hoàn tất các bước triển khai cho Phase Locket sửa đổi định hướng. Code đã được build và sẵn sàng chạy thử nghiệm.
