# TSE Codex Control Buttons Review

Date: 2026-06-12

Scope: audit and implementation plan for TutorHub Secure Exam control buttons. This document intentionally does not implement code. It records what was checked before modifying About, language, exit, WiFi, battery, brightness, refresh, and audio controls.

## 1. Documents Read

- `AGENTS.md`
- `docs/MASTER_SECURE_EXAM_BLUEPRINT_v4_.md`
- `docs/secure_exam_tasks_v2.md`
- `docs/secure_exam_rust_and_seb_learning_sources_ONLY_3_DOCS.md`
- `docs/tse_seb_control_reference_notes.md`
- `docs/tse_seb_control_implementation_plan.md`
- `seb-reference/` source tree for Safe Exam Browser behavior mapping

Important project constraints from those documents:

- Do not edit `seb-reference`; use it only for research.
- Do not copy SEB code directly. Convert behavior/patterns carefully and respect MPL-2.0 guidance.
- Do not disturb the existing final submit flow: `submit_payload.enc`, child exit code `0`, parent final payload detection, and submit success must remain stable.
- Do not introduce `JOptionPane`, modal `JDialog`, Swing GlassPane overlay over JCEF, duplicate submit, black bars, or free exit paths in the active exam client.
- Do not open Windows Settings, Control Panel, Task Manager, Explorer, or other OS escape surfaces during exam.
- Heavy checks such as network probing, battery polling, or process calls must not block the Swing EDT.

## 2. Antigravity Notes vs Real SEB Source

| Area | What Antigravity notes suggest | Real SEB source checked | Conclusion for TSE |
|---|---|---|---|
| Battery | Use a light battery status control similar to SEB. | `SafeExamBrowser.SystemComponents/PowerSupply/PowerSupply.cs`, `UserInterface.Desktop/Controls/Taskbar/PowerSupplyControl.xaml.cs` | Correct direction. SEB polls every 5s using `SystemInformation.PowerStatus`, emits `StatusChanged`, and UI reacts via dispatcher. TSE should implement status-only battery, off EDT, with graceful unavailable state. |
| WiFi / Network | Use simple status/test instead of full OS WiFi manager. | `SafeExamBrowser.SystemComponents/Network/NetworkAdapter.cs`, `NetworkControl.xaml.cs` | Antigravity simplifies heavily. SEB has real WinRT `WiFiAdapter`, scan/connect, and permission handling. For TSE, do not implement connect-to-WiFi now; use status/read-only server connectivity panel. |
| Audio | Audio control should be careful and probably not change OS master audio first. | `SafeExamBrowser.SystemComponents/Audio/Audio.cs`, `AudioControl.xaml.cs` | Correct. SEB uses NAudio CoreAudio, tracks original volume/mute, and restores. Java implementation would need native/JNA or external process. MVP should use internal mute/unmute or show unavailable. |
| Language | VIE/ENG toggle in footer. | `SafeExamBrowser.SystemComponents/Keyboard/Keyboard.cs`, `KeyboardLayoutControl.xaml.cs`, `I18nOperation.cs`, `TextKey.cs` | SEB keyboard control is OS input layout, not UI language. TSE should not switch OS keyboard. Implement TSE UI locale only. |
| Exit | Power button should not exit directly. | `QuitButton.xaml.cs`, `ShellResponsibility.cs`, `Taskbar.xaml.cs` | Correct. SEB button raises an event, centralized shell decides password/confirm/shutdown. TSE footer button must call a controller policy; block during active exam, allow only after final submit/authorized state. |
| Refresh | Refresh should be policy-controlled. | `SafeExamBrowser.Browser/BrowserWindow.cs` reload warning flow | Correct principle. TSE refresh must not reload blindly. It must collect/autosave first and reload only if answer preservation is proven. |
| About | About info should be accessible. | `AboutWindow.xaml.cs` | SEB uses WPF window, but TSE active exam should avoid modal Swing dialogs. Use in-app panel or JCEF DOM overlay. |
| Brightness | Add brightness-like control. | No direct equivalent found in inspected SEB taskbar controls. | Treat as TSE-only visual control. Do not change OS display brightness. Use internal dim/light CSS filter or app theme state. |

## 3. SEB Patterns Worth Reusing

- Thin UI controls: taskbar/header buttons emit events; application shell handles policy.
- Event-driven status updates: system component emits `Changed` or `StatusChanged`, UI updates on UI thread.
- Settings decide whether a control exists; controls should not hardcode business policy.
- Power/quit is centralized and cancellable, never a direct process exit from the button.
- Reload is policy-gated and logged.
- System status controls degrade gracefully when device/service is unavailable.

## 4. SEB Patterns Not To Apply Directly

- Do not port SEB WinRT WiFi scan/connect into TSE MVP. It can create permission problems and OS escape surfaces.
- Do not port NAudio OS master volume control directly into Java. It adds native complexity and must restore original audio state correctly.
- Do not change OS keyboard/input language from the TSE language button.
- Do not open SEB-style native windows or modal dialogs over the active JCEF exam content.
- Do not copy SEB source files or method bodies. Use behavioral mapping only.

## 5. Current TSE Code Observations

| File | Current state | Risk |
|---|---|---|
| `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamFooterStatusBar.java` | Static `VIE`, WiFi icon, volume icon, hardcoded battery 98% charging, power button opens `ExitConfirmDialog` when callback is non-null. | Good visual starting point, but not functional. Active exam currently passes `null` to disable power exit. Future control work must avoid modal `JDialog`. |
| `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamHeaderBar.java` | Refresh icon exists but has no action. No About/Brightness callbacks. Submit button calls callback. | Refresh can be wired safely, but only through controller policy and autosave preservation. |
| `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEExamChildClient.java` | Active child builds header, JCEF browser panel, footer. Submit uses JS DOM overlay in JCEF, calls `collectTSEAnswers()`, writes `submit_payload.enc`, then exits via `Runtime.halt(0)`. | Submit path is sensitive and must not be touched casually. There are still error-path `JOptionPane` and `GlassPane` cleanup references that should be cleaned in a later stabilization pass, but not mixed into control button MVP unless required. |
| `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEJcefLifecycleManager.java` | Message router only handles `SUBMIT_PAYLOAD:`. | New controls that need JS-to-Java callbacks should either add a separate namespaced route such as `TSE_CONTROL:` or keep interactions Java-side. Do not overload submit payload. |
| `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEBrowserPanel.java` | Provides `executeJavaScript`, `loadUrl`, `loadHtml`. | Enough for DOM overlays and CSS-based brightness if controlled carefully. |
| `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEProductionParentSubmitLabLauncher.java` | Parent waits for Rust/child, restores parent, prefers `submit_payload.enc`, falls back to autosave, then submits. Logs final payload detection. | Must preserve logs and behavior: `[TSE_PARENT] Found submit_payload.enc. Using FINAL submit payload.` No control button change should trigger fallback in normal final submit. |

## 6. Key Problems To Fix Later

- Footer controls are mostly decorative; they do not represent real battery/network/audio state.
- Refresh icon has no action, which is safer than a bad action, but incomplete.
- Exit policy is split: footer can open `ExitConfirmDialog` in generic use, while active TSE passes `null`. A future implementation should make this explicit through a policy callback.
- Some active child error paths still use `JOptionPane` and `GlassPane` cleanup. They are not the main submit overlay, but they violate the strictest reading of the prompt and should be removed in a focused cleanup after controls are stable.
- `TSEJcefLifecycleManager` currently has only one message namespace. Control overlays need their own namespace to avoid corrupting submit payload handling.
- There is no reusable environment service for battery/network checks, so adding checks directly into UI classes would mix UI and system logic.

## 7. Risks If Implemented Wrong

- Blocking the EDT with `netsh`, PowerShell, WMI/CIM, ping, or file/network checks can freeze the exam UI.
- A raw browser reload can lose unsaved answers or trigger parent fallback to `autosave_payload.enc`.
- A modal Swing dialog over JCEF can appear behind the browser, trap focus, or break secure desktop behavior.
- A Swing GlassPane overlay can render incorrectly over heavyweight JCEF and create black bars.
- A power/exit button that directly disposes or exits can let students leave the exam.
- Reusing `SUBMIT_PAYLOAD:` for control callbacks can corrupt final submit handling.
- Full OS audio, WiFi, brightness, or keyboard control can create security and recovery problems.
- Copying SEB source directly can create licensing and maintenance issues.

## 8. Safe Implementation Order

### Group 1: About + Language + Exit

Goal:
- Make the simplest controls functional without touching Rust, database, login, server, or submit payload.

Files likely to edit:
- `ExamFooterStatusBar.java`
- `ExamHeaderBar.java`
- `TSEExamChildClient.java`
- New helper if needed: `TSEControlPanelOverlay.java` or controller callbacks

Functions:
- Language toggle switches TSE UI labels between Vietnamese and English only.
- About opens a lightweight in-app panel/overlay, not `JOptionPane` or modal `JDialog`.
- Exit/power opens an in-app blocked-exit explanation during active exam, or delegates to parent policy after allowed final submit.

Logs:
- `[TSE_CONTROL] Language changed: vi|en`
- `[TSE_CONTROL] About opened`
- `[TSE_CONTROL] Exit requested: blocked|allowed`

Tests:
- Run child exam UI.
- Click language: header/footer labels update, exam content remains intact.
- Click about: panel appears and closes; no modal dialog.
- Click power during exam: exit is blocked; child remains active; submit still works.

Rollback:
- Revert only UI callback wiring and helper panel. Submit path should be untouched.

### Group 2: WiFi + Battery

Goal:
- Replace decorative status icons with safe read-only status.

Files likely to edit/create:
- New `TSEEnvironmentService.java`
- `ExamFooterStatusBar.java`
- Optional model classes: `NetworkStatus`, `BatteryStatus`

Functions:
- Battery polling off EDT, with unavailable state for desktop PCs.
- Network status checks server reachability/read-only SSID if available. Do not connect to networks.
- Popup/panel shows status, last checked time, and retry.

Logs:
- `[TSE_ENV] Battery status: percent=..., charging=..., available=...`
- `[TSE_ENV] Network status: online=..., serverReachable=...`

Tests:
- UI remains responsive while polling.
- On no battery device, show unavailable, not 98%.
- Disconnect network or mock failure: icon/panel reflects offline without freezing.

Rollback:
- Keep static icons as fallback if service fails.

### Group 3: Brightness + Refresh

Goal:
- Add safe visual brightness and controlled refresh.

Files likely to edit/create:
- `ExamHeaderBar.java`
- `TSEExamChildClient.java`
- `TSEBrowserPanel.java`
- Possibly `TSEJcefLifecycleManager.java` if JS callback namespace is needed

Functions:
- Brightness changes only the app/browser visual layer, not OS display brightness.
- Refresh first asks through DOM overlay/in-app panel, triggers autosave/collect, then either reloads safely or only revalidates/repaints depending on answer preservation.

Logs:
- `[TSE_CONTROL] Brightness changed: level=...`
- `[TSE_CONTROL] Refresh requested`
- `[TSE_CONTROL] Refresh autosave started|completed|blocked`

Tests:
- Brightness has visible effect and can reset.
- Refresh does not duplicate submit.
- Refresh does not create fallback submit during final submit flow.
- After refresh path, final submit still produces `submit_payload.enc`.

Rollback:
- Disable refresh action and keep icon inert if preservation is not proven.

### Group 4: Sound / Audio

Goal:
- Add audio control without unsafe OS-level master volume changes.

Files likely to edit/create:
- `ExamFooterStatusBar.java`
- Optional `TSEAudioState.java`

Functions:
- MVP: mute/unmute exam media via browser JavaScript where possible, or show "system audio controlled by device" state.
- Long term: native/JNA audio service with original-state restore, if needed.

Logs:
- `[TSE_CONTROL] Audio toggled: muted=true|false`
- `[TSE_CONTROL] Audio unavailable: reason=...`

Tests:
- Clicking audio does not alter Windows master volume unexpectedly.
- If exam HTML has audio/video elements, mute state applies via JS.
- Submit flow remains unchanged.

Rollback:
- Revert to status-only icon.

## 9. Proposed Architecture For Controls

Minimal near-term structure:

- UI components remain thin:
  - `ExamHeaderBar`
  - `ExamFooterStatusBar`
- Controller remains in active child:
  - `TSEExamChildClient` wires callbacks and policies.
- Environment checks move to a service:
  - `TSEEnvironmentService` runs background polling and reports immutable status objects.
- JCEF communication stays namespaced:
  - Existing `SUBMIT_PAYLOAD:` remains only for final/autosave.
  - Future control callbacks should use `TSE_CONTROL:` if needed.

Rules:

- No long-running work on EDT.
- No modal Swing UI in active exam.
- No direct exit from UI button.
- No OS control surface launch.
- No duplicate controls in header/footer.

## 10. Immediate Next Step

Start with Group 1 only. It is the lowest-risk group because it does not require native/system polling and should not touch Rust, parent submit, database, or server config.

Before coding Group 1, create a small patch plan:

- Change `ExamFooterStatusBar` constructor to accept explicit callbacks/policy for language and exit.
- Add optional About callback to `ExamHeaderBar` only if the current header design supports it without redesign.
- Wire callbacks in `TSEExamChildClient`.
- Implement blocked-exit/About as in-app Swing panel or JCEF DOM overlay, not modal dialog.
- Run the acceptance smoke path: start exam child, click controls, final submit, confirm parent sees final `submit_payload.enc`.

