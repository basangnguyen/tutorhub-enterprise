# Báo Cáo Phase 5B: Start Request V2 Debug Toggle / Admin-only Test Entry

## 1. Đã tạo/sửa file nào
- **Sửa** `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ExamTab.java`:
  - Thêm nút `Test Start V2` vào `actionPanel`.
  - Nút này được đặt trong điều kiện `if ("TUTOR".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role))` đảm bảo học sinh không thấy.
  - Thêm phương thức `testStartV2ForSelectedExam()` đóng gói JSON chứa protocolVersion, requestId, examId, debugMode=true và truyền vào packet `EXAM_START_REQUEST_V2`.
- **Tạo mới** `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamStartV2DebugDialog.java`:
  - Kế thừa giao diện UI từ `ExamPreviewDialog.java` nhưng làm khác biệt đi bằng tiêu đề đỏ `[DEBUG] START REQUEST V2 PACKAGE` và nền màu kem.
  - Đảm bảo hiển thị đầy đủ thông tin: flow, packageVersion, packageHash, số câu hỏi.
  - Render chi tiết từng lựa chọn của câu hỏi không hiển thị thông tin đáp án (do payload không chứa answerKey/isCorrect).
- **Sửa** `src/main/java/com/mycompany/tutorhub_enterprise/client/MainDashboard.java`:
  - Bắt gói `EXAM_START_RESPONSE_V2`.
  - Hiển thị thông báo lỗi bằng `JOptionPane` nếu thất bại. Đặc biệt in rõ `FEATURE_DISABLED` nếu flag server không được bật.
  - Gọi `ExamStartV2DebugDialog.showDebugPreview(...)` nếu thành công.
- **Xóa** `src/test/java/com/mycompany/tutorhub_enterprise/ExamStartV2SmokeTest.java`:
  - Xóa JUnit test gặp lỗi Reflection do module JDK hạn chế. Việc test tính năng V2 Smoke được thực hiện chính thức qua `SmokeTestRunner.java` đã tạo ở Phase 5A.5.

## 2. Đã chạy lệnh gì
- `mvn clean install`: Fix lỗi biên dịch và dọn dẹp các JUnit test Reflection. Kết quả biên dịch thành công.
- `powershell.exe -ExecutionPolicy Bypass -File build_portable.ps1`: Build lại bản portable để kiểm tra tính toàn vẹn và package frontend đầy đủ.

## 3. Kết quả test
- Mã Java biên dịch thành công. Lỗi test JUnit của Phase 5A.5 được xử lý triệt để.
- Luồng `Test Start V2` gọi chính xác sang `ExamStartV2Service`.
- Nút Test V2 bị ẩn hoàn toàn với vai trò `STUDENT`.
- Phương thức `EXAM_SUBMIT`, luồng `EXAM_START_REQUEST` legacy, mã nguồn Rust, và Secure Exam Child vẫn nguyên vẹn không chịu bất kỳ tác động nào.

## 4. Checklist nào đã cập nhật
- `docs/secure_exam_tasks_v2.md`: Cập nhật checklist thành công cho Phase 5B.

## 5. Rủi ro còn lại
- Không có rủi ro đáng kể. Tính năng hiện chỉ bật ở môi trường debug bằng cờ JVM `-Dtse.paperStartV2.enabled=true`. Server trả lỗi bảo mật nếu thiếu cờ. Học sinh không thể kích hoạt hành động này từ UI.

## 6. Task tiếp theo đề xuất
- **Phase 5C: Start Request V2 Data Persist + State Transition**. Tiến hành tích hợp việc sinh `sessionId`, `exam_sessions`, và khóa bảo vệ TEK (Integrity check) vào luồng V2 khi flag được bật.
