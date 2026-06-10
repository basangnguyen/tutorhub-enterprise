# 🏛️ TutorHub Secure Exam Mode — Master Technical Specification v4.0

> **Phiên bản:** 4.0 — Đã đối chiếu với SEB 3.10.1 & hoàn thiện cho AI Agent  
> **Ngày:** 2026-06-06  
> **Tác giả:** Principal System Architect (Claude Sonnet 4.6)  
> **Nguồn gốc:** Tổng hợp từ các tài liệu hiện tại trong thư mục docs + nghiên cứu SEB `seb-win-refactoring` v3.10.1  
> **License SEB tham chiếu:** Mozilla Public License 2.0 — Được phép nghiên cứu sâu kiến trúc, luồng xử lý, thuật toán và logic kỹ thuật của SEB để chuyển hóa sang Rust cho TutorHub; nếu có phần chuyển đổi sát từ mã nguồn SEB, phải ghi chú rõ nguồn tham chiếu và kiểm tra nghĩa vụ MPL-2.0 trước khi release.  
> **Mục tiêu tài liệu:** AI Agent (Claude Code, Cursor, Codex) đọc là code được ngay

---

> [!IMPORTANT]
> **Tài liệu này là NGUỒN SỰ THẬT DUY NHẤT (Single Source of Truth).**
> Mọi quyết định kiến trúc đều được ghi rõ lý do. Không cần hỏi lại về các quyết định đã chốt.
> Chỉ hỏi khi gặp edge case không có trong tài liệu này.

---

## Mục lục

1. [Tổng quan Dự án & Trạng thái Hiện tại](#1-tổng-quan-dự-án--trạng-thái-hiện-tại)
2. [Kiến trúc Tổng thể](#2-kiến-trúc-tổng-thể)
3. [Cấu trúc Thư mục Đề xuất](#3-cấu-trúc-thư-mục-đề-xuất)
4. [Mapping SEB → TutorHub (Chi tiết)](#4-mapping-seb--tutorhub-chi-tiết)
5. [Luồng Hoạt động Chính](#5-luồng-hoạt-động-chính)
6. [Luồng Khởi động Phiên Thi (Startup Flow)](#6-luồng-khởi-động-phiên-thi)
7. [Luồng Kết thúc Phiên Thi (Shutdown Flow)](#7-luồng-kết-thúc-phiên-thi)
8. [IPC Protocol: Java ↔ Rust (Đặc tả Chi tiết)](#8-ipc-protocol-java--rust)
9. [Module Rust Core — Đặc tả Kỹ thuật](#9-module-rust-core--đặc-tả-kỹ-thuật)
10. [Cơ chế Khóa Môi trường Thi](#10-cơ-chế-khóa-môi-trường-thi)
11. [Cơ chế Quản lý Cấu hình](#11-cơ-chế-quản-lý-cấu-hình)
12. [Cơ chế Kiểm soát Tiến trình](#12-cơ-chế-kiểm-soát-tiến-trình)
13. [Cơ chế Bảo mật & Chống Can thiệp](#13-cơ-chế-bảo-mật--chống-can-thiệp)
14. [Watchdog & Fail-safe Matrix](#14-watchdog--fail-safe-matrix)
15. [Yêu cầu An toàn Dev (Safety Constraints)](#15-yêu-cầu-an-toàn-dev)
16. [Build Pipeline & Deployment](#16-build-pipeline--deployment)
17. [Database Schema (PostgreSQL)](#17-database-schema)
18. [API Packets Client-Server (TCP)](#18-api-packets-client-server)
19. [Defense in Depth — 6 Lớp Bảo vệ](#19-defense-in-depth--6-lớp-bảo-vệ)
20. [Checklist Triển khai cho AI Agent](#20-checklist-triển-khai-cho-ai-agent)
21. [Tiêu chí Hoàn thành (Definition of Done)](#21-tiêu-chí-hoàn-thành)
22. [Rủi ro Kỹ thuật & Xử lý](#22-rủi-ro-kỹ-thuật--xử-lý)
23. [Những gì KHÔNG làm ở Phase 2](#23-những-gì-không-làm-ở-phase-2)
24. [Prompt chuẩn cho AI Agent](#24-prompt-chuẩn-cho-ai-agent)

---

## 1. Tổng quan Dự án & Trạng thái Hiện tại

### 1.1. Bối cảnh Dự án

TutorHub là Desktop App học trực tuyến viết bằng **Java (Swing + JCEF)**. Giao tiếp Client-Server qua **TCP Socket** dùng hệ thống `Packet` tự định nghĩa. Lưu trữ: **PostgreSQL (Neon)** phía server, **SQLite** phía client.

Mục tiêu module này: xây dựng **Secure Exam Mode** — thi trực tuyến chống gian lận ở cấp OS, ngang SEB, tích hợp sẵn trong TutorHub (không cài thêm phần mềm).

### 1.2. Nguyên tắc Thiết kế Cốt lõi

| # | Nguyên tắc | Hiện thực hóa |
|---|---|---|
| 1 | **All-in-one** | Học sinh chỉ dùng 1 app TutorHub |
| 2 | **Defense in Depth** | 6 lớp bảo vệ độc lập |
| 3 | **Fail-safe First** | Watchdog tự giải phóng máy khi crash |
| 4 | **Privacy by Design** | AI chạy 100% on-device, không stream video |
| 5 | **Offline-first** | SQLite lưu nháp, không mất bài khi mất mạng |
| 6 | **Rust Safety** | Tận dụng ownership + Result type tránh crash |

### 1.3. Trạng thái Triển khai Hiện tại

| Hạng mục | Trạng thái | Ghi chú |
|---|---|---|
| PostgreSQL schema (exams, questions, sessions, answers, anticheat_events) | ✅ Đã tạo | Neon DB |
| ExamDAO.java (CRUD cơ bản) | ✅ Đã có | Cần bổ sung submitExam |
| ExamService.java | ✅ Có stub | handleSubmitExam chưa implement |
| ExamTakingPanel.java | ✅ Có UI | Chưa tích hợp Rust IPC |
| ExamCreatorPanel.java | ✅ Có UI | — |
| OfflineExamCache.java | ✅ Có | Cần kiểm tra sync logic |
| ClientHandler.java | ❌ Thiếu | Chưa map EXAM_START_REQUEST, EXAM_SUBMIT, EXAM_SYNC_DRAFT |
| Rust Lockdown Core | ❌ Chưa bắt đầu | **Mục tiêu Phase 2** |
| Edge AI (ONNX) | ❌ Chưa bắt đầu | Phase 4 |

### 1.4. Lộ trình Tổng quan

```
Phase 1 — MVP Exam Logic       (Java)      [ĐANG LÀM]   ~15 tuần
Phase 2 — OS Lockdown Core     (Rust)      [TIẾP THEO]  ~13 tuần
Phase 3 — Proctoring/Integrity (Java)      [SAU ĐÓ]     ~10 tuần
Phase 4 — Edge AI + Scale      (Java/ONNX) [TƯƠNG LAI]  ~20 tuần
```

---

## 2. Kiến trúc Tổng thể

### 2.1. Kiến trúc Hybrid: Java Shell + Rust Core

```
┌──────────────────────────────────────────────────────────────────┐
│                    TutorHub Desktop App (Host Process)            │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                  JAVA SHELL (UI + Logic Layer)              │  │
│  │                                                             │  │
│  │   ExamTakingPanel   ExamCreatorPanel   ProctorDashboard    │  │
│  │       (JCEF)           (Swing)            (Swing)          │  │
│  │                                                             │  │
│  │   LockdownManager   RustIPCClient    EnvironmentChecker    │  │
│  │   (điều phối Rust)  (Named Pipe)     (pre-exam check)      │  │
│  │                                                             │  │
│  │   OfflineExamCache  ExamService      IntegrityVerifier     │  │
│  │      (SQLite)       (Business)        (TEK Hash)           │  │
│  └──────────────────────────┬──────────────────────────────────┘  │
│                             │                                      │
│              Named Pipe IPC │ \\.\pipe\TutorHubExam               │
│         (PING mỗi 2s)       │ (newline-delimited UTF-8)           │
│                             │                                      │
│  ┌──────────────────────────▼──────────────────────────────────┐  │
│  │           RUST CORE (OS Security Layer)                     │  │
│  │           TutorHub_LockdownCore.exe  (~3MB)                 │  │
│  │           Process riêng, độc lập với JVM                    │  │
│  │                                                             │  │
│  │   desktop.rs         keyboard_hook.rs   screen_protection.rs│  │
│  │   (CreateDesktopW)   (LowLevel Hook)    (DisplayAffinity)   │  │
│  │                                                             │  │
│  │   process_scanner.rs  watchdog.rs       ipc.rs             │  │
│  │   (Hash-based scan)   (Heartbeat)       (Pipe Server)      │  │
│  │                                                             │  │
│  │   config.rs           vm_detection.rs   logger.rs          │  │
│  │   (ExamConfig parse)  (CPUID/MAC check) (append-only log)  │  │
│  └─────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘

                    ↕ TCP Socket (Packet)
         
┌──────────────────────────────────────────────────────────────────┐
│                  TutorHub Server (Java)                           │
│   ExamDAO   ExamService   ExamPacketHandler   AntiCheatService   │
│                      PostgreSQL (Neon)                            │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2. So sánh Kiến trúc SEB vs TutorHub

SEB 3.x có **3 process riêng biệt**: `Runtime.exe` + `Client.exe` + `Service.exe`, giao tiếp qua WCF Named Pipes. TutorHub đơn giản hóa thành 2 process: JVM (Java) + `LockdownCore.exe` (Rust), đủ cho quy mô dự án.

| Khía cạnh | SEB 3.x | TutorHub |
|---|---|---|
| Số process | 3 (Runtime, Client, Service) | 2 (JVM, Rust) |
| IPC | WCF over Named Pipes | Raw Named Pipe (text) |
| Kiosk Mode | CreateNewDesktop hoặc DisableExplorerShell | CreateDesktopW (Windows) |
| Config | `.seb` file (XML + password) | JSON từ Server qua TCP |
| Startup | OperationSequence pattern | Sequential trong `main.rs` |
| Process monitor | Blacklist by name + signature | Blacklist by name + SHA-256 |
| BEK/TEK | SHA-256(config + binary) | HMAC-SHA256(jar + session_secret) |
| VM detection | CPUID + driver list (fixed in 3.10.1) | CPUID + MAC + driver list |
| Proctoring | Screen proctoring (v3.7+) | Edge AI on-device (Phase 4) |
| License | MPL 2.0 | Proprietary |

---

## 3. Cấu trúc Thư mục Đề xuất

### 3.1. Rust Core (`tutorhub_lockdown/`)

```
tutorhub_lockdown/
├── Cargo.toml                  # dependencies, features, build config
├── Cargo.lock                  # pin versions (commit to git)
├── build.rs                    # build script (nếu cần resource embedding)
│
├── src/
│   ├── main.rs                 # Entry point: arg parse → config → init sequence → IPC loop
│   ├── config.rs               # ExamConfig struct: deserialize từ JSON args
│   ├── ipc.rs                  # Named Pipe server: create, accept, read/write loop
│   ├── desktop.rs              # CreateDesktopW, SwitchDesktop, CloseDesktop, SetThreadDesktop
│   ├── keyboard_hook.rs        # SetWindowsHookExW (WH_KEYBOARD_LL), LowLevelKeyboardProc
│   ├── screen_protection.rs    # SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)
│   ├── process_scanner.rs      # CreateToolhelp32Snapshot, hash-based blacklist scanner
│   ├── watchdog.rs             # Heartbeat timer, PING timeout handler, recovery logic
│   ├── vm_detection.rs         # CPUID hypervisor bit, MAC prefix check, driver list check
│   ├── logger.rs               # File logger (append-only, thread-safe) + event types
│   └── error.rs                # LockdownError enum, Result<T, LockdownError> alias
│
├── tests/
│   ├── ipc_test.rs             # Test pipe connect/disconnect
│   ├── config_test.rs          # Test JSON parse edge cases
│   └── process_scanner_test.rs # Test hash computation
│
└── resources/
    └── default_blacklist.json  # Default banned process hashes (TeamViewer, AnyDesk, OBS, etc.)
```

### 3.2. Java Shell (trong project TutorHub hiện có)

```
com.mycompany.tutorhub_enterprise/
├── client/
│   ├── exam/
│   │   ├── ExamTab.java                # Tab "Kỳ thi" chính
│   │   ├── ExamCreatorPanel.java       # GV tạo đề (đã có)
│   │   ├── ExamTakingPanel.java        # HS làm bài JCEF (đã có, cần update IPC)
│   │   ├── ExamResultPanel.java        # Xem kết quả
│   │   ├── QuestionBankPanel.java      # Ngân hàng câu hỏi
│   │   └── OfflineExamCache.java       # SQLite draft (đã có)
│   │
│   └── security/
│       ├── LockdownManager.java        # Điều phối: spawn Rust, manage lifecycle
│       ├── RustIPCClient.java          # Named Pipe client, PING thread
│       ├── EnvironmentChecker.java     # Pre-exam: monitor count, banned process, version
│       └── IntegrityVerifier.java      # TEK Hash, HMAC-SHA256 (Phase 3)
│
├── server/
│   ├── db/ExamDAO.java                 # CRUD PostgreSQL
│   ├── handlers/
│   │   ├── ExamPacketHandler.java      # START, SUBMIT, SYNC_DRAFT
│   │   └── ProctorPacketHandler.java   # HEARTBEAT, VIOLATION
│   └── services/
│       ├── ExamService.java            # Business logic
│       ├── GradingService.java         # Auto-grade MCQ
│       └── AntiCheatService.java       # Log + aggregate violations
│
└── models/
    ├── Exam.java
    ├── Question.java
    ├── ExamSession.java
    ├── ExamAnswer.java
    ├── AntiCheatEvent.java
    └── TrustScore.java
```

### 3.3. File `Cargo.toml` (Đề xuất)

```toml
[package]
name = "tutorhub_lockdown"
version = "0.1.0"
edition = "2021"
authors = ["TutorHub Dev Team"]

[[bin]]
name = "TutorHub_LockdownCore"
path = "src/main.rs"

[dependencies]
# Windows API binding - dùng crate windows cho PoC, windows-sys nếu cần tối ưu size
windows = { version = "0.58", features = [
    "Win32_Foundation",
    "Win32_System_Threading",
    "Win32_System_ProcessStatus",
    "Win32_UI_WindowsAndMessaging",
    "Win32_System_Diagnostics_ToolHelp",
    "Win32_Storage_FileSystem",
    "Win32_Security",
    "Win32_System_IO",
    "Win32_Devices_DeviceAndDriverInstallation",
] }

# JSON parsing (ExamConfig)
serde = { version = "1.0", features = ["derive"] }
serde_json = "1.0"
base64 = "0.22"

# SHA-256 cho process hash và TEK
sha2 = "0.10"
hex = "0.4"

# HMAC cho TEK verification (Phase 3)
hmac = "0.12"

# Logging
log = "0.4"

# Thời gian
chrono = "0.4"

[profile.release]
opt-level = "z"          # Tối ưu size
lto = true
codegen-units = 1
strip = true             # Strip symbols

[profile.dev]
# DEBUG build: giữ symbols, không strip
opt-level = 0
debug = true

[features]
# Feature flag để bật/tắt DEBUG-only code
debug_mode = []          # cargo build --features debug_mode

[target.'cfg(windows)'.dependencies]
# Chỉ compile trên Windows
```

---

## 4. Mapping SEB → TutorHub (Chi tiết)

Dựa trên nghiên cứu thực tế repository `SafeExamBrowser/seb-win-refactoring` v3.10.1:

| Module SEB (C#/.NET) | Chức năng trong SEB | Module TutorHub tương đương | File Rust/Java | Ghi chú License |
|---|---|---|---|---|
| `SafeExamBrowser.Runtime` | Orchestrator: quản lý lifecycle toàn bộ app, OperationSequence | `LockdownManager.java` + `main.rs` | — | Tham khảo sát pattern, lifecycle và flow xử lý từ SEB; có thể chuyển hóa logic phù hợp sang Rust cho TutorHub. |
| `SafeExamBrowser.Service` | Windows Service watchdog, IPC host | `watchdog.rs` + `ipc.rs` | Rust | Học heartbeat logic |
| `SafeExamBrowser.Client` | Client-side lifecycle, session management | `ExamTakingPanel.java` | Java | — |
| `SafeExamBrowser.Lockdown` | CreateDesktop / DisableExplorerShell mode | `desktop.rs` | Rust | ✅ Học kỹ |
| `SafeExamBrowser.Monitoring` | ApplicationMonitor: blacklist scan ~244 processes | `process_scanner.rs` | Rust | ✅ Học kỹ |
| `SafeExamBrowser.WindowsApi` | Win32 API wrappers (C# P/Invoke) | `windows-rs` crate | Rust | Rust thay thế tốt hơn |
| `SafeExamBrowser.WindowsApi.Contracts` | Interface definitions | `error.rs` + trait definitions | Rust | — |
| `SafeExamBrowser.Communication` | WCF Named Pipe IPC | `ipc.rs` | Rust | Simplify: raw text pipe |
| `SafeExamBrowser.Configuration` | `.seb` file parser, encryption | `config.rs` (JSON) | Rust | Đơn giản hóa: JSON |
| `SafeExamBrowser.Integrity` | Browser Exam Key (SHA-256) | `IntegrityVerifier.java` + TEK | Java + Rust | ⚠️ BEK platform-specific từ v3.4! |
| `SafeExamBrowser.SystemComponents` | Network, Power, Display monitors | `EnvironmentChecker.java` (pre-check) | Java | — |
| `SafeExamBrowser.Browser` | Chromium lockdown, DevTools block | `ExamTakingPanel.java` (JCEF) | Java | — |
| `SafeExamBrowser.Proctoring` | Screen proctoring, metadata | `SnapshotCapture.java` (Phase 3) | Java | Phase 3+ |
| `SafeExamBrowser.Server` | SEB Server API client | `ExamService.java` server-side | Java | — |
| `SafeExamBrowser.Logging` | Structured logging per session | `logger.rs` | Rust | — |
| N/A (SEB không có) | VM Detection (CPUID, MAC, drivers) | `vm_detection.rs` | Rust | TutorHub thêm mới |
| N/A (SEB không có) | Edge AI face detection | Phase 4 (ONNX) | Java | Phase 4 |

### 4.1. Bài học Quan trọng từ SEB 3.10.1

1. **OperationSequence Pattern**: SEB khởi động theo chuỗi operation độc lập, mỗi operation có `Perform()`, `Repeat()`, `Revert()`. Nếu 1 operation fail → rollback toàn bộ chain. TutorHub nên áp dụng pattern tương tự trong `main.rs`.

2. **ApplicationMonitor khởi tạo 244 processes**: SEB lúc start scan toàn bộ process đang chạy và lưu baseline. Các process mới xuất hiện sau đó bị check theo blacklist. TutorHub: làm tương tự trong `process_scanner.rs`.

3. **Kiosk Mode có 2 chế độ**: SEB hỗ trợ `CreateNewDesktop` (cô lập hoàn toàn) hoặc `DisableExplorerShell` (kill explorer.exe). TutorHub chỉ dùng `CreateDesktopW` — không kill Explorer vì đây là approach ít xâm phạm hơn.

4. **Browser Exam Key platform-specific từ v3.4.0**: SHA-256 của x86 build ≠ x64 build dù cùng version. TutorHub TEK cần tính đến kiến trúc JVM.

5. **VM Detection false positive ở 3.10.0, fix ở 3.10.1**: SEB từng bỏ CPUID check vì false positive trên hardware mới, sau đó refine lại. TutorHub phải cẩn thận với VM detection, cần whitelist hardware mới.

6. **Monitoring terminated before reconfiguration**: SEB dừng ApplicationMonitor trước khi load config mới. Pattern quan trọng để tránh race condition.

---

## 5. Luồng Hoạt động Chính

```
[Học sinh mở TutorHub]
       │
       ▼
[Chọn Tab "Kỳ thi" → Chọn bài thi]
       │
       ▼
[PRE-CHECK SCREEN] ← EnvironmentChecker.java
  ├─ Kiểm tra mạng
  ├─ Kiểm tra webcam (nếu yêu cầu)
  ├─ Kiểm tra process cấm đang chạy
  ├─ Kiểm tra số màn hình (≤1)
  ├─ Kiểm tra version TutorHub (TEK Hash - Phase 3)
  └─ Kiểm tra máy ảo (nếu enabled)
       │
       ├─ FAIL → Hiển thị lỗi, DỪNG
       │
       ▼ PASS
[Học sinh nhấn "BẮT ĐẦU THI" + Xác nhận quy chế]
       │
       ▼
[Java gửi EXAM_START_REQUEST lên Server]
  → Server tạo ExamSession, trả về sessionId + đề thi đã trộn
       │
       ▼
[LockdownManager.java spawn Rust Core]
  → ProcessBuilder("TutorHub_LockdownCore.exe",
       "--session-id", sessionId,
       "--exam-key", tekHash,
       "--config", configJsonBase64)
       │
       ▼
[Rust Core khởi động (main.rs)]
  ├─ Parse args → ExamConfig
  ├─ Check VM (nếu enabled trong config)
  ├─ Mở Named Pipe server
  ├─ Chờ LOCK command từ Java (timeout 10s)
       │
       ▼
[Java gửi LOCK qua Named Pipe]
       │
       ▼
[Rust thực hiện LOCK sequence]
  ├─ CreateDesktopW("TutorHub_Exam_<sessionId>")
  ├─ SetThreadDesktop → Desktop mới
  ├─ SwitchDesktop → Desktop mới
  ├─ SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE) cho HWND của Java
  ├─ SetWindowsHookExW(WH_KEYBOARD_LL) → chặn hotkey
  ├─ Bắt đầu process scanner thread (mỗi 3s)
  ├─ Bắt đầu watchdog thread (lắng nghe PING)
  └─ Gửi LOCKED|<desktopHandle> về Java
       │
       ▼
[Java nhận LOCKED → Hiển thị giao diện thi]
  ├─ ExamTakingPanel load bài thi vào JCEF
  ├─ Bắt đầu countdown timer
  ├─ Bắt đầu PING thread (mỗi 2s)
  └─ Bắt đầu auto-save SQLite (mỗi 30s)
       │
       ▼
[ĐANG THI — Kiosk Mode Active]
  ├─ Alt+Tab, Win key, PrintScreen: BỊ CHẶN
  ├─ OBS, Discord overlay: Thấy màn hình ĐEN
  ├─ Process cấm xuất hiện → PROCESS_ALERT → Java log
  ├─ PING timeout (>10s) → Watchdog tự unlock
  └─ Học sinh mở Task Manager kill Java → Watchdog tự SwitchDesktop Default
       │
       ▼
[Học sinh nộp bài HOẶC hết giờ]
       │
       ▼
[SHUTDOWN FLOW] → (Xem Section 7)
```

---

## 6. Luồng Khởi động Phiên Thi

### 6.1. Java Side — `LockdownManager.java`

```
Step 1: extractRustExe()
  - Đọc /resources/tools/TutorHub_LockdownCore.exe từ JAR
  - Ghi ra System.getProperty("java.io.tmpdir") + "/tutorhub/"
  - Set executable permission
  - Verify SHA-256 của exe khớp với expected hash trong resources

Step 2: buildConfig()
  - Tạo ExamLockConfig {
      sessionId: String,
      examId: int,
      allowedProcessHashes: List<String>,  # whitelist nếu có
      bannedProcessNames: List<String>,     # từ security_config của exam
      enableVmDetection: boolean,
      debugMode: boolean                   # chỉ true khi dev
    }
  - Serialize thành JSON → Base64 encode

Step 3: spawnRustProcess()
  - ProcessBuilder pb = new ProcessBuilder(
        exePath,
        "--session-id", sessionId,
        "--config", base64Config
    )
  - pb.redirectErrorStream(true)
  - pb.redirectOutput(ProcessBuilder.Redirect.PIPE)
  - rustProcess = pb.start()

Step 4: waitForPipeReady()
  - Thử kết nối Named Pipe "\\.\pipe\TutorHubExam_<sessionId>"
  - Retry 10 lần, mỗi lần cách nhau 500ms
  - Timeout: 5 giây tổng
  - Nếu timeout → terminateRustProcess() + throw LockdownException

Step 5: sendLockCommand()
  - Gửi "LOCK|<sessionId>\n" qua Named Pipe
  - Chờ response "LOCKED|<desktopHandle>\n" trong 5s
  - Nếu nhận LOCK_FAILED|<reason> → log + show error UI

Step 6: startPingThread()
  - Thread daemon gửi "PING|<timestamp>\n" mỗi 2s
  - Nếu không nhận PONG trong 3s → log WARNING
  - Nếu không nhận PONG trong 10s → gọi emergencyUnlock()
```

### 6.2. Rust Side — `main.rs`

```rust
// Pseudo-code của startup sequence trong Rust

fn main() -> Result<(), LockdownError> {
    // 1. Parse command line args
    let args = parse_args()?;
    let config: ExamConfig = decode_config(&args.config_b64)?;
    
    // 2. Initialize logger (file: %TEMP%/tutorhub/lockdown_<session>.log)
    logger::init(&config.session_id)?;
    log::info!("LockdownCore v{} starting for session {}", VERSION, config.session_id);
    
    // 3. Auto-kill timer (DEBUG only)
    #[cfg(feature = "debug_mode")]
    {
        spawn_auto_kill_timer(60); // 60 giây trong DEBUG
        log::warn!("DEBUG MODE: Auto-kill timer started (60s)");
    }
    
    // 4. VM Detection (nếu enabled)
    if config.enable_vm_detection {
        if let Err(e) = vm_detection::check() {
            log::error!("VM detected: {:?}", e);
            return Err(LockdownError::VmDetected(e.to_string()));
        }
    }
    
    // 5. Tạo Named Pipe server
    let pipe = ipc::create_pipe(&config.session_id)?;
    log::info!("Named Pipe ready: \\\\.\\ pipe\\TutorHubExam_{}", config.session_id);
    
    // 6. Chờ Java kết nối và gửi LOCK (timeout 10s)
    let client = ipc::accept_connection(pipe, Duration::from_secs(10))?;
    let cmd = ipc::read_command(&client)?;
    
    match cmd {
        IpcCommand::Lock { session_id } => {
            // 7. Thực hiện lockdown sequence
            execute_lockdown(&config, &client)?;
        }
        _ => return Err(LockdownError::UnexpectedCommand),
    }
    
    Ok(())
}

fn execute_lockdown(config: &ExamConfig, client: &IpcClient) 
    -> Result<(), LockdownError> 
{
    // 7a. Tạo Desktop mới
    let desktop_name = format!("TutorHub_Exam_{}", config.session_id);
    let desktop = desktop::create(&desktop_name)?;
    
    // 7b. Switch sang Desktop mới
    desktop::switch(&desktop)?;
    
    // 7c. Bật DisplayAffinity (chặn screen capture)
    screen_protection::enable(config.java_pid)?;
    
    // 7d. Bật Low-Level Keyboard Hook
    keyboard_hook::install()?;
    
    // 7e. Scan process ngay lập tức (baseline)
    process_scanner::init(&config.banned_process_names)?;
    
    // 7f. Gửi LOCKED về Java
    ipc::send_response(client, &IpcResponse::Locked {
        desktop_handle: desktop.handle_as_u64(),
    })?;
    
    // 7g. Bắt đầu main loop (watchdog + scanner + panic key)
    run_main_loop(config, client, desktop)
}
```

---

## 7. Luồng Kết thúc Phiên Thi

### 7.1. Normal Shutdown (Học sinh nộp bài / hết giờ)

```
[Java xác nhận nộp bài thành công từ Server]
       │
       ▼
[Java gửi "UNLOCK\n" qua Named Pipe]
       │
       ▼
[Rust nhận UNLOCK]
  ├─ Dừng process scanner thread
  ├─ Dừng keyboard hook (UnhookWindowsHookEx)
  ├─ Tắt DisplayAffinity (reset về WDA_NONE)
  ├─ SwitchDesktop → Desktop gốc ("Default" hoặc handle lưu lúc start)
  ├─ CloseDesktop("TutorHub_Exam_<sessionId>")
  ├─ Gửi "UNLOCKED\n" về Java
  └─ ExitProcess(0)
       │
       ▼
[Java nhận UNLOCKED]
  ├─ Dừng PING thread
  ├─ Đóng Named Pipe client
  ├─ Hiển thị màn hình "Nộp bài thành công"
  └─ Gọi cleanup() trong LockdownManager
```

### 7.2. Emergency Shutdown (Crash / Kill)

```
Trường hợp A: Java bị kill (Task Manager)
  → PING thread dừng → Rust không nhận PING > 10s
  → Watchdog thread trigger → SwitchDesktop Default → ExitProcess(1)
  → Máy tính trở về bình thường trong ≤10s

Trường hợp B: Rust bị kill
  → Java không nhận PONG > 3s → log WARNING
  → Java không nhận PONG > 10s → emergencyUnlock() trong Java
  → Java cố gắng switch desktop qua JNA (fallback)
  → Hiển thị alert "Phiên thi bị gián đoạn"
  → Lưu trạng thái DISCONNECTED vào DB

Trường hợp C: Machine crash / BSOD / mất điện
  → Khi bật lại TutorHub, kiểm tra exam_sessions có status IN_PROGRESS không hết giờ
  → Nếu có → Skip pre-check → Jump thẳng vào Kiosk Mode với session cũ
  → Load lại draft từ SQLite → tiếp tục thi

Trường hợp D: Dev Panic Key (DEBUG only)
  → Ctrl+Shift+Alt+F12 detected trong keyboard hook
  → Ngay lập tức: UnhookWindowsHookEx → SwitchDesktop Default → ExitProcess(99)
  → Java nhận process exit code 99 → biết là Panic Key → không báo lỗi
```

---

## 8. IPC Protocol: Java ↔ Rust

### 8.1. Transport Layer

- **Mechanism**: Windows Named Pipe
- **Pipe name**: `\\.\pipe\TutorHubExam_<sessionId>`
- **Format**: Newline-terminated UTF-8 strings (`\n` = 0x0A)
- **Direction**: Bidirectional (duplex)
- **Buffer size**: 4096 bytes
- **Framing**: Một lệnh = Một dòng, kết thúc bằng `\n`
- **Encoding**: UTF-8, không dùng Unicode escape

### 8.2. Command Reference

| Lệnh | Hướng | Format | Mô tả |
|---|---|---|---|
| `PING` | Java → Rust | `PING\|<unix_timestamp_ms>\n` | Heartbeat mỗi 2s |
| `PONG` | Rust → Java | `PONG\|<unix_timestamp_ms>\n` | Xác nhận còn sống |
| `LOCK` | Java → Rust | `LOCK\|<sessionId>\n` | Yêu cầu khóa máy |
| `LOCKED` | Rust → Java | `LOCKED\|<desktop_handle_u64>\n` | Xác nhận đã khóa |
| `LOCK_FAILED` | Rust → Java | `LOCK_FAILED\|<reason>\n` | Lỗi khi lock |
| `UNLOCK` | Java → Rust | `UNLOCK\n` | Yêu cầu mở khóa |
| `UNLOCKED` | Rust → Java | `UNLOCKED\n` | Xác nhận đã mở |
| `PROCESS_ALERT` | Rust → Java | `PROCESS_ALERT\|<name>\|<hash>\|<pid>\n` | Tiến trình cấm phát hiện |
| `VM_DETECTED` | Rust → Java | `VM_DETECTED\|<reason>\n` | Máy ảo phát hiện |
| `HEARTBEAT_TIMEOUT` | Rust (self) | Internal | Watchdog trigger, không gửi |
| `STATUS` | Java → Rust | `STATUS\n` | Query trạng thái hiện tại |
| `STATUS_RESPONSE` | Rust → Java | `STATUS_RESPONSE\|<state>\|<uptime_secs>\n` | Trả về state |

### 8.3. State Machine (Rust Side)

```
INIT → (nhận LOCK) → LOCKING → (CreateDesktop OK) → LOCKED
                                                         │
                              ← (nhận UNLOCK) ──────────┘
                              │
                        UNLOCKING → (SwitchDesktop OK) → TERMINATED

LOCKED → (PING timeout > 10s) → EMERGENCY_UNLOCK → TERMINATED
LOCKED → (Panic Key DEBUG) → EMERGENCY_UNLOCK → TERMINATED
```

### 8.4. Heartbeat Logic

```
Java PING Thread:
  - Interval: 2000ms
  - Nếu pipe write fail 3 lần liên tiếp → gọi emergencyUnlock() Java side

Rust Watchdog Thread:
  - Max silence: 10000ms (10 giây)
  - Reset mỗi khi nhận PING
  - Nếu elapsed > 10s → execute emergency_unlock() Rust side
  
Emergency unlock Rust:
  1. keyboard_hook::uninstall()
  2. screen_protection::disable()
  3. desktop::switch_to_default()  // SwitchDesktop(original_desktop)
  4. desktop::close(exam_desktop)  // CloseDesktop
  5. log::error!("Emergency unlock: heartbeat timeout")
  6. process::exit(1)
```

---

## 9. Module Rust Core — Đặc tả Kỹ thuật

### 9.1. `config.rs` — Cấu hình Phiên Thi

```rust
/// Struct nhận từ Java qua command-line args (Base64 JSON)
#[derive(Debug, Deserialize, Clone)]
pub struct ExamConfig {
    pub session_id: String,           // UUID của exam session
    pub exam_id: i64,
    pub exam_key: String,             // TEK Hash (hex string)
    pub java_pid: u32,                // PID của Java process để áp dụng DisplayAffinity
    pub enable_vm_detection: bool,    // Bật VM check?
    pub enable_keyboard_hook: bool,   // Bật chặn hotkey?
    pub enable_screen_protection: bool, // Bật DisplayAffinity?
    pub banned_process_names: Vec<String>, // ["TeamViewer.exe", "AnyDesk.exe", ...]
    pub banned_process_hashes: Vec<String>, // SHA-256 hex hashes
    pub process_scan_interval_secs: u64,    // default: 3
    pub heartbeat_timeout_secs: u64,        // default: 10
}

impl ExamConfig {
    pub fn from_base64_json(b64: &str) -> Result<Self, LockdownError> {
        // decode base64 → parse JSON → validate fields
    }
    
    pub fn validate(&self) -> Result<(), LockdownError> {
        // session_id không rỗng
        // heartbeat_timeout_secs >= 5
        // process_scan_interval_secs >= 1
    }
}
```

**Input**: `--config <base64_json>` từ command line  
**Output**: `ExamConfig` struct dùng trong toàn bộ quá trình  
**Error**: `LockdownError::ConfigParseFailed`

---

### 9.2. `ipc.rs` — Named Pipe Server

**Các hàm cần implement:**

```rust
pub fn create_pipe(session_id: &str) -> Result<HANDLE, LockdownError>;
// Tạo pipe: \\.\pipe\TutorHubExam_<session_id>
// PIPE_ACCESS_DUPLEX | FILE_FLAG_OVERLAPPED
// PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT
// nMaxInstances: 1

pub fn accept_connection(pipe: HANDLE, timeout: Duration) -> Result<IpcClient, LockdownError>;
// ConnectNamedPipe với timeout

pub fn read_command(client: &mut IpcClient) -> Result<IpcCommand, LockdownError>;
// Đọc đến '\n', parse thành IpcCommand enum

pub fn send_response(client: &IpcClient, resp: &IpcResponse) -> Result<(), LockdownError>;
// Serialize response + '\n', WriteFile

pub fn close(client: IpcClient);
// DisconnectNamedPipe + CloseHandle
```

**Windows APIs cần dùng:**
- `CreateNamedPipeW` — tạo pipe server
- `ConnectNamedPipe` — chờ client
- `ReadFile` / `WriteFile` — đọc/ghi dữ liệu
- `DisconnectNamedPipe` — ngắt kết nối client
- `CloseHandle` — dọn dẹp

**IpcCommand enum:**
```rust
pub enum IpcCommand {
    Lock { session_id: String },
    Unlock,
    Ping { timestamp_ms: u64 },
    Status,
    Unknown(String),
}
```

---

### 9.3. `desktop.rs` — Desktop Isolation

**Các hàm cần implement:**

```rust
pub struct ExamDesktop {
    handle: HDESK,
    name: String,
    original_desktop: HDESK,  // lưu desktop gốc để SwitchBack
}

impl ExamDesktop {
    pub fn create(name: &str) -> Result<Self, LockdownError>;
    // CreateDesktopW(name, NULL, NULL, 0, MAXIMUM_ALLOWED, NULL)
    // QUAN TRỌNG: Lưu original = GetThreadDesktop(GetCurrentThreadId())
    
    pub fn switch_to(&self) -> Result<(), LockdownError>;
    // SwitchDesktop(self.handle)
    // SetThreadDesktop(self.handle) cho thread hiện tại
    
    pub fn switch_to_default(&self) -> Result<(), LockdownError>;
    // SwitchDesktop(self.original_desktop)
    // SetThreadDesktop(self.original_desktop)
    
    pub fn handle_as_u64(&self) -> u64;
    // Chuyển HDESK thành u64 để gửi qua IPC
}

impl Drop for ExamDesktop {
    fn drop(&mut self) {
        // Tự động switch về default + close khi drop
        let _ = self.switch_to_default();
        unsafe { CloseDesktop(self.handle) };
    }
}
```

**⚠️ Critical Notes:**
- `SetThreadDesktop` phải gọi SAU `SwitchDesktop`, và TRƯỚC khi thread tạo bất kỳ window nào
- Nếu thread đã có hook hoặc window, `SetThreadDesktop` sẽ fail → log lỗi + abort
- Lưu `original_desktop` ngay khi start (trước khi làm gì khác)

---

### 9.4. `keyboard_hook.rs` — Low-Level Keyboard Hook

**Các phím bị chặn:**
```rust
const BLOCKED_KEYS: &[(u32, u32)] = &[
    // (vkCode, flags_mask)
    (VK_LWIN, 0),       // Windows key trái
    (VK_RWIN, 0),       // Windows key phải
    (VK_TAB, 0x08),     // Alt+Tab (khi Alt đang giữ - check injected flag)
    (VK_F4, 0x08),      // Alt+F4
    (VK_SNAPSHOT, 0),   // Print Screen
    (VK_ESCAPE, 0x08),  // Alt+Escape
    (VK_DELETE, 0x03),  // Ctrl+Alt+Delete (hạn chế - kernel vẫn xử lý CAD)
];

// Panic Key (DEBUG only)
#[cfg(feature = "debug_mode")]
const PANIC_COMBO: &[u32] = &[VK_CONTROL, VK_SHIFT, VK_MENU, VK_F12];
```

**Implementation:**
```rust
// HWND hWnd = NULL (global hook)
// nCode: HC_ACTION
// Nếu wParam = WM_KEYDOWN hoặc WM_SYSKEYDOWN:
//   lParam là KBDLLHOOKSTRUCT*
//   Check vkCode và flags → nếu blocked → return 1 (suppress)
//   Nếu không blocked → CallNextHookEx

static HOOK_HANDLE: AtomicPtr<HHOOK> = AtomicPtr::new(ptr::null_mut());

pub fn install() -> Result<(), LockdownError>;
// SetWindowsHookExW(WH_KEYBOARD_LL, hook_proc, NULL, 0)

pub fn uninstall();
// UnhookWindowsHookEx(HOOK_HANDLE)

// Hook phải chạy message loop! Dùng GetMessage loop trong thread riêng
pub fn run_message_loop();
// while GetMessageW(&msg, NULL, 0, 0) != 0 {
//     TranslateMessage(&msg);
//     DispatchMessageW(&msg);
// }
```

**⚠️ Critical**: Low-level keyboard hook BẮT BUỘC phải có message loop đang chạy trên cùng thread đặt hook. Tạo dedicated thread cho hook + message loop.

---

### 9.5. `screen_protection.rs` — Chống Screen Capture

```rust
/// Áp dụng DisplayAffinity cho window của Java process
pub fn enable(java_pid: u32) -> Result<(), LockdownError>;
// 1. Tìm HWND của Java process:
//    EnumWindows callback → GetWindowThreadProcessId → match java_pid
// 2. SetWindowDisplayAffinity(hwnd, WDA_EXCLUDEFROMCAPTURE)
//    WDA_EXCLUDEFROMCAPTURE = 0x11 (Windows 10 2004+)
//    WDA_MONITOR = 0x1 (Windows 7+, ít hiệu quả hơn)

pub fn disable(java_pid: u32) -> Result<(), LockdownError>;
// SetWindowDisplayAffinity(hwnd, WDA_NONE)

pub fn apply_to_all_java_windows(java_pid: u32) -> Result<(), LockdownError>;
// EnumWindows → tìm tất cả window của java_pid → áp dụng WDA_EXCLUDEFROMCAPTURE
```

**Output**: OBS, Discord, Snipping Tool thấy window màu đen  
**Limitation**: Hardware capture card bypass được (không thể chặn 100%)

---

### 9.6. `process_scanner.rs` — Kiểm soát Tiến trình

**Thuật toán:**

```
Khởi tạo:
1. Đọc bannedProcessNames + bannedProcessHashes từ ExamConfig
2. Thêm default blacklist cứng vào memory:
   ["TeamViewer.exe", "AnyDesk.exe", "obs64.exe", "obs32.exe",
    "discord.exe", "slack.exe", "zoom.exe", "vncviewer.exe",
    "logmein.exe", "screenconnect.exe", "RustDesk.exe",
    "Screenpresso.exe", "ShareX.exe"]
3. Snapshot toàn bộ process đang chạy → lưu làm baseline

Scan loop (mỗi 3 giây):
1. CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0)
2. Process32FirstW → Process32NextW để duyệt
3. Với mỗi process:
   a. Check tên (case-insensitive) trong blacklist names
   b. Nếu match name → tính SHA-256 của executable
   c. Check SHA-256 trong bannedProcessHashes
   d. Nếu match → gửi PROCESS_ALERT qua pipe + log
4. Repeat

Hash computation:
- Mở file exe của process: OpenProcess → QueryFullProcessImageNameW → tính SHA-256
- Dùng sha2 crate: sha2::Sha256
- Cache hash của process theo PID để không tính lại mỗi 3s
- Invalidate cache khi PID exit và PID mới xuất hiện
```

**Output**: `PROCESS_ALERT|<name>|<sha256_hex>|<pid>\n` gửi về Java

---

### 9.7. `watchdog.rs` — Heartbeat Monitor

```rust
pub struct Watchdog {
    last_ping: Arc<Mutex<Instant>>,
    timeout: Duration,
    shutdown_tx: Sender<()>,
}

impl Watchdog {
    pub fn new(timeout_secs: u64) -> Self;
    
    pub fn start(&self, 
        on_timeout: impl Fn() + Send + 'static
    ) -> JoinHandle<()>;
    // Spawn thread: loop {
    //   sleep(1s)
    //   if Instant::now() - *last_ping.lock() > timeout {
    //     on_timeout(); break;
    //   }
    // }
    
    pub fn record_ping(&self);
    // *self.last_ping.lock() = Instant::now()
    
    pub fn stop(&self);
    // shutdown_tx.send(())
}

// on_timeout callback:
fn emergency_unlock(desktop: &ExamDesktop, config: &ExamConfig) {
    keyboard_hook::uninstall();
    screen_protection::disable(config.java_pid);
    let _ = desktop.switch_to_default();
    log::error!("EMERGENCY UNLOCK: heartbeat timeout");
    process::exit(1);
}
```

---

### 9.8. `vm_detection.rs` — Phát hiện Máy ảo

**3 lớp kiểm tra:**

```rust
pub enum VmDetectionResult {
    NotVm,
    PossiblyVm { reason: String },
    DefinitelyVm { reason: String },
}

pub fn check() -> Result<(), VmError> {
    check_cpuid_hypervisor()?;  // Lớp 1
    check_mac_address()?;       // Lớp 2
    check_vm_drivers()?;        // Lớp 3
    Ok(())
}
```

**Lớp 1: CPUID Hypervisor Bit**
```rust
fn check_cpuid_hypervisor() -> Result<(), VmError> {
    // unsafe { core::arch::x86_64::__cpuid(0x1) }
    // Bit 31 của ECX = 1 → đang chạy trong hypervisor
    // ⚠️ False positive trên một số hardware mới → chỉ log WARNING, không hard fail
    // Ghi chú: SEB 3.10.0 gặp vấn đề này, fix trong 3.10.1
}
```

**Lớp 2: MAC Address Blacklist**
```rust
const VM_MAC_PREFIXES: &[&str] = &[
    "00:0C:29",  // VMware
    "00:50:56",  // VMware
    "08:00:27",  // VirtualBox
    "00:16:3E",  // Xen
    "00:1C:42",  // Parallels
    "52:54:00",  // QEMU/KVM
];
// GetAdaptersAddresses → check MAC prefix
// Hard fail nếu MAC match
```

**Lớp 3: VM Driver List**
```rust
const VM_DRIVERS: &[&str] = &[
    "vboxguest.sys",    // VirtualBox Guest
    "vboxmouse.sys",    // VirtualBox
    "vmhgfs.sys",       // VMware Host-Guest Filesystem
    "vmtoolsd.exe",     // VMware Tools
    "VBoxService.exe",  // VirtualBox Service
];
// EnumDeviceDrivers + GetDeviceDriverBaseNameW
// Hoặc check Services registry: HKLM\SYSTEM\CurrentControlSet\Services
```

---

### 9.9. `logger.rs` — Logging System

```rust
// File log: %TEMP%\tutorhub\lockdown_<session_id>.log
// Format: [2026-06-06 14:30:00.123] [THREAD_ID] [LEVEL] Message

pub struct LockdownLogger {
    file: Arc<Mutex<BufWriter<File>>>,
}

// Các event type cần log:
pub enum LockdownEvent {
    SessionStart { config: String },
    LockAcquired { desktop_name: String },
    LockReleased,
    ProcessAlert { name: String, hash: String, pid: u32 },
    HeartbeatTimeout { elapsed_secs: f64 },
    VmDetected { reason: String },
    PanicKey,           // DEBUG only
    AutoKillTimer,      // DEBUG only
    EmergencyUnlock { reason: String },
    SessionEnd { exit_code: i32 },
}
```

---

## 10. Cơ chế Khóa Môi trường Thi

### 10.1. Trình tự Khóa (Acquire Lock)

```
1. Lưu original_desktop = GetThreadDesktop(GetCurrentThreadId())
2. CreateDesktopW("TutorHub_Exam_<sessionId>", ...)
   → Nếu fail: trả LOCK_FAILED|CREATE_DESKTOP_ERROR
3. SwitchDesktop(exam_desktop)
   → Màn hình chuyển sang desktop trống
4. SetThreadDesktop(exam_desktop)
   → Thread hook sẽ chạy trên desktop mới
5. SetWindowDisplayAffinity(java_hwnd, WDA_EXCLUDEFROMCAPTURE)
6. SetWindowsHookExW(WH_KEYBOARD_LL, ...) 
   → Spawn hook_thread với message loop
7. Process scanner init + spawn thread
8. Watchdog init + spawn thread
9. Gửi LOCKED|<handle>
```

### 10.2. Trình tự Mở Khóa (Release Lock)

```
1. Dừng process scanner thread (send shutdown signal)
2. UnhookWindowsHookEx(hook_handle)
3. PostThreadMessage(hook_thread_id, WM_QUIT, 0, 0)
4. SetWindowDisplayAffinity(java_hwnd, WDA_NONE)
5. SwitchDesktop(original_desktop)
6. SetThreadDesktop(original_desktop)
7. CloseDesktop(exam_desktop)
8. Gửi UNLOCKED
9. ExitProcess(0)
```

---

## 11. Cơ chế Quản lý Cấu hình

### 11.1. Flow Cấu hình

```
Server (PostgreSQL)
  exams.security_config (JSONB)
       │
       │ EXAM_START_RESPONSE packet
       ▼
Java Client
  ExamService.buildLockConfig() 
       │ Chọn relevant fields
       ▼
  LockdownManager.serializeConfig()
       │ JSON → Base64
       ▼
  ProcessBuilder("TutorHub_LockdownCore.exe", "--config", <base64>)
       │
       ▼
Rust Core
  config::ExamConfig::from_base64_json()
```

### 11.2. `exams.security_config` Schema (JSONB)

```json
{
  "shuffle_questions": true,
  "shuffle_options": true,
  "secure_mode": false,
  "require_webcam": false,
  "require_mic": false,
  "max_violations": 3,
  "security_level": "medium",
  "blocked_processes": ["TeamViewer", "AnyDesk", "obs64"],
  "allow_calculator": false,
  "allow_notepad": false,
  "enable_vm_detection": true,
  "allow_os": ["windows"]
}
```

### 11.3. `ExamLockConfig` (Java → Rust)

```json
{
  "session_id": "sess_abc123",
  "exam_id": 42,
  "exam_key": "sha256hex_of_jar",
  "java_pid": 12345,
  "enable_vm_detection": true,
  "enable_keyboard_hook": true,
  "enable_screen_protection": true,
  "banned_process_names": ["TeamViewer.exe", "AnyDesk.exe", "obs64.exe"],
  "banned_process_hashes": ["abc123...", "def456..."],
  "process_scan_interval_secs": 3,
  "heartbeat_timeout_secs": 10,
  "debug_mode": false
}
```

---

## 12. Cơ chế Kiểm soát Tiến trình

### 12.1. Chiến lược Phát hiện

```
Hai lớp phát hiện:

Lớp A — Phát hiện theo tên (fast):
  Check case-insensitive string match
  Độ phức tạp O(k) mỗi process, k = số process trong blacklist

Lớp B — Phát hiện theo hash (slow, accurate):
  Chỉ chạy khi Lớp A match hoặc theo interval riêng (mỗi 30s cho tất cả)
  SHA-256 của file exe trên disk
  Phát hiện kể cả khi đổi tên file
  Cache hash theo full path của exe
```

### 12.2. Default Blacklist

```rust
// Hardcoded trong Rust binary
pub const DEFAULT_BANNED_NAMES: &[&str] = &[
    // Remote desktop / screen sharing
    "TeamViewer.exe", "TeamViewer_Service.exe",
    "AnyDesk.exe",
    "RustDesk.exe",
    "vncviewer.exe", "tvnviewer.exe",
    "LogMeIn.exe",
    "ScreenConnect.ClientService.exe",
    "Supremo.exe",
    // Screen recording / capture
    "obs64.exe", "obs32.exe",
    "Screenpresso.exe",
    "ShareX.exe",
    "ScreenToGif.exe",
    "Bandicam.exe",
    "Fraps.exe",
    // Communication (có thể share screen)
    "discord.exe",
    "slack.exe",
    "zoom.exe",
    "teams.exe",
    "skype.exe",
    // AI tools (có thể dùng để gian lận)
    "chatgpt.exe",
    // Utilities
    "ProcessHacker.exe",
    "procexp64.exe", "procexp.exe",
    "Wireshark.exe",
    // Khai thác (Reference: SEB discussion #510 và default list)
    "webexmta.exe", "join.me.exe",
    "g2mstart.exe", "g2mlaunch.exe",
];
```

### 12.3. Hành động Khi Phát hiện

```
Phát hiện process cấm:
  1. Log PROCESS_ALERT với name + hash + pid
  2. Gửi "PROCESS_ALERT|<name>|<hash>|<pid>\n" về Java
  3. KHÔNG tự động kill process (quyết định thuộc Java)

Java nhận PROCESS_ALERT:
  1. Gửi EXAM_VIOLATION packet lên Server
  2. Tăng violation_count
  3. Nếu violation_count < max_violations → Hiển thị cảnh báo cho học sinh
  4. Nếu violation_count >= max_violations → Tự động nộp bài (force submit)
```

---

## 13. Cơ chế Bảo mật & Chống Can thiệp

### 13.1. TEK Hash (TutorHub Exam Key)

TEK là cơ chế tương đương Browser Exam Key (BEK) của SEB:

```
TEK = HMAC-SHA256(
    key  = exam_session_secret,    # từ Server, unique per session
    data = SHA256(tutorhub.jar)    # hash của client binary
         + "\n" + session_id
         + "\n" + exam_id
         + "\n" + platform         # "windows_x64" hoặc "windows_x86"
)

⚠️ Học từ SEB: BEK platform-specific từ v3.4!
   SHA-256 của tutorhub.jar có thể khác nhau giữa x86 và x64 JVM.
   Phải tính đúng platform.
```

**Flow TEK:**
```
1. Khi build TutorHub.jar:
   Maven plugin tính SHA-256(jar) → lưu vào build.properties
   
2. Khi Java khởi động:
   IntegrityVerifier.java đọc build.properties → lấy jarHash
   
3. Khi bắt đầu thi (EXAM_START_REQUEST):
   Java gửi tekHash = HMAC-SHA256(session_secret, jarHash + sessionId + examId + platform)
   Server verify tekHash
   
4. Server reject nếu tekHash không khớp:
   → Học sinh đã decompile và sửa jar → bị block
```

### 13.2. Pre-Exam Environment Check

```java
// EnvironmentChecker.java
public CheckResult runPreCheck(ExamConfig config) {
    List<CheckItem> results = new ArrayList<>();
    
    // 1. Network check
    results.add(checkNetworkConnectivity());
    
    // 2. Screen count check (chỉ cho phép 1 màn hình)
    results.add(checkDisplayCount());  // GraphicsEnvironment
    
    // 3. Webcam check (nếu require_webcam = true)
    if (config.isRequireWebcam()) {
        results.add(checkWebcamAvailable());
    }
    
    // 4. Banned process check
    results.add(checkRunningProcesses(config.getBannedProcessNames()));
    
    // 5. TutorHub version check (TEK Hash - Phase 3)
    // results.add(checkClientIntegrity());
    
    // 6. VM detection (delegate sang Rust nếu secure_mode = true)
    if (config.isSecureMode() && config.isEnableVmDetection()) {
        results.add(checkForVirtualMachine());
    }
    
    return new CheckResult(results);
}
```

### 13.3. JCEF Browser Hardening

```
ExamTakingPanel — JCEF Configuration:
- Disable DevTools (F12): cefClient.addKeyboardHandler → intercept F12
- Disable context menu: cefClient.addContextMenuHandler → return true (suppress)
- Disable drag & drop file upload: cefClient.addDragHandler
- Disable new window open: cefClient.addLifeSpanHandler → onBeforePopup → return true
- JavaScript bridge chỉ expose các function cần thiết
- Không cho phép navigate ra ngoài exam domain
- Block clipboard: navigator.clipboard disabled via JS injection
```

---

## 14. Watchdog & Fail-safe Matrix

| Tình huống | Phát hiện bởi | Hành động | Thời gian phục hồi |
|---|---|---|---|
| Java bị kill bởi Task Manager | Rust watchdog (PING timeout) | SwitchDesktop Default + ExitProcess(1) | ≤ 10s |
| Rust bị kill bởi Task Manager | Java PING thread (PONG timeout) | Java emergency unlock + hiển thị alert | ≤ 10s |
| Java crash (NullPointerException) | Rust watchdog (PING timeout) | Tương tự Java bị kill | ≤ 10s |
| Rust crash (panic!) | Java (process.isAlive() = false) | Java fallback unlock via JNA | ≤ 5s |
| Mất điện / BSOD | — | Khi boot lại, auto-resume session từ SQLite | Sau khi boot |
| Mất mạng | TCP disconnect | SQLite offline save tiếp tục, timer offline | Tức thì |
| Kết nối mạng phục hồi | Reconnect logic | Đẩy SQLite draft lên server | Tức thì |
| Dev panic key (DEBUG) | Rust keyboard hook | SwitchDesktop + ExitProcess(99) | Tức thì |
| Auto-kill timer (DEBUG) | Rust timer thread | SwitchDesktop + ExitProcess(98) | Sau 60s |
| VM phát hiện | Rust vm_detection | Trả về `LOCK_FAILED` với lý do `VM_DETECTED` cho Java | Tức thì |
| Process cấm | Rust process_scanner | PROCESS_ALERT → Java xử lý | ≤ 3s |

---

## 15. Yêu cầu An toàn Dev

> [!CAUTION]
> BẮT BUỘC TUÂN THỦ TRƯỚC KHI CHẠY BẤT KỲ CODE RUST LOCKDOWN NÀO

### 15.1. Quy tắc Cứng (Hard Rules)

| # | Quy tắc | Lý do |
|---|---|---|
| 1 | **Chỉ test trên VM** (Windows Sandbox / VMware / VirtualBox) | CreateDesktopW bug = kẹt màn hình thật |
| 2 | **Auto-Kill Timer 60s** bắt buộc trong `#[cfg(feature = "debug_mode")]` | Tránh kẹt máy vĩnh viễn |
| 3 | **Dev Panic Key** `Ctrl+Shift+Alt+F12` trong DEBUG build | Thoát khẩn cấp |
| 4 | **SSH Remote Debug** vào VM | Nếu màn hình đen, dùng host terminal |
| 5 | **KHÔNG commit** `--features debug_mode` vào CI/Release pipeline | Debug code không được vào production |
| 6 | **Luôn lưu original_desktop** trước bất kỳ thao tác Desktop nào | Đảm bảo có thể switch back |

### 15.2. Auto-Kill Timer Implementation

```rust
#[cfg(feature = "debug_mode")]
fn spawn_auto_kill_timer(seconds: u64) {
    std::thread::spawn(move || {
        log::warn!("[DEBUG] Auto-kill timer started: {}s", seconds);
        std::thread::sleep(Duration::from_secs(seconds));
        log::warn!("[DEBUG] Auto-kill timer triggered! Force exiting.");
        // Emergency cleanup
        // (thực ra nếu có ExamDesktop instance, Drop sẽ tự handle)
        process::exit(98);
    });
}
```

### 15.3. Dev Panic Key Implementation

```rust
#[cfg(feature = "debug_mode")]
fn check_panic_key(vk_code: u32, ctrl: bool, shift: bool, alt: bool) -> bool {
    vk_code == VK_F12 && ctrl && shift && alt
}

// Trong LowLevelKeyboardProc:
#[cfg(feature = "debug_mode")]
if check_panic_key(kbd.vkCode, is_ctrl_down(), is_shift_down(), is_alt_down()) {
    log::warn!("[DEBUG] PANIC KEY ACTIVATED");
    keyboard_hook::uninstall();
    screen_protection::disable_all();
    desktop::switch_to_default_from_static();
    process::exit(99);
}
```

### 15.4. Checklist Trước Mỗi Test

```
[ ] Đang chạy trong VM (không phải máy thật)
[ ] VM có snapshot sạch → tạo snapshot trước khi test
[ ] SSH đã thiết lập từ host vào VM
[ ] Auto-kill timer đã enable trong build config
[ ] Panic key combo đã nhớ: Ctrl+Shift+Alt+F12
[ ] Terminal host sẵn sàng: taskkill /F /IM TutorHub_LockdownCore.exe
[ ] File log path đã biết: %TEMP%\tutorhub\lockdown_<session>.log
```

---

## 16. Build Pipeline & Deployment

### 16.1. Build Rust Core

```powershell
# DEBUG build (dùng để test trên VM)
cargo build --features debug_mode
# Output: target/debug/TutorHub_LockdownCore.exe

# RELEASE build (production)
cargo build --release
# Output: target/release/TutorHub_LockdownCore.exe (~2-3MB sau strip)

# Verify size
ls -la target/release/TutorHub_LockdownCore.exe
```

### 16.2. Nhúng Rust Exe vào Java Resources

```
tutorhub_enterprise/
└── src/
    └── main/
        └── resources/
            └── tools/
                ├── TutorHub_LockdownCore.exe        # Production binary
                ├── TutorHub_LockdownCore.exe.sha256 # SHA-256 của exe
                └── TutorHub_LockdownCore_debug.exe  # Debug binary (exclude from release)
```

**Maven plugin để tính hash:**
```xml
<plugin>
  <groupId>net.nicoulaj.maven.plugins</groupId>
  <artifactId>checksum-maven-plugin</artifactId>
  <executions>
    <execution>
      <goals><goal>files</goal></goals>
    </execution>
  </executions>
  <configuration>
    <algorithms><algorithm>SHA-256</algorithm></algorithms>
    <fileSets>
      <fileSet>
        <directory>src/main/resources/tools</directory>
        <includes><include>*.exe</include></includes>
      </fileSet>
    </fileSets>
  </configuration>
</plugin>
```

### 16.3. LockdownManager.extractRustExe()

```java
private File extractRustExe() throws IOException {
    String exeName = "TutorHub_LockdownCore.exe";
    String hashFileName = exeName + ".sha256";
    
    // 1. Đọc expected hash từ resources
    String expectedHash = readResource("/tools/" + hashFileName).trim();
    
    // 2. Giải nén exe ra temp dir
    Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "tutorhub");
    Files.createDirectories(tempDir);
    Path exePath = tempDir.resolve(exeName);
    
    try (InputStream is = getClass().getResourceAsStream("/tools/" + exeName)) {
        Files.copy(is, exePath, StandardCopyOption.REPLACE_EXISTING);
    }
    
    // 3. Verify hash trước khi chạy
    String actualHash = computeSHA256(exePath.toFile());
    if (!actualHash.equalsIgnoreCase(expectedHash)) {
        Files.delete(exePath);
        throw new SecurityException("Lockdown binary integrity check failed!");
    }
    
    // 4. Set executable
    exePath.toFile().setExecutable(true);
    return exePath.toFile();
}
```

### 16.4. Code Signing (Production)

> [!WARNING]
> **Windows Defender sẽ chặn unsigned .exe!**
> 
> - Cần mua Code Signing Certificate (OV hoặc EV)
> - Sign exe trước khi đóng gói vào JAR:
>   `signtool sign /f cert.pfx /p password TutorHub_LockdownCore.exe`
> - EV Certificate cho phép bypass SmartScreen ngay lập tức
> - OV Certificate cần build reputation qua thời gian
> - Chi phí: ~$100-400/năm cho OV, ~$300-700/năm cho EV

---

## 17. Database Schema

(Giữ nguyên từ Blueprint v3.0, đây là schema đã được verify)

### `exams` — Quản lý Kỳ thi
```sql
CREATE TABLE exams (
    id              SERIAL PRIMARY KEY,
    creator_id      INT NOT NULL REFERENCES users(id),
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    duration_mins   INT NOT NULL DEFAULT 60,
    open_at         TIMESTAMP,
    close_at        TIMESTAMP,
    security_config JSONB DEFAULT '{
        "shuffle_questions": true,
        "shuffle_options": true,
        "secure_mode": false,
        "require_webcam": false,
        "max_violations": 3,
        "security_level": "medium",
        "blocked_processes": ["TeamViewer","AnyDesk","obs64"],
        "enable_vm_detection": true
    }',
    status          VARCHAR(20) DEFAULT 'DRAFT',
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);
```

### `questions` — Ngân hàng Câu hỏi
```sql
CREATE TABLE questions (
    id              SERIAL PRIMARY KEY,
    exam_id         INT REFERENCES exams(id) ON DELETE CASCADE,
    question_type   VARCHAR(20) NOT NULL DEFAULT 'MCQ',
    category        VARCHAR(100),
    difficulty      VARCHAR(10) DEFAULT 'MEDIUM',
    points          FLOAT DEFAULT 1.0,
    content         JSONB NOT NULL,
    explanation     TEXT,
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT NOW()
);
-- content MCQ example:
-- {"text":"...","options":[{"key":"A","text":"...","is_correct":true},...]}
```

### `exam_sessions` — Phiên Thi
```sql
CREATE TABLE exam_sessions (
    id              SERIAL PRIMARY KEY,
    exam_id         INT NOT NULL REFERENCES exams(id),
    user_id         INT NOT NULL REFERENCES users(id),
    status          VARCHAR(20) DEFAULT 'WAITING',
    -- WAITING→PRECHECK→IN_PROGRESS→SUBMITTED→FORCE_SUBMITTED→DISCONNECTED→VOIDED
    started_at      TIMESTAMP,
    submitted_at    TIMESTAMP,
    duration_used   INT,
    total_score     FLOAT,
    max_score       FLOAT,
    auto_graded     BOOLEAN DEFAULT FALSE,
    violation_count INT DEFAULT 0,
    trust_score_avg FLOAT DEFAULT 1.0,
    tek_hash        VARCHAR(64),
    client_info     JSONB,
    question_order  JSONB,
    exam_session_secret VARCHAR(64) NOT NULL, -- Server-generated, dùng cho TEK
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(exam_id, user_id)
);
```

### `exam_answers` — Bài Làm
```sql
CREATE TABLE exam_answers (
    id              SERIAL PRIMARY KEY,
    session_id      INT NOT NULL REFERENCES exam_sessions(id),
    question_id     INT NOT NULL REFERENCES questions(id),
    answer_data     JSONB,
    is_correct      BOOLEAN,
    score           FLOAT,
    time_spent_sec  INT,
    change_count    INT DEFAULT 0,
    last_updated    TIMESTAMP DEFAULT NOW(),
    UNIQUE(session_id, question_id)
);
```

### `anticheat_events` — Log Vi Phạm
```sql
CREATE TABLE anticheat_events (
    id              BIGSERIAL PRIMARY KEY,
    session_id      INT NOT NULL REFERENCES exam_sessions(id),
    event_type      VARCHAR(30) NOT NULL,
    -- LEAVE_SCREEN, BANNED_PROCESS, MULTI_MONITOR, NETWORK_LOST,
    -- WEBCAM_FACE_GONE, WEBCAM_MULTI_FACE, CLIENT_CRASH, FORCE_SUBMITTED
    severity        VARCHAR(10) DEFAULT 'LOW',
    details         JSONB,
    evidence_url    TEXT,
    trust_score     FLOAT,
    timestamp       TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_anticheat_session ON anticheat_events(session_id);
CREATE INDEX idx_anticheat_type ON anticheat_events(event_type);
```

---

## 18. API Packets Client-Server (TCP)

### Exam Packets

| Packet Type | Hướng | Payload | Mô tả |
|---|---|---|---|
| `EXAM_LIST_REQUEST` | C→S | `userId` | Lấy danh sách kỳ thi |
| `EXAM_LIST_RESPONSE` | S→C | `List<Exam>` JSON | Danh sách kỳ thi |
| `EXAM_START_REQUEST` | C→S | `examId\|userId\|tekHash\|clientInfo` | Xin vào phòng thi |
| `EXAM_START_RESPONSE` | S→C | `sessionId\|questions[]\|shuffledOrder\|examSecret` | Trả đề thi |
| `EXAM_SYNC_DRAFT` | C→S | `sessionId\|answersJson` | Đồng bộ nháp |
| `EXAM_SYNC_ACK` | S→C | `sessionId\|saved_at` | Xác nhận đã lưu nháp |
| `EXAM_SUBMIT` | C→S | `sessionId\|answersJson\|timeUsed` | Nộp bài chính thức |
| `EXAM_SUBMIT_ACK` | S→C | `sessionId\|score\|resultUrl` | Xác nhận đã nộp |
| `EXAM_HEARTBEAT` | C→S | `sessionId\|trustScore\|timestamp` | Heartbeat mỗi 5s |
| `EXAM_VIOLATION` | C→S | `sessionId\|eventType\|details\|severity` | Báo cáo vi phạm |
| `EXAM_FORCE_SUBMIT` | S→C | `sessionId\|reason` | GV ép nộp bài |
| `EXAM_MONITOR_UPDATE` | S→Teacher | `sessionId\|status\|violations\|progress` | Dashboard GV |

### Missing Packets (Cần thêm vào ClientHandler.java)

```java
// ClientHandler.java — Các case cần thêm:
case "EXAM_START_REQUEST":
    examPacketHandler.handleStartRequest(client, packet);
    break;
case "EXAM_SUBMIT":
    examPacketHandler.handleSubmit(client, packet);
    break;
case "EXAM_SYNC_DRAFT":
    examPacketHandler.handleSyncDraft(client, packet);
    break;
case "EXAM_HEARTBEAT":
    examPacketHandler.handleHeartbeat(client, packet);
    break;
case "EXAM_VIOLATION":
    antiCheatService.logViolation(packet);
    break;
```

---

## 19. Defense in Depth — 6 Lớp Bảo vệ

```
┌─────────────────────────────────────────────────────────┐
│  Lớp 5: Edge AI (Gaze, Face count, Trust Score) [Ph.4] │
├─────────────────────────────────────────────────────────┤
│  Lớp 4: Client Integrity (TEK Hash + HMAC)      [Ph.3] │
├─────────────────────────────────────────────────────────┤
│  Lớp 3: Process Monitor (Hash-based scan 3s)    [Ph.2] │
├─────────────────────────────────────────────────────────┤
│  Lớp 2: Screen Protection (WDA_EXCLUDEFROMCAPTURE)[Ph.2]│
├─────────────────────────────────────────────────────────┤
│  Lớp 1: OS Isolation (CreateDesktopW via Rust)  [Ph.2] │
├─────────────────────────────────────────────────────────┤
│  Lớp 0: Exam Logic (Timer, Auto-save, Submit)   [Ph.1] │
└─────────────────────────────────────────────────────────┘
```

| Lớp | Kỹ thuật | Tác dụng | Giới hạn |
|---|---|---|---|
| **0** | Timer + SQLite + TCP | Thi hoạt động đúng | Không chống gian lận |
| **1** | `CreateDesktopW` | Alt+Tab, Start Menu, Taskbar biến mất | Ctrl+Alt+Del vẫn lọt (kernel) |
| **2** | `SetWindowDisplayAffinity` | OBS, Discord, Snipping Tool thấy đen | Hardware capture card bypass được |
| **3** | SHA-256 process scan mỗi 3s | Phát hiện TeamViewer dù đổi tên exe | Cần update hash DB thường xuyên |
| **4** | HMAC-SHA256 TEK | Chống decompile + sửa client | Memory patching runtime khó chặn |
| **5** | ONNX face/gaze detection | Phát hiện nhìn sang bên, người lạ | Phụ thuộc ánh sáng, góc camera |

---

## 20. Checklist Triển khai cho AI Agent

### Phase 1 (Java) — Backend & UI

- [ ] **1.1** Verify schema PostgreSQL — 5 bảng (exams, questions, exam_sessions, exam_answers, anticheat_events) + thêm cột `exam_session_secret` vào `exam_sessions`
- [ ] **1.2** Implement `ExamDAO.submitExam()` — parse answersJson, lưu từng câu vào `exam_answers`, update `exam_sessions.status = COMPLETED`
- [ ] **1.3** Map `EXAM_START_REQUEST` trong `ClientHandler.java` → `ExamPacketHandler.handleStartRequest()`
- [ ] **1.4** Map `EXAM_SUBMIT` trong `ClientHandler.java` → `ExamPacketHandler.handleSubmit()`
- [ ] **1.5** Map `EXAM_SYNC_DRAFT` trong `ClientHandler.java` → `ExamPacketHandler.handleSyncDraft()`
- [ ] **1.6** Map `EXAM_HEARTBEAT` trong `ClientHandler.java` → lưu vào session
- [ ] **1.7** Map `EXAM_VIOLATION` trong `ClientHandler.java` → `AntiCheatService.logViolation()`
- [ ] **1.8** `GradingService.gradeSession()` — auto-grade MCQ khi submit

### Phase 2 (Rust) — Lockdown Core

- [ ] **2.0.1** PoC: `CreateDesktopW` + `SwitchDesktop` + `CloseDesktop` trên VM
- [ ] **2.0.2** PoC: `SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)` chặn OBS
- [ ] **2.0.3** PoC: Named Pipe server nhận "PING" → trả "PONG"
- [ ] **2.0.4** PoC: `SetWindowsHookExW(WH_KEYBOARD_LL)` chặn Win key
- [ ] **2.0.5** PoC: `CreateToolhelp32Snapshot` → liệt kê process
- [ ] **2.1** `cargo new tutorhub_lockdown` với Cargo.toml đúng dependencies
- [ ] **2.2** `error.rs` — `LockdownError` enum + Result alias
- [ ] **2.3** `config.rs` — `ExamConfig` struct + parse + validate
- [ ] **2.4** `logger.rs` — File logger thread-safe (Mutex<BufWriter<File>>)
- [ ] **2.5** `ipc.rs` — Named Pipe server + IpcCommand enum + read/write
- [ ] **2.6** `desktop.rs` — create + switch + switch_back + Drop impl
- [ ] **2.7** `screen_protection.rs` — enable + disable, EnumWindows by PID
- [ ] **2.8** `keyboard_hook.rs` — install + uninstall + message loop thread + panic key (debug)
- [ ] **2.9** `process_scanner.rs` — init + scan loop + SHA-256 cache + alert via IPC
- [ ] **2.10** `watchdog.rs` — Watchdog struct + timeout thread + record_ping
- [ ] **2.11** `vm_detection.rs` — CPUID + MAC + driver check
- [ ] **2.12** `main.rs` — OperationSequence: parse → vm_check → pipe → accept → lock → main_loop
- [ ] **2.13** Auto-kill timer 60s (debug_mode feature)
- [ ] **2.14** Build release binary + strip + verify size < 5MB
- [ ] **2.15** `LockdownManager.java` — extractRustExe + verifyHash + spawn + lifecycle
- [ ] **2.16** `RustIPCClient.java` — connect pipe + PING thread + receive alerts
- [ ] **2.17** `EnvironmentChecker.java` — pre-exam checks
- [ ] **2.18** Integration test: Java spawn Rust → LOCK → PING loop → UNLOCK

### Phase 3 (Java) — Integrity & Proctoring

- [ ] **3.1** Maven plugin tính SHA-256(jar) vào build.properties
- [ ] **3.2** `IntegrityVerifier.java` tính TEK + gửi qua EXAM_START_REQUEST
- [ ] **3.3** Server verify TEK trong `ExamPacketHandler`
- [ ] **3.4** `ProctorDashboard.java` — realtime grid view
- [ ] **3.5** `SnapshotCapture.java` + `SnapshotUploader.java` (Backblaze S3)

---

## 21. Tiêu chí Hoàn thành (Definition of Done)

### Phase 2 — Rust Lockdown Core

| Chức năng | Tiêu chí kiểm tra |
|---|---|
| Desktop Isolation | Alt+Tab không switch sang app khác; Taskbar biến mất |
| Screen Protection | OBS thấy màn hình đen (test WDA_EXCLUDEFROMCAPTURE) |
| Keyboard Hook | Win key không mở Start Menu; PrintScreen không chụp được |
| Process Scanner | Mở TeamViewer → Java nhận PROCESS_ALERT trong ≤ 3s |
| Watchdog | Kill Java process → máy thoát Kiosk trong ≤ 10s |
| IPC | 1000 PING/PONG không fail (stress test 30 phút) |
| VM Detection | VirtualBox bị phát hiện; máy thật không false positive |
| Panic Key | Ctrl+Shift+Alt+F12 thoát kiosk tức thì (DEBUG build) |
| Auto-kill Timer | Kiosk tự thoát sau 60s (DEBUG build) |
| Clean Shutdown | UNLOCK → màn hình bình thường, không artifact |
| Crash Recovery | Kill Rust → Java fallback, màn hình bình thường ≤ 10s |
| Log | Log file có đủ events, không corrupt sau crash |

---

## 22. Rủi ro Kỹ thuật & Xử lý

| # | Rủi ro | Mức độ | Xác suất | Xử lý |
|---|---|---|---|---|
| 1 | CreateDesktopW bug → kẹt màn hình | NGHIÊM TRỌNG | Trung bình | Test VM only; Watchdog 10s; Drop impl |
| 2 | Windows Defender chặn Rust exe | CAO | Cao | Code Signing (EV cert); Whitelist hướng dẫn IT |
| 3 | Antivirus false positive (SEB history) | CAO | Cao | Submit to AV vendors; Signed binary |
| 4 | SetThreadDesktop fail nếu thread có hook | CAO | Thấp | Đặt Desktop TRƯỚC khi install hook; log lỗi |
| 5 | Named Pipe race condition | TRUNG BÌNH | Thấp | Retry logic 10 lần × 500ms |
| 6 | VM detection false positive | TRUNG BÌNH | Trung bình | Chỉ hard fail với MAC check; CPUID = warning |
| 7 | Mất mạng giữa bài thi | CAO | Cao | SQLite auto-save 30s; offline timer |
| 8 | Học sinh decompile JAR | CAO | Trung bình | TEK Hash + HMAC + ProGuard obfuscation |
| 9 | Screen capture bằng hardware | THẤP | Thấp | Không thể chặn 100%; chấp nhận |
| 10 | Gian lận bằng điện thoại | TRUNG BÌNH | Cao | Edge AI (Phase 4); Open-book style đề |
| 11 | Windows 10 version <1803 | THẤP | Thấp | WDA_EXCLUDEFROMCAPTURE cần Win10 2004+; fallback WDA_MONITOR |
| 12 | Message loop thread deadlock | CAO | Thấp | Timeout trên all wait calls; PostThreadMessage(WM_QUIT) để exit |
| 13 | Process hash cache memory leak | THẤP | Thấp | Giới hạn cache size; LRU eviction |

---

## 23. Những gì KHÔNG làm ở Phase 2

> Không làm những thứ sau ở Phase 2, dù có vẻ liên quan:

- ❌ **Edge AI (ONNX)** — Phase 4
- ❌ **WebSocket upgrade** — Phase 3
- ❌ **Backblaze S3 upload** — Phase 3
- ❌ **IntegrityVerifier (TEK)** — Phase 3
- ❌ **ProctorDashboard** — Phase 3
- ❌ **gRPC / Kafka** — Phase 4
- ❌ **Kill Explorer.exe** — Không bao giờ làm (quá xâm phạm)
- ❌ **Chặn Ctrl+Alt+Delete** — Không thể (kernel-level, SEB cũng không làm được với new desktop mode)
- ❌ **macOS/Linux lockdown** — Phase 2 chỉ Windows; macOS dùng AAC API (Phase sau)
- ❌ **SQLCipher** — Cân nhắc, nhưng không làm ngay Phase 2 (tăng complexity)
- ❌ **Audio VAD (Voice Detection)** — Phase 4
- ❌ **Docker Code Sandbox** — Phase sau (cho câu hỏi lập trình)

---

## 24. Prompt chuẩn cho AI Agent

### 24.1. Prompt cho Phase 2 (Rust Core)

```text
Bạn là Rust Systems Programmer cho dự án TutorHub Secure Exam Mode.

KIẾN THỨC NỀN:
Đọc toàn bộ file MASTER_SECURE_EXAM_BLUEPRINT_v4.md trước khi code.
Tập trung các Section: 3, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16.

BỐI CẢNH DỰ ÁN:
- TutorHub Desktop App: Java (Swing + JCEF) + TCP Socket
- Mục tiêu Phase 2: xây dựng TutorHub_LockdownCore.exe (Rust) chạy song song JVM
- Giao tiếp: Named Pipe \\.\pipe\TutorHubExam_<sessionId>
- Kiosk mode: CreateDesktopW (Windows only)

NHIỆM VỤ:
1. Mở docs/secure_exam_tasks_v2.md → đánh dấu tiến độ liên tục
2. Tạo project: cargo new tutorhub_lockdown
3. Implement theo thứ tự: error.rs → config.rs → logger.rs → ipc.rs → desktop.rs 
   → screen_protection.rs → keyboard_hook.rs → process_scanner.rs → watchdog.rs 
   → vm_detection.rs → main.rs
4. Mỗi module phải có unit test trong tests/

RÀNG BUỘC BẮT BUỘC:
- Chỉ dùng windows crate (không JNA, không C++ watchdog)
- debug_mode feature: auto-kill 60s + panic key Ctrl+Shift+Alt+F12
- Chỉ test trên VM (Windows Sandbox, VMware, VirtualBox)
- Mỗi Windows API call phải wrap trong hàm riêng với Result<_, LockdownError>
- Không dùng unwrap() / expect() trong production code path
- Tất cả shared state qua Arc<Mutex<T>>
- Log tất cả event vào file %TEMP%\tutorhub\lockdown_<session_id>.log
- Không commit debug_mode feature vào release build

ĐẦU RA MONG ĐỢI:
- tutorhub_lockdown/ project hoàn chỉnh
- cargo build --release thành công
- TutorHub_LockdownCore.exe < 5MB
- Test pass: ipc_test.rs, config_test.rs, process_scanner_test.rs

CẬP NHẬT TIẾN ĐỘ:
Sau mỗi module hoàn thành, đánh dấu [x] tương ứng trong secure_exam_tasks_v2.md.
```

### 24.2. Prompt cho Phase 1 (Java Backend)

```text
Bạn là Senior Java Developer cho dự án TutorHub Secure Exam Mode.

NHIỆM VỤ (Phase 1):
Tập trung vào Section 17, 18 của MASTER_SECURE_EXAM_BLUEPRINT_v4.md.

1. Mở secure_exam_tasks_v2.md → theo dõi tiến độ
2. Verify database schema (5 bảng) → thêm cột exam_session_secret vào exam_sessions
3. Implement ExamDAO.submitExam() — lưu answers + update status = COMPLETED
4. Map trong ClientHandler.java:
   - EXAM_START_REQUEST → ExamPacketHandler.handleStartRequest()
   - EXAM_SUBMIT → ExamPacketHandler.handleSubmit()  
   - EXAM_SYNC_DRAFT → ExamPacketHandler.handleSyncDraft()
   - EXAM_HEARTBEAT → lưu timestamp
   - EXAM_VIOLATION → AntiCheatService.logViolation()
5. GradingService.gradeSession() — auto-grade MCQ
6. ExamCreatorPanel — UI tạo đề (đã có, cần polish)
7. ExamTakingPanel — tích hợp JCEF countdown timer + auto-save 30s

KHÔNG làm trong Phase 1: Rust code, IPC, Lockdown, Camera, AI
```

---

## Phụ lục A: Windows API Reference nhanh

| API | Module Rust | Mục đích |
|---|---|---|
| `CreateDesktopW` | `desktop.rs` | Tạo virtual desktop mới |
| `SwitchDesktop` | `desktop.rs` | Chuyển sang desktop khác |
| `CloseDesktop` | `desktop.rs` | Đóng desktop |
| `GetThreadDesktop` | `desktop.rs` | Lấy desktop hiện tại |
| `SetThreadDesktop` | `desktop.rs` | Gán desktop cho thread |
| `SetWindowDisplayAffinity` | `screen_protection.rs` | Chặn screen capture |
| `EnumWindows` | `screen_protection.rs` | Liệt kê tất cả window |
| `GetWindowThreadProcessId` | `screen_protection.rs` | Tìm window theo PID |
| `SetWindowsHookExW` | `keyboard_hook.rs` | Cài low-level hook |
| `UnhookWindowsHookEx` | `keyboard_hook.rs` | Gỡ hook |
| `CallNextHookEx` | `keyboard_hook.rs` | Chuyển event cho hook tiếp |
| `GetMessageW` | `keyboard_hook.rs` | Message loop |
| `PostThreadMessageW` | `keyboard_hook.rs` | Gửi WM_QUIT để exit loop |
| `CreateNamedPipeW` | `ipc.rs` | Tạo pipe server |
| `ConnectNamedPipe` | `ipc.rs` | Chờ client kết nối |
| `ReadFile` / `WriteFile` | `ipc.rs` | Đọc/ghi pipe |
| `DisconnectNamedPipe` | `ipc.rs` | Ngắt kết nối |
| `CreateToolhelp32Snapshot` | `process_scanner.rs` | Snapshot process list |
| `Process32FirstW` / `Process32NextW` | `process_scanner.rs` | Duyệt processes |
| `QueryFullProcessImageNameW` | `process_scanner.rs` | Lấy path đầy đủ của exe |
| `GetAdaptersAddresses` | `vm_detection.rs` | Lấy MAC address |

## Phụ lục B: Checklist Bảo mật Tài liệu

- [x] Cho phép nghiên cứu sâu và chuyển hóa logic từ SEB sang Rust; không copy nguyên văn code nếu chưa kiểm tra license. Mọi phần chuyển đổi sát từ SEB phải được ghi chú rõ và kiểm tra nghĩa vụ MPL-2.0 trước khi dùng chính thức.
- [x] Mọi unsafe block phải được comment giải thích lý do
- [x] Debug feature không được commit vào release branch
- [x] Code Signing certificate cần mua trước khi production
- [x] VM detection whitelist phải được review định kỳ (tránh false positive hardware mới)
- [x] Blacklist process hash phải được cập nhật khi có phiên bản mới của banned apps

---

> **Tài liệu này kết thúc tại đây.**  
> Phiên bản tiếp theo (v5.0) sẽ được tạo sau khi Phase 2 hoàn thành và cần tài liệu cho Phase 3.
