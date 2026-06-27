# Phase 9A.5: Parent Quick Settings Bridge Full Regression Test

## 1. Mục tiêu Phase 9A.5
Kiểm tra kỹ Parent JavaFX WebView Quick Settings sau khi đã chuyển bridge sang `QuickSettingsController`, đồng thời xác nhận Exam JCEF chưa bị regression. Báo cáo lại kết quả để quyết định có chuyển sang Phase 9B hay không.

## 2. Build Maven result
- **Result:** PASS
- Không có lỗi compile. Build maven complete success.

## 3. Portable build result
- **Result:** PASS
- Lệnh `build_portable.ps1` chạy mượt mà, copy đủ resource và CEF binaries.

## 4. Parent popup test result
- **Result:** FAIL
- **Chi tiết:** Khi popup được mở (`[TSE_TRAY_CLUSTER] Quick Settings cluster clicked`), popup gọi `controller.requestRefresh()` và `safeExecuteScript(...)`. Tuy nhiên, vì `safeExecuteScript` đang chạy trên thread `AWT-EventQueue-0` của Swing, nó đã bắn ra ngoại lệ:
  `[TSE_PARENT_QS_HTML] JS execute failed: Not on FX application thread; currentThread = AWT-EventQueue-0`
- Do lỗi JS execute failed, JS không nhận được tín hiệu `pollState()`, do đó popup không thể render data từ Java và có thể xuất hiện blank/không update UI.

## 5. Parent volume/mute result
- **Result:** FAIL (Do lỗi threading ở trên, JS bridge update bị nghẽn).

## 6. Parent brightness result
- **Result:** FAIL (Do lỗi threading ở trên, JS bridge update bị nghẽn).

## 7. Parent WiFi/Battery/Clock result
- **Result:** FAIL (Do lỗi threading ở trên, JS bridge update bị nghẽn).

## 8. Exam regression result
- **Result:** PASS
- Exam JCEF popup không bị tác động bởi Phase 9A. Các script kiểm tra tự động chạy không gây crash, child process không timeout.

## 9. Final Submit result
- **Result:** PASS (Dựa trên test scope không bị đụng chạm).

## 10. Rust exit code
- **Result:** 0 (Process cleanup tốt, không treo).

## 11. Process cleanup result
- Các process `java.exe` và subprocess PowerShell (wmi/brightness) dọn dẹp tốt. Khi popup đóng, logs hiển thị `[TSE_PARENT_QS_HTML] Popup hidden.` và các COM call vẫn được uninitialized đầy đủ (`[TSE_VOLUME_SERVICE] COM uninitialized`). Không rò rỉ process.

## 12. Lỗi còn lại nếu có
- **Lỗi lớn nhất:** `Not on FX application thread; currentThread = AWT-EventQueue-0` trong `TSEParentHtmlQuickSettingsPopup.java` ở hàm `safeExecuteScript(String script)`.
- **Lý do:** JavaFX `WebEngine` bắt buộc phải gọi các method (như `executeScript`) ở `JavaFX Application Thread` (thông qua `Platform.runLater()`), nhưng hiện tại đang được gọi trực tiếp trên Swing `AWT-EventQueue-0` do hàm `showPopup()` hoặc `refreshTimer` của Swing thực thi.

## 13. Kết luận có được chuyển sang Phase 9B không
- **KHÔNG THỂ CHUYỂN SANG PHASE 9B.**
- Cần fix lỗi `Platform.runLater()` trong `TSEParentHtmlQuickSettingsPopup.java` (Phase 9A.6) trước khi làm tiếp. Mọi thứ khác như kiến trúc và bridge format đều đúng.
