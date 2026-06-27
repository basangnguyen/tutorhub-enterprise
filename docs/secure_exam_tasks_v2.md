# Danh SÃ¡ch CÃ´ng Viá»‡c: TutorHub Secure Exam Mode
# PhiÃªn báº£n: 2.0 â€” Cáº­p nháº­t sau nghiÃªn cá»©u SEB 3.10.1

> **Kiáº¿n trÃºc:** Java Shell (JCEF UI) + Rust Core (Lockdown OS)  
> **Database:** PostgreSQL (Neon) + JSONB  
> **TÃ i liá»‡u Ä‘áº§y Ä‘á»§:** Äá»c `MASTER_SECURE_EXAM_BLUEPRINT_v4.md` trÆ°á»›c khi code

- `[ ]` ChÆ°a báº¯t Ä‘áº§u
- `[/]` Äang tiáº¿n hÃ nh
- `[x]` ÄÃ£ hoÃ n thÃ nh

---

## âœ… ÄÃƒ HOÃ€N THÃ€NH (Tá»« codebase hiá»‡n táº¡i)

- `[x]` Database schema cÆ¡ báº£n (exams, questions, exam_sessions, exam_answers, anticheat_events)
- `[x]` ExamDAO.java â€” createExam, getExamsByCreator, addQuestion, getQuestionsByExam, createSession, saveAnswer (stub)
- `[x]` ExamService.java (stub)
- `[x]` ExamTakingPanel.java (UI cÆ¡ báº£n)
- `[x]` ExamCreatorPanel.java (UI cÆ¡ báº£n)
- `[x]` OfflineExamCache.java (SQLite cÆ¡ báº£n)

---

## PHASE 1: Database & API Backend (Java)
> **Má»¥c tiÃªu:** HoÃ n thiá»‡n luá»“ng dá»¯ liá»‡u thi Ä‘áº§y Ä‘á»§  
> **Äiá»u kiá»‡n hoÃ n thÃ nh:** GV táº¡o Ä‘á» â†’ HS thi â†’ Há»‡ thá»‘ng lÆ°u vÃ  cháº¥m Ä‘iá»ƒm

### 1.1 Database Schema (PostgreSQL)
- `[ ]` ThÃªm cá»™t `exam_session_secret VARCHAR(64) NOT NULL` vÃ o báº£ng `exam_sessions`
- `[ ]` Verify Ä‘á»§ 5 báº£ng: exams, questions, exam_sessions, exam_answers, anticheat_events
- `[ ]` Táº¡o index: `idx_anticheat_session`, `idx_anticheat_type`, `idx_anticheat_severity`
- `[ ]` Test migration script khÃ´ng lá»—i trÃªn Neon DB

### 1.2 ExamDAO.java â€” HoÃ n thiá»‡n
- `[ ]` Implement `submitExam(sessionId, List<Answer>)`:
  - Parse tá»«ng cÃ¢u tráº£ lá»i
  - Gá»i `saveAnswer()` cho má»—i cÃ¢u
  - Update `exam_sessions.status = 'COMPLETED'`
  - Update `exam_sessions.submitted_at = NOW()`
  - Update `exam_sessions.duration_used`
- `[ ]` Implement `generateSessionSecret()` â€” random 32 bytes â†’ hex string
- `[ ]` Implement `getSessionWithSecret(sessionId)` â€” join vá»›i secret Ä‘á»ƒ verify TEK

### 1.3 ClientHandler.java â€” Map Packets
- `[ ]` Map `EXAM_START_REQUEST` â†’ `ExamPacketHandler.handleStartRequest()`
- `[ ]` Map `EXAM_SUBMIT` â†’ `ExamPacketHandler.handleSubmit()`
- `[ ]` Map `EXAM_SYNC_DRAFT` â†’ `ExamPacketHandler.handleSyncDraft()`
- `[ ]` Map `EXAM_HEARTBEAT` â†’ `ExamPacketHandler.handleHeartbeat()`
- `[ ]` Map `EXAM_VIOLATION` â†’ `AntiCheatService.logViolation()`

### 1.4 ExamPacketHandler.java
- `[ ]` `handleStartRequest()`:
  - Validate examId tá»“n táº¡i vÃ  Ä‘ang PUBLISHED
  - Táº¡o ExamSession má»›i vá»›i status = IN_PROGRESS
  - Generate examSessionSecret
  - Shuffle cÃ¢u há»i (náº¿u config.shuffle_questions = true)
  - Shuffle options (náº¿u config.shuffle_options = true)
  - LÆ°u question_order vÃ o exam_sessions
  - Tráº£ EXAM_START_RESPONSE vá»›i questions + shuffledOrder + sessionId + examSecret
- `[ ]` `handleSubmit()`:
  - Gá»i ExamDAO.submitExam()
  - Gá»i GradingService.gradeSession() cho MCQ
  - Tráº£ EXAM_SUBMIT_ACK vá»›i score
- `[ ]` `handleSyncDraft()`:
  - Gá»i ExamDAO.saveAnswer() cho tá»«ng cÃ¢u
  - Tráº£ EXAM_SYNC_ACK
- `[ ]` `handleHeartbeat()`:
  - Update exam_sessions.last_heartbeat (náº¿u thÃªm cá»™t nÃ y)

### 1.5 GradingService.java
- `[ ]` `gradeSession(sessionId)`:
  - Láº¥y táº¥t cáº£ answers cá»§a session
  - Vá»›i MCQ/True-False: so sÃ¡nh selected vá»›i correct â†’ tÃ­nh score
  - Update `exam_answers.is_correct` vÃ  `exam_answers.score`
  - TÃ­nh `exam_sessions.total_score` vÃ  `exam_sessions.max_score`
  - Set `exam_sessions.auto_graded = true`
- [x] **Step 4.9:** Parse submitPayload JSON and save individual answers to DB `exam_answers`. Do NOT auto-save during exam yet.
- [x] **Step 4.10:** Phase 1 Stabilization & Cleanup (Giáº£m Ä‘á»™ dÃ i log JSON, thÃªm tháº» meta UTF-8, Ä‘áº£m báº£o Network disconnect vÃ  JCEF cleanup khi Ä‘Ã³ng form, khÃ´ng thÃªm tÃ­nh nÄƒng thá»«a).

### 1.8 TSE UI & Network Integration (Phase 1 Recovery)
- `[x]` Step 4.1: Táº¡o Data Model / DTO / Context cho TSE
- `[x]` Step 4.2: Táº¡o TSEExamService interface
- `[x]` Step 4.3: Táº¡o MockTSEExamService Ä‘á»ƒ UI cháº¡y Ä‘Æ°á»£c khÃ´ng cáº§n server
- `[x]` Step 4.4: Táº¡o NetworkTSEExamService dÃ¹ng NetworkManager tháº­t
- `[x]` Step 4.5: Test end-to-end vá»›i server

### 1.9 Checklist Test Phase 1 (End-to-End Core Network)
- `[ ]` Khá»Ÿi Ä‘á»™ng Server + káº¿t ná»‘i Neon Database thÃ nh cÃ´ng.
- `[ ]` Client login thÃ nh cÃ´ng qua websocket.
- `[ ]` Client GET_CONFIG_LIST láº¥y Ä‘Æ°á»£c danh sÃ¡ch bÃ i thi status ACTIVE tá»« database.
- `[ ]` Nháº­p máº­t kháº©u / Báº¯t Ä‘áº§u thi gá»­i EXAM_START_REQUEST thÃ nh cÃ´ng.
- `[ ]` Server táº¡o Ä‘Æ°á»£c `exam_sessions` (IN_PROGRESS) vÃ  tráº£ vá» ná»™i dung cÃ¢u há»i chuáº©n cáº¥u trÃºc HTML UTF-8.
- `[ ]` JCEF load bÃ i thi hiá»ƒn thá»‹ Tiáº¿ng Viá»‡t Ä‘Ãºng chuáº©n, tÆ°Æ¡ng tÃ¡c mÆ°á»£t.
- `[ ]` Khi ná»™p bÃ i (EXAM_SUBMIT), Browser DOM tá»± parse toÃ n bá»™ Ä‘Ã¡p Ã¡n JSON vÃ  gá»­i qua Java CEF Message Router.
- `[ ]` Server báº¯t JSON, validate, lÆ°u tá»«ng cÃ¢u tráº£ lá»i vÃ o `exam_answers`. Update `exam_sessions` thÃ nh SUBMITTED.
- `[ ]` ÄÃ³ng mÃ n hÃ¬nh thi: `TSEJcefLifecycleManager.cleanup()` vÃ  `NetworkManager.disconnect()` cháº¡y mÆ°á»£t khÃ´ng káº¹t á»©ng dá»¥ng.
---

## PHASE 2: Rust Security Core (Lockdown)
> **Má»¥c tiÃªu:** OS-level lockdown ngang Safe Exam Browser  
> **âš ï¸ Báº®T BUá»˜C Ä‘á»c:** `MASTER_SECURE_EXAM_BLUEPRINT_v4.md` Section 15 â€” YÃªu cáº§u An toÃ n Dev  
> **âš ï¸ CHá»ˆ TEST TRÃŠN VM:** Windows Sandbox / VMware / VirtualBox  
> **Äiá»u kiá»‡n hoÃ n thÃ nh:** Há»c sinh khÃ´ng thoÃ¡t Ä‘Æ°á»£c, OBS tháº¥y Ä‘en, Watchdog cá»©u mÃ¡y  
> **Tráº¡ng thÃ¡i:** Rust core foundation modules Ä‘Ã£ implement vÃ  cargo check/test pass.

### 2.0 Proof of Concept (PoC) â€” TrÃªn VM trÆ°á»›c khi code chÃ­nh thá»©c

- `[ ]` **PoC 2.0.1:** `CreateDesktopW` + `SwitchDesktop` + `CloseDesktop` tá»« Rust (trÃªn VM)
  - Káº¿t quáº£ mong Ä‘á»£i: MÃ n hÃ¬nh switch sang desktop trá»‘ng, sau Ä‘Ã³ switch láº¡i
- `[ ]` **PoC 2.0.2:** `SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)` (trÃªn VM)
  - Káº¿t quáº£ mong Ä‘á»£i: OBS tháº¥y mÃ n hÃ¬nh Ä‘en khi apply, Ä‘en bÃ¬nh thÆ°á»ng sau khi reset
- `[ ]` **PoC 2.0.3:** Named Pipe server Rust â†” client test (println trÃªn VM)
  - Káº¿t quáº£ mong Ä‘á»£i: Server nháº­n "PING" â†’ tráº£ "PONG" reliably
- `[ ]` **PoC 2.0.4:** `SetWindowsHookExW(WH_KEYBOARD_LL)` cháº·n Win key (trÃªn VM)
  - Káº¿t quáº£ mong Ä‘á»£i: Win key khÃ´ng má»Ÿ Start Menu
- `[ ]` **PoC 2.0.5:** `CreateToolhelp32Snapshot` liá»‡t kÃª process, tÃ­nh SHA-256 má»™t exe
  - Káº¿t quáº£ mong Ä‘á»£i: In ra danh sÃ¡ch process + hash cá»§a notepad.exe

### 2.1 Project Setup
- `[x]` `cargo new tutorhub_lockdown` (binary project)
- `[x]` Cáº¥u hÃ¬nh `Cargo.toml` theo spec trong MASTER_BLUEPRINT_v4.md Section 3.3
- `[x]` Táº¡o cáº¥u trÃºc thÆ° má»¥c: src/ vá»›i táº¥t cáº£ module files

### 2.2 Foundation Modules
- `[x]` **`error.rs`:** `LockdownError` enum vá»›i táº¥t cáº£ variants:
  ```
  ConfigParseFailed, PipeCreateFailed, PipeConnectFailed,
  DesktopCreateFailed, DesktopSwitchFailed, HookInstallFailed,
  VmDetected, HeartbeatTimeout, UnexpectedCommand
  ```
- `[x]` **`config.rs`:** `ExamConfig` struct + `from_base64_json()` + `validate()`
- `[x]` **`logger.rs`:** File logger thread-safe, format `[timestamp] [thread] [level] msg`

### 2.3 IPC Module
- `[x]` **`ipc.rs`:**
  - `create_pipe(session_id)` â†’ `CreateNamedPipeW`
  - `accept_connection(pipe, timeout)` â†’ `ConnectNamedPipe`
  - `read_command(client)` â†’ Ä‘á»c Ä‘áº¿n `\n`, parse `IpcCommand`
  - `send_response(client, resp)` â†’ serialize + `WriteFile`
  - `close(client)` â†’ `DisconnectNamedPipe` + `CloseHandle`
  - `IpcCommand` enum: Lock, Unlock, Ping, Status, Unknown
  - `IpcResponse` enum: Locked, Unlocked, LockFailed, Pong, StatusResponse, ProcessAlert
- `[x]` Unit test `ipc_test.rs`: connect + send + receive + close (khÃ´ng cáº§n VM)

### 2.4 Desktop Isolation Module
- `[x]` **`desktop.rs`:**
  - `ExamDesktop` struct vá»›i `handle: HDESK` + `original_desktop: HDESK`
  - `create(name)` â†’ `CreateDesktopW` + lÆ°u original
  - `switch_to()` â†’ `SwitchDesktop` + `SetThreadDesktop`
  - `switch_to_default()` â†’ switch vá» original
  - `handle_as_u64()` â†’ cast HDESK â†’ u64
  - `impl Drop`: tá»± Ä‘á»™ng switch_to_default + CloseDesktop
- `[ ]` Test trÃªn VM: create â†’ switch â†’ verify trá»‘ng â†’ switch_back â†’ close

### 2.5 Screen Protection Module
- `[x]` **`screen_protection.rs`:**
  - `enable(java_pid)`: `EnumWindows` tÃ¬m Java window â†’ `SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)`
  - `disable(java_pid)`: `SetWindowDisplayAffinity(WDA_NONE)`
  - `apply_to_all_java_windows(java_pid)` â†’ apply táº¥t cáº£ window cá»§a JVM
- `[ ]` Test trÃªn VM vá»›i OBS Ä‘ang má»Ÿ

### 2.6 Keyboard Hook Module
- `[x]` **`keyboard_hook.rs`:**
  - `install()` â†’ `SetWindowsHookExW(WH_KEYBOARD_LL, hook_proc, NULL, 0)`
  - `uninstall()` â†’ `UnhookWindowsHookEx`
  - `run_message_loop()` â†’ `GetMessageW` loop (PHáº¢I cháº¡y trÃªn hook thread)
  - Blocked keys: VK_LWIN, VK_RWIN, Alt+Tab, Alt+F4, PrintScreen
  - `static HOOK_HANDLE: AtomicPtr<HHOOK>` Ä‘á»ƒ uninstall tá»« thread khÃ¡c
- `[x]` TÃ­nh nÄƒng Panic Key (`Ctrl+Shift+Alt+F12` giáº¿t process ngay) - chá»‰ hoáº¡t Ä‘á»™ng náº¿u debug_mode báº­t
  - *Ghi chÃº test VM:* Do VMware cÃ³ thá»ƒ "nuá»‘t" tá»• há»£p phÃ­m `Ctrl+Alt` vÃ  `F12`, viá»‡c test trÃªn mÃ¡y áº£o sá»­ dá»¥ng thÃªm fallback key lÃ  `Ctrl+Shift+P` thÃ´ng qua má»™t polling thread (`GetAsyncKeyState`) riÃªng biá»‡t. CÃ¡c tÃ­nh nÄƒng nÃ y chá»‰ tá»“n táº¡i trong `debug_mode`.
- `[ ]` Test trÃªn VM: Win key bá»‹ cháº·n, Alt+Tab bá»‹ cháº·n, panic key hoáº¡t Ä‘á»™ng

### 2.7 Process Scanner Module
- `[x]` **`process_scanner.rs`:**
  - `init(banned_names, banned_hashes)` â†’ lÆ°u blacklist + baseline scan
  - `scan_once()` â†’ `CreateToolhelp32Snapshot` â†’ check tá»«ng process
  - TÃ­nh SHA-256 process qua PID â†’ path â†’ file read (cÃ³ cache)
  - Sinh event `PROCESS_ALERT` khÃ´ng tá»± kill process
- `[x]` Test unit `hash_test.rs`
  - Background thread cháº¡y má»—i 3s
  - Gá»­i alert qua channel (khÃ´ng trá»±c tiáº¿p qua pipe tá»« thread nÃ y)
  - Hardcoded `DEFAULT_BANNED_NAMES` list (xem Section 12.2 blueprint)
- `[ ]` Test: cháº¡y TeamViewer trÃªn VM â†’ alert xuáº¥t hiá»‡n trong â‰¤ 3s

### 2.8 Watchdog Module
- `[x]` **`watchdog.rs`:**
  - `Watchdog` struct vá»›i `last_ping: Arc<Mutex<Instant>>` + `timeout: Duration`
  - `start(on_timeout: impl Fn() + Send + 'static)` â†’ spawn timer thread
  - `record_ping()` â†’ update last_ping
  - `stop()` â†’ send shutdown signal qua channel
  - Thread: má»—i 1s check elapsed â†’ náº¿u > timeout â†’ gá»i on_timeout
- `[x]` Test: khÃ´ng gá»­i PING 10s â†’ on_timeout callback Ä‘Æ°á»£c gá»i

### 2.9 VM Detection Module
- `[x]` **`vm_detection.rs`:**
  - `check()` â†’ cháº¡y 3 lá»›p, tráº£ `Ok(())` hoáº·c `Err(VmError)`
  - `check_cpuid_hypervisor()` â†’ CPUID bit 31 ECX (WARNING khÃ´ng hard fail)
  - `get_mac_addresses()` â†’ `GetAdaptersAddresses` â†’ check blacklist (Máº NH)
  - `check_vm_files()` â†’ kiá»ƒm tra file driver/system cá»§a VirtualBox/VMware/QEMU
- `[x]` Test unit MAC prefix match & string match logic
  - Driver list: vboxguest.sys, vmhgfs.sys, vmtoolsd.exe, VBoxService.exe
- `[ ]` Test trÃªn VirtualBox: bá»‹ phÃ¡t hiá»‡n; test trÃªn mÃ¡y tháº­t: PASS

### 2.10 Auto-Kill Timer (Debug Only)
- `[x]` **Trong `main.rs`:**
  ```rust
  #[cfg(feature = "debug_mode")]
  spawn_auto_kill_timer(120); // 120 giÃ¢y
  ```
- `[x]` Verify: build `--features debug_mode` â†’ process tá»± exit sau 120s
- `[x]` Verify: build `--release` (khÃ´ng `debug_mode`) â†’ KHÃ”NG cÃ³ timer (ÄÃ£ build production, loáº¡i bá» log debug vÃ  polling thread)

### 2.11 Main Entry Point
- `[x]` **`main.rs`:**
  - Parse args: `--session-id <id>` + `--config <base64>`
  - Init logger
  - `#[cfg(feature = "debug_mode")]` Auto-kill timer
  - VM detection (náº¿u config.enable_vm_detection)
  - Create Named Pipe server
  - Accept connection (timeout 10s)
  - Read LOCK command
  - Execute lockdown sequence (theo Section 6.2 blueprint)
  - Main loop: receive PING â†’ update watchdog, receive UNLOCK â†’ shutdown, receive PROCESS_ALERT chan â†’ forward via IPC
- `[ ]` Integration test: Java mock â†’ spawn Rust â†’ LOCK â†’ PING 30s â†’ UNLOCK â†’ exit

### 2.12 Build & Package
- `[x]` Build release: `cargo build --release`
- `[x]` Verify size: exe < 5MB
- `[x]` TÃ­nh SHA-256 cá»§a release exe
- `[x]` Copy exe vÃ o `src/main/resources/tools/TutorHub_LockdownCore.exe`
- `[x]` LÆ°u SHA-256 vÃ o `src/main/resources/tools/TutorHub_LockdownCore.exe.sha256`

### 2.13 Java Integration (LockdownManager.java)
- `[x]` `extractRustExe()` â€” giáº£i nÃ©n tá»« JAR â†’ temp dir + verify SHA-256
- `[x]` `buildLockConfig(ExamSession)` â†’ táº¡o JSON config â†’ Base64 encode
- `[x]` `spawnRustProcess(exePath, config)` â†’ ProcessBuilder
- `[x]` `waitForPipeReady(sessionId)` â†’ retry 10 láº§n Ã— 500ms
- `[x]` `sendLockCommand(sessionId)` â†’ gá»­i LOCK + chá» LOCKED/LOCK_FAILED
- `[x]` `startPingThread()` â†’ daemon thread gá»­i PING má»—i 2s
- `[x]` `emergencyUnlock()` â†’ fallback náº¿u Rust máº¥t liÃªn láº¡c
- `[x]` `shutdown()` â†’ gá»­i UNLOCK + chá» UNLOCKED + cleanup

### 2.14 Java IPC Client (RustIPCClient.java)
- `[x]` Connect Named Pipe: `\\.\pipe\TutorHubExam_<sessionId>`
- `[x]` Write/Read vá»›i UTF-8 encoding + newline framing
- `[x]` Listener thread nháº­n PROCESS_ALERT â†’ chuyá»ƒn sang Java event system
- `[x]` Reconnect logic náº¿u pipe disconnect báº¥t ngá»

### 2.15 EnvironmentChecker.java
- `[x]` `checkNetworkConnectivity()` â€” ping server endpoint
- `[x]` `checkDisplayCount()` â€” `GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length`
- `[x]` `checkWebcamAvailable()` â€” náº¿u require_webcam
- `[x]` `checkRunningProcesses(List<String> banned)` â€” liá»‡t kÃª process Windows, check tÃªn
- `[x]` `checkForVirtualMachine()` â€” gá»i Rust PoC exe náº¿u secure_mode

### 2.16 Integration Testing (TrÃªn VM)
- `[x]` Test: Java spawn Rust â†’ káº¿t ná»‘i pipe â†’ gá»­i LOCK â†’ nháº­n LOCKED â†’ PING â†’ UNLOCK â†’ exit (PASS trÃªn Windows 10 VMware)
- `[ ]` Test: PING loop 30 phÃºt khÃ´ng lá»—i (stress test) (ÄÃ£ PASS báº£n 5 phÃºt trÃªn VM, stress test 30 phÃºt sáº½ cháº¡y láº¡i trÆ°á»›c khi release/production)
- `[x]` Test: Kill Java process (Task Manager) â†’ mÃ n hÃ¬nh trá»Ÿ láº¡i láº­p tá»©c (PASS trÃªn Windows 10 VMware)
- `[x]` Test: Kill Rust process (Task Manager) â†’ Java emergency unlock (PASS trÃªn Windows 10 VMware vá»›i emergency reset)
- `[x]` Test: Má»Ÿ OBS â†’ tháº¥y mÃ n hÃ¬nh Ä‘en (OBS recording chá»‰ tháº¥y mÃ n hÃ¬nh Ä‘en/xanh/trá»‘ng, khÃ´ng lá»™ ná»™i dung Kiosk - PASS trÃªn Windows 10 VMware)
- `[x]` Test: Má»Ÿ app cáº¥m (process-alert) â†’ Java nháº­n PROCESS_ALERT (PASS trÃªn Windows 10 VMware vá»›i notepad.exe)
- `[x]` Test: Báº­t VirtualBox/VMware â†’ VM detected (PASS trÃªn Windows 10 VMware nhá» phÃ¡t hiá»‡n VMware MAC prefix)
- `[x]` Test: Dev panic key Ctrl+Shift+Alt+F12 â†’ thoÃ¡t kiosk tá»©c thÃ¬ (PASS trÃªn Windows 10 VMware thÃ´ng qua fallback POLLING_CTRL_SHIFT_P)
- `[x]` Test: Auto-kill debug timer 120s -> thoÃ¡t kiosk (DEBUG build, PASS trÃªn Windows 10 VMware vá»›i exit code 98)

---

## PHASE 3: Integrity & Proctoring (Java)
> Chá»‰ báº¯t Ä‘áº§u sau khi Phase 2 Ä‘Ã£ pass táº¥t cáº£ integration tests

### 3.1 Client Integrity (TEK Hash)
- `[ ]` Maven plugin tÃ­nh SHA-256(tutorhub.jar) â†’ ghi vÃ o build.properties
- `[ ]` `IntegrityVerifier.java`:
  - Äá»c jarHash tá»« build.properties
  - TÃ­nh TEK = HMAC-SHA256(examSessionSecret, jarHash + "\n" + sessionId + "\n" + examId + "\n" + platform)
  - Gá»­i tekHash trong EXAM_START_REQUEST
- `[ ]` Server: `ExamPacketHandler` verify TEK â†’ reject náº¿u sai

### 3.2 Proctor Dashboard (Java Swing)
- `[ ]` `ProctorDashboard.java`:
  - Grid view: má»—i há»c sinh = 1 card (tÃªn, progress, violations, status)
  - Status indicator: Xanh (OK), VÃ ng (cáº£nh bÃ¡o), Äá» (vi pháº¡m), Äen (offline)
  - Auto-refresh tá»« Server má»—i 5s (nháº­n EXAM_MONITOR_UPDATE)
- `[ ]` Action buttons: Force Submit, Extend Time, View Violations
- `[ ]` `ExamMonitorService.java` (Server side): broadcast EXAM_MONITOR_UPDATE tá»›i GV

### 3.3 Snapshot Proctoring
- `[ ]` `SnapshotCapture.java`:
  - Chá»¥p webcam má»—i 3-5 phÃºt
  - Buffer áº£nh trong memory (khÃ´ng ghi disk)
- `[ ]` `SnapshotUploader.java`:
  - Upload lÃªn Backblaze S3
  - LÆ°u URL vÃ o anticheat_events.evidence_url

---

## PHASE 4: Edge AI & Scale (Java/ONNX)
> Chá»‰ báº¯t Ä‘áº§u khi > 5000 concurrent users

### 4.1 Edge AI (On-Device)
- `[ ]` TÃ­ch há»£p ONNX Runtime Java vÃ o pom.xml
- `[ ]` `FaceDetector.java` â€” Ultraface model, 2 frame/s
- `[ ]` `GazeEstimator.java` â€” MediaPipe Face Mesh â†’ ONNX
- `[ ]` `TrustScoreEngine.java` â€” tÃ­nh trust score tá»« face/gaze events
- `[ ]` Gá»­i EXAM_VIOLATION khi: máº¥t máº·t > 3s, nhiá»u hÆ¡n 1 máº·t, nhÃ¬n sang bÃªn > 5s

### 4.2 Scale Infrastructure
- `[ ]` Upgrade TCP â†’ WebSocket
- `[ ]` Protobuf schema (.proto files)
- `[ ]` gRPC server + client
- `[ ]` Kafka cluster + topics
- `[ ]` ClickHouse analytics

---

## Ghi chÃº cho AI Agent

1. **Äá»c trÆ°á»›c khi code:** `MASTER_SECURE_EXAM_BLUEPRINT_v4.md` â€” Ä‘áº·c biá»‡t cÃ¡c section Ä‘Æ°á»£c Ä‘Ã¡nh dáº¥u trong nhiá»‡m vá»¥
2. **Cáº­p nháº­t file nÃ y:** ÄÃ¡nh dáº¥u `[x]` ngay sau khi hoÃ n thÃ nh má»—i task (khÃ´ng pháº£i cuá»‘i session)
3. **KhÃ´ng skip PoC trÆ°á»›c khi cháº¡y/test tháº­t:** Task 2.0.x lÃ  báº¯t buá»™c trÆ°á»›c khi nghiá»‡m thu hoáº·c cháº¡y cÃ¡c module lockdown trÃªn VM/production.
4. **VM only:** Má»i code liÃªn quan Ä‘áº¿n CreateDesktop, Hook, DisplayAffinity â†’ chá»‰ cháº¡y trÃªn VM
5. **Debug feature:** LuÃ´n test vá»›i `--features debug_mode` trÆ°á»›c, KHÃ”NG commit feature nÃ y vÃ o release

### Step 2I.7.4 Production Polish & Hardening for Parent Submit E2E Lab
- [x] Parent ?n/minimize khi g?i Secure Desktop, hi?n l?i khi xong.
- [x] Nï¿½t Retry Submit n?u submit_payload fail.
- [x] Temp cleanup ch? khi submit server success.
- [x] Log rï¿½ cï¿½c thï¿½ng s? vï¿½ check process ng?m.

### Step 2I.7.5 Encrypt Temp Context & Submit Payload for Production Parent Submit Lab
- [x] T?o AES key, mï¿½ hoï¿½ context vï¿½ luu thï¿½nh exam_context.enc
- [x] Truy?n key qua --key cho Rust vï¿½ Java Child
- [x] Child gi?i mï¿½ context, lï¿½m bï¿½i, mï¿½ hoï¿½ output luu thï¿½nh submit_payload.enc
- [x] Parent d?c file .enc, gi?i mï¿½ vï¿½ submit lï¿½n Server
- [x] Khï¿½ng log plaintext, retry v?n ho?t d?ng.

### Step 2I.7.6 Auto-Save & Recovery for Production Parent Submit Lab
- [x] Child t? d?ng g?i collectTSEAnswers() m?i 15s.
- [x] Ghi file autosave_payload.enc v?i AES/GCM.
- [x] B?m N?p bi v?n ghi submit_payload.enc uu tin cao hon.
- [x] Parent fallback sang d?c autosave_payload.enc n?u submit_payload.enc b? thi?u.
- [x] Khng luu plaintext, khng log plaintext.

### Step 2I.7.7 Persist Session Key & Crash Recovery for Production Parent Submit Lab
- [x] T?o interface RecoveryKeyStore.
- [x] T?o WindowsDpapiRecoveryKeyStore dng JNA Crypt32Util.
- [x] Thm dependency jna-platform vo pom.xml.
- [x] Parent ghi recovery_key.enc b?o v? b?ng DPAPI (khng plaintext).
- [x] Parent c nt Recover Pending Submission qut %TEMP% cho tse_exam_* tm payload v recovery_key.enc.
- [x] N?p xong d?n s?ch thu m?c session.

### Step 2I.7.8 Final Production UI & Error Hardening
- [x] ï¿½?i tï¿½n c?a s?: TutorHub Secure Exam ï¿½ Production Launcher.
- [x] Hi?n Status label rï¿½ rï¿½ng.
- [x] X? lï¿½ l?i k?t n?i server khï¿½ng crash.
- [x] Rust fail khï¿½ng crash, hi?n l?i UI d? retry.
- [x] Recover Pending Submission support ch?n nhi?u session qua h?p tho?i.
- [x] Check TutorHub_LockdownCore.exe cï¿½n sï¿½t.
- [x] Cï¿½ nï¿½t Exit rï¿½ rï¿½ng thay vï¿½ ph?i t?t b?ng d?u X.
- [x] Ghi chï¿½: DPAPI b? gi?i h?n trï¿½n cï¿½ng tï¿½i kho?n Windows.

### Step 2I.8.1 Packaging / Installer Plan for TutorHub Secure Exam
- [x] Phï¿½n tï¿½ch 3 hu?ng dï¿½ng gï¿½i (Portable, jpackage, Inno Setup).
- [x] ï¿½? xu?t chi?n lu?c Portable Folder cho production s?m.
- [x] Thi?t k? c?u trï¿½c thu m?c phï¿½n ph?i.
- [x] Ki?m tra v?n d? Path vï¿½ Config file.
- [x] K? ho?ch th? nghi?m vï¿½ rollback rï¿½ rï¿½ng.

### Step 2I.8.2 Build Portable Folder
- [x] T?o build_portable.ps1 ch?y Maven vï¿½ thu gom file.
- [x] T?o c?u trï¿½c thu m?c dist/TutorHubSecureExam/ ch?a app/, runtime/, logs/, temp_jcef/.
- [x] Copy JRE/JDK mini hi?n cï¿½ vï¿½o runtime/.
- [x] Copy Fat JAR vï¿½o app/.
- [x] T?o file app/application.properties co b?n.
- [x] T?o file run.bat dï¿½ng runtime/bin/java.exe ch?y TSEProductionParentSubmitLabLauncher.
- [x] ï¿½ï¿½ th? nghi?m build script thï¿½nh cï¿½ng.

### Step 2I.8.2 Fix Portable Folder jarPath Bug
- [x] ï¿½ï¿½ s?a l?i du?ng d?n tinh 	arget/ trong Parent Launcher.
- [x] Parent Launcher nay dï¿½ uu tiï¿½n c? -Dtutorhub.app.jar.
- [x] C?p nh?t uild_portable.ps1 vï¿½ 
un.bat d? truy?n c? tuong ?ng.
- [x] Hoï¿½n thi?n 100% kh? nang ch?y d?c l?p c?a Portable Folder.

### Step 2I.8.2C Portable Runtime Validation / Clean Machine Test
- [ ] validate_portable.ps1 pass.
- [ ] Portable ch?y du?c t? thu m?c g?c dist/TutorHubSecureExam.
- [ ] Portable ch?y du?c sau khi copy sang thu m?c khï¿½c.
- [ ] Khï¿½ng c?n m? IDE/NetBeans.
- [ ] run.bat khï¿½ng cï¿½n l?i Java Usage.
- [ ] Rust Secure Desktop v?n ch?y.
- [ ] Child JCEF v?n render.
- [ ] Submit Server thï¿½nh cï¿½ng.
- [ ] Khï¿½ng cï¿½n process Rust/Java child treo.


### Step 2I.8.4 Installer Validation & Release Checklist
- [ ] docs/tse_release_validation_checklist.md duoc tao.
- [ ] validate_installed_app.ps1 duoc tao.
- [ ] User da chay Test Plan va tick het cac muc trong Release Checklist.

### Step 2I.8.5 Reduce Runtime Size with jlink Lab
- [x] Tao script build_jlink_runtime.ps1.
- [x] Chay jlink voi cac modules: java.base, java.desktop, java.logging, java.management, java.naming, java.net.http, java.sql, java.xml, jdk.crypto.ec, jdk.unsupported, jdk.charsets.
- [x] Tao duoc runtime mini giam dung luong tu 342.5MB xuong 54.6MB.
- [x] dist/TutorHubSecureExam_jlink_lab/ chay thanh cong Launcher va connect WebSocket Server.
- [ ] User can thu nghiem them E2E de check JCEF va DPAPI co missing module hay khong.

### Step 2I.8.5B Integrate jlink runtime into Portable and Installer Build
- [x] Cap nhat build_portable.ps1 them tham so -UseJlinkRuntime.
- [x] Cap nhat build_installer.ps1 them tham so -UseJlinkRuntime, va override OutputBaseFilename thanh TutorHubSecureExamSetup-jlink.
- [x] Test build_portable.ps1 va build_installer.ps1 thanh cong.
- [x] Installer moi dat muc ~241MB (Giam ~120MB so voi ban goc 360MB).

### Step 2I.8.6 Code Signing & SmartScreen Plan (SKELETON)
- [x] docs/tse_code_signing_plan.md duoc tao de phan tich rui ro SmartScreen, False Positive va de xuat huong giai quyet.
- [x] sign_release.ps1 duoc tao duoi dang skeleton (Ho tro PFX va Cert Store, khong hardcode mat khau, co dry-run).
- [ ] (FUTURE) Khi co chung chi (OV hoac EV), chay sign_release.ps1 de ky file.
*(Luu y: Chua ky production vi chua co certificate)*

### Step 2I.8.7 Final Release Package
- [x] script package_release.ps1 duoc tao de gom installers va docs vao dist/release/TutorHubSecureExam-1.0.0/.
- [x] README_RELEASE.md va RELEASE_NOTES.md duoc tao.
- [x] checksums.sha256 duoc tao de xac thuc tinh nguyen ven cua installer.
*(Luu y: Chua code signed, chua auto-update. San sang de QA manual test)*

### Step 2I.9 Main App Integration
- [x] Step 2I.9.0 Main App Integration Plan.
- [x] Step 2I.9.1 Secure Exam Launcher Bridge.

### Step 2I.9.2 Pass selected examId to Secure Exam Launcher
- [x] Add launchExam(int examId) to SecureExamLauncherBridge
- [x] Extract examId in ExamTab and call launchExam
- [x] Forward args in run.bat generated by build_portable.ps1
- [x] Parse --exam-id in TSEProductionParentSubmitLabLauncher

### Step 2I.9.3 Auto-start/preselect selected examId
- [x] Find examId in configs and preselect it
- [x] Log Preselected examId from main TutorHub app
- [x] Handle exam not found gracefully

### Step 2I.9.4 Auto-start selected exam from TutorHub
- [x] Add startInProgress boolean to prevent double starts
- [x] Extract start button logic into reusable Runnable
- [x] Schedule auto-start with 1500ms delay when requestedExamId is provided
- [x] Log auto-start initialization and execution

### Step 2I.9.5 Restore 3-Step TSE UX and UI Polish
- [x] Integrate CardLayout in Launcher (Login -> Config -> Logs/Start)
- [x] Update --auto-start logic to respect the user click flow
- [x] Generate run_gui.bat via build_portable.ps1 and use javaw.exe
- [x] Add submit confirmation dialog in TSEExamChildClient
- [x] Implement zero-question check before Secure Desktop spawn



### Step 2I.9.5D Final Submit Handler & Parent Cleanup Stabilization
- [x] TSEExamChildClient.java - volatile fields: finalSubmitInProgress, allowProgrammaticExit, autoSaveTimer class-level
- [x] Submit button: guard double-submit, stop autoSaveTimer truoc khi submit
- [x] Confirm dialog: "Quay lai" khong lam gi, "Nop bai" moi trigger flow
- [x] Tach writeFinalPayloadAndExit() va writeAutosavePayload() - log rieng biet
- [x] Exit mechanism: 1 background thread duy nhat (JCEF cleanup + halt), EDT chi dispose frame
- [x] Khong con race condition giua System.exit() va Runtime.halt()
- [x] JCEF cleanup chay tren background thread, khong block EDT
- [x] autoSaveTimer: check finalSubmitInProgress truoc khi chay, stop khi final submit
- [x] allowProgrammaticExit + WindowClosing check
- [x] TSEProductionParentSubmitLabLauncher.java - timeout 30s cho submitExam().get()
- [x] TimeoutException catch rieng voi log [TSE_SUBMIT]
- [x] rustProc.waitFor(): dynamic timeout = examCfg.durationMinutes + 15 (fallback 240)
- [x] Sau Rust exit: force-kill process tree bang taskkill /F /T /PID <rustPid>
- [x] Log [TSE_PARENT] Found submit_payload.enc. Using FINAL submit payload.
- [x] Log [TSE_PARENT] WARNING: submit_payload.enc not found. Falling back to autosave_payload.enc.
- [x] mvn clean install - BUILD SUCCESS (0 error, 38.5s)

### Step 2I.9.29 Phase 6D.5 Child V2 Debug IPC Regression Gate
- [x] Tested full key fetch, hash verify, decrypt, parse, and validate without UI logic.
- [x] Verified Phase 6D codebase is fully functional.
- [x] Created `docs/tse_v2_child_debug_ipc_regression_phase_6d_5.md`.

### Step 2I.9.30 Phase 6E: Child V2 Safe Summary GUI Integration
- [x] Created `TSEV2ChildDebugSummaryPanel.java` to display safe summary without raw tokens.
- [x] Replaced `launchV2DebugSkeleton` JTextArea with the new Summary Panel in `TSEExamChildClient.java`.
- [x] Blocked sensitive fields (secretKey, plaintext, answerKey) from the summary.
- [x] Created `TSEV2ChildDebugSummaryPanelTest.java` (JUnit 5) and verified tests pass.
- [x] Executed `mvn clean install` and `build_portable.ps1` with 0 failures.
- [x] Generated `docs/tse_v2_child_safe_summary_gui_phase_6e.md` documentation.
- [ ] VM/E2E manual test pending for brightness/visual polish of the summary UI.

### Step 2I.9.6 TSE Control Buttons SEB Audit
- [x] Doc lai prompt control buttons va cac tai lieu TSE/SEB bat buoc.
- [x] Doi chieu Antigravity notes voi source SEB that trong seb-reference.
- [x] Audit cac file TSE lien quan: ExamHeaderBar, ExamFooterStatusBar, TSEExamChildClient, TSEBrowserPanel, TSEJcefLifecycleManager, TSEProductionParentSubmitLabLauncher.
- [x] Tao docs/tse_codex_control_review.md gom ket luan dung/sai, rui ro, file can sua va thu tu trien khai 4 nhom control.
- [x] Implement Group 1: About + Language + Exit theo audit, khong dung JOptionPane/JDialog/GlassPane trong active exam.
- [x] Group 1 build verification: mvn clean install PASS, 21 tests, 0 failures.
- [x] Group 1 portable verification: build_portable.ps1 PASS.
- [ ] Group 1 VM/E2E manual test: dist/TutorHubSecureExam/run.bat --exam-id 3, verify Final Submit SUCCESS and no autosave fallback.

### Step 2I.9.7 Internal Vietnamese Input Mode
- [x] Group 1 Language control was repurposed to Input Mode Toggle.
- [x] VIE = internal Vietnamese Telex typing mode inside JCEF.
- [x] ENG = English/raw typing mode.
- [x] This does not translate TSE UI and does not depend on OpenKey/UniKey/EVKey.
- [x] Added JS resource `src/main/resources/tse/tse-vietnamese-input-engine.js` and Java manager `TSEInputModeManager`.
- [x] Build verification: mvn clean install PASS, 21 tests, 0 failures.
- [x] Portable verification: build_portable.ps1 PASS.
- [ ] VM/E2E manual test: dist/TutorHubSecureExam/run.bat --exam-id 3, verify Telex input and Final Submit SUCCESS.

### Step 2I.9.8 Debug-only Vietnamese Input Test Panel
- [x] Added debug-only input textarea injection gated by -Dtutorhub.tse.inputTest=true.
- [x] Production default remains clean: no property means no test panel is injected.
- [x] build_portable.ps1 generates run_input_test.bat with -Dtutorhub.tse.inputTest=true.
- [x] Production run.bat remains unchanged.
- [x] Build verification: mvn clean install PASS, 21 tests, 0 failures.
- [x] Portable verification: build_portable.ps1 PASS.
- [ ] VM/E2E manual test: dist/TutorHubSecureExam/run_input_test.bat --exam-id 3, verify textarea Telex, About, blocked Power, and Final Submit SUCCESS.

### Step 2I.9.9 Input Test Propagation via Encrypted Context
- [x] Fixed run_input_test.bat mode not reaching the Java Child process.
- [x] Parent reads -Dtutorhub.tse.inputTest=true and writes inputTestEnabled into exam_context.enc.
- [x] Child reads inputTestEnabled after decrypting context and uses it to inject the test textarea.
- [x] Added required Parent/Child logs for TSE_INPUT_TEST state and injection.
- [x] No Rust change required.
- [x] Build verification: mvn clean install PASS, 21 tests, 0 failures.
- [x] Portable verification: build_portable.ps1 PASS.
- [ ] VM/E2E manual test: dist/TutorHubSecureExam/run_input_test.bat --exam-id 3, verify textarea appears in Secure Desktop/JCEF and Final Submit SUCCESS.

### Step 2I.9.10 Improve Vietnamese input engine tone placement using open-source research.
- [x] Da nghien cuu ANVIM/AVIM/VNKeys-JS.
- [x] Da sua tone placement cho cac cum nhu oe, oa, ie, uo.
- [x] Khong copy nguyen code, chi hoc thuat toan.
- [x] Khong phu thuoc bo go ngoai.

### Step 2I.9.11 Professionalize Vietnamese Input Engine & Extend to Parent Launcher
- [x] Created TSEVietnameseTelexEngine.java (pure Java port of JS engine, including non-adjacent w modifier).
- [x] Created TSEInputSwingAdapter.java for opt-in Telex on Swing JTextComponent via putClientProperty.
- [x] Created tse-vietnamese-input-test-cases.json with 22 rigorous test cases (including khuyur, gioiws, tuyeens).
- [x] Created TSEVietnameseTelexEngineTest.java (JUnit 5) to verify Java port against JSON cases (22/22 passed).
- [x] Updated TSEInputModeManager to Singleton and propagated inputMode via exam_context.enc to Exam Child.
- [x] Added clickable VIE/ENG toggle button in TSELoginPanel and TSEConfigListPanel.
- [x] Default mode is ENG for Parent Launcher.
- [x] Build verification: mvn clean install PASS, test cases passed.
- [x] Portable verification: build_portable.ps1 PASS.

### Step 2I.9.12 Quick Settings Tray Cluster Architecture Migration
- [x] Deleted TSEParentQuickSettingsPopup (Swing) and migrated Login/Config Quick Settings popup to JavaFX WebView (TSEParentHtmlQuickSettingsPopup).
- [x] Ensured visual-match (95%) with Exam JCEF popup.
- [x] Fixed controller compilation errors to use correct static/instance methods.
- [x] Included Microsoft Fluent UI System Icons (MIT License) locally and updated README.
- [x] Fixed build_portable.ps1 to skip tests so that portable builds cleanly.
- [x] Verified mvn clean install PASS.
- [x] Verified portable build PASS.
- [ ] Pending VM/E2E manual test for Quick Settings popup visuals and lifecycle.
### Step 2I.9.13 Brightness Full Research Fix
- [x] Audited Parent and Exam brightness path from HTML/JS event to Java bridge, QuickSettingsController, and BrightnessService.
- [x] Identified Parent slider as custom div#slider-brightness, not input[type=range].
- [x] Added Parent/Exam JS version markers and brightness delegation/debug logs.
- [x] Fixed Parent JavaFX WebView outside-click guard for in-popup pointer activity.
- [x] Fixed Exam brightness command path to use delegated TSE_BRIGHTNESS_SET events.
- [x] Fixed TSEQuickSettingsManager brightness pending queue handling.
- [x] Created docs/tse_brightness_full_research_fix.md.
- [x] Verified JS syntax checks PASS.
- [x] Verified mvn clean install PASS, 22 tests, 0 failures.
- [x] Verified portable build PASS.
- [x] Verified portable JAR contains updated Parent/Exam brightness resources.
- [ ] Pending VM/E2E manual test: Parent brightness changes real screen brightness, Exam brightness changes real screen brightness, Final Submit SUCCESS, Rust exit code 0, and no hanging Java/Rust/PowerShell process.
### Step 2I.9.14 Brightness VM GUI Acceptance Test & Final Regression
- [x] Detected current session environment before running lockdown GUI test.
- [x] Refused to run Secure Exam lockdown GUI on physical Lenovo machine; VM-only rule still enforced.
- [x] Fixed acceptance version marker mismatch to FULL_BRIGHTNESS_RESEARCH_FIX.
- [x] Rebuilt portable package after marker fix.
- [x] Verified portable JAR contains FULL_BRIGHTNESS_RESEARCH_FIX markers for Parent and Exam JS.
- [x] Cleaned stale TutorHubSecureExam java.exe processes from previous run_input_test sessions.
- [x] Created docs/tse_brightness_vm_acceptance_result.md.
- [ ] Pending VM GUI acceptance: Parent brightness real change, Exam brightness real change, Volume/mute, WiFi/Battery/Clock, Safe Refresh, Power blocked, Final Submit SUCCESS, Rust exit code 0, and post-submit process cleanup.
### Step 2I.9.15 Parent Brightness AWT Fallback after VM log
- [x] Reviewed user VM log showing slider selector/debug is correct but Parent WebView DOM mouse events do not fire.
- [x] Kept Rust and Final Submit untouched.
- [x] Added Java AWT fallback for Parent/Login brightness slider only in TSEParentHtmlQuickSettingsPopup.
- [x] Fallback maps mouse press/drag/release inside div#slider-brightness bounds to brightness percent.
- [x] Fallback mirrors UI through JS setSliderUIValue and commits through setBrightnessCommand -> QuickSettingsController -> BrightnessService.
- [x] Added diagnostic logs: AWT brightness fallback pointerdown/drag/commit.
- [x] Verified JS syntax checks PASS.
- [x] Verified mvn clean install PASS, 22 tests, 0 failures.
- [x] Verified portable build PASS.
- [x] Verified portable JAR contains FULL_BRIGHTNESS_RESEARCH_FIX marker.
- [ ] Pending VM retest: Parent brightness changes real screen brightness and reaches BrightnessService verify after set.
- [ ] Pending Exam/JCEF brightness retest and final regression.
### Step 2I.9.16 Phase 9B.11 Parent AWT Brightness Fallback Acceptance
- [x] Checked current execution environment before running Secure Exam GUI acceptance.
- [x] Confirmed current session is still a Lenovo physical machine, not a disposable VM test-safe environment.
- [x] Did not run run_input_test.bat on the physical machine.
- [x] Verified no TutorHub_LockdownCore/java/javaw/powershell process matching TSE/TutorHub/WmiMonitorBrightness/TutorHubSecureExam was hanging before this phase.
- [x] Verified portable folder is present at dist/TutorHubSecureExam.
- [x] Updated docs/tse_brightness_vm_acceptance_result.md with Phase 9B.11 blocked/pending status.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] No code change in this phase because no new VM runtime error log was available.
- [ ] Pending VM execution: Parent AWT fallback pointerdown/drag/commit, real brightness change, no popup auto-close.
- [ ] Pending VM execution: Exam/JCEF brightness regression.
- [ ] Pending VM execution: Volume/mute, WiFi/Battery/Clock, VIE/ENG, Safe Refresh, Power blocked, Final Submit SUCCESS, Rust exit code 0, and clean process cleanup.
### Step 2I.9.17 Parent Quick Settings Speaker Icon Color Fix
- [x] User confirmed Parent/Login brightness can now change real screen brightness.
- [x] Fixed Parent Quick Settings speaker/mute SVG icon color in parent-quick-settings.css.
- [x] Forced #btn-mute, #icon-vol-on, and #icon-vol-off to use light stroke instead of WebView default black.
- [x] Rebuilt portable folder at dist/TutorHubSecureExam.
- [x] Verified portable JAR contains updated speaker icon CSS.
- [x] Verified mvn test PASS, 22 tests, 0 failures.
- [x] No Rust change.
- [x] No Final Submit change.
- [ ] Pending VM visual confirmation: speaker icon is no longer black in Parent Quick Settings.

### Step 2I.9.18 Parent Quick Settings UI Sync with Exam Popup
- [x] Reviewed Exam popup implementation in tse-tray-flyout.js.
- [x] Reviewed Parent quick settings HTML/CSS/JS and parent popup Java.
- [x] Removed visible outer wrapper background by making Parent WebView body transparent.
- [x] Aligned Parent panel color, radius, shadow, padding with Exam popup direction.
- [x] Removed top-right sun icon from production Login and Configuration panels.
- [x] Help icon now occupies the rightmost top-bar position.
- [x] Kept Brightness, Volume, WiFi, Battery logic unchanged.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] Created docs/tse_parent_popup_ui_sync_phase.md.
- [x] Verified mvn clean install PASS, 22 tests, 0 failures.
- [x] Verified portable build PASS.
- [x] Verified portable JAR contains updated Parent popup CSS.
- [ ] Pending VM visual test: Login/Configuration popup has no outer square, no blank, brightness drag does not close popup.
- [ ] Pending VM regression: Exam popup unchanged, Final Submit SUCCESS, Rust exit code 0.

### Step 2I.9.19 Parent Quick Settings UI Polish Acceptance Attempt
- [x] Checked execution environment before running Secure Exam GUI acceptance.
- [x] Confirmed current session is LENOVO 83DV physical machine, not VM test-safe.
- [x] Did not run run_input_test.bat on physical machine.
- [x] Static verified Parent popup CSS uses transparent host background and Exam-like panel styling.
- [x] Static verified production Login/Configuration panels no longer add sun.svg.
- [x] Static verified help icon remains in the right-side top-bar container.
- [x] Static verified portable JAR includes tse/quick-settings/parent-quick-settings.css.
- [x] Updated docs/tse_parent_popup_ui_sync_phase.md with acceptance blocked/pending status.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] No code change in this acceptance attempt.
- [ ] Pending VM visual acceptance: Login/Configuration popup no outer square, no blank, help icon rightmost.
- [ ] Pending VM regression: Exam popup unaffected, brightness/volume/mute, WiFi/Battery/Clock, VIE/ENG, Safe Refresh, Power blocked, Final Submit SUCCESS, Rust exit code 0, and clean process cleanup.

### Step 2I.9.20 Parent Quick Settings White Host Frame Fix
- [x] Reviewed user VM screenshot showing white host frame behind Parent/Login Quick Settings popup.
- [x] Identified likely root cause as JavaFX WebView/JFXPanel host page fill, not panel CSS border.
- [x] Updated TSEParentHtmlQuickSettingsPopup to make JFXPanel background transparent.
- [x] Updated TSEParentHtmlQuickSettingsPopup to call WebView.setPageFill(Color.TRANSPARENT).
- [x] Updated TSEParentHtmlQuickSettingsPopup to set WebView CSS background transparent.
- [x] Kept Parent popup HTML/CSS layout unchanged to avoid breaking brightness fallback coordinates.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] Verified mvn clean install PASS, 22 tests, 0 failures.
- [x] Verified portable build PASS.
- [ ] Pending VM visual confirmation: white frame behind Login/Configuration Quick Settings popup is removed.

### Step 2I.9.21 Parent Quick Settings Opaque Panel Fix
- [x] Reviewed user VM screenshot confirming white host frame is gone but panel is still too transparent.
- [x] Identified Parent popup panel alpha as the issue: rgba(32, 32, 32, 0.85) shows background details in JavaFX transparent window.
- [x] Updated parent-quick-settings.css to use opaque #202020 for #tse-tray-flyout.
- [x] Kept host/body transparent so the previous white frame does not return.
- [x] Kept popup dimensions, padding, slider layout, and AWT brightness fallback coordinates unchanged.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] Verified mvn clean install PASS, 22 tests, 0 failures.
- [x] Verified portable build PASS.
- [x] Verified portable JAR contains background: #202020.
- [ ] Pending VM visual confirmation: popup no longer shows background image details through the panel.

### Step 2I.9.22 Parent Quick Settings Host Size Match Fix
- [x] Reviewed user VM screenshot showing a faint host/halo frame behind the dark popup.
- [x] Identified mismatch between WebView host size 360x280 and inner panel size 326x246 with margin 16px.
- [x] Updated parent-quick-settings.css so #tse-tray-flyout fills host size: width 360px and height 280px.
- [x] Removed panel margin by setting margin: 0.
- [x] Increased panel padding to 32px to preserve inner content/slider geometry.
- [x] Replaced outer shadow with inset shadow to avoid a separate frame behind the popup.
- [x] Kept html/body transparent so the white host frame does not return.
- [x] Kept AWT brightness fallback constants unchanged because content origin remains about 32px.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] Verified mvn clean install PASS, 22 tests, 0 failures.
- [x] Verified portable build PASS.
- [x] Verified portable JAR contains width 360px, height 280px, padding 32px, margin 0 for Parent popup CSS.
- [ ] Pending VM visual confirmation: faint frame/halo behind Login/Configuration Quick Settings popup is removed.

### Step 2I.9.23 Parent Quick Settings Bottom Clip Fix
- [x] Reviewed user VM screenshot showing the bottom of Parent/Login Quick Settings popup is clipped.
- [x] Identified popup host height 280px is too small after host-size match fix.
- [x] Updated parent-quick-settings.css html/body height from 280px to 320px.
- [x] Updated parent-quick-settings.css #tse-tray-flyout height from 280px to 320px.
- [x] Updated TSEParentHtmlQuickSettingsPopup POPUP_HEIGHT from 280 to 320.
- [x] Kept width 360px, margin 0, padding 32px, and opaque #202020 panel background.
- [x] Kept AWT brightness fallback coordinates unchanged because content origin and slider geometry did not move.
- [x] No Rust change.
- [x] No Final Submit change.
- [ ] Pending mvn clean install verification.
- [ ] Pending portable build verification.
- [ ] Pending VM visual confirmation: popup bottom content is no longer clipped.

Verification update for Step 2I.9.23:
- [x] mvn clean install PASS, 22 tests, 0 failures.
- [x] build_portable.ps1 PASS.
- [x] Verified portable JAR contains Parent popup CSS height 320px.
- [x] Verified source Java uses POPUP_HEIGHT = 320.

### Step 2I.9.24 Parent Quick Settings Size Sync with Exam Popup
- [x] Measured Exam/JCEF Quick Settings popup with headless Chrome render.
- [x] Confirmed Exam visible panel size is 354x285.
- [x] Confirmed Exam CSS content box is width 320px, height 251px, padding 16px, border 1px, content-box sizing.
- [x] Updated Parent/Login/Configuration popup CSS to the same visible panel size: 354x285.
- [x] Updated Parent CSS content box to width 320px, height 251px, padding 16px, content-box sizing.
- [x] Updated TSEParentHtmlQuickSettingsPopup POPUP_WIDTH to 354 and POPUP_HEIGHT to 285.
- [x] Updated Parent AWT brightness fallback constants to measured slider geometry: x=71, y=127, width=204.
- [x] Kept Parent opaque #202020 background to avoid previous see-through wallpaper issue.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] Static render verification: Parent and Exam panels are both 354x285 and Parent bottom bar is visible.
- [ ] Pending mvn clean install verification.
- [ ] Pending portable build verification.
- [ ] Pending VM visual confirmation: Login/Configuration popup size matches Exam popup.

Verification update for Step 2I.9.24:
- [x] mvn clean install PASS, 22 tests, 0 failures.
- [x] build_portable.ps1 PASS.
- [x] Verified portable JAR contains Parent popup CSS width 354px, height 285px, content width 320px, content height 251px, padding 16px.
- [x] Verified Java constants POPUP_WIDTH=354, POPUP_HEIGHT=285, brightness fallback x=71, y=127, width=204.
### Step 2I.9.25 Login Captcha, Password Reveal, and About Dialog
- [x] Checked SEB reference password and About window implementation.
- [x] Confirmed no SEB local login captcha implementation was found; TutorHub captcha is a local UI guard only.
- [x] Added SecureRandom-backed `TSECaptchaService`.
- [x] Login captcha now refreshes and validates before server login request.
- [x] Password eye button now toggles `JPasswordField` reveal/hide.
- [x] Login help icon now opens `TSEAboutDialog`.
- [x] Configuration help icon now opens `TSEAboutDialog`.
- [x] Added `TSECaptchaServiceTest`.
- [x] Created `docs/tse_login_captcha_password_about_phase.md`.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] mvn test PASS, 25 tests, 0 failures.
- [x] mvn clean install PASS, 25 tests, 0 failures.
- [x] mvn test PASS, 25 tests, 0 failures.
- [ ] Pending VM visual confirmation for the Help/About popup.

Verification update for Step 2I.9.26:
- [x] mvn clean install PASS, 25 tests, 0 failures.
- [x] build_portable.ps1 PASS after closing stale portable javaw.exe processes.
- [x] Verified portable JAR contains tutorhub_tse_v1 marker.
- [x] Verified portable JAR contains tutorhub_tse_v1 marker.

Compact UI update for Step 2I.9.26:
- [x] Reworked Help/About popup from a long product-info page into a compact 560x430 dialog.
- [x] Shortened content to product name, version, founder, current screen, intro, core functions, and usage steps.
- [x] Added visible close controls: top-right X and bottom Dong button.
- [x] Kept TSEAboutDialog.showDialog(...) API unchanged.
- [x] No Taskbar/Quick Settings change.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] mvn test PASS, 25 tests, 0 failures.
- [x] build_portable.ps1 PASS.
- [ ] Pending VM visual confirmation for compact Help/About popup.
### Phase 3.5 & 4A: Backend Regression Gate + Exam Paper Backend Foundation
- [x] Audited ClientHandler.java for legacy flow stability.
- [x] Inserted QUESTION_* and EXAM_PAPER_* actions safely before GET_EXAMS.
- [x] Created ExamPaper.java and ExamPaperQuestion.java models.
- [x] Created ExamPaperDAO.java with CRUD and question mapping functions.
- [x] Created ExamPaperService.java to handle validation and packet creation.
- [x] Updated ExamDatabaseManager.java safely to add exam_papers and exam_paper_questions columns.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] No UI changes made.
- [x] Created docs/tse_exam_paper_backend_phase_3_5_and_4a.md.


### Phase 4B: Paper-to-Exam Assignment Backend
- [x] Performed regression check on Phase 4A (mvn clean install and build_portable.ps1 PASS).
- [x] Audited DB schema to ensure safe additive operations.
- [x] Updated `ExamDatabaseManager.java` to add `paper_id` column to `exams` table safely.
- [x] Updated `Exam.java` model to include `paperId`.
- [x] Updated `ExamDAO.java` to handle `paperId` mapping and assignment DB queries.
- [x] Created `ExamAssignmentService.java` to handle strict validation (must be DRAFT status).
- [x] Added `EXAM_ASSIGN_PAPER`, `EXAM_UNASSIGN_PAPER`, `EXAM_GET_ASSIGNED_PAPER` to `ClientHandler.java`.
- [x] No Rust change.
- [x] No Final Submit change.
- [x] No UI changes made.
- [x] Verified mvn clean install PASS.
- [x] Verified build_portable.ps1 PASS.
- [x] Created docs/tse_paper_to_exam_assignment_backend_phase_4b.md report.

### Phase 4E-1.6 Real Role Context Fix for MainDashboard
- [x] Pass real role from DASHBOARD_GO payload in LoginFrame.
- [x] MainDashboard uses real role to control QuestionBankTab visibility.
- [x] Fix compilation and verify build_portable.ps1 PASS.
  
### Phase 5A: Start Request V2 Backend + Feature Flag  
- [x] Implemented EXAM_START_REQUEST_V2  
- [x] Created ExamStartV2Service.java  
- [x] Resolved compilation errors in ClientHandler.java  
- [x] Build and legacy regression tests passed 
  
### Phase 5A.5: V2 Enabled Smoke Test + Feature Flag Consistency  
- [x] Standardize feature flag to tse.paperStartV2.enabled  
- [x] Test flag=false yields FEATURE_DISABLED  
- [x] Test flag=true and error states  
- [x] Verify V2 package security no answer leak 

### Phase 5B: Start Request V2 Debug Toggle / Admin-only Test Entry
- [x] Add Test Start V2 button to ExamTab for TUTOR/ADMIN roles only.
- [x] Created ExamStartV2DebugDialog.java to safely render V2 payloads.
- [x] Wire EXAM_START_RESPONSE_V2 in MainDashboard.java.
- [x] Verified legacy flows remain untouched.
- [x] Verified student view does not show Test Start V2 button.

### Phase 5C: V2 Exam Session / Attempt Lifecycle Backend Prototype
- [x] Schema: Added columns to exam_attempts safely.
- [x] ExamAttemptDAO: Implemented createAttemptV2 with attempt_no logic.
- [x] Security: sessionToken generated via SecureRandom, SHA-256 hashed in DB.
- [x] Service: Updated ExamStartV2Service to handle debugMode properly.
- [x] Test: Legacy flow remains untouched, V2 Session Smoke Test passes.

### Phase 6B: Secure Exam Child V2 Boot Mode Skeleton - No IPC / No Decrypt / No Render Yet
- [x] Parse `--v2-handoff-meta`, `--v2-handoff-enc`, `--v2-debug-only`.
- [x] Mode enum `V2_DEBUG`.
- [x] Render debug skeleton frame with empty content.

### Phase 6C: Parent-to-Child Loopback IPC Key Request Prototype
- [x] Server implementation `V2LoopbackKeyHandoffServer`.
- [x] Client implementation `V2LoopbackKeyHandoffClient`.
- [x] Validate with 62/62 unit tests passing.

### Phase 6D: Child V2 Debug IPC + Decrypt + Safe Summary
- [x] DTO `TSEV2ChildDebugLoadResult` safely mapping decoded metadata.
- [x] Core logic `TSEV2ChildDebugLoader` executing key fetch, decoding, and parsing.
- [x] Secure `TSEV2ChildDebugLoaderTest` validating failures & successes for 6D.
- [x] Phase 6D: JCEF Bridge -> Rust Loopback Key Handoff & Child Prototype
- [x] Phase 6D.5: Child V2 Debug IPC Regression & Documentation Gate
- [x] Phase 6E: Child V2 Safe Summary GUI Integration
- [x] Phase 6E.5: Child V2 Safe Summary GUI Manual + Regression Gate
- [x] Phase 6F: Read-only V2 Package Render in Child - Debug Onlypass.
- [x] Created `docs/tse_v2_child_ipc_decrypt_debug_phase_6d.md`.

### Phase 10I + 10J: Canonical Answer Payload Contract + Safe Parser Implementation Gate
- [x] Create `V2AnswerPayloadContract` and validation DTO.
- [x] Implement `V2AnswerPayloadContractValidator` matching 1:1 schema with client `TSEV2SubmitPayload`.
- [x] Update `V2JsonAnswerPayloadParser` to parse actual canonical JSON under feature flag.
- [x] Implement route `EXAM_SUBMIT_V2_ANSWER_PAYLOAD_CONTRACT_VALIDATE`.
- [x] Write `V2AnswerPayloadContractValidatorTest` and parser tests.
- [x] **Verification**: Verify flags, test success, no unauthorized logging.

### Phase 6G: V2 Answer Selection State Prototype - Debug Only
- [x] Confirmed Phase 6F/6E.5/6D gate docs exist.
- [x] Created RAM-only `TSEV2AnswerSelectionState`.
- [x] Updated V2 debug render panel so options are selectable locally.
- [x] Added safe progress text: `Answered X / Y`.
- [x] Kept autosave and submit disabled.
- [x] Kept state in RAM only; no answer file writes.
- [x] Added security validation before rendering selection UI.
- [x] Ran security search; Phase 6G code has no file write, network send, autosave, or submit call.
- [x] Added unit tests for state and panel selection behavior.
- [x] Verified Phase 6G targeted tests PASS: 9 tests, 0 failures.
- [x] Verified `mvn clean install` PASS: 82 tests, 0 failures.
- [x] Verified `build_portable.ps1` PASS.
- [x] Created `docs/tse_v2_child_answer_selection_state_phase_6g.md`.
- [ ] Pending VM-only legacy `run_input_test.bat --exam-id 3` acceptance.

### Phase 6G.5: V2 Answer Selection State Regression Gate - VM/Legacy Verification
- [x] Read Phase 6G, Phase 6F, Phase 6E.5, Phase 6D gate docs.
- [x] Confirmed current session is LENOVO 83DV and not treated as VM test-safe.
- [x] Did not run lockdown GUI / `run_input_test.bat` on physical machine.
- [x] Verified Phase 6G state is RAM-only.
- [x] Verified Phase 6G state/panel has no file write.
- [x] Verified Phase 6G state/panel has no network send.
- [x] Verified Phase 6G state/panel has no autosave or submit call.
- [x] Ran narrow security scan and classified hits.
- [x] Ran full repo security scan; hits are legacy/docs/test/dependency noise, not a Phase 6G leak.
- [x] Targeted Phase 6G tests PASS: 9 tests, 0 failures.
- [x] `mvn clean install` PASS: 82 tests, 0 failures.
- [x] `build_portable.ps1` PASS.
- [x] Created `docs/tse_v2_child_answer_selection_regression_phase_6g_5.md`.
- [ ] Pending VM manual V2_DEBUG selection UI acceptance.
- [ ] Pending VM legacy `run_input_test.bat --exam-id 3` acceptance.
- [ ] No-Go for autosave/submit implementation until VM acceptance passes.

### Phase 6N / 7C: Submit Dry-run Fast Regression Gate - No VM
- [x] Create `TSEV2SubmitDryRunRegressionTest`.
- [x] Cover full round-trip from snapshot to payload to encrypted payload and reverse.
- [x] Ensure wrong key and tamper fails safely.
- [x] Security scan passed: No leak of tokens or answers.
- [x] Maven build PASS (137 tests).
- [x] Portable build PASS.
- [x] Created `docs/tse_v2_submit_dryrun_fast_regression_phase_6n_7c.md`.

### Phase 7D: Backend Submit Contract - Server-side Dry-run Validation
- [x] Created `V2SubmitDryRunValidationResult.java` without sensitive data (score/answerKey/isCorrect).
- [x] Created `V2SubmitDryRunValidationService.java` with 17 validation rules.
- [x] Updated `ClientHandler.java` with `EXAM_SUBMIT_V2_DRYRUN_VALIDATE` action.
- [x] Created `V2SubmitDryRunValidationServiceTest.java`.
- [x] Created `docs/tse_v2_backend_submit_contract_phase_7d.md`.

### Phase 7D.5: Backend Submit Dry-run Full Regression Gate
- [x] Audited Phase 7D code (no DB writes, no updates to attempt).
- [x] Audited `ClientHandler.java` route (no legacy `EXAM_SUBMIT` override).
- [x] Verified feature flag and role guard check pass.
- [x] Security scan passed (no leak of score/answerKey/isCorrect).
- [x] Full Maven build PASS.
- [x] Portable build PASS.
- [x] Created `docs/tse_v2_backend_submit_dryrun_regression_phase_7d_5.md`.

### Phase 6M / 7B: Encrypted Submit Payload Dry-run - Debug Only
- [x] Created `TSEV2SubmitDryRunMeta`.
- [x] Created `TSEV2EncryptedSubmitDryRunService`.
- [x] Validation: Payload matches render context, safe against leak tokens.
- [x] Key strictly kept in RAM, never written.
- [x] Security scan passed: No file plaintext, no socket.
- [x] Targeted tests `TSEV2EncryptedSubmitDryRunServiceTest` (6 cases pass).
- [x] Maven build PASS (133 tests).
- [x] Portable build PASS.
- [x] GUI integration deferred intentionally.
- [x] Created `docs/tse_v2_encrypted_submit_payload_dryrun_phase_6m_7b.md`.

### Phase 6H: V2 Answer Draft Snapshot Contract - In-memory Only
- [x] Read Phase 6G, Phase 6G.5, and Phase 6F gate docs.
- [x] Kept VM manual V2_DEBUG and legacy GUI acceptance pending; Phase 6H does not touch lockdown/autosave/submit.
- [x] Created `TSEV2AnswerDraftSnapshot` DTO.
- [x] Created `TSEV2AnswerDraftItem` DTO.
- [x] Created RAM-only `TSEV2AnswerDraftSnapshotService`.
- [x] Snapshot contract includes examId, paperId, attemptId, packageHash, questionCount, answeredCount, answers, and snapshotHash.
- [x] Validates selectedOptionId belongs to the matching questionId.
- [x] Blocks sensitive markers such as sessionToken, keyB64, plaintext, answerKey, correctOption, password/passwordHash, and score.
- [x] Updated V2 debug footer text to state draft snapshot is in-memory only.
- [x] No autosave implementation.
- [x] No file write implementation.
- [x] No network/backend call.
- [x] No submit/EXAM_SUBMIT call.
- [x] No Rust/Quick Settings/Taskbar/Parent/JCEF bridge change.
- [x] Security scan completed; hits are only blacklist validation or negative tests.
- [x] Targeted Phase 6H tests PASS: 11 tests, 0 failures.
- [x] `mvn clean install` PASS: 93 tests, 0 failures.
- [x] `build_portable.ps1` PASS.
- [ ] Pending VM-only legacy `run_input_test.bat --exam-id 3` acceptance.

### Phase 6I: Local Encrypted Draft Autosave Prototype - Debug Only
- [x] Read Phase 6H, Phase 6G.5, Phase 6G, and secure exam task docs.
- [x] Kept VM manual V2_DEBUG and legacy GUI acceptance pending; did not run lockdown GUI on physical machine.
- [x] Created `TSEV2LocalEncryptedDraftAutosaveService`.
- [x] Created `TSEV2DraftAutosaveMeta`.
- [x] Implemented RAM-only AES `SecretKey` generation for debug autosave.
- [x] Implemented AES-GCM encrypted draft save/load.
- [x] Writes only `v2_answer_draft_autosave.enc` and `v2_answer_draft_autosave.meta.json`.
- [x] Does not write plaintext draft JSON to disk.
- [x] Does not write key/sessionToken to disk.
- [x] Safe meta excludes answers, selectedOptionId, key/token/plaintext/scoring markers.
- [x] Integrated V2_DEBUG selection change with encrypted local autosave handler.
- [x] UI shows safe autosave status: saved / failed with short error code.
- [x] No Submit/Save/Finish button added.
- [x] No network/backend call.
- [x] No submit/EXAM_SUBMIT call.
- [x] No submit_payload/autosave_payload creation.
- [x] No scoring/correct-answer logic.
- [x] No Rust/Quick Settings/Taskbar/Parent/JCEF bridge change.
- [x] Added `TSEV2LocalEncryptedDraftAutosaveServiceTest`.
- [x] Updated `TSEV2SelectionPanelTest`.
- [x] Targeted Phase 6I tests PASS: 15 tests, 0 failures.
- [x] Narrow security scan completed; hits are RAM-only API type, blacklist validation, encrypted/safe-meta write, or negative tests only.
- [x] `mvn clean install` PASS: 104 tests, 0 failures.
- [x] `build_portable.ps1` PASS.
- [ ] Pending VM-only legacy `run_input_test.bat --exam-id 3` acceptance.

### Phase 6J: Local Draft Restore Prototype - Debug Only
- [x] Read Phase 6I, Phase 6H, Phase 6G, and secure exam task docs.
- [x] Kept VM manual V2_DEBUG and legacy GUI acceptance pending; did not run lockdown GUI on physical machine.
- [x] Extended `TSEV2LocalEncryptedDraftAutosaveService` with restore error codes.
- [x] Implemented `tryLoadEncryptedDraft(...)` for `.enc` + `.meta.json`.
- [x] Implemented render-model match validation before apply.
- [x] Implemented apply-to-selection-state after validation only.
- [x] Preserved RAM-only draft key strategy; no key persistence added.
- [x] Integrated V2_DEBUG panel startup restore using the same debug RAM key context.
- [x] UI reflects restored radio buttons and `Answered X / Y` progress.
- [x] UI shows safe restore status without raw path/key/plaintext.
- [x] Wrong key maps to `ERROR_DRAFT_DECRYPT_FAILED`.
- [x] Tamper/hash mismatch maps to `ERROR_DRAFT_HASH_MISMATCH`.
- [x] Unsafe meta maps to `ERROR_DRAFT_META_UNSAFE`.
- [x] Unsafe payload maps to `ERROR_DRAFT_PAYLOAD_UNSAFE`.
- [x] Context/option mismatch maps to `ERROR_DRAFT_CONTEXT_MISMATCH`.
- [x] No Submit/Save/Finish button added.
- [x] No network/backend call.
- [x] No submit/EXAM_SUBMIT call.
- [x] No submit_payload/autosave_payload creation.
- [x] No scoring/correct-answer logic.
- [x] No Rust/Quick Settings/Taskbar/Parent/JCEF bridge change.
- [x] Added `TSEV2LocalEncryptedDraftRestoreTest`.
- [x] Updated `TSEV2SelectionPanelTest`.
- [x] Targeted Phase 6J tests PASS: 19 tests, 0 failures.
- [x] Narrow security scan completed; hits are RAM-only API type, blacklist validation, encrypted/safe-meta write, or negative tests only.
- [x] `mvn clean install` PASS: 117 tests, 0 failures.
- [x] `build_portable.ps1` PASS.
- [x] Created `docs/tse_v2_local_encrypted_draft_restore_phase_6j.md`.
- [ ] Pending VM-only legacy `run_input_test.bat --exam-id 3` acceptance.

### Phase 7E: Server-side Submit Payload Persistence - Dry-run DB Only
- [x] Read Phase 7D.5 gate document and confirmed GO for DB dry-run persistence.
- [x] Added dry-run-only table migration in `V2SubmitDryRunPayloadDAO.ensureSchema()`.
- [x] Created `V2SubmitDryRunRecord`.
- [x] Created `V2SubmitDryRunPayloadDAO` with insert/find helpers for `v2_submit_dryrun_payloads`.
- [x] Created `V2SubmitDryRunPersistenceResult` safe metadata DTO.
- [x] Created `V2SubmitDryRunPersistenceService`.
- [x] Added feature flag `tse.v2.submitDryRunPersistence.enabled`, default false.
- [x] Added socket action `EXAM_SUBMIT_V2_DRYRUN_PERSIST`.
- [x] Route returns `EXAM_SUBMIT_V2_DRYRUN_PERSIST_OK` or `EXAM_SUBMIT_V2_DRYRUN_PERSIST_ERROR`.
- [x] Validation must pass before any DB insert.
- [x] Unsafe payload markers are blocked before DB insert.
- [x] Result DTO does not expose answers or selected option IDs.
- [x] No `exam_results` write added.
- [x] No attempt status update to `SUBMITTED` added.
- [x] No legacy `EXAM_SUBMIT` call or modification for the dry-run persist path.
- [x] No legacy `submit_payload.enc` path added.
- [x] No Final Submit, Rust, Quick Settings, Taskbar, Parent, or JCEF change.
- [x] Added `V2SubmitDryRunPersistenceServiceTest`.
- [x] Targeted Phase 7E tests PASS: 14 tests, 0 failures.
- [x] Security scan completed; hits are only blacklist/negative tests, the V2 dry-run INSERT, or pre-existing legacy code outside the new route.
- [x] `mvn clean install` PASS: 160 tests, 0 failures.
- [x] `build_portable.ps1` PASS.
- [x] Created `docs/tse_v2_server_submit_dryrun_persistence_phase_7e.md`.

### Phase 7F: Server-side Submit Dry-run Persistence Regression Gate - No VM
- [x] Read Phase 7E and preceding documents.
- [x] Audited DAO, schema ensure code for additive properties and non-modifying characteristics.
- [x] Verified `isSafeToPersist` rule accurately denies answers and password payload JSON.
- [x] Created regression test `V2SubmitDryRunPersistenceRegressionTest.java` that covers 14 distinct constraints.
- [x] Security scan passed (No leak inside execution context).
- [x] Full Maven build PASS.
- [x] Portable build PASS.
- [x] Created `docs/tse_v2_server_submit_dryrun_persistence_regression_phase_7f.md`.
- [ ] Pending VM-only legacy `run_input_test.bat --exam-id 3` acceptance.
### Phase 7G: V2 Server-side Submit Record Prototype - No Grading
- [x] Implemented V2SubmitRecord model, DAO, and Service.
- [x] Added EXAM_SUBMIT_V2_RECORD_CREATE handler in ClientHandler.
- [x] Updated tests to JUnit 5 and fixed compilation issues.
- [x] Verified build and tests pass successfully.
- [x] Created docs/tse_v2_server_submit_record_prototype_phase_7g.md.

### Phase 7H: Submit Record Regression + Hash Standardization Gate - No VM
- [x] Standardized payloadHash to SHA-256 hex string validation.
- [x] Audited schemas to ensure additive-only without legacy touch.
- [x] Implemented regression tests for invalid hashes.
- [x] Performed security scan successfully on V2 DB classes.
- [x] Produced tse_v2_submit_record_regression_phase_7h.md docs.

### Phase 7I: V2 Attempt Finalization Draft + Submit Record State Machine - No Grading
- [x] Standardized V2SubmitRecord state machine (FINALIZATION_DRAFTED).
- [x] Implemented V2AttemptFinalizationDraftService with idempotency.
- [x] Added socket action EXAM_SUBMIT_V2_FINALIZATION_DRAFT with role validation.
- [x] Performed security scan to ensure no legcay grading logic leakage.
- [x] Created comprehensive unit tests (185 total passing tests).
- [x] Produced tse_v2_attempt_finalization_draft_phase_7i.md documentation.

### Phase 7J + 7K: V2 Finalization Ledger + Full Regression - No Grading / No Final Submit
- [x] Implemented V2AttemptFinalizationLedgerDAO for ledger DB ops.
- [x] Implemented V2AttemptFinalizationLedgerService utilizing Mock DAO tests for rapid iteration.
- [x] Registered EXAM_SUBMIT_V2_FINALIZATION_LEDGER socket action in ClientHandler.
- [x] Enforced idempotency check and "FINALIZATION_DRAFTED" prerequisite check.
- [x] Phase 10E: Manual Candidate V2 Submit Execution Trigger (Prepare-only due to pending score schema).
- [x] Phase 10F: Manual Candidate Execution Audit Ledger.
- [ ] Phase 10G: ...Candidate Submit Orchestrator GatePrototype - No Grading
- [x] Implemented V2AttemptClosureDraftRecord, DAO, Service for Closure Drafts.
- [x] Implemented V2ServerSubmitNoGradingOrchestratorService to orchestrate DryRunPersist -> Record -> FinalizationDraft -> Ledger -> ClosureDraft.
- [x] Orchestrator is idempotent and halts on any step failure.
- [x] Orchestrator does NOT grade, does NOT write to exam_results, does NOT update status to SUBMITTED.
- [x] Passed all 200 unit tests via Maven build.
- [x] Portable build success. No legacy flow touched.


### Phase 10C: Manual Candidate V2 Submit Route
- [x] Phase 10D: Candidate Submit Orchestrator GatePrototype - No Grading
- [x] Implemented V2AttemptClosureDraftRecord, DAO, Service for Closure Drafts.
- [x] Implemented V2ServerSubmitNoGradingOrchestratorService to orchestrate DryRunPersist -> Record -> FinalizationDraft -> Ledger -> ClosureDraft.
- [x] Orchestrator is idempotent and halts on any step failure.
- [x] Orchestrator does NOT grade, does NOT write to exam_results, does NOT update status to SUBMITTED.
- [x] Passed all 200 unit tests via Maven build.


### Phase 7L + 7M: V2 Attempt Closure Draft + Submit Orchestrator Prototype - No Grading
- [x] Implemented V2AttemptClosureDraftRecord, DAO, Service for Closure Drafts.
- [x] Implemented V2ServerSubmitNoGradingOrchestratorService to orchestrate DryRunPersist -> Record -> FinalizationDraft -> Ledger -> ClosureDraft.
- [x] Orchestrator is idempotent and halts on any step failure.
- [x] Orchestrator does NOT grade, does NOT write to exam_results, does NOT update status to SUBMITTED.
- [x] Passed all 200 unit tests via Maven build.
- [x] Added EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING to ClientHandler.
- [x] Created docs/tse_v2_attempt_closure_draft_and_orchestrator_phase_7l_7m.md.

### Phase 7N + 7O: Client-side No-Grading Server Submit Bridge + Full Regression Gate
- [x] Create TSEV2ServerSubmitTransport.
- [x] Create TSEV2ServerNoGradingSubmitBridgeResult.
- [x] Create TSEV2ServerNoGradingSubmitBridgeService.
- [x] Create TSEV2ClientSubmitPayloadPrepareService.
- [x] Add "Server Submit Dry-run" UI button in TSEV2ReadOnlyExamPanel.
- [x] Ensure feature flag tse.v2.clientServerNoGradingSubmit.enabled works.
- [x] Reject any unsafe response (answerKey, score, etc).
- [x] Write Unit Tests for Bridge, Prepare Service and Panel.
- [x] Run Security Scan and Maven Build.
- [x] Run Portable Build.
- [x] Create documentation and update task list.
- [x] Validate run_input_test.bat is PENDING.
- [x] Phase 7N/7O verification: canonical action is EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING.
- [x] Phase 7N/7O verification: client bridge uses canonical _OK/_ERROR responses and rejects unsafe response fields.
- [x] Phase 7N/7O verification: targeted tests PASS (28 tests).
- [x] Phase 7N/7O verification: full mvn clean install PASS (214 tests).
- [x] Phase 7N/7O verification: portable build PASS; VM run_input_test remains PENDING by physical-machine safety rule.

 # # #   P h a s e   7 P   +   7 Q :   R e a l   S u b m i t   T r a n s i t i o n   S a f e t y   D e s i g n   +   F e a t u r e   F l a g / R o u t e   H a r d e n i n g 
 -   [ x ]   C r e a t e   \ V 2 S u b m i t F e a t u r e F l a g s \   t o   c o n s o l i d a t e   a l l   V 2   f e a t u r e   f l a g s   w i t h   d e f a u l t   \  a l s e \ . 
 -   [ x ]   C r e a t e   \ V 2 S u b m i t A c t i o n s \   t o   d e f i n e   c a n o n i c a l   a c t i o n   n a m e s   f o r   t h e   V 2   p i p e l i n e . 
 -   [ x ]   R e f a c t o r   e x i s t i n g   \ V 2 S u b m i t . . . S e r v i c e \   c l a s s e s   a n d   \ C l i e n t H a n d l e r \   t o   u s e   t h e   c e n t r a l i z e d   f l a g s   a n d   a c t i o n s . 
 -   [ x ]   C r e a t e   \ d o c s / t s e _ v 2 _ r e a l _ s u b m i t _ t r a n s i t i o n _ s a f e t y _ d e s i g n _ p h a s e _ 7 p _ 7 q . m d \   d o c u m e n t i n g   t r a n s i t i o n   b o u n d a r i e s . 
 -   [ x ]   R u n   s e c u r i t y   s c a n   t o   v e r i f y   l e g a c y   r o u t e s ,   F i n a l   S u b m i t ,   a n d   R u s t   p r o c e s s e s   r e m a i n   u n c o m p r o m i s e d . 
 -   [ x ]   F u l l   M a v e n   r e g r e s s i o n   t e s t   a n d   p o r t a b l e   b u i l d   v e r i f y   s u c c e s s f u l . 
 
 
 
 # # #   P h a s e   7 Q . 5 :   F u l l   R e g r e s s i o n   G a t e   f o r   F e a t u r e   F l a g   +   R o u t e   H a r d e n i n g 
 -   [ x ]   R u n   f u l l   P o r t a b l e   b u i l d   ( p a s s e d ) . 
 -   [ x ]   V e r i f i e d   l e g a c y   E X A M _ S U B M I T ,   R u s t ,   F i n a l   S u b m i t ,   g r a d i n g   a n d   d a t a b a s e   r e s u l t s   a r e   c o m p l e t e l y   u n t o u c h e d . 
 -   [ x ]   C r e a t e d   d o c s / t s e _ v 2 _ f e a t u r e _ f l a g _ r o u t e _ h a r d e n i n g _ r e g r e s s i o n _ p h a s e _ 7 q _ 5 . m d . 

### Phase 7R / 8A: Real Submit Preflight Contract
- [x] Create V2RealSubmitPreflightResult.
- [x] Create V2RealSubmitPreflightService to check conditions before real submit.
- [x] Add feature flag tse.v2.realSubmitPreflight.enabled.
- [x] Create action EXAM_SUBMIT_V2_REAL_PREFLIGHT.
- [x] Ensure Preflight handles all boundary cases without grading or real submit.
- [x] Unit test in V2RealSubmitPreflightServiceTest.

### Phase 8A.5: Real Submit Preflight Full Regression + DB Isolation Gate
- [x] Refactor V2RealSubmitPreflightService for dependency injection.
- [x] Update V2RealSubmitPreflightServiceTest to run 100% offline (mock DAOs).
- [x] Full Maven test suite and Portable build validation.

### Phase 8B: Real Submit State Transition Draft
- [x] Create V2RealSubmitTransitionDraftRecord and DAO.
- [x] Create V2RealSubmitTransitionDraftResult.
- [x] Create V2RealSubmitTransitionDraftService with idempotency logic.
- [x] Add feature flag tse.v2.realSubmitTransitionDraft.enabled.
- [x] Add action EXAM_SUBMIT_V2_REAL_TRANSITION_DRAFT and ClientHandler socket route.
- [x] Reject if preflight is not READY_FOR_REAL_SUBMIT_DRAFT.
- [x] Strict boundary: NO grading, NO exam_results, NO SUBMITTED attempt updates.
- [x] Unit test in V2RealSubmitTransitionDraftServiceTest offline.
### Phase 8C: Real Submit Attempt Status Transition Gate
- [x] Create V2RealSubmitAttemptStatusTransitionGateResult.
- [x] Create V2RealSubmitAttemptStatusTransitionGateService.
- [x] Add feature flag tse.v2.realSubmitAttemptStatusTransitionGate.enabled.
- [x] Add action EXAM_SUBMIT_V2_REAL_ATTEMPT_STATUS_GATE and ClientHandler socket route.
- [x] Double-layer validation: Preflight is READY_FOR_REAL_SUBMIT_DRAFT, Transition Draft is REAL_SUBMIT_TRANSITION_DRAFTED.
- [x] Strict boundary: NO grading, NO exam_results, NO SUBMITTED attempt updates, NO DAO/DB table creation.
- [x] Unit test in V2RealSubmitAttemptStatusTransitionGateServiceTest offline.
- [x] Run security scan and maven full build.
- [x] Run portable build and update docs.

### Phase 8D + 8E: Attempt Status Transition Draft Persistence + Readiness Orchestrator
- [x] Create V2AttemptStatusTransitionDraftRecord and DAO.
- [x] Create V2AttemptStatusTransitionDraftResult and Service.
- [x] Create V2RealSubmitReadinessOrchestratorService.
- [x] Add feature flags and actions.
- [x] Test offline and build portable.

### Phase 8F + 8G: Controlled Attempt Status Execution + Execution Ledger
- [x] Create V2ExamAttemptStatusDAO with transaction support.
- [x] Create V2AttemptStatusExecutionLedgerRecord and DAO.
- [x] Create V2AttemptStatusExecutionResult and Service.
- [x] Integrate into ClientHandler.
- [x] Offline unit tests with CAS failure and ledger insert failure.
- [x] Maven build and portable build.

### Phase 8H + 9A + 9B: Post-Submit Integrity Audit + Grading Preflight + Server-side Score Draft
- [x] Create V2PostSubmitIntegrityAuditResult and Service.
- [x] Create V2GradingPreflightResult and Service.
- [x] Create V2ScoreDraftRecord and DAO.
- [x] Create V2ScoreDraftResult and Service.
- [x] Create V2AnswerKeyResolver and V2AnswerPayloadParser boundary interfaces.
- [x] Add feature flags and actions for Audit, Preflight, and Score Draft.
- [x] Implement offline unit tests for all three services.
- [x] Integrate routes into ClientHandler.
- [x] Run security scan to ensure no raw answers, answerKey, or exam_results leak to client.
- [x] Run Maven full build and portable build.

### Phase 9C + 9D + 9E: Score Draft Integrity Audit + Official Result Draft + Result Publication Readiness Gate
- [x] Create V2ScoreDraftIntegrityAuditResult and Service.
- [x] Create V2OfficialResultDraftRecord, DAO, Result and Service.
- [x] Create V2ExamResultsReadOnlyProbe and Impl.
- [x] Create V2ResultPublicationReadinessResult and Service.
- [x] Add feature flags and actions.
- [x] Implement offline unit tests for all three services using standard Java classes (no Mockito).
- [x] Integrate routes into ClientHandler.
- [x] Phase 9F + 9G: Controlled exam_results Write + Result Publication Ledger
- [x] Phase 9H + 9I: Final Result Handoff Response + Publication Verification Gate
- [x] Phase 9J + 9K: Final Attempt Status Readiness Gate + Controlled Final Status Execution Ledger
- [x] Run security scan (`findstr`).
- [x] Run full `mvn clean install` and confirm NO database mutating code (except Phase 9F/9G, 9K CAS writes).
- [x] Create `docs/tse_v2_final_attempt_status_readiness_and_execution_phase_9j_9k.md`.

### Phase 10A + 10B: V2 Shadow Integration into Student Exam Flow + Primary Flow Cutover Readiness Gate
- [x] Create V2StudentFlowShadowCheckResult and Service.
- [x] Create V2StudentFlowCutoverReadinessResult and Service.
- [x] Add feature flags and actions for Shadow Check and Cutover Readiness.
- [x] Implement offline unit tests and strictly forbid `SUBMITTED`/`COMPLETED` attempts.
- [x] Integrate routes into ClientHandler.
- [x] Run security scan (`findstr`).
- [x] Run full `mvn clean install` and confirm NO database mutating code.
- [x] Create `docs/tse_v2_student_flow_shadow_integration_phase_10a_10b.md`.
 
 # # #   P h a s e   1 0 C   +   1 0 D :   M a n u a l   C a n d i d a t e   V 2   S u b m i t   R o u t e   +   C a n d i d a t e   S u b m i t   O r c h e s t r a t o r   G a t e 
 
 -   [ x ]   P h a s e   1 0 C :   M a n u a l   C a n d i d a t e   V 2   S u b m i t   R o u t e 
 
 -   [ x ]   P h a s e   1 0 D :   C a n d i d a t e   S u b m i t   O r c h e s t r a t o r   G a t e 
 
 ### Phase 10E + 10F: Manual Candidate V2 Submit Execution Trigger + Execution Audit Ledger
- [x] Create V2ManualCandidateSubmitExecutionResult and Service (PREPARE-ONLY)
- [x] Create V2ManualCandidateExecutionAuditResult and Service
- [x] Create V2ManualCandidateExecutionLedgerRecord and DAO
- [x] Add feature flags and actions
- [x] Implement unit tests and verify PREPARE-ONLY status
- [x] Update ClientHandler
- [x] Run security scan
- [x] Maven build and portable build pass

### Phase 10G + 10H: Payload/AnswerKey Resolver Schema Implementation + Score Draft Dependency Hardening
- [x] Implement V2DatabaseAnswerKeyResolver concrete (SELECT only)
- [x] Implement V2JsonAnswerPayloadParser (unavailable-safe)
- [x] Harden V2ScoreDraftService to prevent NPE
- [x] Implement V2ScoreDraftDependencyHealthService and Result
- [x] Update V2SubmitFeatureFlags and Actions
- [x] Implement offline unit tests covering unavailable edge-cases
- [x] Run security scan without exposing answer keys or payloads
- [x] Maven build and portable build pass


