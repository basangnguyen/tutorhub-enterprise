# TSE Login Captcha, Password Reveal, and About Dialog Phase

Date: 2026-06-17

## Goal

Make three previously visual-only controls on the TutorHub Secure Exam Login and Configuration screens functional:

- Local captcha challenge and refresh.
- Password reveal/hide button.
- Top-right help icon opening application information.

## Reference Notes

Safe Exam Browser reference was checked under `seb-reference`:

- `SafeExamBrowser.UserInterface.Desktop/Windows/PasswordDialog.xaml`
- `SafeExamBrowser.UserInterface.Desktop/Windows/AboutWindow.xaml.cs`
- `SafeExamBrowser.UserInterface.Desktop/WindowFactory.cs`

Findings:

- SEB uses platform password controls (`PasswordBox`) for masked password input.
- SEB exposes application information through a dedicated `AboutWindow` created by the UI window factory.
- No local login captcha implementation was found in the SEB reference tree. TutorHub captcha is therefore treated as a local UI guard only, not as the primary authentication boundary.

## Implementation

### Captcha

Added `TSECaptchaService`:

- Uses `SecureRandom`.
- Generates a six-digit numeric challenge.
- Displays the challenge as spaced digits.
- Accepts user input with optional whitespace.
- Refreshes after local captcha failure and after server login failure.

The login request is not sent to the server unless captcha verification passes.

### Password Reveal

The trailing eye icon in `TSELoginPanel` is now an interactive button:

- Hidden mode uses `JPasswordField` echo char `\u25CF`.
- Visible mode uses `setEchoChar((char) 0)`.
- Icon toggles between `eye.svg` and `eye-off.svg`.

### About Dialog

Added `TSEAboutDialog` and wired it to:

- Login help icon.
- Configuration help icon.

The dialog shows:

- Product name.
- Current screen.
- Build/version fallback.
- Secure Exam feature summary.
- Local captcha security note.

## Files Changed

- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSECaptchaService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEAboutDialog.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSELoginPanel.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEConfigListPanel.java`
- `src/test/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSECaptchaServiceTest.java`

## Verification

Commands run:

```powershell
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" --no-transfer-progress test
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" --no-transfer-progress clean install
powershell -ExecutionPolicy Bypass -File .\build_portable.ps1
```

Results:

- Maven test: PASS, 25 tests.
- Maven clean install: PASS.
- Portable build: PASS.

## Manual VM Test Checklist

Run only in VM test-safe:

```powershell
cd D:\Ban_sao_du_an\dist\TutorHubSecureExam
.\run_input_test.bat --exam-id 3
```

Check:

- Captcha changes when refresh icon is clicked.
- Wrong captcha blocks login before server auth.
- Correct captcha allows normal server auth.
- Password eye shows password text.
- Eye-off hides password again.
- Login help icon opens About dialog.
- Configuration help icon opens About dialog.
- Quick Settings, VIE/ENG, Safe Refresh, Power block, Final Submit remain unchanged.

## Risks

- Captcha is local-only and should not be considered a bot-defense boundary by itself.
- Backend should still enforce authentication throttling, account lockout policy, and server-side abuse protection.
- Visual acceptance for About dialog still needs VM/manual check because lockdown UI was not launched on the physical Lenovo machine.

## Help Popup Polish Update

Date: 2026-06-17

The top-right Help/About popup was redesigned as an official TutorHub Secure Exam product information dialog.

Updates:

- Replaced the debug-style information table with structured sections.
- Added mandatory product metadata: `TutorHub Secure Exam`, `tutorhub_tse_v1`, and founder `Nguyen Ba Sang`.
- Added concise Vietnamese content for introduction, core functions, and usage guide.
- Kept the existing `TSEAboutDialog.showDialog(...)` entry point used by Login and Configuration screens.
- Did not touch Taskbar, Quick Settings, Rust, Final Submit, or exam runtime logic.

Verification:

- `mvn --no-transfer-progress test` PASS, 25 tests, 0 failures.
- `mvn --no-transfer-progress clean install` PASS, 25 tests, 0 failures.
- `build_portable.ps1` PASS after closing stale portable Java processes, and the portable JAR now contains `tutorhub_tse_v1`.

## Compact Help Popup Update

Date: 2026-06-17

After VM visual feedback, the Help/About popup was reduced to a compact dialog because the first product-info version was too tall, clipped content, and felt heavy.

Updates:

- Dialog size changed to 560x430.
- Content shortened to the essential product summary.
- Header now includes a visible top-right close button.
- Footer now includes a visible Dong button.
- Kept product metadata: TutorHub Secure Exam, tutorhub_tse_v1, founder Nguyen Ba Sang.
- Kept the existing showDialog API and did not touch Taskbar, Quick Settings, Rust, Final Submit, or exam runtime logic.

Verification:

- mvn --no-transfer-progress test PASS, 25 tests, 0 failures.
- build_portable.ps1 PASS.
- Portable JAR contains tutorhub_tse_v1 marker.