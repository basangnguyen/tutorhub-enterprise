# TutorHub Secure Exam: Main App Integration Plan

## 1. Blueprint Original Idea vs Real Production Decision

**Original Blueprint Idea**: The TutorHub Desktop App (Java Swing) was supposed to act as the primary window, and when an exam is started, it would lock down the OS using the Rust Core, spawn JCEF locally in the same JVM, and intercept everything.

**Real Production Decision**: 
- The Main TutorHub App acts **only** as the Exam Portal.
- When the user clicks "Tham gia Kỳ thi", it spawns a completely separate process (the Secure Exam Launcher `run.bat`).
- The Secure Exam Launcher (`TSEProductionParentSubmitLabLauncher`) initializes, spawns the Rust Secure Desktop (`tutorhub_lockdown.exe`), which then spawns the Child Java process (`TSEExamChildClient`) containing JCEF onto the Secure Desktop.

**Reasoning**: Chromium Embedded Framework (JCEF) strictly requires being initialized inside the Secure Desktop to render correctly. Spawning JCEF in the default desktop and then trying to switch to the Secure Desktop results in hardware acceleration context loss and a black/empty screen. Process isolation is the only reliable way on Windows.

## 2. Integration Steps
- **Step 2I.9.1**: Build `SecureExamLauncherBridge` to intelligently locate the installed `run.bat` (checking system properties, common paths, and shortcuts) and use `ProcessBuilder` to launch it without blocking the Swing Event Dispatch Thread (EDT).
- **Step 2I.9.2 - 2I.9.6**: (Future) Pass `examId` and one-time authentication tokens via IPC or command line arguments, handle status sync between Portal and Exam processes.
