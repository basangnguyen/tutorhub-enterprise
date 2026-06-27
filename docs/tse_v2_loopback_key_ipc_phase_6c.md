# Phase 6C: Parent-to-Child Loopback IPC Key Request Prototype

## 1. Mục tiêu phase
Xây dựng một prototype cho cơ chế IPC (Inter-Process Communication) thông qua Localhost Loopback HTTP để Child có thể yêu cầu an toàn AES key từ Parent. Việc trao đổi sẽ dựa trên `handoffId` và một mã ngẫu nhiên `nonce` đã được lưu sẵn trong metadata.

**Quan trọng**: Phase này chỉ xây dựng các lớp xử lý mạng ở mức dịch vụ (Service) cùng với test tích hợp tương ứng. Nó chưa được đấu nối vào quá trình khởi chạy thật sự của Secure Exam Child (Parent chưa tự động mở cổng HTTP trước khi gọi file exe của Child, và Child cũng chưa thật sự gửi request khi ở V2 mode).

## 2. Vì sao dùng POST loopback IPC
- **Bảo mật**: Việc truyền key trực tiếp qua Command Line (CLI) hay Environment Variables đều dễ bị OS bắt được thông qua các command như `wmic process get commandline` hay Process Explorer.
- **Tại sao lại dùng POST**: Dùng GET dễ khiến các query string bị lưu lại vào system logs, server access logs (dù là loopback), hoặc proxy. Phương thức POST với payload nằm trong JSON body đảm bảo `nonce` và `handoffId` không bị lộ qua URL.
- **Tại sao lại là Loopback HTTP**: Java tích hợp sẵn thư viện `com.sun.net.httpserver.HttpServer` vô cùng nhẹ và không phụ thuộc thư viện thứ 3. Giao thức HTTP đơn giản, dễ tích hợp và dễ debug. Quá trình giao tiếp chỉ mở đúng cho IP `127.0.0.1`.

## 3. Endpoint contract
- **Endpoint**: `POST /v2/handoff/key/consume`
- **Host**: `127.0.0.1` (Chỉ local)
- **Port**: Port ngẫu nhiên (random port 0) do Parent quyết định và báo lại cho Child qua `.meta.json`.
- **Request Body**:
```json
{
  "handoffId": "uuid",
  "nonce": "random-nonce",
  "clientMode": "V2_DEBUG"
}
```
- **Response Success**: `200 OK`
```json
{
  "success": true,
  "keyB64": "base64-aes-key",
  "handoffId": "uuid"
}
```
- **Response Error**: `400 Bad Request`
```json
{
  "success": false,
  "errorCode": "INVALID_NONCE_OR_HANDOFF"
}
```

## 4. Nonce design
- Một `nonce` 24 bytes ngẫu nhiên được sinh ra bởi `SecureRandom` và Base64URL encode trong Parent lúc khởi tạo Handoff.
- Nonce này được ghi thẳng vào `v2_handoff_runtime.meta.json` để truyền qua file cho Child.
- Parent sẽ lưu tạm mapping `handoffId -> nonce` vào bộ nhớ.
- Khi Child gọi API, nó phải truyền đúng nonce thì Parent mới trả key. Điều này chống lại các chương trình scan port ở localhost tìm kiếm cổng mở ngẫu nhiên để ăn trộm key.

## 5. Registry consume flow
- Giao thức yêu cầu `V2RuntimeKeyRegistry.consumeKey(handoffId)` (được phát triển ở Phase 5H).
- Hàm này tự động tiêu hủy (remove) key sau khi truy xuất thành công.
- Ngay sau đó, mapping nonce cũng bị gỡ bỏ.
- Do vậy, nếu Child (hoặc một kẻ xấu) cố gắng request thêm lần thứ 2, nó sẽ luôn thất bại.

## 6. Metadata update
- Lớp `V2RuntimeHandoffMeta` đã được mở rộng để lưu các trường IPC bao gồm:
  - `parentIpcHost`
  - `parentIpcPort`
  - `nonce`
- Hàm ghi meta trong `V2RuntimeHandoffService` đã được cập nhật để cho phép đưa các trường IPC vào `.meta.json`.
- `V2RuntimeHandoffDryRunCoordinator` cũng được cập nhật để sinh thử nonce ngẫu nhiên.

## 7. Security validation
Đã rà soát kỹ code:
- Không log `keyB64` ra console.
- Client IPC che đi lỗi chi tiết trả về từ `conn.getErrorStream()`, chỉ in ra mã `errorCode`.
- Server từ chối không phục vụ nếu method là GET.
- Server tự động stop sau TTL nếu Child không kịp request.
- Các bài kiểm thử test failed khi truyền thiếu, truyền sai nonce hoặc consume 2 lần.

## 8. Unit/integration test result
- Đã tạo `V2LoopbackKeyHandoffServerTest.java` (Sử dụng JUnit 5).
- Bài test kiểm tra đúng đắn các rủi ro bảo mật:
  - Random port > 0.
  - POST hợp lệ trả về đúng key (Round trip).
  - Yêu cầu lần 2 bị reject (`consumeKey` xoá registry).
  - Sai nonce bị reject.
  - Thiếu nonce bị reject.
  - GET request bị reject.
- Unit Test chạy 62/62 tests PASS thông qua `mvn clean install`.

## 9. Legacy regression result
- Build `build_portable.ps1` PASS thành công.
- Lệnh giả lập luồng cũ `run_input_test.bat --exam-id 3` PASS thành công, Launcher bật lên và module V2 không hề bị gọi nhầm.

## 10. Rủi ro còn lại
- Trong môi trường thực tế, nếu ứng dụng Firewall chặn kết nối localhost (dù hiếm), hệ thống IPC sẽ thất bại.
- Nếu Child tốn hơn TTL (vd 5 phút) để tải thì server của Parent sẽ tự đóng. Sẽ cân nhắc tăng TTL lúc render JCEF thật.

## 11. Phase tiếp theo: 6D Child calls IPC in debug mode
Phase 6D sẽ ráp nối server IPC này vào vòng đời chuẩn bị thi của Parent Launcher, và cấu hình `TSEExamChildClient` để nó tự động parse meta, gọi IPC bằng `V2LoopbackKeyHandoffClient` và giải mã dữ liệu lấy package trả về.
