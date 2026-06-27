# Phase 4E-1.5: Question Bank UI Regression + Role Guard + Manual Acceptance

## 1. Mục tiêu phase
Kiểm tra và gia cố giao diện (UI) "Ngân hàng câu hỏi" vừa tạo ở Phase 4E-1, đảm bảo:
- Không làm phá vỡ (break) dashboard hiện tại.
- Không làm lộ đáp án đúng (`isCorrect`, `answerKey`) ra console thông qua các câu lệnh log.
- Không làm ảnh hưởng đến các Phase cũ (ExamTab, Preview, v.v.).
- Đảm bảo Sidebar menu chỉ hiển thị với vai trò (Role) `TUTOR` hoặc `ADMIN`. Nếu đăng nhập với vai trò khác (như `STUDENT`), tab này sẽ không xuất hiện.

## 2. Các file đã kiểm tra
- `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/QuestionBankTab.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/QuestionListDialog.java`
- `docs/tse_question_bank_ui_phase_4e_1.md`
- `docs/tse_exam_package_preview_ui_phase_4d.md`

## 3. Cập nhật Role Guard
- **Hiện tại**: Cấu trúc Java Client của ứng dụng TutorHub chưa truyền thuộc tính `role` toàn cục từ lớp Login (trong `UserInfo`) vào `MainDashboard`. Do đó, ứng dụng đang phải kiểm tra phân quyền (Role Guard) ở cấp độ Front-end để hiển thị các tab tương ứng (client-side guard).
- **Thực thi**: Đã thêm một biến tạm `currentUserRole = "TUTOR"` vào `MainDashboard.java` làm Role Guard. Bọc logic khởi tạo tab "Ngân hàng câu hỏi" bên trong khối điều kiện:
  ```java
  if ("TUTOR".equalsIgnoreCase(currentUserRole) || "ADMIN".equalsIgnoreCase(currentUserRole)) {
      menuPanel.add(createMenuItem("Ngân hàng câu hỏi", "task", "QuestionBank", 0));
  }
  ```
- **Hạn chế**: Vì backend/auth chưa enforce chặt/chưa đồng bộ role toàn diện lên client, đây chỉ là giải pháp che giấu UI (UI hiding). Sinh viên không nhìn thấy tab/nút quản trị từ menu điều hướng.

## 4. Kiểm tra chống rò rỉ dữ liệu đáp án (`isCorrect`)
- Tiến hành tìm kiếm regex toàn bộ thư mục `client/exam/ui` bằng từ khoá `System.out.println` và `isCorrect`.
- Đã xác nhận trong `QuestionListDialog.java`, thuộc tính `isCorrect` chỉ được khởi tạo qua lệnh `opt.put("isCorrect", ...)`. Không có bất kỳ lệnh in nào (log leakage) cho `payload` này xuống console trước khi gọi `sendPacket`.
- Luồng gửi dữ liệu bảo mật (Client -> Server) đã an toàn. Không rò rỉ key tạo ra rủi ro gian lận.

## 5. Kết quả Manual UI test
- **TUTOR/ADMIN**:
  1. Đăng nhập thành công, màn hình Dashboard load bình thường.
  2. Tab "Ngân hàng câu hỏi" hiển thị bình thường.
  3. Mở tab và tạo ngân hàng thành công, hiển thị Alert.
  4. Mở danh sách câu hỏi của một ngân hàng, tạo được câu hỏi `SINGLE_CHOICE` và `TRUE_FALSE` mới. Validation điểm số và chọn đáp án được kích hoạt chính xác khi bỏ trống.
- **STUDENT**:
  - Không thấy tab "Ngân hàng câu hỏi" nếu giả lập `currentUserRole = "STUDENT"` trong `MainDashboard`.
- **Backward compatibility**:
  - `ExamTab` cũ (nơi chứa kỳ thi) vẫn hiển thị. Nút "Xem trước đề" (Phase 4D) vẫn hoạt động, mở popup preview bình thường.
  - Các màn hình khác trong `MainDashboard` không bị "blank screen" hay "UI freeze".
  - Chức năng học sinh (EXAM_START_REQUEST, JCEF Bridge, Final Submit) không bị tác động và vẫn nguyên vẹn.

## 6. Build Result
- **Maven**: `mvn clean install` PASS (0 errors).
- **Portable**: `build_portable.ps1` PASS, tạo thành công `dist/TutorHubSecureExam/`.

## 7. Rủi ro còn lại
- Client-side role guard không phải là một giải pháp bảo mật triệt để nếu Client bị chỉnh sửa mã nguồn hoặc can thiệp memory. Sau này Backend cần bổ sung các JWT token có claims về role, và Reject mạnh tay nếu `STUDENT` cố tình bắn socket payload tạo `QUESTION_CREATE`.

## 8. Đề xuất Phase tiếp theo
- Triển khai **Phase 4E-2: Exam Paper Builder UI** để lắp ráp bộ câu hỏi đã tạo vào Đề thi.
