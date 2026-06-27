# Phase 15B: VM-only Desktop Demo Manual Execution Packet

## Critical Warning
**DO NOT RUN THIS ON A PHYSICAL MACHINE.**
This test switches the Windows Desktop (`SwitchDesktop`) to an isolated UI shell. Although auto-return is integrated, physical testing may lock standard inputs and require an immediate hard restart.

## Pre-requisites & Checklist
- [ ] 1. Ensure the host is a designated Windows Virtual Machine (VM).
- [ ] 2. Create a VM snapshot prior to starting this execution.
- [ ] 3. Close all unnecessary applications inside the VM to avoid data loss.

## Execution Procedure
1. **Capability Probe Check**: Open PowerShell inside the VM and navigate to `rust-core`. Run:
   ```powershell
   cargo run --release -- --probe
   ```
   Ensure the output indicates success and capabilities are present.

2. **Trigger Safe Demo**: Run the actual desktop isolation sequence securely.
   ```powershell
   cargo run --release -- --desktop-demo-safe
   ```

3. **Validation Steps**:
   - [ ] 4. Confirm the `SwitchDesktop` occurs (blank secure desktop appearance).
   - [ ] 5. Confirm that the application sleeps and performs auto-return within 5 seconds.
   - [ ] 6. Confirm the handles are safely closed and the thread exits cleanly.
   - [ ] 7. Confirm no zombie Rust processes remain in the background (`Get-Process rust-core`).
   - [ ] 8. Verify the registry and autostart folder are empty/unmodified.

## Status
- `--desktop-demo-safe`: PENDING / NOT RUN - VM-only manual test
- Smoke status: PENDING / NOT RUN
