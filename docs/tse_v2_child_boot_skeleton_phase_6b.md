# Phase 6B: Secure Exam Child V2 Boot Skeleton

## 1. Mục tiêu phase
Tạo ra nhánh khởi chạy (boot skeleton) cho Child process trong chế độ V2 mà không phá vỡ tính tương thích với luồng legacy. Child cần nhận biết được khi nào Parent truyền lệnh khởi chạy V2 để thực hiện xác thực bước đầu mà chưa cần giao tiếp IPC hay giải mã.

## 2. Vì sao chỉ là skeleton
Chúng ta chia nhỏ việc tích hợp V2 vào Child ra thành nhiều bước để đảm bảo tính ổn định. Phase này chỉ tập trung vào việc:
- Parse đúng arguments mới.
- Rẽ nhánh luồng chương trình một cách an toàn.
- Xác thực cơ bản file `.meta.json`.
- Chặn không cho thực thi JCEF hoặc request AES key khi chưa đến Phase sau.
Điều này giúp dễ dàng test cô lập logic nhận dạng V2 mà không phải debug cùng lúc cả IPC server/client, quá trình giải mã AES và render JCEF.

## 3. Args mới
Đã tạo thêm parser arguments qua `TSEChildLaunchArgsParser` để hỗ trợ:
- `--v2-handoff-meta <path>`: Bắt buộc để nhận diện mode V2. Path trỏ tới file meta.
- `--v2-handoff-enc <path>`: Tuỳ chọn, path trỏ tới file mã hoá.
- `--v2-debug-only`: Cờ debug.

## 4. Legacy args preserved
Cấu trúc parse `--context`, `--output`, và `--key` vẫn được giữ nguyên không thay đổi và hoạt động y như cũ khi không có cờ `--v2-handoff-meta`.

## 5. V2 debug mode behavior
- Nếu Child nhận diện được V2, nó sẽ in ra log `[TSE_CHILD] V2 Boot Recognized. Entering Debug Skeleton Mode.`
- Child sẽ không đọc nội dung key.
- Child sẽ hiển thị một Swing Frame `JFrame` giả lập mang tính placeholder thay cho luồng thi bình thường.
- Giao diện placeholder thông báo cho học sinh/tester về tình trạng của mode V2 (nhận diện thành công, các file được truyền).

## 6. Meta validation
- Đã cài đặt một lớp xác thực `meta` đơn giản sử dụng Gson trong `launchV2DebugSkeleton()`.
- Kiểm tra tính tồn tại của `handoffId`.
- Kiểm tra tính tồn tại của `encryptedFileSha256`.
- Cảnh báo nếu flow không phải `PAPER_START_V2`.
- Không tiến hành decrypt payload hay kết nối HTTP Loopback.

## 7. Files changed
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEExamChildClient.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEChildLaunchArgs.java` (New)
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEChildLaunchArgsParser.java` (New)
- `src/test/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEChildLaunchArgsParserTest.java` (New)
- `run_v2_child_skeleton_test.bat` (New)

## 8. Tests run
- `mvn clean install` - Passed (55/55 unit tests).
- `run_v2_child_skeleton_test.bat` - Passed (Hiển thị được màn hình V2 Debug Skeleton, parse thành công file meta ảo và báo lỗi/success tương ứng).

## 9. Legacy regression result
- `build_portable.ps1` - Passed.
- `run_input_test.bat --exam-id 3` - Passed (Khởi chạy bình thường, parse được legacy args và mở đúng JCEF).

## 10. Rủi ro còn lại
- Hiện tại V2 mode chưa có liên kết giao tiếp (IPC) với Parent, nên chưa thể lấy AES key.
- Placeholder hiện tại chưa xử lý sự kiện đóng/exit một cách chính xác nhất như luồng production. 

## 11. Phase tiếp theo đề xuất
- **Phase 6C Loopback IPC prototype**: Cài đặt HTTP Server trên Parent, HTTP Client trên Child để hiện thực hoá cơ chế IPC và exchange `handoffId` lấy AES key an toàn.
