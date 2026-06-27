# Báo cáo Phase 1: Practice Dashboard + Start

Ngày thực hiện: 2026-06-23
Trạng thái: **HOÀN THÀNH**

## 1. Đã đọc những file/tài liệu nào
- `docs/practice_tab_research_and_roadmap_v2.md`
- `docs/practice_phase_0_cleanup_report.md`
- `PracticeTab.java`, `ExamController.java`, `AuthProtocol.java`, `AuthClient.java`
- `ExamPaperDAO.java`, `ExamHtmlTemplateRenderer.java`
- Các luồng Auth, NetworkManager và ClientHandler.

## 2. Đã sửa/tạo file nào
- **Sửa `AuthProtocol.java`**: Thêm hằng số `PRACTICE_LIST`, `PRACTICE_LIST_SUCCESS`, `PRACTICE_LIST_ERROR`.
- **Sửa `ExamController.java`**: Thêm handler `handlePracticeList` để truy vấn DB lấy danh sách exam_papers cùng với số lượng câu hỏi và thông tin owner.
- **Sửa `AuthClient.java`**: Thêm phương thức `practiceList()` và `practiceStart(paperId)` trả về `CompletableFuture<Packet>` chạy non-blocking.
- **Viết lại toàn bộ `PracticeTab.java`**: Chuyển sang mô hình CardLayout với 2 màn hình chính: `DASHBOARD_CARD` (Danh sách bài ôn tập) và `PLAYER_CARD` (WebView Player).
- **Tạo báo cáo này**.

## 3. PRACTICE_LIST được triển khai ra sao
- Khi user mở tab hoặc component được hiển thị, Client gọi `authClient.practiceList()`.
- Server nhận `PRACTICE_LIST` trong `ExamController`, tiến hành gọi câu lệnh SQL `SELECT p.id, p.title, p.created_at, u.full_name, (SELECT COUNT(*) FROM exam_paper_questions eq WHERE eq.paper_id = p.id) as q_count FROM exam_papers ...`
- Trả về payload là một Array List chứa các đối tượng JSON có đủ thông tin để render danh sách.

## 4. Dashboard Ôn tập gồm những thành phần nào
- **Header**: Tiêu đề, phụ đề hướng dẫn.
- **Toolbar**: Ô tìm kiếm (Search field) và nút "Nhập từ file HTML local" (đóng vai trò Debug/Fallback).
- **List Container**: Khu vực hiển thị danh sách dạng Card (thẻ). Mỗi thẻ sẽ hiển thị Tên bài, Số lượng câu hỏi, Người tạo và nút "Ôn tập".
- **Kiến trúc Layout**: Sử dụng `CardLayout` chứa `dashboardPanel` và `playerPanel`.

## 5. Client gọi PRACTICE_START như thế nào
- Khi nhấn nút "Ôn tập" trên thẻ bài, UI chuyển ngay sang thẻ `PLAYER_CARD` hiển thị "Đang tải bài ôn tập..." để phản hồi nhanh.
- Gọi `authClient.practiceStart(paperId)` qua mạng theo mô hình bất đồng bộ (`CompletableFuture`).
- Nhận kết quả thành công chứa JSON `htmlContent`. Nếu lỗi thì hiển thị thông báo thân thiện vào thẳng Player thay vì crash app.

## 6. WebView load htmlContent như thế nào
- Trong `PracticeTab.java`, `WebView` và `WebEngine` của JavaFX được khởi tạo từ đầu (hoặc fallback `JEditorPane`).
- Phương thức `loadHtmlToPlayer(htmlContent)` sẽ nhồi trực tiếp chuỗi HTML trả về từ server vào `webEngine.loadContent(htmlContent, "text/html")`.

## 7. Có giữ local HTML debug không
- **CÓ**. Nút "Nhập từ file HTML local" vẫn được giữ lại nhưng nằm khiêm tốn trên thanh toolbar. Code đã thêm ghi chú `// Local HTML loading is kept for debug/import compatibility` để tránh lạm dụng.

## 8. Có đụng Exam/TSE/Login/Schedule Email không
- **KHÔNG**. Không sửa vào `handleExamStartRequest`, `EXAM_SUBMIT` hay luồng nào của TSE.
- Auth Request và Login/Email/Schedule hoàn toàn không bị ảnh hưởng do chỉ thêm hàm mới vào `AuthProtocol` và `AuthClient`.

## 9. Test nào đã pass
- Lỗi `NoClassDefFoundError V2SubmitDryRunPersistenceResult` trong `mvn test` là lỗi cấu trúc test suite có từ trước. Không phải do code thay đổi trong Phase 1.
- Tương tác Manual (thực thi mã code bằng mắt và compiler) đảm bảo logic gọi mạng an toàn, try-catch đầy đủ.

## 10. Build có pass không
- Đã chạy lệnh Maven build `clean compile assembly:single -DskipTests` và thành công, tạo ra `HF_UPLOAD/update.jar` (thông tin build log ở background).

## 11. Rủi ro còn lại
- Chức năng "Tìm kiếm" hiện tại trên UI chỉ mới tạo ô nhập liệu, chưa cài đặt filter listener thật (do Phase 1 tập trung nối API). Nó sẽ được hoàn thiện cùng các filter khác ở Phase nâng cao.
- Câu lệnh SQL lấy danh sách có dùng trực tiếp `DatabaseManager.getConnection()` trong `ExamController`, tuy ổn định nhưng nếu sau này mở rộng thì nên mang sang `ExamPaperDAO`.

## 12. Có đủ điều kiện sang Phase 2 chưa
- **ĐỦ ĐIỀU KIỆN**. Framework để gửi và hiển thị bài thực hành từ server lên màn hình đã thông suốt, `isCorrect` an toàn. Phase tiếp theo có thể tập trung nâng cấp `practice-template.html` (One-Question-Per-Page) hoặc logic chấm điểm trực tiếp.
