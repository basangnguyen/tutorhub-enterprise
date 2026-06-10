# Agent Instructions — TutorHub Secure Exam Mode

Bạn là AI coding agent hỗ trợ phát triển TutorHub Secure Exam Mode.

## Quy tắc bắt buộc

1. Luôn đọc toàn bộ thư mục `docs` trước khi code:
   - `docs/MASTER_SECURE_EXAM_BLUEPRINT_v4.md`
   - `docs/secure_exam_tasks_v2.md`
   - `docs/secure_exam_rust_and_seb_learning_sources_ONLY_3_DOCS.md`

2. Sau đó đọc thư mục `seb-reference` để nghiên cứu Safe Exam Browser v3.10.1.

3. Không code ngay sau khi đọc. Trước mỗi task, phải tạo kế hoạch ngắn gồm:
   - Mục tiêu task
   - File sẽ tạo/sửa
   - API hoặc module liên quan
   - Rủi ro kỹ thuật
   - Cách test

4. Chỉ bắt đầu code khi kế hoạch rõ ràng và bám sát tài liệu.

5. Phase hiện tại là **Phase 2: Rust Lockdown Core**.
   Không làm lan sang Phase 3 hoặc Phase 4.

6. Làm theo đúng thứ tự trong `MASTER_SECURE_EXAM_BLUEPRINT_v4.md` và `secure_exam_tasks_v2.md`.

   **Giai đoạn PoC bắt buộc:**
   - PoC `CreateDesktopW` + `SwitchDesktop` + `CloseDesktop` trên VM.
   - PoC `SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)` chặn OBS.
   - PoC Named Pipe server nhận `PING` → trả `PONG`.
   - PoC `SetWindowsHookExW(WH_KEYBOARD_LL)` chặn Win key.
   - PoC `CreateToolhelp32Snapshot` → liệt kê process.

   **Giai đoạn tạo Rust project và implement module:**
   - Tạo project `tutorhub_lockdown`.
   - Implement `error.rs`.
   - Implement `config.rs`.
   - Implement `logger.rs`.
   - Implement `ipc.rs`.
   - Implement `desktop.rs`.
   - Implement `screen_protection.rs`.
   - Implement `keyboard_hook.rs`.
   - Implement `process_scanner.rs`.
   - Implement `watchdog.rs`.
   - Implement `vm_detection.rs`.
   - Implement `main.rs`.

   **Giai đoạn build và tích hợp Java:**
   - Thêm Auto-kill timer 60s cho `debug_mode`.
   - Build release binary và kiểm tra dung lượng.
   - Implement `LockdownManager.java`.
   - Implement `RustIPCClient.java`.
   - Implement `EnvironmentChecker.java`.
   - Integration test: Java spawn Rust → `LOCK` → PING loop → `UNLOCK`.

7. Không chạy test lockdown thật trên máy chính.
   Các phần liên quan `CreateDesktopW`, `SwitchDesktop`, keyboard hook, screen protection phải được thiết kế/test theo hướng VM.

8. Sau mỗi task hoàn thành, cập nhật tiến độ vào:
   - `docs/secure_exam_tasks_v2.md`

9. Không dùng `unwrap()` hoặc `expect()` trong production code path.
   Dùng `Result<T, LockdownError>` và xử lý lỗi rõ ràng.

10. Không tự ý đổi kiến trúc nếu chưa ghi rõ:

- Lý do thay đổi
- Lợi ích
- Rủi ro
- Ảnh hưởng tới các module khác

11. Không xóa, rename hoặc di chuyển các file tài liệu trong `docs` nếu chưa được yêu cầu.

12. Không sửa trực tiếp mã nguồn trong `seb-reference`.
    Thư mục này chỉ dùng để nghiên cứu Safe Exam Browser v3.10.1 và chuyển hóa logic phù hợp sang Rust cho TutorHub.

13. Khi tham khảo sát logic từ SEB, phải ghi chú rõ module SEB liên quan và đảm bảo tuân thủ hướng dẫn license/MPL-2.0 trong tài liệu.

14. Sau mỗi task, báo cáo theo format:

- Đã tạo/sửa file nào
- Đã chạy lệnh gì
- Kết quả test
- Checklist nào đã cập nhật
- Rủi ro còn lại
- Task tiếp theo đề xuất
