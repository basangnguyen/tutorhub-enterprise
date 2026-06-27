# Locket Popup Visual Match Polish Report

Ngay thuc hien: 2026-06-25

## 1. Da doc lai ngu canh/report chua

Co. Da doc:
- docs/locket_popup_web_full_rework_report.md
- docs/locket_global_feed_web_popup_direction_fix.md
- src/main/resources/locket-web/locket-popup.html, .css, .js (baseline cu)
- src/main/java/.../LocketWebPopupDialog.java
- src/main/java/.../HomeTab.java

## 2. Da chinh popup giong anh mau thu 2 chua

Co. Toan bo layout 3 cot da duoc redesign theo anh mau thu 2.

## 3. Da sua file nao

- src/main/resources/locket-web/locket-popup.html (Rewrite hoan toan)
- src/main/resources/locket-web/locket-popup.css (Rewrite hoan toan)
- src/main/resources/locket-web/locket-popup.js (Rewrite hoan toan)
- src/main/java/.../LocketWebPopupDialog.java (Tang kich thuoc popup)

## 4. Kich thuoc popup hien tai

Truoc: Fixed 980x680 px.
Sau: 88% chieu rong man hinh x 86% chieu cao man hinh, toi thieu 880x600.

## 5. Cot trai da giong mau chua

Co. Card "Tao bai moi" voi icon + tron tim pastel o giua, label ben duoi.
Section "Gan day" voi avatar tron + cham tim + ten + "Vua xong".

## 6. Khu giua da giong mau chua

Co. Header avatar tron + ten + thoi gian + nut ... goc phai.
Anh lon bo goc 20px. Nut prev/next tron. Caption input. Social strip.

## 7. Dai cam xuc/nhan tin da giong mau chua

Co. 7 emoji reactions: like heart love haha wow sad angry.
Nut Nhan tin co icon chat, bo goc 999px, border tim nhat.
Camera mode an social-strip va viewer-meta.

## 8. Cot phai da giong mau chua

Co. 5 nut doc: Phat, Anh, OK (tim lon), Tuy chon, Thoat (do).
Khong con nut Xoa.

## 9. Da doi Xoa thanh Thoat chua

Co. HTML moi chi dung "Thoat".

## 10. Nut Phat hoat dong the nao

toggleSlideshow() bat/tat setInterval(nextPhoto, 5000). Button active class khi bat.
Bam Thoat tu dong stopSlideshow() truoc emit LOCKET_CLOSE.

## 11. Nut Anh hoat dong the nao

Gui LOCKET_PICK_IMAGE sang Java. Java mo JFileChooser, validate jpg/jpeg/png/webp <= 8MB.
Convert JPEG, tao base64, gui onPickedImage(dataUrl, fileName) cho JS preview.

## 12. Nut OK/camera hoat dong the nao

Che do xem anh: bam OK -> LOCKET_CAMERA_START -> camera mode.
Camera mode: bam OK -> LOCKET_CAMERA_CAPTURE -> chup anh, dong webcam, preview.
Neu khong co webcam: hien loi than thien.

## 13. Camera mode an cam xuc/nhan tin chua

Co. CSS: .viewer-panel.camera-mode .social-strip, .viewer-meta { display: none; }

## 14. Nhan tin chuyen tab the nao

JS emit LOCKET_MESSAGE_OPEN -> Java goi messageCallback -> MainDashboard chuyen sang Chat.

## 15. Co con classId/chon lop khong

Khong. Da kiem tra toan bo code trong pham vi chinh sua. Khong co classId, khong co chon lop.

## 16. Co DROP/TRUNCATE/DELETE nguy hiem khong

Khong.

## 17. Build/test ket qua

node --check: PASS (ca home-social.js va locket-popup.js)
Maven: BUILD SUCCESS (25.298 s)

## 18. update.jar da copy chua

Co. Da copy vao D:\Ban_sao_du_an\HF_UPLOAD\update.jar.

## 19. Rui ro con lai

- Emoji co the khong hien dung neu JavaFX WebView thieu font emoji Windows.
- Camera framerate co the lag tren may yeu (stream moi 160ms).
- messageCallback chi hoat dong neu HomeTab truyen callback day du khi mo popup.
- Chua test thu cong GUI tren runtime that.
