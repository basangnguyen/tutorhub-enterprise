# Release Notes - TutorHub Secure Exam 1.0.0

This is the first internal release of the TutorHub Secure Exam application featuring robust desktop isolation.

## Core Features
- Full Exam Workflow (Login -> Configuration -> JCEF Questions -> Submit).
- Neon PostgreSQL Database Integration.
- Cloud WebSocket Server connectivity.
- Offline Auto-save capabilities.

## Security Features (Phase 2 - Rust Lockdown)
- **Secure Desktop Isolation**: Parent launcher isolates the exam UI into a separate secure desktop (`SwitchDesktop`), preventing interaction with background apps.
- **Keyboard Hook**: Blocks dangerous shortcuts (Windows Key, Alt-Tab, Ctrl-Alt-Del mitigation) using low-level keyboard hooks (`WH_KEYBOARD_LL`).
- **Screen Protection**: Prevents screen capturing by tools like OBS using `SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)`.
- **Process Scanner**: Detects and logs forbidden background processes.
- **Watchdog Timer**: Auto-kills the lockdown environment if communication is lost or if testing exceeds safe limits.
- **Process Isolation**: The parent Java process acts as a communication bridge, while the Child Java process renders the browser within the isolated desktop, preventing sandbox escapes.
- **Encrypted Payloads**: The Child encrypts submissions using a dynamic symmetric key.
- **DPAPI Recovery**: The symmetric key is securely stored using Windows DPAPI, ensuring only the local user can decrypt recovered exam data.

## Packaging Features
- **Inno Setup Installer**: Fully automated installation, uninstallation, and shortcut creation.
- **JLink Mini Runtime**: Offers an 84% reduction in JRE size by stripping unused modules.

## Test Status
- Phase 1 (Core Flow): **PASS**
- Phase 2 (Rust Secure Desktop): **PASS**
- Phase 2 (Process Isolation & Cryptography): **PASS**
- Phase 2 (Portable & Installer Build): **PASS**

## Known Limitations
- Code signing is not yet implemented.
- Auto-update is not included.
