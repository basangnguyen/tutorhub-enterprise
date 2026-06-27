# TutorHub Secure Exam – Taskbar & Quick Settings Architecture Research v2

**Author**: Antigravity AI Architect  
**Date**: 2026-06-14  
**Status**: Research & Design (No Code Changes)  
**Scope**: Taskbar, Footer, Tray, Quick Settings, System Components  

---

## 1. Executive Summary

TSE đã xây dựng thành công một hệ thống taskbar nội bộ hoạt động được trên cả Parent (Login/Config) và Exam (JCEF). Tuy nhiên kiến trúc hiện tại có nhiều vấn đề nghiêm trọng cần giải quyết trước khi release:

1. **State phân tán** – Không có single source of truth. Footer, Parent popup, Exam popup mỗi nơi giữ state riêng.
2. **Logic trùng lặp** – `TSEQuickSettingsManager` (Exam) và `TSEParentHtmlQuickSettingsPopup` (Parent) duplicate hoàn toàn logic gọi Volume/Brightness/WiFi/Battery.
3. **Timer/Worker chồng chéo** – Footer poll 2s, Parent popup refresh 5s, Exam popup mỗi lần mở lại query từ đầu. Không có lifecycle rõ ràng.
4. **Không có security policy matrix** – Mọi control đều "cho phép" mà không có cơ chế enable/disable theo policy.
5. **WiFi chỉ read-only nhưng code Exam popup hiển thị danh sách mạng** – Gây hiểu nhầm cho user.
6. **Brightness dùng WMI/PowerShell** – Chậm, spawn process mỗi lần, không phù hợp exam environment.
7. **Volume dùng CoreAudio/JNA trực tiếp** – Không CoInitialize/CoUninitialize chuẩn, leak potential.
8. **Không có Clock/DateTime** – Windows 11 luôn hiển thị giờ, TSE chưa có.
9. **Thiếu graceful shutdown** – Footer timer không dừng khi Final Submit bắt đầu, có thể gây race condition.

Báo cáo này phân tích chi tiết hiện trạng, đối chiếu SEB/Windows 11, và đề xuất roadmap 14 phase cụ thể.

---

## 2. TSE Current Taskbar Deep Audit

### 2.1 Thành phần hiện tại trên footer

| Thành phần | UI Location | Swing Component | Status |
|---|---|---|---|
| Language (VIE/ENG) | Footer left-right | `JButton` in `ExamFooterStatusBar` | ✅ Hoạt động |
| WiFi icon | Tray cluster | `JLabel` in `TSEQuickSettingsTrayCluster` | ✅ Icon-only |
| Volume icon | Tray cluster | `JLabel` in `TSEQuickSettingsTrayCluster` | ✅ Icon-only |
| Battery icon | Tray cluster | `BatteryStatusIcon` (Graphics2D) | ✅ Custom drawn |
| Power/Exit | Footer right | `JButton` in `ExamFooterStatusBar` | ✅ Block in exam |
| Clock/DateTime | — | **KHÔNG CÓ** | ❌ Missing |

### 2.2 Logic phân bổ

```
┌─────────────────────────────────────────────────────────────────┐
│ ExamFooterStatusBar (Swing)                                     │
│  ├── Timer 2000ms → updateStatus()                              │
│  │   ├── SwingWorker → TSENetworkStatusProvider.getStatus()     │
│  │   ├── SwingWorker → TSEBatteryStatusProvider.getStatus()     │
│  │   └── SwingWorker → TSEVolumeController.getStatus()          │
│  ├── TSEQuickSettingsTrayCluster (WiFi/Vol/Battery icon group)  │
│  ├── Language button (VIE/ENG)                                  │
│  └── Power button                                               │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ Parent: TSEParentHtmlQuickSettingsPopup (JavaFX WebView)        │
│  ├── JWindow + JFXPanel + WebView                               │
│  ├── parent-quick-settings.html/.css/.js                        │
│  ├── JavaAppBridge (setVolume/setBrightness/toggleMute)          │
│  ├── Timer 5000ms → refreshStateAsync() [SwingWorker]           │
│  ├── AtomicBoolean isRefreshing (de-duplicate)                  │
│  ├── Version tracking (lastVolumeVersion/lastBrightnessVersion) │
│  └── AWTEventListener for click-outside dismiss                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ Exam: TSEQuickSettingsManager → JCEF DOM injection              │
│  ├── tse-tray-flyout.js (injected into exam HTML)               │
│  ├── payload gửi qua executeJavaScript()                        │
│  ├── cefQuery callback cho set brightness/volume/mute           │
│  ├── WiFi network scan thread                                   │
│  ├── Brightness cache TTL 3s                                    │
│  ├── Volume cache TTL 2s                                        │
│  └── Static state fields (isScanning, cachedNetworks, etc.)     │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 Vấn đề nghiêm trọng phát hiện

#### P1: State phân tán (Critical)
- `ExamFooterStatusBar` poll riêng → update icon riêng
- `TSEParentHtmlQuickSettingsPopup` poll riêng → update WebView riêng
- `TSEQuickSettingsManager` query riêng → inject JS riêng
- Không có `QuickSettingsStateStore` chung → mỗi nơi có thể hiển thị state khác nhau

#### P2: Logic duplicate (High)
- Volume get/set: xuất hiện trong cả `TSEQuickSettingsManager`, `TSEParentHtmlQuickSettingsPopup.JavaAppBridge`, `ExamFooterStatusBar`
- Brightness get/set: `TSEQuickSettingsManager.setBrightness()` vs `TSEParentHtmlQuickSettingsPopup.JavaAppBridge.setBrightness()`
- Battery: `ExamFooterStatusBar` poll riêng, `TSEParentHtmlQuickSettingsPopup.refreshStateAsync()` query riêng, `TSEQuickSettingsManager.buildPayload()` query riêng

#### P3: Timer chồng nhau (High)
- Footer: Timer 2s → spawn 3 SwingWorker song song
- Parent popup: Timer 5s → 1 SwingWorker gom 4 query
- Exam popup: Mỗi lần mở → full query
- **Kết quả**: Nếu Parent popup mở, footer timer + popup timer cùng chạy = 2 query volume mỗi 2s

#### P4: CoInitialize leak trong TSEVolumeController (Medium-High)
- `getEndpointVolume()` gọi `Ole32.INSTANCE.CoInitializeEx()` mỗi lần
- `CoUninitialize()` chỉ gọi trong `getStatus()` nhưng **KHÔNG** gọi trong `setVolume()` và `setMuted()`
- Nếu thread pool reuse, COM apartment có thể bị corrupt

#### P5: PowerShell process spawn cho brightness (High)
- Mỗi lần get/set brightness = 1 `powershell.exe` process
- Trung bình 2 process/5s khi popup mở
- Slow, security risk (PowerShell có thể bị chặn bởi GPO)

#### P6: Footer không dừng khi Final Submit (Medium)
- `TSEExamChildClient.writeFinalPayloadAndExit()` gọi `footerRef[0].stopStatusPolling()` trong exit thread
- Nhưng nếu submit payload viết chậm, footer timer có thể tiếp tục spawn worker
- Race condition nhẹ, chưa gây lỗi thực tế nhưng rủi ro

#### P7: WiFi scan dùng `netsh` command line (Medium)
- Locale-dependent (regex parse tiếng Việt vs tiếng Anh)
- Slow (150-500ms mỗi lần)
- Không event-driven

#### P8: Không có Clock/DateTime (Low-Medium)
- Windows 11 luôn hiển thị giờ ở taskbar
- Thí sinh cần biết giờ trong phòng thi
- Hiện tại phải nhìn header (nếu có)

---

## 3. SEB Architecture Lessons

### 3.1 Kiến trúc tổng quát SEB

```
Contracts (Interfaces)
  ├── IAudio: HasOutputDevice, OutputMuted, OutputVolume, VolumeChanged event
  ├── INetworkAdapter: Status, Type, Changed event, ConnectToWirelessNetwork()
  ├── IPowerSupply: StatusChanged event, GetStatus()
  ├── IKeyboard: KeyboardChanged event
  └── ISystemComponent: Initialize(), Terminate()

System Components (Implementations)
  ├── Audio: NAudio CoreAudioApi, event-driven OnVolumeNotification
  ├── NetworkAdapter: Windows.Devices.WiFi API, event + 5s timer hybrid
  ├── PowerSupply: System.Windows.Forms.SystemInformation, 5s timer
  └── Keyboard: InputLanguage, event-driven

Client (Orchestrator)
  └── ShellOperation:
      ├── InitializeSystemComponents() - gọi .Initialize() cho tất cả
      ├── InitializeTaskbar() - thêm UI control theo settings policy
      ├── TerminateSystemComponents() - gọi .Terminate() cho tất cả
      └── Settings-based policy: ShowAudio, ShowNetwork, ShowClock, etc.

UI (WPF)
  ├── ActionCenter/AudioControl: subscribes audio.VolumeChanged
  ├── ActionCenter/NetworkControl: subscribes adapter.Changed
  ├── ActionCenter/PowerSupplyControl: subscribes powerSupply.StatusChanged
  └── Taskbar controls: same interfaces, different layout
```

### 3.2 Những pattern SEB làm rất tốt

| Pattern | SEB Implementation | TSE Nên Học |
|---|---|---|
| **Event-driven Audio** | `OnVolumeNotification` callback từ NAudio → `VolumeChanged` event | ✅ Nên migrate sang event |
| **Slider DragStarted/DragCompleted** | Unsubscribe `ValueChanged` khi drag, resubscribe khi drop | ✅ Đây là cách chống snap-back chuẩn |
| **Interface contract** | `IAudio`, `INetworkAdapter`, `IPowerSupply` | ✅ Tách interface rõ ràng |
| **Settings-based policy** | `ShowAudio`, `ShowNetwork`, `ShowClock` | ✅ Cần có policy matrix |
| **Lifecycle management** | `Initialize()` → `Terminate()` rõ ràng | ✅ Hiện TSE thiếu |
| **Save/Restore volume** | Lưu `originalVolume`, restore khi `Terminate()` | ✅ Nên có |
| **WiFi connect + credential prompt** | `CredentialsRequired` event → UI nhập password | ⚠️ Cân nhắc kỹ |
| **WiFi qua Windows.Devices.WiFi API** | Native UWP API, async, event-driven | ⚠️ Không có tương đương Java trực tiếp |

### 3.3 Những pattern KHÔNG nên copy

| Pattern | Lý do không phù hợp |
|---|---|
| WPF Popup (mouse-leave dismiss) | TSE dùng HTML/CSS, không có WPF |
| NAudio dependency | TSE dùng JNA/COM trực tiếp đã đủ |
| Windows.Devices.WiFi UWP API | Java không gọi được UWP trực tiếp, phải qua JNI/bridge |
| Single executable model | TSE có Parent/Child process model |

---

## 4. Windows 11 / Fluent Design Lessons

> **Disclaimer**: Phân tích dựa trên Microsoft public docs, Fluent Design System docs, và hành vi quan sát được. Không giả vờ đã đọc Windows source code.

### 4.1 Windows 11 Quick Settings

- **Trigger**: Click cluster WiFi/Volume/Battery → mở chung 1 popup
- **Layout**: Tiles trên (WiFi, Bluetooth, Airplane Mode, etc.) → Sliders giữa (Brightness, Volume) → Battery/Date dưới
- **Dismiss**: Click outside, Esc key, Win+A toggle
- **Animation**: Slide-up 200ms, fade
- **Background**: Acrylic/Mica blur effect
- **Slider**: Track height ~4px, thumb 16px circle, accent color #60CDFF
- **Typography**: Segoe UI Variable, 13-14px body, 12px caption

### 4.2 Windows 11 chống snap-back

Windows 11 dùng cơ chế:
1. **Immediate visual update** khi user kéo slider
2. **System API set** khi user thả slider (committed state)
3. **System notification callback** xác nhận giá trị mới → NO further action needed
4. User action LUÔN ưu tiên hơn system notification trong khi đang interact

### 4.3 Những phần TSE có thể mô phỏng hợp pháp

- Rounded corners (border-radius: 12-16px)
- Dark translucent background (rgba(32,32,32,0.85-0.95))
- Accent color #60CDFF cho slider thumb
- Segoe UI font
- Tile layout
- Slider 4px track + 16px thumb
- Subtle shadow (0 8px 32px rgba(0,0,0,0.4))
- Consistent 8px/12px/16px spacing
- SVG stroke icons (Fluent UI System Icons – MIT license)

### 4.4 Những phần KHÔNG được copy

- Mica/Acrylic native blur (proprietary API)
- Windows native network flyout
- Windows Settings app integration
- Microsoft branding/logo
- Windows Store icon assets (proprietary)

---

## 5. WiFi Capability Research

### 5.1 Đánh giá 6 options

| Tiêu chí | Option A: Read-only | Option B: Scan-only | Option C: Whitelist Connect | Option D: Profile Connect | Option E: Password UI | Option F: Windows UI |
|---|---|---|---|---|---|---|
| **Bảo mật** | ✅ Rất tốt | ✅ Tốt | ⚠️ Trung bình | ⚠️ Cần thiết kế kỹ | ❌ Rủi ro cao | ❌ Rất rủi ro |
| **UX phòng thi** | ⚠️ Hạn chế | ⚠️ Gây nhầm lẫn | ✅ Phù hợp | ✅ Phù hợp | ⚠️ Phức tạp | ❌ Thoát khỏi lockdown |
| **Phá lockdown?** | Không | Không | Không nếu policy đúng | Không | Có thể | Có |
| **Cần admin?** | Không | Không | Không | Không | Không | N/A |
| **Portable?** | ✅ | ✅ | ✅ | ⚠️ Cần profile sẵn | ✅ | ❌ |
| **Locale-dependent?** | ✅ (netsh parse) | ✅ (netsh parse) | ✅ (netsh/API) | ⚠️ | ✅ | Không |
| **Giống SEB?** | Không (SEB cho connect) | Phần nào | ✅ Gần nhất | ✅ | ✅ (SEB có credential prompt) | ❌ |
| **Nên làm ngay?** | ✅ Giữ | ⚠️ Phase sau | ⚠️ Phase sau | ⚠️ Phase sau | ❌ Chưa nên | ❌ Không bao giờ |

### 5.2 Kết luận WiFi

**Phase hiện tại (Phase 0-7)**: Giữ **Option A (Read-only)** + hiển thị SSID đang kết nối.

**Phase 8+**: Nâng lên **Option B (Scan available networks)** nhưng **không cho connect**.

**Tương lai xa (nếu cần)**: **Option C (Whitelist connect)** với điều kiện:
- SSID whitelist được giám thị/admin cấu hình trước trong file config exam
- Không nhập password trong exam UI
- Chỉ connect tới mạng đã có profile Windows sẵn

**KHÔNG BAO GIỜ**: Option F (mở Windows network UI thật).

---

## 6. Volume Control Research

### 6.1 Hiện trạng TSE

- **API**: JNA COM → `IAudioEndpointVolume` trực tiếp
- **Vấn đề CoInitialize**: Gọi `CoInitializeEx(COINIT_MULTITHREADED)` mỗi lần `getEndpointVolume()` → có thể clash nếu thread khác đã init COM apartment
- **Vấn đề CoUninitialize**: Chỉ gọi trong `getStatus()`, KHÔNG gọi trong `setVolume()`/`setMuted()` → leak
- **Cache**: TTL 2s, đúng hướng
- **Event-driven**: KHÔNG có. Hoàn toàn polling

### 6.2 SEB so sánh

- **API**: NAudio (wrapper CoreAudioApi), C# managed
- **Event-driven**: ✅ `OnVolumeNotification` callback tự động fire khi volume thay đổi (kể cả từ app khác hoặc hardware button)
- **Slider anti-snap-back**: `DragStarted` → unsubscribe `ValueChanged`, `DragCompleted` → `SetVolume()` + resubscribe
- **Save/Restore**: Lưu `originalVolume`/`originallyMuted` khi `Initialize()`, restore khi `Terminate()`

### 6.3 Đề xuất kiến trúc Volume mới

```
VolumeService (singleton)
  ├── initialize() → CoInitializeEx, get endpoint, save original state
  ├── terminate() → restore original, CoUninitialize, release COM
  ├── getSnapshot() → VolumeSnapshot { percent, muted, supported, writable }
  ├── setVolume(percent, requestId)
  ├── setMuted(muted, requestId)
  ├── addListener(VolumeChangeListener)
  │   └── VolumeChangeListener.onVolumeChanged(VolumeSnapshot, requestId)
  ├── Cache:
  │   ├── In-memory VolumeSnapshot (updated after every get/set)
  │   ├── TTL 2s for OS re-query (fallback if no event)
  │   └── Immediate update after set (no wait for OS callback)
  └── Event support:
      ├── Ideally: IAudioEndpointVolumeCallback via JNA (complex but possible)
      └── Fallback: Lightweight poll 3s ONLY when popup open
```

### 6.4 Quyết định kỹ thuật Volume

1. **Giữ JNA/COM CoreAudio** – đã hoạt động, không cần thêm dependency
2. **Fix CoInitialize/CoUninitialize** – gọi đúng per-thread, hoặc dùng singleton thread
3. **Thêm save/restore original state** – giống SEB
4. **Event-driven nếu khả thi** – `IAudioEndpointVolumeCallback` qua JNA (phức tạp nhưng đúng kiến trúc)
5. **Fallback: poll 3s chỉ khi popup mở** – tối ưu CPU

---

## 7. Brightness Control Research

### 7.1 Hiện trạng TSE

- **API**: PowerShell → WMI `WmiMonitorBrightness` / `WmiMonitorBrightnessMethods`
- **Vấn đề**: Spawn `powershell.exe` mỗi lần get/set → 200-800ms mỗi lần
- **Giới hạn WMI**: Chỉ hoạt động trên laptop internal display, KHÔNG hoạt động trên external monitor hoặc desktop
- **Timeout**: 2s/3s → có thể block worker thread
- **Security risk**: GPO có thể chặn PowerShell

### 7.2 Options brightness

| Option | Mô tả | Ưu | Nhược |
|---|---|---|---|
| **A: Read-only** | Chỉ đọc, không cho set | An toàn nhất | Thiếu chức năng |
| **B: WMI set (hiện tại)** | PowerShell → WMI | Hoạt động trên laptop | Chậm, spawn process |
| **C: Native monitor API** | `SetMonitorBrightness()` Win32 | Nhanh, không spawn process | Chỉ laptop, cần JNA binding phức tạp |
| **D: DDC/CI** | `PhysicalMonitorFromHMONITOR` + DDC | External monitor support | Rủi ro hardware, chậm (I2C bus) |
| **E: Adaptive** | Thử WMI, fallback read-only, disable nếu fail | Best of both worlds | Phức tạp logic |

### 7.3 Kết luận Brightness

**Khuyến nghị: Option E (Adaptive)**

Quy trình:
1. Khởi động: thử WMI get brightness
2. Nếu thành công → `supported=true, writable=true`
3. Nếu timeout/fail → `supported=false` → slider disabled, hiển thị "Thiết bị không hỗ trợ"
4. Cache kết quả "supported" lâu dài (không cần thử lại mỗi lần popup mở)
5. Khi set: dùng WMI nhưng qua **dedicated background thread** (không spawn PowerShell mỗi lần)

**Tối ưu**: Migrate từ PowerShell → JNA gọi `SetMonitorBrightness()` trực tiếp nếu có thời gian (Phase 9).

---

## 8. Battery / Date Time / Input Mode Research

### 8.1 Battery

- **API hiện tại**: JNA → `GetSystemPowerStatus()` ✅ Tốt, nhanh, no process spawn
- **Poll interval**: 2s trong footer → hợp lý nhưng có thể tăng lên 5s vì pin thay đổi chậm
- **Desktop (không có pin)**: Hiển thị icon AC, tooltip "Đang cắm nguồn" ✅ Đúng
- **SEB**: Dùng `SystemInformation.PowerStatus` (tương đương) + Timer 5s ✅ Giống nhau
- **Event-driven?**: Windows có `SystemEvents.PowerModeChanged` nhưng chỉ fire khi chuyển AC/battery, không fire khi % thay đổi → timer vẫn cần
- **Kết luận**: Giữ nguyên cách hiện tại, chỉ cần gom vào `BatteryService` và tăng interval lên 5s

### 8.2 Date/Time

- **Hiện tại**: TSE KHÔNG hiển thị giờ ở footer ❌
- **Windows 11**: Luôn hiển thị "10:45 AM" + ngày ở tray
- **SEB**: Có `Clock` trong cả ActionCenter và Taskbar, controlled by `ShowClock` setting
- **Khuyến nghị**:
  - Thêm `ClockService` với Timer 60s (không cần giây)
  - Footer hiển thị "HH:mm" bên phải, trước Power button
  - Format: `java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))`
  - **Policy**: `showClock` = true/false tùy cấu hình exam
  - **Trong exam**: Có thể ẩn theo policy nếu giám thị yêu cầu (ví dụ: thi có giới hạn thời gian, không cho thấy đồng hồ hệ thống)

### 8.3 Input Mode (VIE/ENG)

- **Hiện tại**: `TSEInputModeManager` singleton ✅ Tốt
- **Flyout**: JCEF DOM trong exam, chưa có trong Parent ⚠️
- **Đồng bộ Parent/Exam**: Input mode lưu trong context, truyền qua `--context` ✅
- **Kết luận**: Giữ nguyên, chỉ cần gom vào `QuickSettingsStateStore`

---

## 9. UI/UX Design Target

### 9.1 Design tokens chung

```css
/* Design tokens – dùng chung cho Parent CSS và Exam JS injection */
:root {
  --qs-bg: rgba(32, 32, 32, 0.95);
  --qs-border: rgba(255, 255, 255, 0.1);
  --qs-text: #F5F5F5;
  --qs-text-dim: #9CA3AF;
  --qs-accent: #60CDFF;
  --qs-tile-bg: rgba(255, 255, 255, 0.05);
  --qs-tile-border: rgba(255, 255, 255, 0.08);
  --qs-hover: rgba(255, 255, 255, 0.08);
  --qs-active: rgba(255, 255, 255, 0.04);
  --qs-radius-popup: 16px;
  --qs-radius-tile: 8px;
  --qs-radius-btn: 6px;
  --qs-shadow: 0 8px 32px rgba(0,0,0,0.4);
  --qs-font: 'Segoe UI Variable', 'Segoe UI', sans-serif;
  --qs-font-size-body: 13px;
  --qs-font-size-title: 14px;
  --qs-font-size-caption: 12px;
  --qs-spacing-xs: 4px;
  --qs-spacing-sm: 8px;
  --qs-spacing-md: 12px;
  --qs-spacing-lg: 16px;
}
```

### 9.2 Shared JSON schema

```json
{
  "version": 1,
  "requestId": "req_1718345678",
  "wifi": {
    "status": "connected",
    "ssid": "PhongThi_5G",
    "signal": 85
  },
  "volume": {
    "supported": true,
    "writable": true,
    "percent": 45,
    "muted": false,
    "version": 3
  },
  "brightness": {
    "supported": true,
    "writable": true,
    "percent": 70,
    "version": 2
  },
  "battery": {
    "hasBattery": true,
    "isCharging": false,
    "percent": 67
  },
  "clock": {
    "time": "10:45",
    "date": "14/06/2026"
  },
  "inputMode": "vi"
}
```

### 9.3 Icon system

- Sử dụng **Microsoft Fluent UI System Icons** (MIT license, GitHub: microsoft/fluentui-system-icons)
- Đã có 6 icon trong `src/main/resources/tse/icons/fluent/`
- Cần thêm: wifi, wifi-off, brightness, clock
- Parent (CSS): dùng inline SVG hoặc `<img>` tag
- Exam (JCEF): dùng inline SVG trong JS template
- Footer (Swing): dùng FlatSVGIcon (hiện tại đã dùng)

---

## 10. Security Policy Matrix

### 10.1 Control-level policy

| Control | Default Login/Config | Default Exam | Configurable? | Risk nếu bật |
|---|---|---|---|---|
| WiFi display | read-only | read-only | ✅ | Low |
| WiFi scan | disabled | disabled | ✅ | Low |
| WiFi connect | disabled | disabled | ✅ whitelist-only | Medium |
| Volume slider | writable | writable | ✅ | Low |
| Volume mute | writable | writable | ✅ | Low |
| Brightness slider | adaptive | adaptive | ✅ | Low |
| Battery display | read-only | read-only | ❌ always-on | None |
| Clock display | show | configurable | ✅ | None |
| Input mode | VIE/ENG | VIE/ENG | ✅ | None |
| Refresh | allowed | blocked during submit | ❌ | Medium nếu sai |
| About | allowed | allowed | ❌ | None |
| Exit | allowed | blocked | ❌ | **Critical** nếu sai |

### 10.2 Phân tích rủi ro bảo mật

| Risk | Severity | Hiện trạng | Mitigation |
|---|---|---|---|
| WiFi connect thoát lockdown | Critical | Không cho connect ✅ | Giữ nguyên |
| Volume slider gọi COM leak | Medium | Có risk ⚠️ | Fix CoInitialize |
| Brightness spawn PowerShell | Medium | GPO có thể chặn ⚠️ | Migrate native API |
| Footer timer chạy khi submit | Low-Medium | Race condition nhẹ ⚠️ | Stop timer trước submit |
| Popup mở Windows Settings | Critical | Không mở ✅ | Policy enforce |
| Network credential trong exam | High | Không có ✅ | Không implement |
| Exit bypass lockdown | Critical | Blocked ✅ | Rust enforce |

---

## 11. Proposed Target Architecture

### 11.1 Layer diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                     UI LAYER (per-context)                       │
├──────────────────────────┬──────────────────────────────────────┤
│  Parent Login/Config     │  Exam Child                          │
│  ┌─────────────────┐    │  ┌──────────────────────────────┐    │
│  │ Footer (Swing)   │    │  │ Footer (Swing)               │    │
│  │  └ TrayCluster   │    │  │  └ TrayCluster               │    │
│  │  └ Language btn   │    │  │  └ Language btn               │    │
│  │  └ Clock label    │    │  │  └ Clock label                │    │
│  │  └ Power btn      │    │  │  └ Power btn (blocked)        │    │
│  ├─────────────────┤    │  ├──────────────────────────────┤    │
│  │ JavaFX WebView   │    │  │ JCEF DOM Injection            │    │
│  │ Quick Settings    │    │  │ Quick Settings                │    │
│  │ (HTML/CSS/JS)     │    │  │ (HTML/CSS/JS)                 │    │
│  └──────┬──────────┘    │  └──────┬───────────────────────┘    │
│         │ ParentBridge   │         │ ExamJcefBridge              │
└─────────┼────────────────┴─────────┼────────────────────────────┘
          │                          │
┌─────────▼──────────────────────────▼────────────────────────────┐
│                  CONTROLLER LAYER (singleton)                    │
│  ┌───────────────────────────────────────────────────────┐      │
│  │ QuickSettingsController                                │      │
│  │  ├── getSnapshot() → QuickSettingsSnapshot             │      │
│  │  ├── setVolume(percent, requestId)                     │      │
│  │  ├── setBrightness(percent, requestId)                 │      │
│  │  ├── toggleMute(requestId)                             │      │
│  │  ├── refreshAll()                                      │      │
│  │  └── addListener(QuickSettingsChangeListener)          │      │
│  └───────────────────────────────────────────────────────┘      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                     STATE STORE                                  │
│  ┌───────────────────────────────────────────────────────┐      │
│  │ QuickSettingsStateStore (in-memory, thread-safe)       │      │
│  │  ├── VolumeSnapshot   { percent, muted, version }      │      │
│  │  ├── BrightnessSnapshot { percent, supported, version } │      │
│  │  ├── BatterySnapshot  { percent, charging, hasBattery } │      │
│  │  ├── NetworkSnapshot  { status, ssid, signal }          │      │
│  │  ├── ClockSnapshot    { time, date }                    │      │
│  │  ├── InputModeSnapshot { mode }                         │      │
│  │  └── SecurityPolicy   { per-control enable/disable }    │      │
│  └───────────────────────────────────────────────────────┘      │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                  SERVICE LAYER (singletons)                      │
│  ┌────────────┐ ┌──────────────┐ ┌─────────────┐ ┌──────────┐  │
│  │VolumeService│ │BrightnessServ│ │BatteryService│ │NetworkServ│  │
│  │ JNA/COM     │ │ WMI/JNA      │ │ JNA Kernel32 │ │ netsh     │  │
│  │ cache+event │ │ cache+thread │ │ poll 5s      │ │ poll+cache│  │
│  └────────────┘ └──────────────┘ └─────────────┘ └──────────┘  │
│  ┌────────────┐ ┌──────────────┐                                │
│  │ClockService │ │InputModeServ │                                │
│  │ timer 60s   │ │ (existing)   │                                │
│  └────────────┘ └──────────────┘                                │
└──────────────────────────┬──────────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────────┐
│                  SECURITY POLICY LAYER                           │
│  ┌───────────────────────────────────────────────────────┐      │
│  │ TSESecurityPolicy                                      │      │
│  │  ├── canSetVolume() → boolean                          │      │
│  │  ├── canSetBrightness() → boolean                      │      │
│  │  ├── canConnectWifi() → boolean                        │      │
│  │  ├── canShowClock() → boolean                          │      │
│  │  ├── canExit() → boolean                               │      │
│  │  └── loaded from exam config / lockdown policy          │      │
│  └───────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
```

### 11.2 Nguyên tắc thiết kế

1. **UI KHÔNG gọi OS trực tiếp** – luôn đi qua Controller → Service
2. **Footer KHÔNG spawn worker** – chỉ subscribe StateStore changes
3. **Popup KHÔNG giữ state lâu dài** – đọc từ StateStore khi mở, push changes qua Controller
4. **Parent/Exam dùng cùng Controller, StateStore, Services**
5. **Mọi user action có requestId** – stale response rejection
6. **Timer lifecycle rõ ràng** – start khi cần, stop khi không cần, shutdown khi exit

---

## 12. State Management Model

### 12.1 State machine cho slider

```
                    ┌────────────┐
                    │    IDLE     │ ←── updateState accepted
                    └──────┬─────┘
                           │ oninput
                    ┌──────▼─────┐
                    │  DRAGGING   │ ←── updateState BLOCKED
                    └──────┬─────┘
                           │ onchange (mouse up)
                    ┌──────▼──────────┐
                    │ COMMIT_PENDING   │ ←── updateState BLOCKED
                    │ (send to backend)│
                    └──────┬──────────┘
                           │ backend acknowledges
                    ┌──────▼─────┐
                    │ COMMITTED   │ ←── updateState BLOCKED until version matches
                    └──────┬─────┘
                           │ updateState with version >= local version
                    ┌──────▼─────┐
                    │    IDLE     │
                    └────────────┘
```

### 12.2 QuickSettingsSnapshot

```java
public class QuickSettingsSnapshot {
    public final int snapshotVersion;
    public final long timestamp;
    
    // Volume
    public final boolean volumeSupported;
    public final boolean volumeWritable;
    public final int volumePercent;
    public final boolean volumeMuted;
    public final int volumeVersion;
    
    // Brightness
    public final boolean brightnessSupported;
    public final boolean brightnessWritable;
    public final int brightnessPercent;
    public final int brightnessVersion;
    
    // Battery
    public final boolean hasBattery;
    public final boolean isCharging;
    public final int batteryPercent;
    
    // Network
    public final String wifiStatus; // "connected" | "disconnected" | "no_adapter"
    public final String wifiSsid;
    public final int wifiSignal;
    
    // Clock
    public final String clockTime; // "HH:mm"
    public final String clockDate; // "dd/MM/yyyy"
    
    // Input
    public final String inputMode; // "vi" | "en"
    
    // Convert to JSON for HTML bridges
    public String toJson() { ... }
}
```

### 12.3 Cache / refresh strategy

| Component | Cache TTL | Refresh trigger | During drag | During submit |
|---|---|---|---|---|
| Volume | 2s | Popup open, callback | Skip | Skip |
| Brightness | 10s | Popup open, after set | Skip | Skip |
| Battery | 5s | Footer timer | Always ok | Skip |
| WiFi | 30s | Popup open, manual | Always ok | Skip |
| Clock | 60s | Footer timer | Always ok | Always ok |
| Input | ∞ | User action only | N/A | N/A |

---

## 13. Component-by-Component Recommendation

| Component | Keep/Refactor/Replace | Recommended Action |
|---|---|---|
| `ExamFooterStatusBar` | **Refactor** | Remove worker spawning, subscribe to StateStore |
| `TSEQuickSettingsTrayCluster` | **Keep** | Minimal changes, update icon from StateStore |
| `TSEParentHtmlQuickSettingsPopup` | **Refactor** | Replace direct OS calls with Controller calls |
| `TSEQuickSettingsManager` | **Refactor** | Merge logic into QuickSettingsController |
| `TSEVolumeController` | **Refactor** → `VolumeService` | Fix COM lifecycle, add cache, consider event |
| `TSEBrightnessController` | **Refactor** → `BrightnessService` | Dedicated thread, adaptive detect, cache |
| `TSEBatteryStatusProvider` | **Refactor** → `BatteryService` | Gom into service with lifecycle |
| `TSENetworkStatusProvider` | **Refactor** → `NetworkService` | Gom into service with cache and lifecycle |
| `BatteryStatusIcon` | **Keep** | Đã tốt, Graphics2D custom widget |
| `TSEInputModeManager` | **Keep** | Đã tốt, singleton |
| `parent-quick-settings.html/css/js` | **Refactor** | Dùng shared JSON schema, design tokens |
| `tse-tray-flyout.js` | **Refactor** | Dùng shared JSON schema, add state machine |
| **(NEW)** `QuickSettingsController` | **Create** | Central orchestrator |
| **(NEW)** `QuickSettingsStateStore` | **Create** | Single source of truth |
| **(NEW)** `ClockService` | **Create** | Timer 60s, format time |
| **(NEW)** `TSESecurityPolicy` | **Create** | Policy matrix |

---

## 14. Roadmap Phase-by-Phase

### Phase 0: Full Audit + Baseline Test

**Mục tiêu**: Xác nhận trạng thái hiện tại hoạt động ổn định trước khi refactor.

**Vì sao**: Phải có baseline để biết refactor có gây regression không.

**Việc cần làm**:
- Chạy `run_input_test.bat --exam-id 3` trên portable
- Verify slider Volume/Brightness không snap-back
- Verify click outside đóng popup
- Verify Final Submit + Rust exit code 0
- Document tất cả log output quan trọng

**Không được làm**: Sửa code

**Acceptance criteria**: Tất cả test pass, có baseline log

**Lệnh test**: `cd dist\TutorHubSecureExam && .\run_input_test.bat --exam-id 3`

**Commit**: ❌ (chỉ document)

---

### Phase 1: Security Policy Matrix

**Mục tiêu**: Tạo `TSESecurityPolicy.java` với per-control enable/disable.

**Vì sao**: Cần policy trước khi refactor để mọi control đều checked.

**File tạo mới**:
- `src/main/java/.../ui/TSESecurityPolicy.java`

**Việc cần làm**:
- Define policy fields: `canSetVolume`, `canSetBrightness`, `canConnectWifi`, `showClock`, `canExit`, `canRefresh`
- Load defaults cho Login/Config vs Exam
- Không integrate vào UI chưa, chỉ tạo class

**Rủi ro**: Thấp

**Acceptance criteria**: Compile pass, policy class có test cases

**Commit**: ✅ Có thể commit riêng

---

### Phase 2: QuickSettingsSnapshot + JSON Schema

**Mục tiêu**: Tạo data model chung cho Parent/Exam popup.

**File tạo mới**:
- `src/main/java/.../ui/QuickSettingsSnapshot.java`

**Việc cần làm**:
- Define immutable snapshot class
- `toJson()` method
- Builder pattern hoặc factory method
- Unit test serialize/deserialize

**Rủi ro**: Thấp

**Acceptance criteria**: JSON output matches shared schema

**Commit**: ✅

---

### Phase 3: QuickSettingsStateStore

**Mục tiêu**: Tạo single source of truth thread-safe.

**File tạo mới**:
- `src/main/java/.../ui/QuickSettingsStateStore.java`

**Việc cần làm**:
- Thread-safe atomic updates
- `getSnapshot()` trả về immutable copy
- `updateVolume()`, `updateBrightness()`, `updateBattery()`, `updateNetwork()`, `updateClock()`
- Listener registration
- Version tracking

**Rủi ro**: Medium – cần đảm bảo thread safety

**Acceptance criteria**: Concurrent access test pass

**Commit**: ✅

---

### Phase 4: ClockService + Footer Clock

**Mục tiêu**: Thêm hiển thị giờ vào footer.

**File sửa**: `ExamFooterStatusBar.java`
**File tạo mới**: `ClockService.java`

**Việc cần làm**:
- `ClockService`: Timer 60s, format "HH:mm"
- Footer: thêm `JLabel` clock bên phải trước Power button
- Swing Timer update clock label

**Rủi ro**: Thấp

**Acceptance criteria**: Clock hiển thị chính xác, format đúng

**Commit**: ✅

---

### Phase 5: Battery Service Cleanup

**Mục tiêu**: Gom `TSEBatteryStatusProvider` thành `BatteryService` có lifecycle.

**File sửa**: `TSEBatteryStatusProvider.java` → refactor thành `BatteryService`

**Việc cần làm**:
- `initialize()` / `terminate()` lifecycle
- Timer 5s thay vì 2s (battery thay đổi chậm)
- Push update vào StateStore
- Remove trùng lặp trong Footer/Popup

**Rủi ro**: Thấp

**Acceptance criteria**: Battery icon vẫn chính xác, timer chỉ chạy 1 instance

**Commit**: ✅

---

### Phase 6: Volume Service Cleanup

**Mục tiêu**: Fix COM lifecycle, gom thành `VolumeService`.

**File sửa**: `TSEVolumeController.java` → refactor thành `VolumeService`

**Việc cần làm**:
- Singleton dedicated COM thread
- Proper CoInitialize/CoUninitialize
- Save/restore original volume/mute (giống SEB)
- Push update vào StateStore
- RequestId tracking
- Remove duplicate logic trong Manager/Popup

**Rủi ro**: Medium-High – COM lifecycle phức tạp

**Acceptance criteria**: No COM leak, volume get/set reliable, original state restored on exit

**Lệnh test**: `run_input_test.bat --exam-id 3` + manual slider test

**Commit**: ✅ Sau test kỹ

---

### Phase 7: WiFi Research Implementation Decision

**Mục tiêu**: Quyết định dùng Option A, B, hay C cho WiFi.

**Việc cần làm**:
- Research thêm `netsh wlan` alternatives
- Evaluate WlanAPI JNA binding feasibility
- Document decision
- Nếu giữ Option A: refactor `TSENetworkStatusProvider` → `NetworkService`

**Rủi ro**: Thấp (nếu giữ read-only)

**Commit**: ✅

---

### Phase 8: WiFi Implementation

**Mục tiêu**: Implement NetworkService theo decision Phase 7.

**File sửa**: `TSENetworkStatusProvider.java` → `NetworkService`

**Việc cần làm**: Tùy decision Phase 7

**Commit**: ✅

---

### Phase 9: Brightness Adaptive Implementation

**Mục tiêu**: Migrate brightness sang adaptive model.

**File sửa**: `TSEBrightnessController.java` → `BrightnessService`

**Việc cần làm**:
- Detect support lần đầu, cache kết quả
- Nếu supported: dedicated thread cho WMI (không spawn PowerShell)
- Nếu unsupported: slider disabled, text "Không hỗ trợ"
- Save/restore original brightness
- Push update vào StateStore

**Rủi ro**: Medium

**Commit**: ✅

---

### Phase 10: Parent/Exam Bridge Unification

**Mục tiêu**: Parent và Exam popup cùng đọc từ StateStore, cùng gọi Controller.

**File sửa**: `TSEParentHtmlQuickSettingsPopup.java`, `TSEQuickSettingsManager.java`

**Việc cần làm**:
- Parent: JavaAppBridge → gọi Controller thay vì gọi VolumeController/BrightnessController trực tiếp
- Exam: cefQuery handler → gọi Controller
- Footer: subscribe StateStore thay vì spawn worker riêng
- Remove tất cả duplicate logic

**Rủi ro**: Medium-High

**Commit**: ✅ Sau test E2E

---

### Phase 11: UI Polish Windows 11-like

**Mục tiêu**: Cải thiện visual 95% match giữa Parent/Exam popup.

**File sửa**: `parent-quick-settings.html/css/js`, `tse-tray-flyout.js`

**Việc cần làm**:
- Apply shared design tokens
- Add clock to popup
- Consistent icon styling
- Hover/active micro-animations
- Typography refinement

**Rủi ro**: Thấp

**Commit**: ✅

---

### Phase 12: Footer Lifecycle Cleanup

**Mục tiêu**: Footer timer dừng chính xác khi submit/exit.

**File sửa**: `ExamFooterStatusBar.java`, `TSEExamChildClient.java`

**Việc cần làm**:
- `stopAllTimers()` gọi sớm trong submit flow
- `QuickSettingsController.shutdown()` gọi trước JCEF cleanup
- Verify no orphan threads

**Rủi ro**: Low-Medium

**Commit**: ✅

---

### Phase 13: E2E Secure Exam Testing

**Mục tiêu**: Test toàn bộ flow Login → Config → Exam → Submit → Exit.

**Việc cần làm**:
- Test tất cả slider interactions
- Test popup open/close lifecycle
- Test Final Submit success
- Test Rust exit code 0
- Test portable build
- Test trên Windows 10 + Windows 11

**Rủi ro**: Phát hiện regression

**Commit**: ❌ (test only)

---

### Phase 14: Release / Commit Strategy

**Mục tiêu**: Commit tất cả thay đổi đã test.

**Việc cần làm**:
- `git status` review tất cả file
- Commit message rõ ràng per-phase
- Update `docs/secure_exam_tasks_v2.md`
- Tag version

**Commit**: ✅

---

## 15. File Refactor Map

| File | Action | Phase |
|---|---|---|
| `TSESecurityPolicy.java` | **NEW** | 1 |
| `QuickSettingsSnapshot.java` | **NEW** | 2 |
| `QuickSettingsStateStore.java` | **NEW** | 3 |
| `ClockService.java` | **NEW** | 4 |
| `ExamFooterStatusBar.java` | **MODIFY** (add clock, subscribe store) | 4, 10, 12 |
| `TSEBatteryStatusProvider.java` | **REFACTOR** → `BatteryService` | 5 |
| `TSEVolumeController.java` | **REFACTOR** → `VolumeService` | 6 |
| `TSENetworkStatusProvider.java` | **REFACTOR** → `NetworkService` | 7-8 |
| `TSEBrightnessController.java` | **REFACTOR** → `BrightnessService` | 9 |
| `TSEParentHtmlQuickSettingsPopup.java` | **MODIFY** (use Controller) | 10 |
| `TSEQuickSettingsManager.java` | **MODIFY** (use Controller) | 10 |
| `QuickSettingsController.java` | **NEW** | 10 |
| `parent-quick-settings.html/css/js` | **MODIFY** (design tokens, clock) | 11 |
| `tse-tray-flyout.js` | **MODIFY** (design tokens, state machine) | 11 |
| `TSEExamChildClient.java` | **MODIFY** (lifecycle cleanup) | 12 |

---

## 16. Test Strategy

### 16.1 Unit tests

- `QuickSettingsSnapshot.toJson()` serialization
- `QuickSettingsStateStore` thread safety
- `TSESecurityPolicy` defaults
- `ClockService` format

### 16.2 Integration tests

- Footer subscribe StateStore → icon update
- Parent popup open → Controller.getSnapshot()
- Slider drag → Controller.setVolume() → StateStore update → no snap-back

### 16.3 E2E tests

| Test | Command | Expected |
|---|---|---|
| Login → Config → Exam | `run.bat --exam-id 3` | Full flow success |
| Quick Settings popup | Click cluster | Popup opens, correct data |
| Volume slider | Drag slider | No snap-back, OS volume changes |
| Final Submit | Click Submit → Confirm | `submit_payload.enc` written, exit code 0 |
| Portable build | `build_portable.ps1` | `dist\TutorHubSecureExam` complete |

---

## 17. Risk Register

| Risk | Severity | Likelihood | Mitigation |
|---|---|---|---|
| COM leak khi refactor Volume | High | Medium | Dedicated COM thread, proper cleanup |
| WMI brightness fail trên desktop | Medium | High | Adaptive detect, graceful disable |
| Footer timer race during submit | Medium | Low | Stop timers before submit |
| Parent/Exam visual mismatch | Low | Medium | Shared design tokens, visual test |
| Regression sau refactor | High | Medium | Phase-by-phase commit, E2E test mỗi phase |
| netsh locale parsing | Medium | Medium | Regex pattern for cả EN/VN |
| JCEF lifecycle conflict | High | Low | Không đụng JCEF core |
| PowerShell GPO block | Medium | Low | Migrate to native API (Phase 9) |
| JavaFX WebView memory | Low | Low | Singleton popup, lazy init |

---

## 18. Final Recommendation

### Quyết định kỹ thuật rõ ràng

| # | Câu hỏi | Quyết định |
|---|---|---|
| 1 | Tiếp tục JavaFX WebView cho Parent? | ✅ **Có** – nhẹ hơn Parent JCEF, đã hoạt động |
| 2 | Giữ JCEF DOM popup cho Exam? | ✅ **Có** – exam browser là JCEF, DOM injection hợp lý |
| 3 | Bỏ brightness nếu WMI chậm? | ❌ **Không bỏ** – dùng adaptive: tự detect, disable nếu unsupported |
| 4 | Brightness nên dùng gì? | **WMI + adaptive detect** → tương lai: native `SetMonitorBrightness()` |
| 5 | Volume dùng gì? | **Giữ JNA/CoreAudio**, fix COM lifecycle, consider event callback |
| 6 | WiFi read-only hay connect? | **Read-only** bây giờ, whitelist-connect tương lai nếu cần |
| 7 | Icon system? | **Fluent UI System Icons** (MIT) + FlatSVGIcon cho Swing, inline SVG cho HTML |
| 8 | Tách QuickSettingsStateStore? | ✅ **Có** – class riêng, singleton, thread-safe |
| 9 | Chung JSON schema? | ✅ **Có** – `QuickSettingsSnapshot.toJson()` |
| 10 | Event-driven Volume? | ✅ **Nên** nếu JNA callback khả thi, fallback poll 3s |

### Tóm tắt ưu tiên

1. **Ưu tiên cao nhất**: Fix COM lifecycle Volume (Phase 6) – rủi ro crash/leak
2. **Ưu tiên cao**: StateStore + Controller (Phase 2-3, 10) – giảm duplicate, giảm poll
3. **Ưu tiên trung bình**: Clock (Phase 4), Battery cleanup (Phase 5)
4. **Ưu tiên thấp**: UI Polish (Phase 11), WiFi upgrade (Phase 7-8)
5. **Không làm**: Parent JCEF, mở Windows Settings, WiFi connect đại trà

---

## Appendix A: SEB Source Reference Notes

Tham khảo SEB v3.10.1 theo MPL-2.0 license:

- `SafeExamBrowser.SystemComponents/Audio/Audio.cs`: Event-driven audio via NAudio `OnVolumeNotification`
- `SafeExamBrowser.UserInterface.Desktop/Controls/ActionCenter/AudioControl.xaml.cs`: Slider `DragStarted`/`DragCompleted` pattern
- `SafeExamBrowser.SystemComponents/PowerSupply/PowerSupply.cs`: Timer 5s polling, `StatusChanged` event
- `SafeExamBrowser.SystemComponents/Network/NetworkAdapter.cs`: `Windows.Devices.WiFi` API, event + timer hybrid, `CredentialsRequired` event for WiFi password
- `SafeExamBrowser.Client/Operations/ShellOperation.cs`: Policy-driven component initialization (`ShowAudio`, `ShowNetwork`, `ShowClock`, etc.)

## Appendix B: Fluent UI System Icons License

Microsoft Fluent UI System Icons are licensed under **MIT License**. Free for commercial use. Source: https://github.com/microsoft/fluentui-system-icons

Các icon TSE đang dùng/cần thêm:
- `speaker_2_24_regular.svg` ✅ có
- `speaker_mute_24_regular.svg` ✅ có
- `battery_24_regular.svg` ✅ có
- `power_24_regular.svg` ✅ có
- `arrow_clockwise_24_regular.svg` ✅ có
- `info_24_regular.svg` ✅ có
- `wifi_1_24_regular.svg` ❌ cần thêm
- `wifi_off_24_regular.svg` ❌ cần thêm
- `brightness_high_24_regular.svg` ❌ cần thêm
- `clock_24_regular.svg` ❌ cần thêm
