# TutorHub Secure Exam - Phase 4E-1.6: Real Role Context Fix

## 1. Mục tiêu phase
Thay thế giải pháp tạm `currentUserRole = "TUTOR"` trong `MainDashboard.java` bằng cơ chế lấy role thật từ login payload, đồng thời đảm bảo bảo mật ở cả giao diện client và server backend (Role Guard).

## 2. Vấn đề hardcode currentUserRole = TUTOR
Trong quá trình làm Phase 4E-1.5, phát hiện tab `QuestionBankTab` được render và hiển thị dựa vào biến `currentUserRole` bị hardcode là `"TUTOR"` ở `MainDashboard.java`. Nếu không có cơ chế role thực, học sinh hoặc các người dùng không có thẩm quyền có thể truy cập trái phép.

## 3. User/session/role flow hiện tại
- Tại server, logic đăng nhập (`AuthService.authenticateWithPassword`) lấy thông tin role từ DB và truyền vào `LoginSession`.
- `ClientHandler.java` nhận `LoginSession` và chuẩn bị `dashboardPayload` với định dạng `DASHBOARD_GO|userId|role|avatarBase64`.
- Tại client, `LoginFrame.java` gọi API auth và chỉ tách `userId` từ payload, bỏ qua `role` và `avatarBase64`, dẫn đến việc `MainDashboard.java` phải tự đặt giá trị tĩnh (fallback).

## 4. Cách đã sửa role context
1. Cập nhật `LoginFrame.java` để tách lấy chuỗi `role` từ `dashboardPayload`.
2. Sửa `MainDashboard.java` constructor để có thể nhận thêm chuỗi tham số `role`. 
3. Gán `role` nhận được vào thuộc tính `currentUserRole` trong `MainDashboard`, nếu `role` là chuỗi rỗng thì mặc định là `"STUDENT"`.
4. Bổ sung việc lưu trữ `role` thực của client tại server (`ClientHandler.role`) ngay sau khi đăng nhập thành công.

## 5. Role guard trong MainDashboard
Trên giao diện, điều kiện sau được thực thi tại `MainDashboard.java`:
```java
if ("TUTOR".equalsIgnoreCase(currentUserRole) || "ADMIN".equalsIgnoreCase(currentUserRole)) {
    questionBankTab = new QuestionBankTab(...);
    menuPanel.add(createMenuItem("Ngân hàng câu hỏi", "task", "QuestionBank", 0));
}
```
Role không phải TUTOR/ADMIN (bao gồm STUDENT, unknown, null) sẽ không được tạo tab này, do đó hoàn toàn vô hình.

## 6. Backend role guard nếu có
Bổ sung hàm kiểm tra `hasAdminOrTutorRole()` tại `ClientHandler.java` dùng cho các message gửi từ client:
- `QUESTION_BANK_CREATE`
- `QUESTION_BANK_LIST`
- `QUESTION_CREATE`
- `QUESTION_LIST_BY_BANK`
- `QUESTION_GET_DETAIL`
Các action này đều bị chặn nếu role của socket connection gửi tới không phải `TUTOR` hoặc `ADMIN`.

## 7. Manual test TUTOR/ADMIN
- Đăng nhập dưới tài khoản gia sư hoặc quản trị viên, thông tin role được trả về đúng từ payload.
- Màn hình MainDashboard load thành công kèm theo tab "Ngân hàng câu hỏi" trên sidebar.
- Các API quản trị vẫn chạy bình thường.

## 8. Manual test STUDENT
- Đăng nhập dưới tài khoản STUDENT.
- MainDashboard load thành công nhưng hoàn toàn không render `QuestionBankTab` và menu tương ứng trên sidebar.

## 9. Build result
- Maven build `clean install`: Thành công
- Packaged qua `build_portable.ps1`: Thành công và test chạy ổn.

## 10. Rủi ro còn lại
- Hiện tại một số trường hợp đăng nhập bằng mạng xã hội (OAuth) đang được giả định là `STUDENT` do server không trả trực tiếp. Có thể cần truy vấn CSDL để cấp role linh hoạt sau này.
- Các module khác ngoài QUESTION_ chưa được thêm role guard trên server.

## 11. Phase tiếp theo đề xuất
- Tiến hành Phase 4E-2: Exam Paper Builder UI để bắt đầu lắp ráp đề thi từ ngân hàng câu hỏi.
