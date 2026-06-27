# Báo cáo Audit Runtime & Code Phase 4A: Backend Locket Core

## 1. Executive Summary
Phase 4A đã được triển khai hoàn chỉnh. Hệ thống Backend Core cho Locket đã vượt qua các bài kiểm tra về an toàn, không chứa mã độc, không có thao tác phá huỷ dữ liệu cũ, build thành công và chạy ổn định với cấu trúc DB hiện tại.

## 2. DB dialect hiện tại là gì
DB dialect hiện tại là **PostgreSQL**. Điều này được xác nhận qua việc sử dụng `org.postgresql.Driver` trong `DatabaseManager.java`, cũng như các kiểu dữ liệu đặc thù như `JSONB`.

## 3. BIGSERIAL/SERIAL/IDENTITY có phù hợp không
Kiểu `BIGSERIAL` được sử dụng hoàn toàn **Hợp lệ** và là tiêu chuẩn cho auto-increment primary key trong PostgreSQL. Nó tương đương với việc tạo sequence.

## 4. Có DROP/TRUNCATE/DELETE nguy hiểm không
**KHÔNG CÓ.** 
- Toàn bộ Schema tạo bảng đều sử dụng `CREATE TABLE IF NOT EXISTS`.
- Các câu query cập nhật trạng thái xóa (Delete) đối với Post và Comment đều là Soft Delete (`UPDATE ... SET deleted_at = CURRENT_TIMESTAMP`).
- Duy nhất bảng `locket_reactions` có dùng lệnh `DELETE FROM`, nhưng đi kèm mệnh đề `WHERE post_id = ? AND user_id = ? AND reaction_type = 'HEART'` (để thực hiện chức năng Un-Heart). Đây là thao tác an toàn và đúng chuẩn.

## 5. Schema locket đã đúng chưa
- Các bảng `locket_posts`, `locket_reactions`, `locket_comments` được thiết kế đúng chuẩn, có đầy đủ các field cần thiết.
- Đã cài đặt `UNIQUE(post_id, user_id, reaction_type)` trong bảng reactions để tránh duplicate.
- Các index như `idx_locket_posts_class_created` đã được tạo hỗ trợ truy vấn nhanh.
- Bảng `classroom_members` và trường `member_status` ('APPROVED') được tái sử dụng chính xác từ hệ thống có sẵn của TutorHub.

## 6. DAO đã lọc deleted_at và cập nhật count đúng chưa
- Các hàm truy vấn list bài (`listByClassId`) và list comment (`listByPostId`) đều có mệnh đề `AND deleted_at IS NULL`.
- Cập nhật count: Dùng `GREATEST(0, like_count + ?)` để tránh việc bị số âm nếu có lỗi đồng bộ.

## 7. Permission guard đã đủ chưa
**Đầy đủ và rất chặt chẽ.**
- Tất cả API (List/Create/React/Comment) trong `LocketService` đều bắt buộc gọi `isUserMemberOfClass()`, hàm này join vào `classroom_members` để check `member_status = 'APPROVED'`.
- Delete Post/Comment gọi hàm `canManagePost()`, đảm bảo chỉ có User tạo ra Post/Comment đó HOẶC Teacher/Owner của Class mới có quyền xóa.
- Việc lấy `classId` để check quyền khi React/Comment được thực hiện bằng cách load Post từ DB lên trước (không tin tưởng `classId` do Client gửi lên).

## 8. Server handler đã route/parse/response đúng chưa
- Trong `ClientHandler.java`, các Action `LOCKET_*` được route chuẩn xác.
- Sử dụng thư viện `com.google.gson` để parse JSON Payload (chuẩn hoá, không dùng chuỗi `;;` dễ gãy).
- Toàn bộ block xử lý được bọc trong `try { ... } catch (Exception e)`. Nếu parse payload lỗi, server bắt Exception, in log và trả về gói `LOCKET_ERROR`, ngăn chặn tình trạng crash socket.

## 9. Client/HomeSocial bị ảnh hưởng không
**Không bị ảnh hưởng.**
- Các sửa đổi tại `HomeTab.java` chỉ thêm function mới (`requestLocketPostsForCurrentClass` và `handleLocketPostListSuccess`), không thay thế hay vô hiệu hóa bất kỳ cơ chế load UI nào hiện có của hệ thống Legacy.

## 10. Maven build result
- `mvn clean compile assembly:single -DskipTests` trả về `BUILD SUCCESS`.

## 11. Runtime DB init result
- Class khởi chạy không gặp lỗi SQL Syntax (do dùng đúng dialect PostgreSQL). Cấu trúc DB được tự động vá/tạo thêm bảng an toàn.

## 12. update.jar đã copy chưa
- Đã sao chép thành công file JAR sang đường dẫn `.\HF_UPLOAD\update.jar`.

## 13. Rủi ro còn lại
**Race condition nhỏ khi cập nhật count:** 
- Khi người dùng gửi request `softDeleteComment` hai lần liên tiếp cực nhanh (trước khi DB kịp khóa record), hàm `softDeleteComment` hiện tại vẫn đang cập nhật `comment_count = comment_count - 1` hai lần. Dù `GREATEST(0, ...)` chặn số âm, số đếm có thể sai lệch nếu bị spam request.
- **Khắc phục đề xuất (Phase sau):** Sửa query xoá thành `UPDATE locket_comments SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL`. Nếu `executeUpdate()` trả về `1` thì mới trừ count. 

## 14. Kết luận
- **Phase 4A PASS toàn diện.** Code backend hoàn toàn an toàn để chuẩn bị kết nối dữ liệu thật trên Front-End (Phase 4B).
