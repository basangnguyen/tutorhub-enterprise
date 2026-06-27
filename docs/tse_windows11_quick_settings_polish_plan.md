# TSE Windows 11 Quick Settings Polish Plan

## 1. Scope
- Fix Volume Mute COM vtable indexing bug so system mute works.
- Implement Volume = 0 -> mute, and Volume > 0 -> unmute logic.
- Synchronize Quick Settings functionality across Login, Configuration, and Exam interfaces.
- Polish Quick Settings UI to match Windows 11 aesthetics (hover/selected states, real-time percentages).

## 2. Windows 11 Quick Settings UI Research
- Hover on system tray icons shows a slightly rounded, translucent white/gray background.
- Active/selected icons show a distinct background block.
- Brightness and volume sliders show percentages that update continuously during sliding (`oninput` events).

## 3. Current TSE Footer / Tray Architecture
- We have `ExamFooterStatusBar` rendering a Swing UI with SVG icons for Exam mode.
- `TSEQuickSettingsManager` handles the state and JS payload bridging.
- We need to extract the Swing footer into a reusable component so `TSEProductionParentSubmitLabLauncher` can use it for the Login and Configuration cards.

## 4. Login / Configuration / Exam UI Differences
- In Exam mode, JCEF handles the UI via HTML/JS, but the footer is native Java Swing.
- In Login/Config, there's no JCEF loaded yet, so clicking the Quick Settings button must open a Java-based Quick Settings popup or we need a way to open the HTML overlay.
- Wait, does JCEF load on Login? Usually, the browser panel is instantiated later. 
- However, if the user requires the EXACT SAME popup, we will need to create a floating `JWindow` with a single JCEF instance that serves Quick Settings, OR we implement the Quick Settings UI purely in Swing so it works flawlessly everywhere without needing a full browser just for a popup. Given we already have the HTML, I will instantiate an undecorated floating Swing popup containing the JCEF.

## 5. Volume Mute Bug Root Cause
- The vtable index for `IAudioEndpointVolume` was incorrect in `TSEVolumeController.java`.
- `SetMute` was mapped to index 10 (which is `SetChannelVolumeLevel`).
- `GetMute` was mapped to index 11 (which is `SetChannelVolumeLevelScalar`).
- **Correction:** `SetMute` is index 14, `GetMute` is index 15.

## 6. Unified Footer Strategy
- Create a `TSEUnifiedFooter` class that extends `JPanel`.
- Use this class in `TSEExamShellFrame` (Exam), and also add it to `TSEProductionParentSubmitLabLauncher` (Login and Config).

## 7. Icon State Strategy
- Update JS to render SVG `volume-mute` icon when `payload.volumeMuted` is true or `payload.volumePercent == 0`.
- For `percent > 0`: standard volume icon.
- Update Swing Footer to change its Volume icon based on status polling.

## 8. Slider Percent Display Strategy
- Add JS `oninput` handlers to volume and brightness sliders to update a text node (e.g. `45%`) in real-time.
- On `onchange`, call `window.cefQuery` to apply the value.

## 9. Hover / Selected Interaction Strategy
- Native Swing footer buttons will get `MouseAdapter` with `mouseEntered` / `mouseExited` to paint a rounded translucent background.
- Click highlights the button.

## 10. Test Plan
- Build and test locally.
- Manually test Volume Mute.
- Check footer on Login screen.

## 11. Acceptance Criteria
- Meets all 16 requirements specified by user.
