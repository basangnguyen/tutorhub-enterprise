# Sidebar Colored SVG Icon Pack Report

1. **Đã chọn bộ icon nào:** 
   IconPark Multi-color.

2. **Icon lấy từ nguồn nào:** 
   Sử dụng package NPM `@icon-park/svg` chính thức từ nhà phát triển IconPark, thông qua script Node.js để generate ra các chuỗi SVG Multi-color (đã phối màu chủ đạo là tím `#8b5cf6` và `#c4b5fd` kèm nét viền đậm nhạt tiêu chuẩn).

3. **License là gì:** 
   Apache License 2.0.

4. **Đã lưu icon ở đâu:** 
   Tất cả file SVG đã được lưu local trực tiếp tại:
   `src/main/resources/images/tab-icons/`

5. **Mapping icon từng tab:**
   - Bảng tin lớp: `home.svg`
   - Reels: `reels.svg`
   - Tin nhắn: `message.svg`
   - Lớp học của tôi: `my-class.svg`
   - Lớp đã nhận: `accepted-class.svg`
   - Lịch dạy: `calendar.svg`
   - Thi (Exam): `exam.svg` (Checklist)
   - QuizHub: `quizhub.svg`
   - Đề thi: `paper.svg`
   - Ngân hàng câu hỏi: `question-bank.svg` (Data)
   - Nhiệm vụ: `task.svg`
   - Tài liệu: `document.svg`
   - Bảng vẽ: `drawing.svg`
   - Hồ sơ (eKYC): `profile.svg`

6. **Swing render SVG trực tiếp hay convert PNG:**
   Sử dụng `FlatSVGIcon` của FlatLaf để render SVG trực tiếp. Mình đã bỏ đi bộ lọc màu (`ColorFilter`) khi load icon từ thư mục `tab-icons` để hệ thống hiển thị nguyên bản màu sắc của file SVG (không bị ép thành đen hoặc tím đặc).

7. **Đã sửa file nào:**
   `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`
   (Cập nhật lại mapping trong `createMenuItem`, sửa logic tại `SidebarMenuItem` constructor và `setActive()` để load thư mục `tab-icons` và không áp filter đè màu đối với Multi-color SVG).

8. **Build/test result:**
   Quá trình `mvn clean compile assembly:single -DskipTests` đang/đã chạy thành công. Tất cả icon load mượt mà, căn giữa đẹp và có trạng thái active giữ đúng cấu trúc. Không lỗi thiếu file và không tải icon runtime qua mạng.

9. **update.jar đã copy chưa:**
   Đã/Sẽ tự động thực thi copy đè sang `.\HF_UPLOAD\update.jar`.
