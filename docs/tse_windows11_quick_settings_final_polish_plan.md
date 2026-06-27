# TSE Windows 11 Quick Settings JCEF Architectural Migration Plan

## 1. Goal & Architecture Shift

Currently, the `TSELoginPanel` and `TSEConfigListPanel` (Parent screens) use a Swing-based popup (`TSEParentQuickSettingsPopup`). Even with styling improvements, a Swing popup cannot perfectly match the JCEF DOM popup used in the Exam screen (`tse-tray-flyout.js`) because rendering engines differ (Graphics2D vs Chromium/CSS).

**New Architecture (Option A)**:
All 3 screens (Login, Configuration, Exam) will use the same JCEF DOM code for the Quick Settings popup.
- **Exam**: Continues to inject the Quick Settings flyout inside the main JCEF browser.
- **Login / Config**: Will launch a small borderless `JWindow` containing a *secondary* `TSEBrowserPanel` that loads a dedicated HTML file `tse-quick-settings-popup.html`. This HTML file will reuse the existing JS/CSS from the Exam screen to ensure 100% UI consistency.

## 2. Why Swing popup cannot match JCEF DOM perfectly

- **Drop Shadows**: Swing requires manual Graphics2D radial gradients or soft clipping which often looks chunky or introduces performance artifacts. CSS `box-shadow: 0 8px 32px rgba(0,0,0,0.4)` handles alpha-blended smooth blur perfectly.
- **Translucency (Acrylic/Blur)**: Swing cannot achieve native Windows 11 `backdrop-filter: blur(10px)` easily without complex JNI hooking to DWM. CSS handles `backdrop-filter` natively within the Chromium surface.
- **Transitions/Micro-animations**: CSS `transition: background 0.1s` provides smooth hover state transitions out of the box, whereas Swing requires a custom `javax.swing.Timer` for every animated property.
- **Sliders**: `JSlider` styling via FlatLaf helps, but achieving a pixel-perfect 4px track with a scaling thumb and exact hover interaction state is still rigid compared to `<input type="range">` styled with CSS `::-webkit-slider-thumb`.

## 3. Implementation Details

### 3.1 Shared Quick Settings DOM Resource Strategy

To avoid code duplication, we will create:
- `src/main/resources/tse/tse-quick-settings-popup.html`: A minimal HTML wrapper container.
- We will reuse `tse-tray-flyout.js` by injecting or loading it inside `tse-quick-settings-popup.html`. 

`tse-quick-settings-popup.html` structure:
```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { margin: 0; overflow: hidden; background: transparent; }
        /* Any base font/reset styles here */
    </style>
</head>
<body>
    <script src="tse-tray-flyout.js"></script>
    <script>
        // Init the flyout locally in this window
        window.onload = function() {
            window.TSETrayFlyout.init();
        };
    </script>
</body>
</html>
```

### 3.2 Parent JCEF Popup Architecture (`TSEParentJcefQuickSettingsPopup.java`)

We will create `TSEParentJcefQuickSettingsPopup.java` containing:
- A `JWindow` (undecorated, transparent background).
- A `TSEBrowserPanel` added to the center.
- `AWTEventListener` or `WindowFocusListener` to close/hide the `JWindow` when the user clicks outside.
- The browser will load `tse-quick-settings-popup.html` and bridge callbacks via `TSEJcefLifecycleManager`.

### 3.3 Java ↔ JCEF Message Bridge

The Parent JCEF popup needs to route `cefQuery` requests just like the Exam browser does.
- The existing `TSEJcefLifecycleManager` routes `TSE_VOLUME_MUTE`, `TSE_VOLUME_SET`, `TSE_BRIGHTNESS_SET`, etc. Since `TSEBrowserPanel` automatically hooks into the lifecycle manager, these commands will automatically be routed to the Java controllers (`TSEVolumeController`, `TSEBrightnessController`) without additional work.
- We will need a way to trigger `showQuickSettings` initially. `TSEQuickSettingsManager.showQuickSettingsDOM()` will be updated to accept the new popup's `TSEBrowserPanel` and send the payload.

### 3.4 Battery Icon Redesign

We will update `BatteryStatusIcon.java`:
- Redraw the battery icon using `Graphics2D`.
- Use a Fluent Design-like style: 18x18 size inside a 36x28 hitbox. Thin outline, slight rounded corners, small battery head on the right.
- Fill the body based on the percentage, leaving the head empty.

## 4. Required Modifications

- `TSELoginPanel.java`: Change `showQuickSettingsSwingPopup()` to call `TSEParentJcefQuickSettingsPopup.showPopup(anchor)`.
- `TSEConfigListPanel.java`: Same change.
- `TSEParentQuickSettingsPopup.java`: Delete or mark `@Deprecated`.
- `TSEQuickSettingsManager.java`: Expose a method to initialize/refresh the JCEF DOM popup for the Parent context.
- `BatteryStatusIcon.java`: Re-implement `paintIcon` with new 2D logic.

## 5. Regression Risks

| Risk | Mitigation |
|------|------------|
| Multiple Chromium instances consume excessive memory | The Parent popup will instantiate one `TSEBrowserPanel` lazily and reuse it or dispose it gracefully. |
| JCEF routing conflicts | `TSEJcefLifecycleManager` routes globally; as long as the commands are stateless (e.g. Volume/Brightness), they won't conflict. |
| Parent window focus stealing | `JWindow` must be set `setFocusableWindowState(false)` if it shouldn't steal focus, but it needs focus for click-away detection. Will use `windowDeactivated` event to hide the popup. |

## 6. Test Checklist

- [ ] Login screen: Quick Settings opens JCEF popup, looks exactly like Exam.
- [ ] Config screen: Quick Settings opens JCEF popup, looks exactly like Exam.
- [ ] Exam screen: Quick Settings flyout inside main browser remains working.
- [ ] Click outside closes the Parent popup.
- [ ] Mute, Volume, Brightness commands from Parent popup successfully reach Java backend.
- [ ] Battery icon on footer looks Fluent-style and remains compact.
- [ ] `mvn clean install` passes.
- [ ] Portable build passes.
