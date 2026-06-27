# Phase 6A: Secure Exam Child V2 Integration Plan

**Trạng thái:** Lập kế hoạch (Planning)
**Mục tiêu:** Phân tích, thiết kế kiến trúc và định ra lộ trình tích hợp V2 Handoff vào Secure Exam Child mà không làm gián đoạn luồng thi legacy.

## 1. Mục tiêu Phase 6A
Tạo bản thiết kế kỹ thuật cho việc xoá bỏ việc truyền AES Key qua command line, thay vào đó Child sẽ tự động fetch key từ Parent thông qua một kênh IPC an toàn sau khi khởi động. Luồng V2 sẽ chạy song song với luồng legacy thông qua boot arguments mới.

## 2. Current V2 Assets Summary
Hệ thống V2 hiện tại đã có:
- `v2_handoff_runtime.enc`: Chứa payload mã hoá.
- `v2_handoff_runtime.meta.json`: Chứa metadata, hash, và `handoffId`.
- `V2RuntimeKeyRegistry`: Hệ thống lưu trữ in-memory, thread-safe, single-consume, có TTL.
- `V2RuntimeHandoffReader`: Logic giải mã và kiểm tra mã độc từ JSON payload.

## 3. Current Child Launch Architecture
Luồng khởi chạy legacy:
1. `TSEProductionParentSubmitLabLauncher` sinh ra thư mục tạm.
2. Ghi `exam_context.enc` và `submit_payload.enc`.
3. Sinh AES Key (Base64).
4. Khởi chạy Rust `TutorHub_LockdownCore.exe` với cờ `--spawn-child` kèm theo `--context <path>`, `--output <path>`, và `--key <base64>`.
5. Rust khởi chạy Java Child `TSEExamChildClient` và chuyển tiếp các arguments này.
6. Child đọc `--key` từ command line và giải mã `--context`.

## 4. Parent -> Child Handoff Boundary
Vấn đề của legacy là AES key bị rò rỉ qua command line.
Trong luồng V2:
- Parent chỉ truyền đường dẫn của `.meta.json` cho Child.
- Cờ `--key` sẽ bị vô hiệu hoá hoặc không được cấp.
- Child phải tự giao tiếp ngược lại với Parent để đổi `handoffId` lấy AES key.

## 5. Key Handoff IPC Options
Làm thế nào để Child lấy key từ Parent?
- **Option 1: STDIN/STDOUT.** Parent ghi key vào STDIN của Rust, Rust pipe sang Child. *Nhược điểm:* Đòi hỏi sửa đổi Rust Lockdown (vi phạm ràng buộc của Phase).
- **Option 2: Named Pipes.** Dùng JNA tạo named pipe. *Nhược điểm:* Phức tạp trên Windows, dễ dính lỗi memory leak với JNA nếu không cẩn thận.
- **Option 3: Localhost Loopback HTTP Server.** Dùng `com.sun.net.httpserver.HttpServer` có sẵn trong JDK, chạy trên `127.0.0.1` với cổng ngẫu nhiên.

## 6. Recommended IPC Design
**Đề xuất: Localhost Loopback HTTP Server (REST IPC).**
- Rất nhẹ, có sẵn trong JDK, không cần thêm thư viện.
- An toàn vì chỉ bind vào `127.0.0.1`.
- **Luồng:**
  1. Parent tạo `HttpServer` tại cổng `0` (ngẫu nhiên).
  2. Parent lấy port thực tế và ghi vào trường `parentIpcPort` trong file `.meta.json`.
  3. Parent có 1 endpoint: `GET /v2/handoff/key?id=<handoffId>&nonce=<nonce>`.
  4. Child khởi động, đọc meta file, gọi HTTP GET tới Parent.
  5. Parent verify ID, lấy key từ `V2RuntimeKeyRegistry` (tiêu thụ 1 lần), trả về cho Child.
  6. Parent tự động shut down HttpServer sau 5 phút (TTL) hoặc ngay sau khi key được tiêu thụ.

## 7. Child Boot Mode Proposal
- Child sẽ hỗ trợ các flag mới để không xung đột với legacy:
  `--v2-handoff-meta <path>`
  `--v2-handoff-enc <path>`
  `--v2-debug-only`
- Nếu có `--v2-handoff-meta`, Child kích hoạt V2 Boot Mode.
- Nếu không có, hệ thống rơi về nhánh `--context` (Legacy Mode).

## 8. Child Reader Flow
1. Child khởi chạy, nhận diện `--v2-handoff-meta`.
2. Đọc file `meta.json`, extract `handoffId`, `parentIpcPort`, `encryptedFileSha256`, `nonce`.
3. Gọi HTTP GET `http://127.0.0.1:<parentIpcPort>/v2/handoff/key?id=<handoffId>`.
4. Nếu thất bại (hết TTL, key đã bị lấy), Child văng lỗi và đóng.
5. Nếu thành công, giữ AES key trong biến cục bộ.
6. Verify SHA256 của file `.enc`.
7. Decrypt file `.enc` trong bộ nhớ thành JSON.
8. Parse thành `V2ExamHandoffBundle`.

## 9. Renderer Integration Options
- **Legacy:** Đang dùng JCEF + DOM Injection.
- **V2 Debug Mode (Phase 6B-6D):** Đề xuất tái sử dụng `V2ExamPackageRenderer` (Swing JPanel) đã code từ Phase trước. Thay vì add `TSEBrowserPanel` (JCEF) vào JFrame, ta add `V2ExamPackageRenderer` vào JFrame. Điều này giúp dễ dàng render read-only toàn bộ package mà không cần đụng tới JCEF DOM/JS bridge phức tạp.
- Sau khi luồng V2 ổn định (Phase 6E+), ta mới phát triển JCEF HTML Renderer cho V2.

## 10. Submit/Autosave Out-of-Scope
Phase 6 hiện tại **KHÔNG** cover việc submit bài thi V2. V2 Boot Mode sẽ khoá/ẩn nút Nộp bài. Không sửa `EXAM_SUBMIT` hay ghi `submit_payload.enc`. Việc này sẽ được thiết kế riêng ở Phase sau.

## 11. Security Risks & Mitigations
- **Port Scanner Interception:** Một app độc hại trên máy có thể quét port và gọi endpoint. *Giải pháp:* Trong `meta.json` cấp thêm một `nonce` ngẫu nhiên. Chỉ gọi `GET` kèm đúng `handoffId` và `nonce` mới được trả key.
- **Child Crash / Double Launch:** Nếu Child crash và học sinh mở lại, `consumeKey` lần 2 sẽ fail vì key đã bị huỷ. Học sinh buộc phải mở lại từ Parent để cấp `handoffId` mới. Trùng khớp với luồng bảo mật tiêu chuẩn.

## 12. Backward Compatibility Requirements
- Không sửa file `.enc` cũ, không sửa hàm decrypt cũ của Child.
- Luồng `if (contextPath != null && keyB64 != null)` trong `TSEExamChildClient` phải được giữ nguyên hoàn toàn.

## 13. Proposed Phase 6B/6C/6D Roadmap
- **Phase 6B: Child V2 Debug Launch Skeleton** (Thêm parser args trong Child, chưa nối IPC).
- **Phase 6C: Parent-to-Child Key Request IPC Prototype** (Cài đặt Localhost Loopback HTTP Server trên Parent, HTTP Client trên Child).
- **Phase 6D: Child Decrypt + Render Read-Only V2 Package** (Ghép nối IPC với Reader và Render lên Swing Panel).
- **Phase 6E: JCEF Renderer & Selection State** (Chuẩn bị giao diện thi thực tế cho học sinh).
- **Phases 6F-6H:** Luồng Autosave và Submit V2.

## 14. Files Likely Affected in Future Phases
- `TSEProductionParentSubmitLabLauncher.java` (Khởi tạo HTTP Server, truyền flags mới cho Rust).
- `TSEExamChildClient.java` (Sửa hàm `main` để rẽ nhánh boot mode).
- `V2RuntimeHandoffMeta.java` (Thêm trường `parentIpcPort` và `nonce`).

## 15. Tests Required Before Any Real Integration
- Test IPC Server/Client độc lập (đảm bảo loopback hoạt động tốt, không dính firewall Windows).
- Đảm bảo `mvn clean install` vẫn pass.
- Đảm bảo Legacy `run_input_test.bat` vẫn không bị phá vỡ.

## 16. Go/No-Go Recommendation
**GO.** 
Giải pháp Loopback HTTP Server giải quyết triệt để vấn đề rò rỉ key qua command line mà không cần can thiệp vào Rust hay JNI phức tạp, đồng thời tích hợp `V2RuntimeKeyRegistry` cực kỳ an toàn. Đề xuất xin duyệt để chuyển sang Phase 6B.
