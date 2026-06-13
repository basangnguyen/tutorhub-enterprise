# TSE Windows 11 Tray Behavior Plan

## 1. Windows 11 Sources Studied
- **Microsoft Fluent Design System & Fluent UI library:** Focuses on standard corner radii (8px for flyouts, 4px for buttons/tiles), typography (Segoe UI Variable), and spacing.
- **Windows 11 Quick Settings UI:** Observations of the unified popup that groups Wi-Fi, Bluetooth, Airplane mode, volume, brightness, and battery.
- **Windows 11 Language/Input Switcher UI:** The flyout invoked by the system tray language abbreviation (e.g., ENG/VIE) or `Win + Space`.
- **Windows 11 Date/Time Calendar Flyout UI:** The calendar pane invoked by clicking the clock.
- **WinUI 3 Guidelines:** Material effects (Mica/Acrylic) and layering strategies for transient surfaces.

## 2. Windows 11 Quick Settings Behavior
- **Unified Trigger:** Clicking *any* of the Network (Wi-Fi), Sound (Volume), or Power (Battery) icons on the taskbar opens a **single, unified Quick Settings popup**.
- **Layout:** 
  - Top section: A grid of rounded-rectangle toggles (tiles) for Wi-Fi, Bluetooth, etc.
  - Middle section: Sliders for brightness and volume with corresponding icons.
  - Bottom section (status bar): Battery percentage/status and a settings gear icon.
- **Drill-down:** Clicking the "arrow" next to Wi-Fi swaps the current view to a Wi-Fi list view within the *same* popup size.

## 3. Windows 11 Language/Input Switcher Behavior
- **Standalone Trigger:** Clicking the language abbreviation (e.g., "ENG" or "VIE") on the taskbar opens a dedicated Language Switcher popup.
- **Layout:** A vertical list of available keyboards/input methods.
- **Visuals:** The currently active mode has a distinct accent-colored highlight or vertical bar indicator on its left edge.
- **Content:** It does not contain volume, network, or battery settings.

## 4. Windows 11 Date/Time Calendar Behavior
- **Standalone Trigger:** Clicking the Date/Time area on the far right of the taskbar opens a dedicated Calendar flyout.
- **Layout:** Displays the current time and date at the top, followed by a month-view calendar grid. 
- **Content:** Completely separate from Quick Settings.

## 5. Visual Style Notes
- **Background:** Dark theme simulation of Acrylic material (e.g., semi-transparent very dark gray `#1c1c1c` with a subtle backdrop blur `backdrop-filter: blur(20px)`).
- **Border Radius:** 
  - Flyout container: `12px` to `16px`.
  - Inner tiles/buttons: `4px` to `8px`.
- **Shadow:** Soft, large shadow to indicate high elevation (e.g., `box-shadow: 0 8px 32px rgba(0,0,0,0.4)`).
- **Spacing/Padding:** Generous padding around the container (`16px`) and `8px` gaps between tiles.
- **Typography:** `Segoe UI Variable`, `Segoe UI`, or system fonts. Size `13px` to `15px`.
- **Colors:** 
  - Accent color: System blue (`#0078D4` or `#60CDFF` in dark mode).
  - Text: White or off-white (`#ffffff`, `#e0e0e0`).
  - Hover state: Slight white overlay (e.g., `rgba(255, 255, 255, 0.06)`).

## 6. TSE Mapping
- **Current TSE State:** TSE currently opens *separate* DOM flyouts for Wi-Fi and Battery. 
- **Target TSE State:**
  - **Quick Settings:** Group Wi-Fi and Battery into a *single* Quick Settings popup. Since Volume/Brightness aren't managed right now, we will just show Wi-Fi (list/status) and Battery.
  - **Input Switcher:** The `VIE/ENG` toggle currently just swaps inline. We should change it to open an Input Switcher popup (like Windows 11) containing `English (ENG)` and `Vietnamese Telex (VIE)`.
  - **Date/Time:** If a clock is added to the footer, it should open a standalone calendar popup.

## 7. Component Architecture
- **DOM Container:** `#tse-tray-container` with high z-index.
- **JS Manager:** `TSETrayFlyout` (in `tse-tray-flyout.js`) refactored to handle generic views:
  - `showQuickSettings(payload)`
  - `showInputSwitcher(payload)`
- **Java Overlays:** Refactor `TSEWiFiOverlayPanel` and `TSEBatteryOverlayPanel` into a unified `TSEQuickSettingsManager` that gathers both network and battery data and calls `showQuickSettings`.

## 8. Event Flow
1. User clicks Wi-Fi or Battery icon in the Java `ExamFooterStatusBar`.
2. Java fetches Wi-Fi list and Battery status simultaneously.
3. Java executes JS: `window.TSETrayFlyout.showQuickSettings(jsonData)`.
4. JS renders the unified Quick Settings popup anchored above the icons.
5. Clicking outside the DOM element closes the popup.

## 9. CSS / DOM Rendering Strategy
- Use CSS Variables for theme colors to easily mimic Windows 11 Dark Mode.
- Use `backdrop-filter: blur(20px)` and background `rgba(32, 32, 32, 0.85)` for Acrylic effect.
- Use Flexbox for layout grids (e.g., tiles, lists).

## 10. Security Restrictions
- **No OS Settings:** Flyout buttons (like "Network Settings" or "Manage Keyboards") must be hidden or disabled to prevent opening the real Windows OS menus.
- **No Connect/Disconnect:** Wi-Fi list remains read-only to prevent tampering.
- **DOM Only:** All rendering must stay within the JCEF DOM to ensure it is not blocked or captured by the Secure Desktop environment.

## 11. Acceptance Criteria
- Clicking Wi-Fi or Battery opens the **same** unified Quick Settings popup.
- Quick Settings popup accurately mimics Windows 11 radius, spacing, shadow, and Acrylic blur.
- Language/Input toggle opens a standalone Windows 11-style language flyout.
- Flyouts are rendered entirely in JCEF DOM.
- No native Windows settings/apps are accessible.
