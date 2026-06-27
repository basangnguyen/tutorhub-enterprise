# Phase 15D: Desktop Isolation Recovery & Abort Runbook

## Overview
This runbook provides recovery procedures specifically designed for Virtual Machine testing of the Rust Lockdown Core `CreateDesktopW`/`SwitchDesktop` functionality (`--desktop-demo-safe`).

## 1. Expected Auto-Return Behavior
The demo automatically schedules a 5-second sleep thread before returning to the default desktop thread. If you trigger the demo and see a blank screen, wait 5 seconds for it to recover.

## 2. Recovery on Failure (VM Only)
If the desktop fails to revert after 10-15 seconds, the OS desktop handle has not been successfully restored or the Rust process crashed while the alternate desktop was active.
### Immediate VM Actions:
1. **Send Ctrl+Alt+Del via hypervisor**: Often this bypasses the active desktop isolation layer and prompts the security screen on the default desktop. If successful, use Task Manager to terminate `rust-core.exe`.
2. **Reboot VM**: If the UI is completely unresponsive, force restart the virtual machine via the hypervisor controls.

### Revert VM State:
1. Shutdown the VM.
2. In the hypervisor (e.g., VirtualBox, VMware, Hyper-V), revert to the snapshot created prior to step execution (as mandated in the Phase 15B Packet).

## 3. Post-Recovery Validation
Once the VM is restored or restarted:
- **Validate Processes**: Open Task Manager (or PowerShell `Get-Process`) and ensure `rust-core.exe` is not running.
- **Validate Registry**: Check `HKCU\Software\Microsoft\Windows\CurrentVersion\Run` or `RunOnce` to confirm no unwanted auto-starts exist.

## 4. Crucial Warning
**DO NOT RETRY ON PHYSICAL MACHINES.** If a failure occurs in the VM, it means the codebase is unstable. Do not attempt to bypass the system or hide operations. Debug and fix the error directly in the code before rerunning within the VM.
