# TSE Volume Control Plan

## 1. Scope
Implement a real Windows system volume controller for the TutorHub Secure Exam (TSE) client. The volume slider and mute controls in the Windows 11 style Quick Settings flyout must be able to read and modify the system master volume. This must be done invisibly, without invoking external GUI tools, blocking the JVM exit, or interfering with existing safe modes like Brightness or Final Submit.

## 2. SEB Audio / Volume Research Notes
From `D:\Ban_sao_du_an\seb-reference\SafeExamBrowser.SystemComponents\Audio\Audio.cs`, we see that:
- SEB utilizes the Windows Core Audio API (via the `NAudio.CoreAudioApi` wrapper).
- It accesses the `MMDeviceEnumerator` to acquire the default audio endpoint (`DataFlow.Render, Role.Console`).
- It monitors changes via `AudioEndpointVolume.OnVolumeNotification`.
- It performs Get/Set on `MasterVolumeLevelScalar` (a float from 0.0 to 1.0) and `Mute` (boolean).
- SEB manages saving the initial volume, enforcing muted states, and restoring audio on termination.
- **Lesson for TSE**: The Core Audio API is indeed the correct and robust path. We should fetch the master volume scalar and mute state using exactly the same COM interfaces, but translated for our Java environment. We don't need the extensive device monitoring or background enforcing that SEB does (unless required later), just the ability to Get and Set from the Quick Settings.

## 3. Windows Volume API Research
Because Java doesn't have a built-in cross-platform volume API that reliably maps to the Windows Master Volume, we will use **JNA (Java Native Access)** which is already included in the `pom.xml` (`jna-platform`). 

Instead of spawning `powershell` continuously (which is slow and discouraged for volume sliders), we will define the required Windows COM interfaces manually extending `com.sun.jna.platform.win32.COM.Unknown`:
1. `IMMDeviceEnumerator` (CLSID: `BCDE0395-E52F-467C-8E3D-C4579291692E`)
2. `IMMDevice`
3. `IAudioEndpointVolume` (IID: `5CDF2C82-841E-4546-9722-0CF74078229A`)

Using `Ole32.INSTANCE.CoCreateInstance` and direct vTable index calls (e.g., `GetMasterVolumeLevelScalar` at index 9, `SetMasterVolumeLevelScalar` at index 7), we can read/write volume in ~1-2 milliseconds natively in Java, matching SEB's approach but without requiring C# dependencies.

## 4. Current Quick Settings Integration
- The Quick Settings flyout already exists via DOM and JCEF JS bridge.
- The Brightness control proved that our bridge logic works perfectly with debouncing.
- We will add the UI updates to `tse-tray-flyout.js` using `TSE_VOLUME_GET`, `TSE_VOLUME_SET:X`, and `TSE_VOLUME_MUTE:X` custom messages.

## 5. Proposed Architecture
**Model:**
```java
public final class TSEVolumeStatus {
    public final boolean supported;
    public final boolean writable;
    public final int percent;
    public final boolean muted;
    public final String method;
    public final String message;
    // Constructor...
}
```

**Controller:** `TSEVolumeController.java`
- Initializes JNA COM context (`Ole32.INSTANCE.CoInitializeEx`).
- Implements `public TSEVolumeStatus getStatus()`
- Implements `public TSEVolumeStatus setVolume(int percent)`
- Implements `public TSEVolumeStatus setMuted(boolean muted)`
- All logic wrapped in a fast `try-finally` block ensuring COM references are released immediately to prevent memory leaks or system hangs.

**JS Bridge:** `TSEQuickSettingsManager.java` & `TSEJcefLifecycleManager.java`
- Add routing for `TSE_VOLUME_GET`, `TSE_VOLUME_SET`, `TSE_VOLUME_MUTE`.
- The JS side will use `oninput` for smooth visual sliding and `onchange` / `setTimeout` debouncing to send requests to Java.

## 6. Security Restrictions
- `SUBMIT_PAYLOAD` will strictly NOT be used for volume.
- No external `.exe` or `powershell` window will spawn for this.
- JNA calls operate inside the Java process, strictly sandboxed to COM interaction.
- Errors in COM instantiation gracefully fallback to `supported=false` without crashing the client.

## 7. Cleanup / Shutdown Safety
- Since we use direct JNA COM calls, there are no dangling external processes. 
- The `CoUninitialize` method will be called if a long-lived COM thread is used. However, we will use a thread pool or simple daemon threads.
- `TSEVolumeController.shutdownNowNoBlock()` will clear any pending tasks.
- Final exit `Runtime.halt(0)` remains unaffected, ensuring Rust can exit with code 0 smoothly.

## 8. Test Plan
- Run `mvn clean install` to verify JNA compilation.
- Manually adjust the volume via Quick Settings slider.
- Verify system tray volume changes in real-time.
- Verify muting toggles the system mute state.
- Close the client naturally and verify `Rust exit code 0`.

## 9. Acceptance Criteria
- Quick Settings slider effectively changes Windows 11 system volume via Core Audio API.
- Quick Settings mute button toggles mute state.
- No visible command prompts.
- No interference with Safe Refresh, Brightness, or Final Submit.
- JCEF continues to run stably without blocks.
- `Rust exit code 0` observed after final exit.
