# Secure Exam Rust & SEB Learning Sources for AI Agent

> **Phiên bản:** 2.1 — Đã đồng bộ theo đúng 3 file hiện có trong thư mục `docs`  
> **Mục tiêu:** Xác định bộ tài liệu nội bộ, nguồn học Rust, nguồn học Windows API và nguồn tham khảo Safe Exam Browser để AI Agent có thể chuẩn bị trước khi triển khai `TutorHub_LockdownCore.exe`.  
> **Phạm vi:** TutorHub Secure Exam Mode theo kiến trúc **Java Shell + Rust Core**.  
> **Mốc SEB tham khảo:** Safe Exam Browser **3.10.1 for Windows** / Build **3.10.1.864**.  
> **Ngôn ngữ triển khai TutorHub Lockdown Core:** Rust + `windows-rs`.  
> **License SEB tham chiếu:** Mozilla Public License 2.0. AI Agent được phép nghiên cứu sâu kiến trúc, thuật toán, luồng xử lý và logic kỹ thuật của SEB để chuyển hóa sang Rust cho TutorHub. Nếu có phần chuyển đổi rất sát từ mã nguồn SEB, phải ghi chú rõ nguồn tham chiếu và kiểm tra nghĩa vụ MPL-2.0 trước khi release.

---

> [!IMPORTANT]
> File này **không thay thế** `MASTER_SECURE_EXAM_BLUEPRINT_v4.md`.  
> File này chỉ là **Learning Sources & Research Guide** cho AI Agent trước khi code Phase 2 Rust Core.  
> Nếu có mâu thuẫn giữa các file, luôn ưu tiên `MASTER_SECURE_EXAM_BLUEPRINT_v4.md`.

---

## 1. Vai trò của file này trong thư mục `docs`

File này dùng để trả lời 4 câu hỏi chính:

1. AI Agent phải đọc những file nội bộ nào trước khi code?
2. AI Agent cần học Rust từ nguồn chính thống nào?
3. AI Agent cần học Windows API / `windows-rs` từ nguồn nào?
4. AI Agent cần nghiên cứu Safe Exam Browser ở bản nào, module nào và theo nguyên tắc nào?

File này phù hợp cho các AI Agent như:

```text
Claude Code
Codex
Cursor
Antigravity
Gemini CLI / AI coding agent tương tự
```

---

## 2. File nội bộ bắt buộc đọc trong thư mục `docs`

Theo cấu trúc thư mục hiện tại, `docs` chỉ còn 3 file sau:

```text
docs/
├── MASTER_SECURE_EXAM_BLUEPRINT_v4.md
├── secure_exam_tasks_v2.md
└── secure_exam_rust_and_seb_learning_sources_ONLY_3_DOCS.md
```

AI Agent phải đọc theo đúng thứ tự này:

| Thứ tự | File                                                       | Vai trò                                                          | Bắt buộc |
| ------ | ---------------------------------------------------------- | ---------------------------------------------------------------- | -------- |
| 1      | `MASTER_SECURE_EXAM_BLUEPRINT_v4.md`                       | Bản thiết kế chính / Single Source of Truth cho Secure Exam Mode | Bắt buộc |
| 2      | `secure_exam_tasks_v2.md`                                  | Checklist công việc, dùng để cập nhật tiến độ khi code           | Bắt buộc |
| 3      | `secure_exam_rust_and_seb_learning_sources_ONLY_3_DOCS.md` | File hiện tại: nguồn học Rust, Windows API và SEB cho AI Agent   | Bắt buộc |

> [!IMPORTANT]
> Nếu có mâu thuẫn giữa các file, luôn ưu tiên `MASTER_SECURE_EXAM_BLUEPRINT_v4.md`.  
> `secure_exam_tasks_v2.md` chỉ dùng để theo dõi tiến độ triển khai.  
> File hiện tại chỉ dùng để định hướng nguồn học và cách nghiên cứu trước khi code.

## 3. Quyết định kiến trúc hiện tại

TutorHub **không fork và không sửa trực tiếp Safe Exam Browser**. TutorHub tự phát triển Secure Exam Mode riêng theo kiến trúc:

```text
TutorHub Desktop App
= Java Swing / JCEF UI
+ Rust Lockdown Core
+ Named Pipe IPC
+ SQLite Offline Cache
+ PostgreSQL Server DB
```

Phase 2 Lockdown Core được chốt dùng:

```text
Rust + windows-rs
```

Không dùng các hướng cũ sau làm lõi chính:

```text
Không dùng JNA làm lõi lockdown.
Không dùng JNativeHook làm hướng chính.
Không dùng C++ Watchdog.
Không dùng Java Watchdog chạy cùng JVM làm lớp bảo vệ chính.
Không kill Explorer.exe làm phương án mặc định.
```

### 3.1. Nguyên tắc học từ SEB

AI Agent được phép:

```text
- Nghiên cứu sâu mã nguồn SEB Windows 3.x.
- Đọc kỹ module, interface, lifecycle, operation flow, error handling, recovery logic.
- Học cách SEB tổ chức Runtime, Client, Service, Communication, Lockdown, Monitoring, Configuration, Integrity.
- Chuyển hóa logic, thuật toán và hành vi hệ thống của SEB sang Rust cho TutorHub.
- Thiết kế module Rust có hành vi tương đương với các module SEB nếu phù hợp.
```

AI Agent phải cẩn thận:

```text
- Không dán nguyên văn code SEB vào TutorHub nếu chưa kiểm tra nghĩa vụ license.
- Không dịch 1-1 từng file hoặc từng class SEB sang Rust mà không ghi chú nguồn tham chiếu.
- Nếu phần nào chuyển đổi rất sát từ SEB, phải đánh dấu rõ trong comment/tài liệu.
- Trước khi release sản phẩm, cần kiểm tra nghĩa vụ Mozilla Public License 2.0.
```

Cách hiểu đúng:

```text
Được học rất sát SEB.
Được chuyển hóa logic SEB sang Rust.
Được tạo Rust implementation tương đương hành vi.
Nhưng mọi phần chuyển đổi sát cần được ghi chú và kiểm tra license trước khi dùng chính thức.
```

---

## 4. Mốc Safe Exam Browser nên tham khảo

| Hạng mục                      | Quyết định                            |
| ----------------------------- | ------------------------------------- |
| Dòng SEB nên nghiên cứu       | Safe Exam Browser for Windows 3.x     |
| Mốc ổn định nên tham khảo     | SEB 3.10.1 for Windows                |
| Build tham khảo               | 3.10.1.864                            |
| Repository                    | `SafeExamBrowser/seb-win-refactoring` |
| License                       | Mozilla Public License 2.0            |
| Browser engine                | Chromium                              |
| Công nghệ SEB gốc             | C# / .NET                             |
| Công nghệ TutorHub triển khai | Java Swing/JCEF + Rust Core           |

### 4.1. Vì sao chọn SEB 3.10.1?

SEB 3.10.1 là mốc ổn định phù hợp để nghiên cứu kiến trúc SEB Windows 3.x. Các điểm đáng học cho TutorHub:

```text
- Runtime lifecycle.
- Lockdown / monitoring architecture.
- Create New Desktop / kiosk mode.
- Application monitoring.
- Browser Exam Key.
- Integrity verification.
- Virtual machine detection.
- System hardware verification.
- Configuration model.
- Communication giữa các process.
- Error recovery và fail-safe behavior.
```

### 4.2. Vì sao không dùng SEB 2.x?

```text
SEB 2.x là legacy, không sát với SEB Windows 3.x dùng Chromium.
TutorHub nên học kiến trúc từ seb-win-refactoring, tức dòng SEB Windows 3.x.
```

### 4.3. Vì sao không lấy 3.10.2 làm mốc chính?

```text
Nếu 3.10.2 đang ở trạng thái Release Candidate hoặc chưa ổn định trên trang download chính thức,
không dùng làm mốc production/reference chính.
Dùng 3.10.1 làm mốc ổn định để AI Agent không bị lệch tài liệu.
```

---

## 5. Vị trí mã nguồn SEB trong dự án

Mã nguồn SEB nên để ngoài `docs`, ngang cấp với `docs`:

```text
D:\Ban_sao_du_an\seb-reference\
```

Cấu trúc tổng thể nên là:

```text
D:\Ban_sao_du_an
├── docs\
│   ├── MASTER_SECURE_EXAM_BLUEPRINT_v4.md
│   ├── secure_exam_tasks_v2.md
│   └── secure_exam_rust_and_seb_learning_sources_ONLY_3_DOCSsecure_exam_rust_and_seb_learning_sources_COMPLETED.md
└── seb-reference\
    └── SafeExamBrowser source code checked out at v3.10.1
```

> [!NOTE]
> `seb-reference` là mã nguồn tham khảo kỹ thuật. Không đặt SEB source vào `docs` để tránh lẫn giữa tài liệu nội bộ TutorHub và source tham chiếu bên ngoài.

---

## 6. Tài liệu Rust bắt buộc đọc trước khi code Rust Core

| Ưu tiên | Tài liệu                      | Link                                          | Lý do giữ lại                                                                 |
| ------- | ----------------------------- | --------------------------------------------- | ----------------------------------------------------------------------------- |
| 1       | The Rust Programming Language | https://doc.rust-lang.org/book/               | Nền tảng Rust: ownership, borrowing, error handling, module, concurrency      |
| 2       | The Cargo Book                | https://doc.rust-lang.org/cargo/              | `Cargo.toml`, dependency, feature flags, debug/release build                  |
| 3       | Rust Standard Library         | https://doc.rust-lang.org/std/                | `std::process`, `std::thread`, `std::time`, `std::sync`, `std::fs`, `std::io` |
| 4       | The Rustonomicon              | https://doc.rust-lang.org/nomicon/            | Unsafe Rust, FFI, raw pointer, layout, invariants                             |
| 5       | The Rust Reference            | https://doc.rust-lang.org/reference/          | Tra cứu chính xác cú pháp, type system, ABI, unsafe semantics                 |
| 6       | Clippy Book                   | https://doc.rust-lang.org/clippy/             | Lint code trước khi merge                                                     |
| 7       | Rustfmt / Style Guide         | https://doc.rust-lang.org/stable/style-guide/ | Giữ code thống nhất, dễ review                                                |

### 6.1. Trọng tâm Rust cần học cho dự án này

AI Agent không cần học Rust chung chung quá rộng. Cần tập trung vào:

```text
- Ownership / borrowing trong code systems-level.
- Result<T, E> và error propagation bằng `?`.
- Custom error enum: LockdownError.
- Module system: tách `main.rs`, `ipc.rs`, `desktop.rs`, `watchdog.rs`, ...
- Arc<Mutex<T>> cho shared state giữa thread.
- std::thread và message loop.
- FFI / unsafe block khi gọi Win32 API.
- Trait để đóng gói Windows API wrapper nếu cần mock/test.
- Unit test và integration test bằng `cargo test`.
- Feature flag `debug_mode` cho Auto-Kill Timer và Panic Key.
```

### 6.2. Nguồn không bắt buộc

| Tài liệu           | Quyết định     | Lý do                                                     |
| ------------------ | -------------- | --------------------------------------------------------- |
| Rust by Example    | Chỉ tham khảo  | Tốt cho học nhanh, nhưng không đủ cho systems-level code  |
| Rustlings          | Không bắt buộc | Phù hợp người mới học Rust, không cần ép AI Agent đọc     |
| Rust Playground    | Không bắt buộc | Dùng thử snippet nhỏ, không liên quan trực tiếp đến dự án |
| Embedded Rust Book | Không cần      | Dự án là Windows desktop, không phải embedded             |

---

## 7. Tài liệu Rust + Windows API bắt buộc

| Ưu tiên | Tài liệu                    | Link                                                                      | Dùng cho module                                    |
| ------- | --------------------------- | ------------------------------------------------------------------------- | -------------------------------------------------- |
| 1       | Microsoft windows-rs GitHub | https://github.com/microsoft/windows-rs                                   | Nguồn chính cho crate `windows` và `windows-sys`   |
| 2       | windows-rs API Docs         | https://microsoft.github.io/windows-docs-rs/                              | Tra cứu namespace, function, type trong Rust       |
| 3       | windows crate               | https://crates.io/crates/windows                                          | Binding tương đối an toàn, phù hợp gọi Win32/WinRT |
| 4       | windows-sys crate           | https://crates.io/crates/windows-sys                                      | Binding cấp thấp hơn, nhẹ hơn, phù hợp exe nhỏ     |
| 5       | Windows API Index           | https://learn.microsoft.com/en-us/windows/win32/apiindex/windows-api-list | Tra cứu Win32 API gốc                              |

### 7.1. Khuyến nghị crate

```text
Ưu tiên dùng `windows` crate cho bản PoC vì API dễ đọc hơn.
Khi cần tối ưu dung lượng hoặc kiểm soát ABI sâu hơn, cân nhắc `windows-sys`.
Không tự ý thêm crate lạ nếu chưa có lý do rõ ràng.
```

### 7.2. Quy tắc dùng `unsafe`

```text
- Mỗi Windows API call phải được wrap trong hàm Rust riêng.
- Mỗi unsafe block phải có comment giải thích điều kiện an toàn.
- Không để unsafe lan rộng ra toàn module nếu chỉ một dòng cần unsafe.
- Không dùng unwrap()/expect() trong production code path.
- Luôn trả về Result<_, LockdownError>.
```

---

## 8. Microsoft Learn — API sát với TutorHub Lockdown Core

| TutorHub module        | API / Chủ đề                 | Link                                                                                                          |
| ---------------------- | ---------------------------- | ------------------------------------------------------------------------------------------------------------- |
| `desktop.rs`           | Window Stations and Desktops | https://learn.microsoft.com/en-us/windows/win32/winstation/window-stations-and-desktops                       |
| `desktop.rs`           | CreateDesktopW               | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-createdesktopw                         |
| `desktop.rs`           | OpenDesktopW                 | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-opendesktopw                           |
| `desktop.rs`           | SwitchDesktop                | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-switchdesktop                          |
| `desktop.rs`           | CloseDesktop                 | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-closedesktop                           |
| `desktop.rs`           | GetThreadDesktop             | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getthreaddesktop                       |
| `desktop.rs`           | SetThreadDesktop             | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setthreaddesktop                       |
| `screen_protection.rs` | SetWindowDisplayAffinity     | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowdisplayaffinity               |
| `screen_protection.rs` | EnumWindows                  | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-enumwindows                            |
| `screen_protection.rs` | GetWindowThreadProcessId     | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getwindowthreadprocessid               |
| `keyboard_hook.rs`     | SetWindowsHookExW            | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-setwindowshookexw                      |
| `keyboard_hook.rs`     | LowLevelKeyboardProc         | https://learn.microsoft.com/en-us/windows/win32/winmsg/lowlevelkeyboardproc                                   |
| `keyboard_hook.rs`     | GetMessageW                  | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-getmessage                             |
| `keyboard_hook.rs`     | PostThreadMessageW           | https://learn.microsoft.com/en-us/windows/win32/api/winuser/nf-winuser-postthreadmessagew                     |
| `ipc.rs`               | Named Pipes overview         | https://learn.microsoft.com/en-us/windows/win32/ipc/named-pipes                                               |
| `ipc.rs`               | CreateNamedPipeW             | https://learn.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-createnamedpipew                       |
| `ipc.rs`               | ConnectNamedPipe             | https://learn.microsoft.com/en-us/windows/win32/api/namedpipeapi/nf-namedpipeapi-connectnamedpipe             |
| `ipc.rs`               | DisconnectNamedPipe          | https://learn.microsoft.com/en-us/windows/win32/api/namedpipeapi/nf-namedpipeapi-disconnectnamedpipe          |
| `ipc.rs`               | CreateFileW                  | https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-createfilew                            |
| `ipc.rs`               | ReadFile                     | https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-readfile                               |
| `ipc.rs`               | WriteFile                    | https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-writefile                              |
| `watchdog.rs`          | WaitForSingleObject          | https://learn.microsoft.com/en-us/windows/win32/api/synchapi/nf-synchapi-waitforsingleobject                  |
| `watchdog.rs`          | GetExitCodeProcess           | https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-getexitcodeprocess |
| `process_scanner.rs`   | Tool Help Library            | https://learn.microsoft.com/en-us/windows/win32/toolhelp/tool-help-library                                    |
| `process_scanner.rs`   | CreateToolhelp32Snapshot     | https://learn.microsoft.com/en-us/windows/win32/api/tlhelp32/nf-tlhelp32-createtoolhelp32snapshot             |
| `process_scanner.rs`   | Process32FirstW              | https://learn.microsoft.com/en-us/windows/win32/api/tlhelp32/nf-tlhelp32-process32firstw                      |
| `process_scanner.rs`   | Process32NextW               | https://learn.microsoft.com/en-us/windows/win32/api/tlhelp32/nf-tlhelp32-process32nextw                       |
| `process_scanner.rs`   | QueryFullProcessImageNameW   | https://learn.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-queryfullprocessimagenamew             |
| `vm_detection.rs`      | GetAdaptersAddresses         | https://learn.microsoft.com/en-us/windows/win32/api/iphlpapi/nf-iphlpapi-getadaptersaddresses                 |

---

## 9. Safe Exam Browser — nguồn học kiến trúc và logic

| Nguồn                             | Link                                                                        | Dùng để học                                       |
| --------------------------------- | --------------------------------------------------------------------------- | ------------------------------------------------- |
| SEB Official Website              | https://safeexambrowser.org/                                                | Tổng quan, tài liệu, download, release note       |
| SEB Latest Download Page          | https://safeexambrowser.org/download_en.html                                | Xác định bản ổn định chính thức mới nhất          |
| SEB Windows 3.10.1 GitHub Release | https://github.com/SafeExamBrowser/seb-win-refactoring/releases/tag/v3.10.1 | Mốc release ổn định nên tham khảo                 |
| SEB Windows User Manual           | https://safeexambrowser.org/windows/win_usermanual_en.html                  | Cách SEB vận hành ở phía người dùng Windows       |
| SEB Windows Source Code           | https://github.com/SafeExamBrowser/seb-win-refactoring                      | Học kiến trúc SEB Windows 3.x                     |
| SEB Server Source Code            | https://github.com/SafeExamBrowser/seb-server                               | Tham khảo cách SEB Server quản lý kỳ thi/cấu hình |
| SEB Configuration Tool            | https://safeexambrowser.org/windows/win_usermanual_en.html#configuration    | Tham khảo tư duy cấu hình `.seb`                  |

### 9.1. Module SEB nên nghiên cứu

| Module SEB                             | Lý do học                                                 | TutorHub tương ứng                  | Cách chuyển hóa sang Rust                                       |
| -------------------------------------- | --------------------------------------------------------- | ----------------------------------- | --------------------------------------------------------------- |
| `SafeExamBrowser.Runtime`              | Điều phối lifecycle, operation sequence, startup/shutdown | `LockdownManager.java` + `main.rs`  | Chuyển thành Rust startup sequence + Java lifecycle manager     |
| `SafeExamBrowser.Client`               | Client lifecycle, session logic                           | `ExamTakingPanel.java`              | Học flow, không cần chuyển hết sang Rust                        |
| `SafeExamBrowser.Service`              | Service / watchdog role                                   | `watchdog.rs`, `ipc.rs`             | Chuyển logic heartbeat/recovery sang Rust process riêng         |
| `SafeExamBrowser.Lockdown`             | Kiosk/lockdown, desktop isolation                         | `desktop.rs`, `keyboard_hook.rs`    | Chuyển logic tương đương sang `CreateDesktopW`, `SwitchDesktop` |
| `SafeExamBrowser.WindowsApi`           | Đóng gói Win32 API                                        | Rust `windows-rs` wrappers          | Tạo wrapper an toàn bằng Result + LockdownError                 |
| `SafeExamBrowser.WindowsApi.Contracts` | Interface/contract cho API layer                          | Rust trait/interface nếu cần        | Dùng trait để test/mocking nếu phù hợp                          |
| `SafeExamBrowser.Monitoring`           | Theo dõi process/window/session                           | `process_scanner.rs`, `watchdog.rs` | Chuyển logic monitoring sang scanner thread                     |
| `SafeExamBrowser.Communication`        | Giao tiếp giữa process/module                             | `ipc.rs`                            | Chuyển WCF Named Pipe thành raw Named Pipe text protocol        |
| `SafeExamBrowser.Configuration`        | Quản lý cấu hình thi                                      | `config.rs`                         | Chuyển `.seb` config concept thành JSON config từ server        |
| `SafeExamBrowser.Browser`              | Browser lockdown logic                                    | JCEF Exam Browser                   | Học behavior: chặn popup, DevTools, navigation                  |
| `SafeExamBrowser.Integrity` / BEK      | Browser Exam Key, integrity                               | TutorHub TEK Hash + HMAC            | Học ý tưởng BEK, thiết kế TEK riêng                             |
| `SafeExamBrowser.Logging`              | Structured logging                                        | `logger.rs`                         | Chuyển thành append-only file log theo session                  |

### 9.2. Điểm cần học riêng từ SEB 3.10.1

| Chủ đề                                | Vì sao quan trọng với TutorHub                          |
| ------------------------------------- | ------------------------------------------------------- |
| OperationSequence / Runtime lifecycle | Giúp Lockdown Core startup/shutdown có rollback rõ ràng |
| Virtual machine detection             | Liên quan module `vm_detection.rs`                      |
| System hardware verification          | Tham khảo cho pre-check/integrity                       |
| Application monitoring signature      | Liên quan process scanner/hash-based detection          |
| Browser Exam Key platform-specific    | Cảnh báo quan trọng khi thiết kế TEK Hash               |
| Kiosk mode / Create New Desktop       | Sát với Rust `CreateDesktopW`                           |
| Display / screen monitoring           | Sát với screen protection và proctoring                 |
| Configuration model                   | TutorHub có thể đơn giản hóa bằng JSON config từ server |
| Logging + recovery                    | Quan trọng khi Rust/Java crash                          |

### 9.3. Nguyên tắc chuyển hóa SEB sang Rust

```text
SEB Windows chủ yếu viết bằng C#/.NET, không phải Rust.
AI Agent cần đọc SEB để hiểu hành vi và logic kỹ thuật.
Sau đó chuyển hóa sang Rust theo kiến trúc TutorHub, không bắt buộc giữ nguyên cấu trúc class/module của SEB.
```

Quy tắc khi chuyển hóa:

```text
1. Ưu tiên hành vi tương đương, không ưu tiên giữ nguyên code shape.
2. Module Rust phải idiomatic: Result, ownership, module boundary rõ ràng.
3. Unsafe Win32 API phải được bọc trong wrapper an toàn.
4. Nếu logic chuyển đổi rất sát từ SEB, ghi chú nguồn tham chiếu.
5. Không dùng tên file/class/function của SEB nếu không cần thiết.
6. Không đưa nguyên comment/header/license block của SEB vào file Rust nếu chưa xác định nghĩa vụ license.
7. Trước khi release, review license MPL-2.0 cho mọi phần tham chiếu sát.
```

---

## 10. Nguồn nên loại khỏi file học chính

| Nguồn                                 | Quyết định               | Lý do                                             |
| ------------------------------------- | ------------------------ | ------------------------------------------------- |
| Blog cá nhân không chính thống        | Loại                     | Dễ lỗi thời, dễ lệch kiến trúc                    |
| StackOverflow snippets                | Không đưa vào docs       | Chỉ dùng khi debug lỗi cụ thể                     |
| Crate lạ chưa phổ biến                | Không đưa vào docs       | Tránh tăng rủi ro dependency                      |
| Tutorial keyboard hook không rõ nguồn | Loại                     | Dễ chứa pattern nguy hiểm/lỗi thời                |
| JNA tutorial                          | Loại khỏi Phase 2        | Master Blueprint đã chốt Rust Core                |
| C++ watchdog tutorial                 | Loại                     | Master Blueprint đã chốt Rust Watchdog            |
| SEB 2.x repo `seb-win`                | Không dùng làm mốc chính | SEB 2.x là legacy, không sát với Chromium/SEB 3.x |
| Code snippet copy-paste từ forum      | Loại                     | Dễ lỗi, khó kiểm soát license và bảo mật          |

---

## 11. Cách tải/lưu tài liệu vào dự án

### 11.1. Lưu file learning sources

File này nên nằm tại:

```text
D:\Ban_sao_du_an\docs\secure_exam_rust_and_seb_learning_sources_COMPLETED.md
```

### 11.2. Tải tài liệu Rust offline bằng rustup

```powershell
rustup component add rust-docs
rustup doc --book
rustup doc --std
rustup doc --cargo
```

### 11.3. Clone đúng source SEB Windows 3.x

Vì `seb-reference` nằm ngoài `docs`, dùng lệnh:

```powershell
cd D:\Ban_sao_du_an
git clone https://github.com/SafeExamBrowser/seb-win-refactoring.git seb-reference
cd seb-reference
git fetch --tags
git checkout v3.10.1
```

Nếu chỉ muốn tải nhanh không cần lịch sử Git đầy đủ:

```powershell
cd D:\Ban_sao_du_an
git clone --depth 1 --branch v3.10.1 https://github.com/SafeExamBrowser/seb-win-refactoring.git seb-reference
```

### 11.4. Kiểm tra version SEB source đã tải

```powershell
cd D:\Ban_sao_du_an\seb-reference
git describe --tags
git log -1 --oneline
git branch --show-current
```

Kỳ vọng:

```text
v3.10.1
```

---

## 12. Cách AI Agent nên nghiên cứu trước khi code

AI Agent nên làm theo thứ tự:

```text
Bước 1: Đọc MASTER_SECURE_EXAM_BLUEPRINT_v4.md.
Bước 2: Đọc secure_exam_tasks_v2.md để biết task hiện tại.
Bước 3: Đọc  để biết quy tắc test an toàn.
Bước 4: Đọc file learning sources này.
Bước 5: Mở seb-reference và nghiên cứu module SEB liên quan.
Bước 6: Ghi lại mapping SEB module → TutorHub Rust module nếu cần.
Bước 7: Chỉ bắt đầu code khi đã hiểu module cần làm và tiêu chí hoàn thành.
```

### 12.1. Thứ tự học SEB theo module

```text
1. SafeExamBrowser.Runtime
2. SafeExamBrowser.Communication
3. SafeExamBrowser.Lockdown
4. SafeExamBrowser.WindowsApi
5. SafeExamBrowser.Monitoring
6. SafeExamBrowser.Configuration
7. SafeExamBrowser.Integrity
8. SafeExamBrowser.Browser
9. SafeExamBrowser.Service
```

### 12.2. Thứ tự code Rust Phase 2

```text
1. error.rs
2. config.rs
3. logger.rs
4. ipc.rs
5. desktop.rs
6. screen_protection.rs
7. keyboard_hook.rs
8. process_scanner.rs
9. watchdog.rs
10. vm_detection.rs
11. main.rs
12. Java integration: LockdownManager.java + RustIPCClient.java
```

---

## 13. Prompt chuẩn cho AI Agent

```text
Bạn là Rust Systems Programmer cho dự án TutorHub Secure Exam Mode.

Trước khi code Phase 2, hãy đọc các file nội bộ trong thư mục docs theo thứ tự:
1. docs/MASTER_SECURE_EXAM_BLUEPRINT_v4.md
2. docs/secure_exam_tasks_v2.md
3. docs/secure_exam_rust_and_seb_learning_sources_COMPLETED.md

Sau đó nghiên cứu mã nguồn SEB tham khảo tại:
- seb-reference/

Hãy nghiên cứu các nguồn chính thống trong file learning sources:
1. The Rust Programming Language
2. The Cargo Book
3. Rust Standard Library
4. The Rustonomicon
5. The Rust Reference
6. Microsoft windows-rs
7. windows-rs API Docs
8. Microsoft Learn Win32 APIs
9. Safe Exam Browser Windows 3.10.1 source code

Mục tiêu:
- Học Rust + Windows API đủ để triển khai TutorHub_LockdownCore.exe.
- Nghiên cứu sâu kiến trúc, module, lifecycle, thuật toán và logic kỹ thuật của SEB 3.10.1.
- Chuyển hóa logic phù hợp từ SEB sang Rust theo kiến trúc Java Shell + Rust Core của TutorHub.
- Nếu có phần chuyển đổi rất sát từ SEB, ghi chú rõ nguồn tham chiếu và kiểm tra nghĩa vụ MPL-2.0 trước khi release.

Ràng buộc bắt buộc:
- Không dùng JNA làm lõi lockdown.
- Không dùng JNativeHook làm hướng chính.
- Không dùng C++ Watchdog.
- Rust Core là process riêng.
- Giao tiếp Java ↔ Rust qua Named Pipe.
- Test CreateDesktop / DisplayAffinity / Hook / Watchdog chỉ trên VM.
- DEBUG build phải có Auto-Kill 60s và Dev Panic Key.
- Mỗi Windows API call phải wrap bằng Result<_, LockdownError>.
- Không dùng unwrap()/expect() trong production code path.
- Mọi tiến độ phải cập nhật vào docs/secure_exam_tasks_v2.md.

Khi code:
- Code từng module nhỏ.
- Viết test tối thiểu cho config, ipc, process scanner logic.
- Log đầy đủ sự kiện quan trọng.
- Không bỏ qua fail-safe và emergency unlock.
```

---

## 14. Cấu trúc thư mục hiện tại nên giữ

```text
D:\Ban_sao_du_an
├── docs\
│   ├── MASTER_SECURE_EXAM_BLUEPRINT_v4.md
│   ├── secure_exam_tasks_v2.md
│   └── secure_exam_rust_and_seb_learning_sources_COMPLETED.md
└── seb-reference\
    └── SafeExamBrowser source code checked out at v3.10.1
```

---

## 15. Checklist hoàn thiện file này

- [x] Xác định rõ vai trò file: learning sources, không phải blueprint chính.
- [x] Đồng bộ với kiến trúc Java Shell + Rust Core.
- [x] Giữ SEB 3.10.1 làm mốc tham khảo chính.
- [x] Bổ sung nguồn học Rust chính thống.
- [x] Bổ sung nguồn học Windows API / `windows-rs`.
- [x] Bổ sung module SEB cần nghiên cứu.
- [x] Chỉnh lại định hướng: được nghiên cứu sâu và chuyển hóa logic SEB sang Rust.
- [x] Giữ cảnh báo license MPL-2.0 cho phần chuyển đổi sát.
- [x] Bổ sung prompt chuẩn cho AI Agent.
- [x] Bổ sung thứ tự nghiên cứu và thứ tự code Phase 2.

---

> **Kết luận:**  
> File này đang được giữ trong `docs` với tên `secure_exam_rust_and_seb_learning_sources_COMPLETED.md`.  
> AI Agent phải đọc file này sau Master Blueprint và trước khi bắt đầu code Rust Phase 2.
