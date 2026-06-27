# TSE Brightness VM Acceptance Result

## 1. Parent brightness test result

Status: **Not executed in this session**.

Reason: current machine is detected as a physical Lenovo laptop, not a dedicated VM test target.

```text
Manufacturer: LENOVO
Model: 83DV
Name: LAPTOP-891ALRRU
HypervisorPresent: True
BIOS Manufacturer: LENOVO
```

`HypervisorPresent=True` only means Windows has a hypervisor layer enabled; it does not prove this session is inside a disposable VM. Project rule requires Secure Exam/lockdown GUI testing to be performed in VM, so Parent/Login GUI brightness drag was not run here.

Prepared artifact result:

- Portable folder rebuilt at `D:\Ban_sao_du_an\dist\TutorHubSecureExam`.
- Parent JS in portable JAR contains:

```text
[TSE_QS_PARENT_JS] LOADED version=FULL_BRIGHTNESS_RESEARCH_FIX
```

Expected VM test path:

```text
[TSE_QS_PARENT_JS] LOADED version=FULL_BRIGHTNESS_RESEARCH_FIX
[TSE_QS_PARENT_JS] brightness slider FOUND id=slider-brightness ...
[TSE_QS_PARENT_JS] brightness pointerdown ...
[TSE_QS_PARENT_JS] brightness input ... or drag ...
[TSE_QS_PARENT_JS] send setBrightnessCommand ...
[TSE_PARENT_QS_HTML] setBrightnessCommand raw=...
[TSE_QS_CONTROLLER] setBrightness ...
[TSE_BRIGHTNESS_SERVICE] setBrightness ...
[TSE_BRIGHTNESS_SERVICE] verify after set percent=...
```

## 2. Exam brightness test result

Status: **Not executed in this session**.

Reason: Exam/JCEF brightness test requires entering Secure Exam flow and lockdown regression. Per project rule, this must be run in VM, not on the main physical machine.

Prepared artifact result:

- Exam JS in portable JAR contains:

```text
[TSE_QS_EXAM_JS] LOADED version=FULL_BRIGHTNESS_RESEARCH_FIX
```

Expected VM test path:

```text
[TSE_QS_EXAM_JS] LOADED version=FULL_BRIGHTNESS_RESEARCH_FIX
[TSE_QS_EXAM_JS] brightness slider FOUND ...
[TSE_QS_EXAM_JS] brightness pointerdown/input/drag ...
[TSE_QS_EXAM_JS] send command=TSE_BRIGHTNESS_SET:...
[TSE_JCEF_BRIDGE] request=TSE_BRIGHTNESS_SET:...
[TSE_QS_EXAM] setBrightness percent=...
[TSE_QS_CONTROLLER] setBrightness ...
[TSE_BRIGHTNESS_SERVICE] setBrightness ...
[TSE_BRIGHTNESS_SERVICE] verify after set percent=...
```

## 3. Log command path

Static/code-path and packaging verification passed:

1. Parent custom slider selector:

```text
div#slider-brightness.custom-slider.brightness-slider[data-role=brightness-slider][name=brightness]
```

2. Parent event path:

```text
parent-quick-settings.js
  -> document capture delegation
  -> custom div slider hit-test / drag handling
  -> sendBrightnessToJava(percent)
  -> window.javaApp.setBrightnessCommand(payload)
  -> TSEParentHtmlQuickSettingsPopup.JavaAppBridge.setBrightnessCommand(...)
  -> QuickSettingsController.setBrightness(...)
  -> BrightnessService.setBrightness(...)
```

3. Exam event path:

```text
tse-tray-flyout.js
  -> document capture delegation
  -> sendExamBrightnessToJava(percent)
  -> cefQuery("TSE_BRIGHTNESS_SET:<percent>")
  -> TSEJcefLifecycleManager request route
  -> TSEQuickSettingsManager.setBrightness(...)
  -> QuickSettingsController.setBrightness(...)
  -> BrightnessService.setBrightness(...)
```

## 4. Volume / WiFi / Battery / Clock regression

Status: **Pending VM GUI test**.

Not executed here because it requires interacting with Parent/Login popup and Exam/JCEF popup.

Required VM checks:

- Volume slider changes real system volume.
- Mute/unmute toggles correctly.
- WiFi remains read-only and shows current state.
- Battery shows current state.
- Clock updates correctly.

## 5. Safe Refresh result

Status: **Pending VM GUI test**.

Required VM check:

- Safe Refresh still works after opening Quick Settings and changing brightness.

## 6. Final Submit result

Status: **Pending VM GUI test**.

Required VM expected logs:

```text
[TSE_PARENT] Found submit_payload.enc. Using FINAL submit payload.
Submit SUCCESS
```

Must not show:

```text
WARNING: submit_payload.enc not found. Falling back to autosave_payload.enc.
Child process timeout! Forcing kill...
```

## 7. Rust exit code

Status: **Pending VM GUI test**.

Rust was not edited in this phase.

Required VM expected result:

```text
Rust exit code = 0
```

## 8. Process cleanup result

Pre-test cleanup finding:

- Found 3 stale `java.exe` processes from previous `run_input_test.bat --exam-id 3` sessions.
- They had no visible main window title and were running from `D:\Ban_sao_du_an\dist\TutorHubSecureExam`.
- Cleaned only those TutorHub Secure Exam Java processes before rebuilding portable.

Post-build check:

- No `TutorHub_LockdownCore` process was running.
- No persistent `java.exe` / `javaw.exe` matching `TSE|TutorHub|TutorHubSecureExam` remained after cleanup/build.
- PowerShell entries observed during checks were the active diagnostic commands themselves, not hanging WMI brightness processes.

Required VM post-submit command:

```powershell
Get-Process TutorHub_LockdownCore -ErrorAction SilentlyContinue

Get-CimInstance Win32_Process -Filter "name='java.exe' OR name='javaw.exe' OR name='powershell.exe'" |
Where-Object { $_.CommandLine -match "TSE|TutorHub|WmiMonitorBrightness|TutorHubSecureExam" }
```

Expected:

```text
No hanging Java process.
No hanging Rust LockdownCore process.
No hanging PowerShell WMI brightness process.
```

## 9. Build and artifact verification

Commands run:

```powershell
node --check D:\Ban_sao_du_an\src\main\resources\tse\quick-settings\parent-quick-settings.js
node --check D:\Ban_sao_du_an\src\main\resources\tse\tse-tray-flyout.js

cd D:\Ban_sao_du_an
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean install
powershell -ExecutionPolicy Bypass -File .\build_portable.ps1
```

Results:

```text
JS syntax checks: PASS
mvn clean install: BUILD SUCCESS, 22 tests, 0 failures
build_portable.ps1: PASS
Portable JAR contains FULL_BRIGHTNESS_RESEARCH_FIX markers: PASS
```

## 10. Ket luan co duoc sang Phase 10 khong

**No. Do not move to Phase 10 yet.**

Phase 9B.10 is prepared and the portable artifact is ready for VM GUI acceptance, but the actual brightness hardware/UI acceptance checks are still pending because this session is on a physical Lenovo machine, not the required VM test environment.

## 11. Follow-up from VM log: Parent DOM mouse event did not fire

User VM log showed:

```text
[TSE_PARENT_QS_JS] [TSE_QS_PARENT_JS] slider debug id=slider-brightness class=custom-slider brightness-slider disabled=false pointerEvents=auto rect=87,143,176,4 topElement=DIV#slider-brightness.custom-slider brightness-slider(slider)
[TSE_PARENT_QS_HTML] Click inside popup, ignore outside close.
```

But it did not show:

```text
[TSE_QS_PARENT_JS] GLOBAL mousedown
[TSE_QS_PARENT_JS] brightness pointerdown
[TSE_QS_PARENT_JS] brightness input
[TSE_QS_PARENT_JS] send setBrightnessCommand
```

New root cause refinement:

- The slider selector, disabled state, pointer-events and overlay check are correct.
- Java AWT receives the mouse event inside the popup.
- JavaFX WebView DOM mouse/pointer handlers are not receiving the slider interaction in this Parent/Login popup path.
- Therefore the command path is cut before `window.javaApp.setBrightnessCommand(...)`.

Fix applied after this log:

- Added Parent/Login AWT fallback in `TSEParentHtmlQuickSettingsPopup.java`.
- The fallback handles `MOUSE_PRESSED`, `MOUSE_DRAGGED`, and `MOUSE_RELEASED` inside the known brightness slider hit bounds.
- It mirrors the UI through `setSliderUIValue('slider-brightness', percent)`.
- It commits through the same Java bridge path by calling `setBrightnessCommand(...)`, preserving backend behavior through `QuickSettingsController` and `BrightnessService`.
- Added visible diagnostic logs:

```text
[TSE_PARENT_QS_HTML] AWT brightness fallback pointerdown percent=...
[TSE_PARENT_QS_HTML] AWT brightness fallback drag percent=...
[TSE_PARENT_QS_HTML] AWT brightness fallback commit percent=...
[TSE_PARENT_QS_HTML] setBrightnessCommand raw=...
[TSE_QS_CONTROLLER] setBrightness ...
[TSE_BRIGHTNESS_SERVICE] setBrightness ...
[TSE_BRIGHTNESS_SERVICE] verify after set percent=...
```

Build result after fallback:

```text
node --check parent-quick-settings.js: PASS
node --check tse-tray-flyout.js: PASS
mvn clean install: BUILD SUCCESS, 22 tests, 0 failures
build_portable.ps1: PASS
Portable JAR marker check: PASS
```

Next VM test should rerun:

```powershell
cd D:\Ban_sao_du_an\dist\TutorHubSecureExam
.\run_input_test.bat --exam-id 3
```

Expected Parent/Login result:

- Terminal now shows `AWT brightness fallback pointerdown/drag/commit`.
- Terminal shows `setBrightnessCommand raw=...`.
- Terminal reaches `BrightnessService.setBrightness`.
- Screen brightness changes after release.

## 12. Phase 9B.11 Parent AWT Fallback Acceptance + Exam Brightness Regression

Status: **Blocked in this Codex session; pending VM execution**.

Reason:

```text
Manufacturer: LENOVO
Model: 83DV
Name: LAPTOP-891ALRRU
BIOS Manufacturer: LENOVO
SMBIOSBIOSVersion: NECN50WW
HypervisorPresent: True
```

`HypervisorPresent=True` is not sufficient proof that the session is running inside a disposable VM. The active project rule forbids running real Secure Exam/lockdown tests on the main physical machine, so `run_input_test.bat --exam-id 3` was not executed here.

Prepared state:

- Portable build exists at `D:\Ban_sao_du_an\dist\TutorHubSecureExam`.
- Last portable timestamp: 2026-06-16 17:07.
- No `TutorHub_LockdownCore` process was found before this phase.
- No `java.exe`, `javaw.exe`, or `powershell.exe` process matching `TSE|TutorHub|WmiMonitorBrightness|TutorHubSecureExam` was found before this phase.
- No code was changed during Phase 9B.11 because no new VM runtime error log was available.

VM command still required:

```powershell
cd D:\Ban_sao_du_an\dist\TutorHubSecureExam
.\run_input_test.bat --exam-id 3
```

Acceptance items still pending:

1. Parent AWT fallback log:

```text
[TSE_PARENT_QS_HTML] AWT brightness fallback pointerdown percent=...
[TSE_PARENT_QS_HTML] AWT brightness fallback drag percent=...
[TSE_PARENT_QS_HTML] AWT brightness fallback commit percent=...
[TSE_PARENT_QS_HTML] setBrightnessCommand raw=...
[TSE_QS_CONTROLLER] setBrightness
[TSE_BRIGHTNESS_SERVICE] setBrightness
[TSE_BRIGHTNESS_SERVICE] verify after set percent=...
```

2. Parent brightness changes real screen brightness.
3. Parent popup does not close while dragging.
4. Exam/JCEF brightness reaches `TSE_BRIGHTNESS_SET` and `BrightnessService`.
5. Volume/mute, WiFi, Battery, Clock, VIE/ENG, Safe Refresh, Power blocked.
6. Final Submit succeeds.
7. Rust exit code is 0.
8. Post-submit process cleanup is clean.

Conclusion:

```text
Do not move to Phase 10.
Phase 9B.11 remains pending until VM GUI acceptance is actually executed.
```

## 13. Parent Quick Settings speaker icon color follow-up

Status: **Source and portable build updated; pending VM visual confirmation**.

User VM result after the AWT fallback:

- Parent/Login brightness can now change real screen brightness.
- A visual issue remains: the speaker/mute icon in Parent Quick Settings renders black on the dark flyout.

Fix applied:

- Updated `src/main/resources/tse/quick-settings/parent-quick-settings.css`.
- Forced `.icon-btn`, `#btn-mute`, `#icon-vol-on`, and `#icon-vol-off` to use light SVG stroke colors.
- Kept Rust, Final Submit, and brightness command path unchanged.

Verification:

```text
mvn test: PASS, 22 tests, 0 failures
build_portable.ps1: PASS
Portable JAR CSS check: PASS
```

Next VM check:

```text
Open Parent/Login Quick Settings and confirm the speaker icon is light/white, not black.
Brightness should remain functional.
```
