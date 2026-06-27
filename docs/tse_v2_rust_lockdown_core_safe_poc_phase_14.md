# Phase 14A + 14B + 14C + 14D: Rust Lockdown Core Safe PoC

## 1. Overview
This phase implements a Safe Proof of Concept (PoC) for the Rust Lockdown Core. It proves that the Rust process can be correctly spawned from Java, that the Windows Desktop isolation APIs (`CreateDesktopW`, `SwitchDesktop`) can be called securely without crashing, and that the Java/Rust IPC Integration Gate can capture output and status securely.

**Important**: This phase strictly follows the VM-only, Safe Demo strategy. No hooks or persistent lockdown changes were made to the primary system. The `--desktop-demo-safe` is a manual-only trigger designed explicitly for VM execution to prevent users from being locked out.

## 2. Components Developed
### Rust Application (`rust-core`)
- Created using `cargo new rust-core`.
- Dependency injected: `windows-sys = "0.52"` with required features (`Win32_Foundation`, `Win32_System_StationsAndDesktops`, `Win32_System_Threading`, `Win32_Security`, `Win32_Graphics_Gdi`).
- `main.rs`: CLI argument parser to handle `--probe`, `--desktop-demo-safe`, and `--ipc-diagnostic`.
- `capability_probe.rs`: Calls basic Windows API to test thread access and desktop handler availability.
- `desktop_isolation.rs`: Implements the `Safe CreateDesktopW` demo logic which runs briefly, sleeps for 5 seconds, and restores the original desktop.
- `ipc_server.rs`: Implements standard JSON-based IPC reporting struct for seamless deserialization by Java.

### Java Backend Integration
- **`V2RustLockdownCoreProbeService.java`**: Invokes the `rust-core.exe` (or `rust-core`) binary using `ProcessBuilder` with a 3-second timeout for the `--probe` command.
- **`V2RustLockdownCoreProbeResult.java`**: DTO mapping the `ProbeResult` emitted by Rust.
- **`V2SubmitActions.java`**: Hooked `EXAM_SUBMIT_V2_RUST_LOCKDOWN_CORE_PROBE` action.
- **`V2SubmitFeatureFlags.java`**: Added `tse.v2.rustLockdownCoreProbe.enabled` feature flag.

## 3. Security & Stability Policies Followed
- **No Hooks**: The code has no global hooks (e.g., `SetWindowsHookEx`) yet.
- **No Production Leakage**: All changes in Java are gated behind `tse.v2.rustLockdownCoreProbe.enabled=false` by default.
- **VM Safe Demo**: The `desktop_isolation.rs` sleeps for 5 seconds and relies on automatic garbage collection/thread exit to release handles. It must be manually run via CLI for demonstration.

## 4. Audit & Quality Assurance
- **Compile Time**: `cargo build --release` completed without feature-gate errors. `mvn clean install` passed all 648 unit tests.
- **Static Check**: Added security boundaries. The Java service sanitizes timeouts and guards against unbounded execution.
- **Portable Build**: `build_portable.ps1` completed successfully, ensuring the Java component packages correctly.

## 5. Next Steps
Move to Phase 15 for further core lockdown expansion within the Rust environment.
