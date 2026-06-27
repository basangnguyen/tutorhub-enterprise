# TSE Parent Quick Settings UI Sync Phase

## 1. Muc tieu chinh UI

Dong bo Quick Settings popup o man hinh Login/Configuration theo popup Exam:

- Bo cam giac co mot khung/hinh chu nhat boc ngoai popup.
- Giu panel toi, bo goc mem, shadow va padding theo tinh than Windows 11 cua Exam popup.
- Xoa icon sun o goc phai tren man hinh Login/Configuration.
- De icon help nam tai vi tri ngoai cung ben phai, thay cho vi tri icon sun cu.
- Khong thay doi logic Brightness, Volume, WiFi, Battery, Rust, kiosk flow hoac Final Submit.

## 2. File da sua

- `src/main/resources/tse/quick-settings/parent-quick-settings.css`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSELoginPanel.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEConfigListPanel.java`
- `docs/tse_parent_popup_ui_sync_phase.md`

## 3. So sanh truoc/sau Login popup

Truoc:

- `html, body` cua Parent popup co nen xam `#3B3B3B`, tao cam giac popup bi boc trong mot hinh chu nhat.
- Panel Parent dung radius lon hon Exam.
- Goc phai tren cua Login/Configuration co ca help icon va sun icon.

Sau:

- `html, body` transparent de chi con thay panel Quick Settings.
- Panel Parent dung nen toi `rgba(32, 32, 32, 0.85)`, blur, shadow, border va radius gan voi Exam popup.
- Top bar chi con help icon, va icon nay tu canh ra vi tri ngoai cung ben phai.

## 4. Cach dong bo theo Exam popup

Exam popup trong `src/main/resources/tse/tse-tray-flyout.js` dung:

- Panel toi `rgba(32, 32, 32, 0.85)`.
- `border-radius: 12px`.
- `box-shadow: 0 8px 32px rgba(0,0,0,0.4)`.
- `padding: 16px`.
- Border mo `rgba(255, 255, 255, 0.1)`.

Parent popup CSS da duoc can theo cac gia tri nay, nhung khong doi cau truc DOM hay handler slider.

## 5. Cach xoa vien ngoai

Nguyen nhan vien ngoai nam o CSS Parent:

```css
html, body {
    background: #3B3B3B;
}
```

Da doi thanh:

```css
html, body {
    background: transparent;
}
```

JWindow/JFXPanel/WebView hien da co nen trong suot, nen thay doi nay loai bo hinh chu nhat boc ngoai ma khong can doi kich thuoc popup.

## 6. Cach xoa icon anh sang goc phai

Da xoa dong add icon `sun.svg` trong:

- `TSELoginPanel.buildTopBar()`
- `TSEConfigListPanel.buildTopBar()`

Khong xoa icon brightness ben trong Quick Settings popup.

## 7. Cach doi icon dau cham hoi

Sau khi xoa sun icon, panel ben phai chi con help icon. Vi container duoc dat o cell ben phai cua top bar, help icon tu dong nam tai vi tri ngoai cung ben phai, tuong duong vi tri sun icon cu.

## 8. Test build

Can chay:

```powershell
cd D:\Ban_sao_du_an
& "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" clean install
powershell -ExecutionPolicy Bypass -File .\build_portable.ps1
```

## 9. Test UI Login/Configuration

Can kiem tra tren VM/test-safe:

```powershell
cd D:\Ban_sao_du_an\dist\TutorHubSecureExam
.\run_input_test.bat --exam-id 3
```

Checklist thu cong:

- Popup Quick Settings mo duoc.
- Popup khong blank/trang.
- Khong con nen xam/hinh chu nhat boc ngoai.
- Brightness keo duoc va popup khong tu dong dong.
- Volume/mute van hoat dong.
- WiFi/Battery/Clock van hien thi dung.
- Sun icon tren top bar da bien mat.
- Help icon nam ngoai cung ben phai.

## 10. Test regression Exam

Can kiem tra tren VM/test-safe:

- Exam popup van giu giao dien cu.
- Brightness/Volume/WiFi/Battery van hoat dong.
- Power/Exit van bi block trong Exam.
- Final Submit SUCCESS.
- Rust exit code = 0.

## 11. Loi con lai neu co

- Chua the chay GUI lockdown test trong phien Codex nay neu moi truong hien tai la may Lenovo vat ly.
- Can VM visual confirmation de xac nhan Parent popup khong con vien ngoai va help icon da dung vi tri.

## 12. Phase UI Polish Acceptance Test Result

Ngay kiem tra: 2026-06-17

Trang thai moi truong:

- Phien Codex hien tai dang chay tren may LENOVO 83DV, BIOS Lenovo NECN51WW.
- Theo rule du an, khong chay Secure Exam lockdown GUI tren may chinh/physical Lenovo.
- Vi vay `run_input_test.bat --exam-id 3` khong duoc chay trong phien nay.

Ket qua static verification:

- Parent popup CSS co `html, body { background: transparent; }`.
- Parent popup panel dung nen `rgba(32, 32, 32, 0.85)`, blur, shadow va `border-radius: 12px`.
- Portable JAR co resource `tse/quick-settings/parent-quick-settings.css`.
- `TSELoginPanel.java` khong con add `sun.svg`, chi con `circle-help.svg`.
- `TSEConfigListPanel.java` khong con add `sun.svg`, chi con `circle-help.svg`.

Ket qua VM acceptance:

1. Login popup result: Pending VM visual test.
2. Configuration popup result: Pending VM visual test.
3. Icon sun/help result: Static PASS; VM visual confirmation pending.
4. Exam regression result: Pending VM test.
5. Final Submit result: Pending VM test.
6. Rust exit code: Pending VM test.
7. Process cleanup result: No Secure Exam VM run was started in this session; cleanup acceptance pending VM test.
8. Ket luan: Chua du dieu kien chuyen phase tiep theo cho den khi chay acceptance trong VM test-safe va co ket qua Final Submit/Rust/process cleanup.

## 13. White Host Frame Follow-up Fix

Ngay sua: 2026-06-17

Hien tuong:

- VM screenshot cho thay Parent/Login Quick Settings popup van con mot khung mau trang phia sau panel toi.
- CSS `html, body` da transparent, nen khung trang khong phai la vien/padding CSS cua panel.
- Nguyen nhan kha nang cao nam o JavaFX `WebView`/`JFXPanel` host surface van paint nen mac dinh mau trang.

File da sua:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEParentHtmlQuickSettingsPopup.java`

Thay doi:

- Dat `JFXPanel` background thanh mau trong suot.
- Dat `WebView.setPageFill(Color.TRANSPARENT)`.
- Dat `WebView` style `-fx-background-color: transparent;`.

Pham vi:

- Chi tac dong Parent/Login/Configuration HTML Quick Settings popup.
- Khong doi HTML/CSS layout cua slider.
- Khong doi AWT brightness fallback coordinates.
- Khong sua Rust.
- Khong sua Final Submit.

Build verification:

- `mvn clean install`: PASS, 22 tests, 0 failures.
- `build_portable.ps1`: PASS.

VM visual verification:

- Pending user/VM confirmation: khung trang phia sau popup da bien mat.

## 14. Parent Popup Opaque Panel Follow-up Fix

Ngay sua: 2026-06-17

Hien tuong:

- VM screenshot xac nhan khung trang phia sau da bien mat.
- Tuy nhien panel popup van qua trong suot, nen thay duoc chi tiet anh nen phia sau.
- `backdrop-filter` khong che nen tot trong JavaFX transparent window nhu JCEF/browser that.

File da sua:

- `src/main/resources/tse/quick-settings/parent-quick-settings.css`

Thay doi:

- Doi nen `#tse-tray-flyout` tu `rgba(32, 32, 32, 0.85)` sang `#202020`.
- Giu host transparent de khong lam khung trang quay lai.
- Giu kich thuoc, margin, padding, slider layout va AWT brightness fallback coordinates.

Build verification:

- `mvn clean install`: PASS, 22 tests, 0 failures.
- `build_portable.ps1`: PASS.
- Portable JAR da chua `background: #202020` trong `tse/quick-settings/parent-quick-settings.css`.

VM visual verification:

- Pending user/VM confirmation: popup khong con nhin xuyen chi tiet anh nen.

## 15. Parent Popup Host Size Match Follow-up Fix

Ngay sua: 2026-06-17

Hien tuong:

- VM screenshot cho thay van con mot khung/halo mo phia sau popup.
- Nguyen nhan la panel toi nam ben trong WebView host voi `margin: 16px`, trong khi host window la `360x280`.
- Vung host lon hon panel nen co the tao cam giac co khung phia sau.

File da sua:

- `src/main/resources/tse/quick-settings/parent-quick-settings.css`

Thay doi:

- Cho `#tse-tray-flyout` phu kin kich thuoc host: `width: 360px`, `height: 280px`.
- Doi `margin` cua panel tu `16px` thanh `0`.
- Doi `padding` tu `16px` thanh `32px` de noi dung ben trong giu gan dung vi tri cu.
- Doi outer shadow thanh `inset` de khong tao bong/khung mo phia sau.
- Giu `html, body` transparent de khong lam khung trang quay lai.

Ly do khong doi AWT brightness fallback:

- Truoc do: host margin 16 + panel padding 16 = noi dung bat dau khoang 32px.
- Sau sua: host margin 0 + panel padding 32 = noi dung van bat dau khoang 32px.
- Vi vay slider geometry gan nhu duoc giu nguyen, khong can doi constants trong `TSEParentHtmlQuickSettingsPopup.java`.

Build verification:

- `mvn clean install`: PASS, 22 tests, 0 failures.
- `build_portable.ps1`: PASS.
- Portable JAR da chua CSS moi: `width: 360px`, `height: 280px`, `padding: 32px`, `margin: 0`.

VM visual verification:

- Pending user/VM confirmation: khung/halo mo phia sau popup da bien mat.

## 16. Parent Popup Bottom Clip Follow-up Fix

Ngay sua: 2026-06-17

Hien tuong:

- VM screenshot cho thay phan day cua popup bi cat mat mot so net.
- Dong Battery/Charging va nut settings o goc duoi khong hien day du.
- Nguyen nhan la host/panel sau khi khop size chi cao `280px`, trong khi noi dung thuc te can them khoang dem day.

File da sua:

- `src/main/resources/tse/quick-settings/parent-quick-settings.css`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEParentHtmlQuickSettingsPopup.java`

Thay doi:

- Tang `html, body` height tu `280px` len `320px`.
- Tang `#tse-tray-flyout` height tu `280px` len `320px`.
- Tang `POPUP_HEIGHT` trong Java tu `280` len `320` de JWindow/JFXPanel/WebView dong bo voi CSS.
- Giu `width: 360px`, `margin: 0`, `padding: 32px`, nen `#202020`.

Ly do khong doi AWT brightness fallback:

- Thay doi chi tang chieu cao theo truc Y ve phia duoi.
- X/Y origin cua noi dung va slider brightness khong doi.
- `BRIGHTNESS_SLIDER_X/Y/WIDTH/HEIGHT` tiep tuc phu hop voi layout hien tai.

Build verification:

- `mvn clean install`: PASS, 22 tests, 0 failures.
- `build_portable.ps1`: PASS.
- Portable JAR da chua CSS moi: `height: 320px` cho `html, body` va `#tse-tray-flyout`.
- Source Java da dung `POPUP_HEIGHT = 320`.

VM visual verification:

- Pending user/VM confirmation: day popup khong con bi cat.

## 17. Parent Popup Size Sync with Exam Popup

Ngay sua: 2026-06-17

Muc tieu:

- Dong bo kich thuoc popup Quick Settings cua Login/Configuration voi popup trong Exam/JCEF.
- Dung Exam popup lam source of truth, khong tiep tuc can chinh theo cam tinh tu screenshot.

Ket qua do Exam popup bang headless Chrome:

- Visible panel: `354 x 285`.
- CSS content box: `width: 320px`, `height: 251px`.
- Padding: `16px`.
- Border: `1px`.
- Box sizing: `content-box`.

File da sua:

- `src/main/resources/tse/quick-settings/parent-quick-settings.css`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/exam/ui/TSEParentHtmlQuickSettingsPopup.java`

Thay doi:

- Parent host `html, body`: `354 x 285`.
- Parent `#tse-tray-flyout`: `width: 320px`, `height: 251px`, `padding: 16px`, `box-sizing: content-box`.
- Java `POPUP_WIDTH = 354`, `POPUP_HEIGHT = 285`.
- Cap nhat AWT brightness fallback theo slider render moi: `x=71`, `y=127`, `width=204`.

Ghi chu:

- Parent van giu background opaque `#202020` de tranh hien tuong nhin xuyen anh nen da duoc phat hien truoc do.
- Khong sua Rust.
- Khong sua Final Submit.

Static render verification:

- Exam popup panel: `354 x 285`.
- Parent popup panel: `354 x 285`.
- Parent bottom bar visible: PASS.

Build verification:

- `mvn clean install`: PASS, 22 tests, 0 failures.
- `build_portable.ps1`: PASS.
- Portable JAR da chua Parent popup CSS moi: `354x285`, content box `320x251`, padding `16px`.

VM visual verification:

- Pending user/VM confirmation: Login/Configuration popup co cung kich thuoc voi Exam popup.
