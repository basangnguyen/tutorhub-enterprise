# TutorHub Secure Exam - Taskbar / Footer / Tray / Quick Settings Architecture Research

## 1. Executive Summary

TutorHub Secure Exam (TSE) đang đi đúng hướng về mặt sản phẩm và bảo mật: tự dựng taskbar/footer nội bộ, không mở Windows Settings, không dùng modal hệ điều hành trong màn hình thi, Quick Settings trong Exam được render bằng JCEF DOM thay vì cố đè Swing popup lên browser.

Vấn đề chính hiện nay không phải là thiếu một vài chỉnh sửa nhỏ cho slider hay icon, mà là thiếu một `QuickSettingsStateStore` làm **single source of truth**. State đang bị phân tán giữa Parent JavaFX WebView, Exam JCEF DOM, Swing footer và các controller native. Vì vậy nếu chỉ sửa từng lỗi slider riêng lẻ, hệ thống sẽ tiếp tục sinh lỗi đồng bộ, stale state, worker chồng nhau và khó nghiệm thu Final Submit.

Kết luận kiến trúc:

- Parent/Login/Config nên tiếp tục dùng JavaFX WebView popup, không quay lại Parent JCEF.
- Exam nên tiếp tục dùng JCEF DOM popup.
- Swing footer chỉ nên render state từ store, không tự query OS lung tung.
- WiFi chỉ read-only trong secure exam.
- Brightness phải là adaptive feature: supported writable, supported readonly hoặc unsupported.
- Volume nên giữ CoreAudio/JNA nhưng cần lifecycle/cache/event rõ hơn.
- Không mở Windows Settings / Control Panel / Network Flyout thật.
- Không dùng `JOptionPane`, `JDialog` hoặc modal native trong active exam.
- Không poll PowerShell WMI brightness liên tục.
- Không làm ảnh hưởng Final Submit và Rust exit code 0.

## 2. Hiện Trạng Taskbar / Footer / Tray / Quick Settings Của TSE

Hiện tại TSE có ba lớp UI chính:

| Khu vực | File chính | Hiện trạng |
|---|---|---|
| Footer khi thi | `ExamFooterStatusBar.java` | Swing footer ở dưới màn hình thi, có input mode, tray cluster và nút power |
| Tray cluster | `TSEQuickSettingsTrayCluster.java` | Cụm icon WiFi / volume / battery, click mở Quick Settings |
| Quick Settings trong Exam | `tse-tray-flyout.js` + `TSEQuickSettingsManager.java` | Popup DOM trong JCEF, tránh lỗi overlay giữa Swing và Chromium |
| Quick Settings Parent | `TSEParentHtmlQuickSettingsPopup.java` + `parent-quick-settings.*` | JavaFX WebView trong `JWindow`, dùng cho Login/Config/Parent |
| Native provider/controller | `TSEVolumeController`, `TSEBrightnessController`, `TSENetworkStatusProvider`, `TSEBatteryStatusProvider` | Đọc/ghi trạng thái OS nhưng còn tách rời, chưa có state store chung |

Điểm đang tốt:

- TSE không mở công cụ hệ điều hành thật trong secure exam.
- Popup trong Exam nằm trong DOM/JCEF nên ít rủi ro hơn Swing overlay.
- Volume dùng CoreAudio/JNA, đúng hướng hơn so với shell command.
- Battery dùng Win32 `GetSystemPowerStatus`, nhẹ và hợp lý.
- Brightness có timeout và shutdown process để giảm rủi ro treo.

Điểm cần xử lý:

- Footer timer dễ spawn worker chồng nhau.
- Parent và Exam đang dùng hai logic state khác nhau.
- Exam DOM popup đang giữ state riêng bằng biến JS rời rạc.
- Payload JSON/JS còn bị build thủ công ở một số nơi.
- Native service chưa có lifecycle chuẩn kiểu initialize, subscribe, terminate, restore.

## 3. Kiến Trúc TSE Hiện Tại

Luồng hiện tại có thể hiểu như sau:

```text
Swing Footer / Tray Cluster
        |
        v
TSEQuickSettingsManager
        |
        +-- TSEVolumeController
        +-- TSEBrightnessController
        +-- TSENetworkStatusProvider
        +-- TSEBatteryStatusProvider
        |
        v
Exam JCEF DOM Flyout
```

Parent/Login/Config lại đi qua một nhánh khác:

```text
Parent Swing UI
        |
        v
TSEParentHtmlQuickSettingsPopup
        |
        v
JavaFX WebView + JavaAppBridge
        |
        +-- TSEVolumeController
        +-- TSEBrightnessController
        +-- TSENetworkStatusProvider
        +-- TSEBatteryStatusProvider
```

Nhánh Exam và nhánh Parent cùng đụng đến các controller nhưng không dùng chung một state model chính thức. Đây là nguồn gốc của nhiều lỗi khó nhìn thấy ngay.

## 4. Những Gì Học Được Từ Safe Exam Browser

Safe Exam Browser (SEB) đáng học ở kiến trúc shell và system components, không phải ở giao diện pixel-perfect.

Những bài học quan trọng:

| Module SEB | Bài học áp dụng cho TSE |
---|---|
| `ShellOperation` | Shell quyết định control nào được bật, không để từng UI tự quyết |
| `Audio` | Có lifecycle `Initialize` / `Terminate`, subscribe volume change, lưu state ban đầu và revert khi kết thúc |
| `PowerSupply` | Poll nhẹ theo timer riêng, phát event `StatusChanged` cho UI |
| `NetworkAdapter` | Tách network state khỏi UI, scan khi cần, emit event khi network thay đổi |
| `Taskbar` / `ActionCenter` | UI shell chỉ add control, render và close, không trực tiếp xử lý OS quá sâu |

Điểm nên học trực tiếp:

- Tách system component khỏi UI.
- Có lifecycle rõ ràng.
- Có event model thay vì UI tự query liên tục.
- Khi đóng shell, tất cả control phải close/cleanup.
- Không để popup/control sống dai sau khi session kết thúc.

Điểm không nên copy:

- Không copy code SEB nguyên văn.
- Không dùng asset/icon SEB.
- Không mở rộng WiFi connect theo SEB ở giai đoạn này, vì TSE cần WiFi read-only trong secure exam.
- Không áp dụng máy móc toàn bộ kiến trúc WPF/.NET của SEB vào Java Swing/JCEF.

## 5. Những Gì Học Được Từ Windows 11 Quick Settings / Fluent Design

Windows 11 Quick Settings cho TSE ba bài học chính:

1. **Unified flyout**: click vào network, sound, battery nên mở cùng một Quick Settings surface.
2. **Transient surface**: flyout phải nhẹ, đóng bằng click ngoài / Esc, không phải modal dialog.
3. **Immediate feedback + controlled commit**: slider phản hồi ngay trong UI, nhưng commit xuống OS phải debounce, version/requestId và chống stale response.

Áp dụng vào TSE:

- Dùng một Quick Settings popup cho WiFi, volume, brightness, battery.
- Input mode VIE/ENG là flyout riêng hoặc toggle riêng, không trộn lẫn logic với volume/network.
- Dùng design tokens chung: radius, spacing, color, typography.
- Parent WebView và Exam JCEF có thể khác bridge, nhưng nên dùng cùng JSON schema và visual tokens.
- Không dùng asset proprietary của Windows. Có thể dùng Fluent UI System Icons vì license MIT.

## 6. Bảng So Sánh TSE / SEB / Windows 11

| Tiêu chí | TSE hiện tại | SEB | Windows 11 | Nhận xét |
|---|---|---|---|---|
| UI architecture | Swing footer + JCEF DOM + JavaFX WebView | WPF shell | Native WinUI shell | TSE đúng hướng nhưng bị chia renderer |
| System component architecture | Provider/controller rời rạc | Component có lifecycle | OS service/state tập trung | TSE cần service/store chung |
| Taskbar layout | Footer nội bộ dưới màn hình thi | Taskbar riêng trong kiosk | Native taskbar | TSE phù hợp secure exam |
| Quick Settings popup | Exam DOM, Parent JavaFX WebView | WPF popup/action center | WinUI flyout | TSE cần thống nhất schema/state |
| Volume control | CoreAudio/JNA | CoreAudio qua NAudio | Native audio stack | Nên giữ CoreAudio/JNA |
| Brightness control | PowerShell WMI | Không có | Native brightness slider | TSE nên adaptive, không poll liên tục |
| WiFi display/control | `netsh`, read-only | WinRT WiFiAdapter, có connect | Native WiFi panel | TSE nên giữ read-only |
| Battery display | JNA `GetSystemPowerStatus` | `SystemInformation.PowerStatus` | Native battery | TSE đang hợp lý |
| Language/input mode | VIE/ENG toggle/flyout | Keyboard layout control | Input switcher | TSE cần tách state input khỏi Quick Settings core |
| Refresh | Safe refresh trong JCEF | Browser reload/controls | App/browser-specific | Không để refresh ảnh hưởng timer/final submit |
| About | Overlay nội bộ | About window/control | App surface | Trong exam không dùng modal native |
| Exit | Power blocked trong exam | Quit control theo settings | Power/system menu | TSE đang đúng: blocked |
| State management | Phân tán | Event/component model | Central OS state | Root cause chính của lỗi TSE |
| Event-driven vs polling | Polling + vài cache | Event-driven + timer nhẹ | Event-driven OS | TSE nên chuyển dần sang event/store |
| Cache strategy | Rải rác theo class | Nằm trong component | OS-managed | Cần cache TTL tập trung |
| Thread/worker lifecycle | Footer có thể spawn worker chồng | Initialize/Terminate | OS-managed | TSE cần lifecycle manager |
| Security risk | Thấp nếu không mở Settings | Kiểm soát nội bộ | Native OS có nhiều entrypoint | TSE phải giữ UI nội bộ |
| Performance risk | Medium: PowerShell/WiFi polling | Low/medium | Low | Tối ưu polling và timeout |
| Maintainability | Medium/low do duplicate logic | High hơn | N/A | Cần schema/store/shared tokens |
| Visual polish | Đã khá giống Win11 nhưng chưa thống nhất | SEB functional | Polished | Cần tách design tokens |
| Portable build compatibility | Có rủi ro JavaFX + JCEF + assets | Installer riêng | Native OS | Cần test portable sau mỗi phase |

## 7. Ưu Điểm Của Taskbar TSE Hiện Tại

- Kiến trúc không mở Windows system UI thật, phù hợp secure exam.
- Exam Quick Settings render bằng DOM trong JCEF, tránh lỗi heavyweight/lightweight Swing overlay.
- Parent đã chuyển sang JavaFX WebView, giúp CSS/HTML tốt hơn Swing custom drawing.
- Volume đi theo CoreAudio/JNA, đúng hướng kỹ thuật.
- Battery provider nhẹ.
- Brightness có fallback unsupported và timeout, không giả UI.
- Final Submit đã có bước shutdown Quick Settings / footer polling trước exit.
- Fluent icons đã được đưa vào resource riêng, tránh dùng asset Windows proprietary.

## 8. Nhược Điểm Của Taskbar TSE Hiện Tại

- State phân tán giữa nhiều nơi.
- Parent và Exam dùng logic UI/JS khác nhau.
- Footer vẫn tự poll OS và spawn worker.
- Brightness dùng PowerShell WMI, không phù hợp polling dày.
- WiFi dùng `netsh`, phụ thuộc locale và output text.
- JSON payload có nơi build thủ công.
- Native controllers chưa có lifecycle chuẩn, chưa có restore/cleanup đầy đủ như SEB.
- Chưa có test contract cho Quick Settings state.
- Chưa có phân tầng rõ: UI intent, state store, service, native integration.

## 9. Root Cause Các Lỗi Hiện Nay

Root cause chính:

```text
State bị phân tán giữa:
- Parent JavaFX WebView
- Exam JCEF DOM
- Swing footer
- TSEQuickSettingsManager
- Native controllers/providers
```

Hệ quả:

- Slider có thể bị backend refresh overwrite khi user đang kéo.
- Parent và Exam hiển thị cùng một trạng thái theo hai cách khác nhau.
- Cache volume/brightness/wifi không đồng nhất.
- Worker/timer có thể còn chạy khi popup đóng hoặc final submit.
- Sửa một lỗi nhỏ ở Exam không tự động sửa Parent.
- Rất khó viết test nghiệm thu ổn định.

Vì vậy không nên tiếp tục sửa từng lỗi slider riêng lẻ. Cần gom lại bằng `QuickSettingsStateStore`.

## 10. Kiến Trúc Mục Tiêu Đề Xuất

Kiến trúc mục tiêu:

```text
Swing Footer / Tray Cluster
        |
        v
QuickSettingsController
        |
        v
QuickSettingsStateStore
        |
        +-- VolumeService
        +-- BrightnessService
        +-- NetworkService
        +-- BatteryService
        |
        +-- ParentWebViewBridge
        +-- ExamJcefBridge
```

Nguyên tắc:

- UI không gọi trực tiếp OS provider.
- UI gửi intent: `OPEN_POPUP`, `SET_VOLUME`, `TOGGLE_MUTE`, `SET_BRIGHTNESS`, `REFRESH_WIFI`, `CLOSE_POPUP`.
- Store phát snapshot mới cho footer/popup.
- Service xử lý OS/native, timeout, cache, error.
- Bridge chỉ chuyển JSON giữa Java và JS.
- Parent và Exam dùng chung JSON schema, chỉ khác cơ chế bridge.

## 11. QuickSettingsStateStore / Single Source Of Truth

`QuickSettingsStateStore` nên là nơi duy nhất nắm trạng thái Quick Settings.

Nhiệm vụ:

- Giữ snapshot mới nhất.
- Quản lý version/requestId.
- Chống stale response.
- Chặn backend overwrite khi user đang drag.
- Quản lý trạng thái popup open/closed.
- Quản lý TTL cache.
- Phát event/snapshot cho footer, Parent WebView và Exam JCEF.

Không nên để store:

- Gọi trực tiếp PowerShell/netsh trong EDT.
- Render UI.
- Biết chi tiết HTML/CSS.
- Biết chi tiết Swing/JCEF/JavaFX.

## 12. State Management Model Đề Xuất

Snapshot đề xuất:

```text
QuickSettingsSnapshot
- popupState: CLOSED | OPENING | OPEN | CLOSING
- volume:
  - supported
  - writable
  - percent
  - muted
  - pending
  - version
  - requestId
  - error
- brightness:
  - supported
  - writable
  - percent
  - pending
  - version
  - requestId
  - error
- network:
  - adapterAvailable
  - connected
  - ssid
  - signal
  - networks
  - loading
  - lastScanAt
  - error
- battery:
  - hasBattery
  - charging
  - percent
  - low
  - critical
- inputMode:
  - ENG | VIE
```

State rule cho slider:

| State | Ý nghĩa | Backend update có được overwrite UI không |
|---|---|---|
| `IDLE` | Không có user action | Có |
| `DRAGGING` | User đang kéo slider | Không |
| `COMMIT_PENDING` | Đã gửi intent xuống service | Không, trừ response cùng requestId |
| `COMMITTED` | Backend xác nhận | Có, nếu version mới hơn hoặc bằng |
| `FAILED` | Service lỗi | Revert về last confirmed + inline error |

## 13. Roadmap Phát Triển Chi Tiết Theo Phase

### Phase 0: Audit baseline hiện tại

- Mục tiêu: Chốt hiện trạng trước khi refactor.
- File liên quan: toàn bộ danh sách ở mục 14.
- Việc cần làm:
  - Ghi lại hành vi hiện tại của Parent/Exam/footer.
  - Chụp log khi mở Quick Settings, kéo volume, brightness, final submit.
  - Không sửa logic.
- Rủi ro: Baseline thiếu sẽ làm khó so sánh sau refactor.
- Acceptance criteria:
  - Có checklist hiện trạng.
  - Có danh sách lỗi tái hiện được.
- Lệnh test nếu có:
  - `mvn clean install`
  - `powershell -ExecutionPolicy Bypass -File .\build_portable.ps1`
  - VM/manual test portable.
- Có được commit sau phase này không: Có, nếu chỉ commit tài liệu audit.

### Phase 1: Tạo QuickSettingsSnapshot / JSON schema chung

- Mục tiêu: Chuẩn hóa payload giữa Parent và Exam.
- File liên quan:
  - `TSEQuickSettingsManager.java`
  - `TSEParentHtmlQuickSettingsPopup.java`
  - `parent-quick-settings.js`
  - `tse-tray-flyout.js`
- Việc cần làm:
  - Định nghĩa object snapshot Java.
  - Serialize bằng Gson.
  - Parent/Exam dùng chung field name.
- Rủi ro: Sai field làm popup không render.
- Acceptance criteria:
  - Parent và Exam nhận cùng schema.
  - Không build JSON bằng nối chuỗi thủ công cho state chính.
- Lệnh test nếu có:
  - `mvn clean install`
- Có được commit sau phase này không: Có, nếu build pass.

### Phase 2: Tạo QuickSettingsStateStore

- Mục tiêu: Single source of truth.
- File liên quan:
  - Tạo mới `QuickSettingsStateStore.java`
  - Tạo mới `QuickSettingsSnapshot.java`
  - `TSEQuickSettingsManager.java`
  - `TSEParentHtmlQuickSettingsPopup.java`
- Việc cần làm:
  - Store giữ snapshot immutable.
  - Có listener/subscriber.
  - Có method apply intent.
  - Có version/requestId.
- Rủi ro: Dễ làm rộng quá mức.
- Acceptance criteria:
  - Footer, Parent, Exam có thể lấy snapshot từ store.
  - Không gọi OS provider trực tiếp từ UI mới.
- Lệnh test nếu có:
  - `mvn clean install`
- Có được commit sau phase này không: Có, nếu unit/build pass.

### Phase 3: Refactor VolumeService

- Mục tiêu: Giữ CoreAudio/JNA nhưng có lifecycle rõ.
- File liên quan:
  - `TSEVolumeController.java`
  - `TSEVolumeStatus.java`
  - `TSEQuickSettingsManager.java`
  - `TSEJcefLifecycleManager.java`
- Việc cần làm:
  - Bọc thành `VolumeService`.
  - Chuẩn hóa get/set/mute.
  - Xem lại `CoInitializeEx` / `CoUninitialize`.
  - Optional: lưu initial volume/mute và quyết định có restore không.
- Rủi ro: COM leak, audio device edge case.
- Acceptance criteria:
  - Slider volume đổi volume thật.
  - Mute/unmute ổn.
  - Không ảnh hưởng Final Submit.
- Lệnh test nếu có:
  - `mvn clean install`
  - Manual test trên Windows.
- Có được commit sau phase này không: Có, nếu test pass.

### Phase 4: Refactor BrightnessService hoặc quyết định fallback/read-only

- Mục tiêu: Brightness adaptive, không poll PowerShell liên tục.
- File liên quan:
  - `TSEBrightnessController.java`
  - `TSEQuickSettingsManager.java`
  - `parent-quick-settings.js`
  - `tse-tray-flyout.js`
- Việc cần làm:
  - Phân loại `supported writable`, `supported readonly`, `unsupported`.
  - Query cache TTL rõ ràng.
  - Chỉ set khi commit, không set khi dragging.
  - Timeout và kill active PowerShell process khi shutdown.
- Rủi ro: WMI chậm, unsupported trên desktop/external monitor.
- Acceptance criteria:
  - Laptop supported chỉnh được.
  - Desktop unsupported hiển thị disabled.
  - Không treo khi final submit.
- Lệnh test nếu có:
  - Manual test laptop + desktop/VM.
- Có được commit sau phase này không: Có, nếu unsupported path cũng sạch.

### Phase 5: Refactor NetworkService read-only

- Mục tiêu: WiFi chỉ đọc, scan có kiểm soát.
- File liên quan:
  - `TSENetworkStatusProvider.java`
  - `TSEQuickSettingsManager.java`
  - `tse-tray-flyout.js`
- Việc cần làm:
  - Scan khi popup mở hoặc user refresh.
  - TTL cache rõ.
  - Không connect/disconnect.
  - Locale handling tốt hơn.
- Rủi ro: `netsh` output khác ngôn ngữ.
- Acceptance criteria:
  - Có WiFi thì hiện SSID/signal.
  - Không WiFi thì báo read-only/adapter unavailable.
  - Click network không mở Windows Settings.
- Lệnh test nếu có:
  - Manual test laptop có WiFi + VM không WiFi.
- Có được commit sau phase này không: Có.

### Phase 6: Refactor BatteryService

- Mục tiêu: Battery nhẹ, không spawn worker chồng nhau.
- File liên quan:
  - `TSEBatteryStatusProvider.java`
  - `BatteryStatusIcon.java`
  - `ExamFooterStatusBar.java`
- Việc cần làm:
  - Chuẩn hóa battery snapshot.
  - Low/critical state.
  - Timer nhẹ 5-10s hoặc event giả lập qua scheduler chung.
- Rủi ro: Desktop không có pin.
- Acceptance criteria:
  - Laptop hiện pin/sạc đúng.
  - Desktop hiện AC hoặc ẩn battery hợp lý.
- Lệnh test nếu có:
  - Manual rút/cắm sạc.
- Có được commit sau phase này không: Có.

### Phase 7: Chuẩn hóa Parent WebView Bridge và Exam JCEF Bridge

- Mục tiêu: Cùng intent/schema, khác bridge.
- File liên quan:
  - `TSEParentHtmlQuickSettingsPopup.java`
  - `TSEJcefLifecycleManager.java`
  - `TSEQuickSettingsManager.java`
- Việc cần làm:
  - Định nghĩa bridge API chung.
  - Parent JavaFX gọi intent vào controller/store.
  - Exam JCEF route query vào controller/store.
- Rủi ro: Bridge routing ảnh hưởng submit payload nếu trộn sai.
- Acceptance criteria:
  - Không dùng `SUBMIT_PAYLOAD` cho volume/brightness/wifi.
  - Language/input event không phá submit.
- Lệnh test nếu có:
  - `mvn clean install`
  - Final Submit E2E.
- Có được commit sau phase này không: Có, nếu Final Submit pass.

### Phase 8: Tách DOM UI / CSS / JS / design tokens

- Mục tiêu: UI sạch, ít inline string, dễ polish.
- File liên quan:
  - `parent-quick-settings.html`
  - `parent-quick-settings.css`
  - `parent-quick-settings.js`
  - `tse-tray-flyout.js`
  - `src/main/resources/tse/icons/fluent/`
- Việc cần làm:
  - Dùng CSS variables/design tokens.
  - Tách template render function.
  - Dùng chung spacing/radius/color.
  - Không dùng asset proprietary.
- Rủi ro: Resource path trong portable.
- Acceptance criteria:
  - Parent/Exam nhìn thống nhất.
  - Portable copy đủ resource.
- Lệnh test nếu có:
  - `powershell -ExecutionPolicy Bypass -File .\build_portable.ps1`
- Có được commit sau phase này không: Có.

### Phase 9: Sửa lifecycle footer/timer/worker

- Mục tiêu: Footer chỉ render store, không query OS lung tung.
- File liên quan:
  - `ExamFooterStatusBar.java`
  - `TSEQuickSettingsTrayCluster.java`
  - `TSEExamChildClient.java`
- Việc cần làm:
  - Gỡ worker chồng nhau.
  - Footer subscribe snapshot.
  - Stop polling chắc chắn khi final submit.
  - Không để background thread non-daemon cản exit.
- Rủi ro: Icon footer không cập nhật nếu event/store sai.
- Acceptance criteria:
  - Footer update state đúng.
  - Final Submit không timeout.
- Lệnh test nếu có:
  - Final Submit E2E trong portable/VM.
- Có được commit sau phase này không: Có, nếu E2E pass.

### Phase 10: E2E test + release

- Mục tiêu: Nghiệm thu toàn bộ Quick Settings taskbar.
- File liên quan:
  - `build_portable.ps1`
  - toàn bộ file UI/service đã refactor.
- Việc cần làm:
  - Build portable.
  - Test Login/Config/Exam.
  - Test Final Submit.
  - Test Rust exit code 0.
  - Test unsupported paths.
- Rủi ro: Lỗi chỉ xuất hiện trong portable hoặc VM.
- Acceptance criteria:
  - Không modal native trong active exam.
  - Không mở Windows Settings.
  - Không thanh đen.
  - Không hai nút Nộp bài.
  - Không fallback autosave khi final submit bình thường.
  - Không child process timeout.
- Lệnh test nếu có:
  - `mvn clean install`
  - `powershell -ExecutionPolicy Bypass -File .\build_portable.ps1`
  - `dist\TutorHubSecureExam\run_input_test.bat --exam-id 3`
- Có được commit sau phase này không: Có, nếu toàn bộ test pass.

## 14. Danh Sách File Cần Refactor

Các file cần audit/refactor:

```text
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/ExamFooterStatusBar.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEQuickSettingsTrayCluster.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEParentHtmlQuickSettingsPopup.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEQuickSettingsManager.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEExamShellFrame.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEExamChildClient.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEJcefLifecycleManager.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEBrightnessController.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEVolumeController.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEBatteryStatusProvider.java
src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSENetworkStatusProvider.java
src/main/resources/tse/quick-settings/parent-quick-settings.html
src/main/resources/tse/quick-settings/parent-quick-settings.css
src/main/resources/tse/quick-settings/parent-quick-settings.js
src/main/resources/tse/tse-tray-flyout.js
src/main/resources/tse/icons/fluent/
build_portable.ps1
```

Nhóm ưu tiên cao:

1. `TSEQuickSettingsManager.java`
2. `TSEParentHtmlQuickSettingsPopup.java`
3. `tse-tray-flyout.js`
4. `parent-quick-settings.js`
5. `ExamFooterStatusBar.java`

## 15. Quyết Định Kỹ Thuật Khuyến Nghị

- Giữ Parent/Login/Config bằng JavaFX WebView.
- Giữ Exam bằng JCEF DOM popup.
- Không quay lại Parent JCEF.
- Không quay lại Swing-only popup cho Quick Settings.
- Dùng `QuickSettingsStateStore` làm single source of truth.
- Dùng schema JSON chung cho Parent và Exam.
- Dùng design tokens chung cho CSS.
- Volume giữ CoreAudio/JNA.
- Brightness adaptive, không poll WMI liên tục.
- WiFi read-only.
- Battery dùng provider nhẹ, store/scheduler quản lý update.
- Footer render state từ store.
- Không dùng modal native trong active exam.
- Không làm ảnh hưởng Final Submit và Rust exit code 0.

## 16. Rủi Ro Còn Lại

| Rủi ro | Mức độ | Cách giảm |
|---|---|---|
| JavaFX WebView + JCEF cùng tồn tại làm portable phức tạp | Trung bình | Test portable sau Phase 8/10 |
| PowerShell WMI brightness timeout | Cao | Cache, timeout, kill process, adaptive unsupported |
| COM volume leak | Cao | Audit `CoInitializeEx` / `CoUninitialize`, lifecycle rõ |
| `netsh` parse sai do locale | Trung bình | Parser phòng thủ, fallback empty/read-only |
| Worker footer chồng nhau | Trung bình | Store/scheduler chung, không spawn mỗi tick |
| Stale response ghi đè slider | Cao | requestId/version/state machine |
| Refactor ảnh hưởng Final Submit | Cao | Không đụng submit path nếu không cần, test E2E mỗi phase lớn |
| Resource HTML/CSS/JS thiếu trong portable | Trung bình | Kiểm tra `build_portable.ps1` copy resource |

## 17. Bộ Test Nghiệm Thu

Checklist nghiệm thu:

- [ ] Login mở Quick Settings được.
- [ ] Config mở Quick Settings được.
- [ ] Exam mở Quick Settings trong JCEF DOM được.
- [ ] Click ngoài đóng popup.
- [ ] Esc đóng popup nếu focus nằm trong popup.
- [ ] Volume slider đổi âm lượng thật.
- [ ] Mute/unmute hoạt động.
- [ ] Brightness supported chỉnh được trên laptop.
- [ ] Brightness unsupported disabled rõ ràng trên desktop/VM.
- [ ] Không poll PowerShell WMI liên tục khi user không mở popup.
- [ ] WiFi hiển thị read-only.
- [ ] Click WiFi network không mở Windows Settings.
- [ ] Battery hiển thị đúng khi cắm/rút sạc.
- [ ] VIE/ENG vẫn hoạt động.
- [ ] About vẫn mở/đóng được bằng overlay nội bộ.
- [ ] Power/Exit vẫn blocked trong active exam.
- [ ] Safe Refresh không ảnh hưởng timer/server state.
- [ ] Final Submit ghi `submit_payload.enc`.
- [ ] Parent ưu tiên final payload, không fallback autosave khi submit bình thường.
- [ ] Không có `Child process timeout! Forcing kill...`.
- [ ] Không có hai nút `Nộp Bài`.
- [ ] Không có thanh ngang đen.
- [ ] Rust exit code 0.
- [ ] Không còn process Java child/Rust treo sau submit.

Lệnh test đề xuất:

```powershell
cd D:\Ban_sao_du_an
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean install
powershell -ExecutionPolicy Bypass -File .\build_portable.ps1
cd D:\Ban_sao_du_an\dist\TutorHubSecureExam
.\run_input_test.bat --exam-id 3
```

## 18. Việc Tuyệt Đối Không Nên Làm

- Không mở Windows Settings.
- Không mở Control Panel.
- Không mở Network Flyout thật của Windows.
- Không cho connect/disconnect WiFi trong Secure Exam ở giai đoạn này.
- Không dùng `JOptionPane`, `JDialog`, modal native hoặc GlassPane trong active exam.
- Không dùng `SUBMIT_PAYLOAD` để route volume/brightness/wifi.
- Không poll PowerShell WMI brightness liên tục.
- Không để footer tự query OS lung tung.
- Không dùng asset/icon proprietary của Windows hoặc SEB.
- Không sửa Final Submit/Rust exit trong cùng phase với UI polish nếu không có test riêng.
- Không build lại kiến trúc lớn nếu chưa có baseline.
- Không commit khi chưa test phase tương ứng.

## 19. Kết Luận

TSE taskbar/Quick Settings hiện tại đã đi đúng hướng về bảo mật và trải nghiệm: UI nội bộ, không mở hệ thống thật, popup trong JCEF cho Exam và WebView cho Parent. Tuy nhiên để đạt độ ổn định giống sản phẩm lớn, cần dừng sửa lặt vặt từng slider và chuyển sang kiến trúc có `QuickSettingsStateStore`.

Thứ tự nên làm tiếp:

1. Chốt snapshot/schema chung.
2. Tạo state store.
3. Dồn volume/brightness/network/battery vào service layer.
4. Chuẩn hóa bridge Parent/Exam.
5. Sau đó mới polish DOM/CSS.
6. Cuối cùng mới chạy E2E portable/VM và release.

Nếu làm đúng lộ trình này, TSE sẽ có taskbar/Quick Settings ổn định hơn, dễ test hơn, gần tư duy SEB và Windows 11 hơn, đồng thời không phá các ràng buộc quan trọng như Final Submit, Rust exit code 0 và secure exam lockdown.

