# TSE Brightness Full Research Fix

## 1. Tong quan loi

BrightnessService da probe duoc WMI va co log `supported=true`, `writable=true`, nhung thao tac keo slider trong Quick Settings khong di den Java backend. Chuoi log bi dung truoc cac diem:

- Khong co `[TSE_QS_PARENT_JS] brightness input/change`.
- Khong co `[TSE_PARENT_QS_HTML] setBrightnessCommand raw=...`.
- Khong co `[TSE_QS_CONTROLLER] setBrightness ...`.
- Khong co `[TSE_BRIGHTNESS_SERVICE] setBrightness ...`.

Ket luan sau audit: loi chinh nam o tang UI event/bridge, khong phai tang WMI. Rieng Parent popup con co them rui ro outside-click listener dong popup khi nguoi dung click/drag ben trong WebView.

## 2. Log ban dau

Log truoc khi sua cho thay service khoi tao thanh cong:

```text
[TSE_BRIGHTNESS_SERVICE] initialize
[TSE_BRIGHTNESS_SERVICE] supported=true
[TSE_BRIGHTNESS_SERVICE] writable=true
[TSE_PARENT_QS_HTML] WebView load SUCCEEDED
[TSE_PARENT_QS_HTML] HTML ready. Triggering JS init.
[TSE_PARENT_QS_JS] [TSE_QS_PARENT_JS] brightness slider FOUND value=23
```

Nhung khi keo slider khong thay event/bridge/backend logs. Ngoai ra co log:

```text
[TSE_PARENT_QS_HTML] Outside click detected. Hiding popup.
[TSE_PARENT_QS_HTML] Popup hidden.
```

## 3. Ket qua resource JS loaded

Da them version marker:

- Parent: `[TSE_QS_PARENT_JS] LOADED version=FULL_BRIGHTNESS_RESEARCH_FIX`
- Exam: `[TSE_QS_EXAM_JS] LOADED version=FULL_BRIGHTNESS_RESEARCH_FIX`

Da build portable va kiem tra JAR:

```powershell
cd D:\Ban_sao_du_an\dist\TutorHubSecureExam
jar tf .\app\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar | findstr /i "parent-quick-settings tse-tray-flyout"
```

Ket qua co resource:

```text
tse/quick-settings/parent-quick-settings.css
tse/quick-settings/parent-quick-settings.html
tse/quick-settings/parent-quick-settings.js
tse/tse-tray-flyout.js
```

Da extract resource tu JAR portable va xac nhan co:

```text
parent-quick-settings.js: [TSE_QS_PARENT_JS] LOADED version=FULL_BRIGHTNESS_RESEARCH_FIX
parent-quick-settings.js: brightness delegation installed
parent-quick-settings.js: debugBrightnessSlider
tse-tray-flyout.js: [TSE_QS_EXAM_JS] LOADED version=FULL_BRIGHTNESS_RESEARCH_FIX
tse-tray-flyout.js: installExamBrightnessDelegation
tse-tray-flyout.js: brightness delegation installed
```

## 4. Slider selector that

Parent Quick Settings slider that:

```html
<div id="slider-brightness"
     class="custom-slider brightness-slider"
     data-role="brightness-slider"
     name="brightness"
     tabindex="0">
```

Day la custom `div`, khong phai `input[type=range]`. Track va thumb con:

```html
<div id="slider-brightness-track" ... pointer-events: none></div>
<div id="slider-brightness-thumb" ... pointer-events: none></div>
```

`pointer-events:none` chi nam tren track/thumb de click di ve container, day la hop le. Container slider khong bi set `pointer-events:none`.

Exam Quick Settings slider that sau khi sua:

```html
<input type="range"
       id="exam-slider-brightness"
       class="brightness-slider"
       data-role="brightness-slider"
       name="brightness">
```

## 5. Slider debug: disabled / pointer-events / elementFromPoint

Da them debug:

- Parent: `debugBrightnessSlider()`
- Exam: `debugExamBrightnessSlider()`

Log mong doi khi popup mo:

```text
[TSE_QS_PARENT_JS] brightness slider FOUND id=slider-brightness disabled=false pointerEvents=auto rect=... topElement=...
[TSE_QS_EXAM_JS] brightness slider FOUND id=exam-slider-brightness disabled=false pointerEvents=auto rect=... topElement=...
```

Neu test VM thay:

- `disabled=true`: loi mapping snapshot/service writable.
- `pointerEvents=none`: loi CSS.
- `topElement` khong phai slider hoac thanh phan hop le cua slider: co overlay chan pointer.

## 6. Root cause chinh xac

1. Parent slider that la custom `div#slider-brightness`, trong khi huong fix cu va nhieu audit truoc do mac dinh slider la `input[type=range]`. Ket qua la listener `input/change` khong bat duoc thao tac tren Parent slider.
2. Parent popup dung JavaFX WebView nam trong Swing JWindow. AWT outside-click listener co the nhan click/drag trong WebView nhu outside click, lam popup bi an truoc khi chuoi event brightness commit xuong Java.
3. Parent JS co rui ro state update/poll render lai UI luc dang keo slider, lam mat gesture/draft value.
4. Exam JCEF dung `innerHTML` de render flyout. Neu chi bind truc tiep len node cu thi listener co the mat sau render. Can delegation capture va bind sau render.
5. `TSEQuickSettingsManager.setBrightness` co rui ro queue pending command: goi lai `setBrightness()` khi `brightnessSetInProgress` van true co the lam pending command bi ghi de/mat. Da sua thanh worker loop xu ly latest pending value.

## 7. Bang audit 15 cau

| # | Cau hoi | Ket qua |
|---|---|---|
| 1 | Parent brightness slider nam o HTML nao? | `src/main/resources/tse/quick-settings/parent-quick-settings.html`. |
| 2 | ID/class/data-role that la gi? | `id=slider-brightness`, `class=custom-slider brightness-slider`, `data-role=brightness-slider`, `name=brightness`. |
| 3 | JS moi co load tu JAR/portable khong? | Co. Da extract portable JAR va thay version marker/delegation. |
| 4 | Event listener bind truoc hay sau render DOM? | Parent dung document capture delegation nen khong phu thuoc node cu. Exam goi delegation sau `this.show(...)` va cung la document capture. |
| 5 | Co renderState/updateState lam mat listener khong? | Listener delegation khong mat. Parent `updateState()` skip full render khi `brightnessDragging=true`. |
| 6 | Slider co disabled khong? | Theo code path, disabled duoc set theo snapshot support/writable. Debug log se in `disabled=...`. Chua chay GUI/VM de xac nhan tren may test. |
| 7 | CSS co pointer-events none khong? | Track/thumb co `pointer-events:none`, container khong. Debug log se in computed style. |
| 8 | Co overlay de len slider khong? | Chua thay trong source. Debug `elementFromPoint` da them de xac nhan khi test GUI. |
| 9 | Outside-click listener co dong popup khi click/drag WebView khong? | Co rui ro theo log ban dau. Da them `isInsidePopup()` va 700ms activity guard. |
| 10 | Parent bridge expose dung `setBrightnessCommand` khong? | Co. Bridge log raw payload, parse JSON/plain number, clamp percent, goi controller. |
| 11 | Exam JCEF command `TSE_BRIGHTNESS_SET` co duoc gui khong? | JS da gui `TSE_BRIGHTNESS_SET:<percent>` qua `cefQuery`; can VM GUI test de xac nhan log runtime. |
| 12 | `TSEJcefLifecycleManager` route dung khong? | Co route `TSE_BRIGHTNESS_SET:` va log `[TSE_JCEF_BRIDGE] request=...`. |
| 13 | `TSEQuickSettingsManager` co goi controller khong? | Co. Da sua queue va goi `controller.setBrightness(...)`. |
| 14 | `QuickSettingsController` co dung injected `BrightnessService` khong? | Co log `brightnessServiceInjected=true/false`; Parent/Exam deu inject service khi khoi tao. |
| 15 | `BrightnessService.setBrightness` co chay that khong? | Code path co log va goi WMI. Can test tren may/VM co brightness WMI de xac nhan man hinh doi sang that. |

## 8. File da sua

- `src/main/resources/tse/quick-settings/parent-quick-settings.html`
- `src/main/resources/tse/quick-settings/parent-quick-settings.js`
- `src/main/resources/tse/tse-tray-flyout.js`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEParentHtmlQuickSettingsPopup.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEQuickSettingsManager.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/BrightnessService.java`

Khong sua Rust. Khong sua Final Submit. Khong polish UI. Khong chuyen Phase 10.

## 9. Parent brightness test

Da test tinh:

- `node --check parent-quick-settings.js`: pass.
- Maven compile/test: pass.
- Portable JAR co JS moi: pass.

Chua chay GUI Parent brightness tren may chinh vi Secure Exam/lockdown flow phai test tren VM theo `AGENTS.md`. Khi test VM, can thay chuoi log:

```text
[TSE_QS_PARENT_JS] LOADED version=FULL_BRIGHTNESS_RESEARCH_FIX
[TSE_QS_PARENT_JS] brightness delegation installed
[TSE_QS_PARENT_JS] brightness slider FOUND ...
[TSE_QS_PARENT_JS] brightness pointerdown value=...
[TSE_QS_PARENT_JS] brightness input percent=...
[TSE_QS_PARENT_JS] brightness pointerup commit percent=...
[TSE_QS_PARENT_JS] send setBrightnessCommand payload=...
[TSE_PARENT_QS_HTML] setBrightnessCommand raw=...
[TSE_QS_CONTROLLER] setBrightness percent=...
[TSE_BRIGHTNESS_SERVICE] setBrightness percent=...
```

## 10. Exam brightness test

Da test tinh:

- `node --check tse-tray-flyout.js`: pass.
- Maven compile/test: pass.
- Portable JAR co JS moi: pass.

Chua chay GUI Exam brightness tren may chinh vi lockdown/secure desktop phai test VM. Khi test VM, can thay:

```text
[TSE_QS_EXAM_JS] LOADED version=FULL_BRIGHTNESS_RESEARCH_FIX
[TSE_QS_EXAM_JS] brightness delegation installed
[TSE_QS_EXAM_JS] brightness slider FOUND ...
[TSE_QS_EXAM_JS] brightness input percent=50
[TSE_QS_EXAM_JS] brightness change percent=50
[TSE_QS_EXAM_JS] send command=TSE_BRIGHTNESS_SET:50
[TSE_JCEF_BRIDGE] request=TSE_BRIGHTNESS_SET:50
[TSE_QS_EXAM] setBrightness percent=50
[TSE_QS_CONTROLLER] setBrightness percent=50, brightnessServiceInjected=true
[TSE_BRIGHTNESS_SERVICE] setBrightness percent=50
```

## 11. Final Submit / Rust result

Khong sua Final Submit va khong sua Rust. Chua chay E2E Final Submit/Rust exit tren may chinh do rule khong test lockdown that ngoai VM.

Can verify trong VM:

```text
[TSE_PARENT] Found submit_payload.enc. Using FINAL submit payload.
Submit SUCCESS
Rust exit code = 0
```

Khong duoc co:

```text
WARNING: submit_payload.enc not found. Falling back to autosave_payload.enc.
Child process timeout! Forcing kill...
```

## 12. Process cleanup result

Build/test command-line khong tao Rust lockdown process. Chua chay GUI lockdown nen khong co ket luan process cleanup E2E. Sau test VM, can kiem tra khong con process Java/Rust/PowerShell treo.

## 13. Lenh da chay

```powershell
node --check D:\Ban_sao_du_an\src\main\resources\tse\quick-settings\parent-quick-settings.js
node --check D:\Ban_sao_du_an\src\main\resources\tse\tse-tray-flyout.js
cd D:\Ban_sao_du_an
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean install
powershell -ExecutionPolicy Bypass -File .\build_portable.ps1
jar tf .\dist\TutorHubSecureExam\app\TutorHub_Maven-1.0-SNAPSHOT-jar-with-dependencies.jar | findstr /i "parent-quick-settings tse-tray-flyout"
```

Ket qua:

- JS syntax: pass.
- Maven: `BUILD SUCCESS`, 22 tests pass.
- Portable build: pass.
- JAR resource/version/delegation check: pass.

## 14. Ket luan co duoc chuyen sang Phase 10 khong

Khong. Task nay chi la Brightness research/fix trong Quick Settings. Con thieu VM/E2E manual test cho brightness that, Final Submit va Rust cleanup, nen khong duoc chuyen Phase 10.
