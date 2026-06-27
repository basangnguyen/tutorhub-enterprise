# TutorHub Secure Exam Rust Core

This module is the Phase 14 Safe Proof of Concept (PoC) for the Rust Lockdown Core.

## Safe PoC Design Principles

- **No physical machine lockdown**: This tool operates purely in a probe / safe demo capacity for VM environments.
- **No malicious hooks**: It does not install global keyboard hooks, mouse hooks, nor does it intercept system key sequences (Alt+Tab, Ctrl+Alt+Del, Win Key).
- **No taskmgr/explorer disabling**: Task Manager and Explorer remain fully operational.
- **No persistence**: The program does not modify the Windows registry or setup autostart routines.

## Commands

### `--probe`
Probes the system environment (Windows API compatibility, basic VM detection) and safely tests if a desktop could be created or opened. Emits JSON output used by the Java Backend.

### `--desktop-demo-safe`
Demonstrates creating a Windows Desktop (`CreateDesktopW`) and switching to it (`SwitchDesktop`) for exactly 5 seconds before returning to the original desktop. This MUST ONLY be run manually in a VM test environment.

### `--ipc-diagnostic`
Simple ping to verify executable invocation and basic JSON serialization works from the Java `ProcessBuilder`.
