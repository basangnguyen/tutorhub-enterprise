# TutorHub Secure Exam 1.0.0 Internal Test

**Date:** 2026-06-08

## Package Contents
- `TutorHubSecureExamSetup.exe`: Full installer containing complete JRE (~360MB). Use if you encounter missing module issues.
- `TutorHubSecureExamSetup-jlink.exe`: Minimized installer containing a stripped JRE (~241MB). Recommended for end-users.
- `checksums.sha256`: SHA-256 hashes to verify installer integrity.
- `RELEASE_NOTES.md`: Details of what is included in this release.
- `docs/tse_release_validation_checklist.md`: Complete testing checklist for QA.
- `docs/tse_code_signing_plan.md`: The planned code signing workflow.

## Installation Instructions
1. Download the desired installer (`TutorHubSecureExamSetup-jlink.exe` is recommended).
2. Double-click the installer.
3. If prompted by Windows SmartScreen ("Windows protected your PC"), click **More info** -> **Run anyway**. *(This is a known limitation until the executable is signed with an EV certificate).*
4. Follow the setup wizard to install the application.

## Prerequisites for Testing
Ensure the backend services are running before testing:
1. Start the Neon Database or local PostgreSQL.
2. Start the TutorHub Web Socket Server on `localhost:7860`.

## Quick E2E Test
1. Launch the application from the Desktop shortcut.
2. Login with valid credentials.
3. Ensure server connectivity is established.
4. Select an exam and start it.
5. Wait for the Secure Desktop to initialize and the Child JCEF process to load the exam UI.
6. Verify keyboard shortcuts (Windows Key, Alt-Tab) are blocked.
7. Submit the exam.
8. Verify submission payload arrives at the Server and is saved in Neon DB.

## Uninstallation
- Open Windows Settings -> Apps -> Installed apps.
- Search for "TutorHub Secure Exam" and click Uninstall.

## Known Limitations
- **No Production Code Signing**: The executables are currently unsigned. They may trigger Antivirus false positives or Windows SmartScreen.
- **DPAPI Recovery Limit**: The DPAPI encrypted auto-save files can only be decrypted by the same Windows User Account on the exact same machine. They cannot be transferred.
- **No Auto-Update**: This version does not support automatic updates.
