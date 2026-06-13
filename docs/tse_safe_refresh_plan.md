# TSE Safe Refresh Plan

## 1. Scope
TSE sẽ cung cấp tính năng **Safe Refresh** trên `ExamHeaderBar`. Tính năng này sẽ:
- Thu thập và lưu lại trạng thái bài thi hiện tại (các trường nhập liệu) và chế độ gõ `VIE/ENG`.
- Khởi động lại (reload) bộ máy trình duyệt an toàn ở mức JCEF Core, không khởi động lại Java Process hay thoát Lockdown.
- Phục hồi lại dữ liệu bài thi (answers) và chế độ gõ ngay khi trình duyệt reload xong.
- Hiển thị overlay Loading tinh tế, không làm mất luồng tập trung của học sinh.

## 2. SEB Research Notes

### 1. SEB xử lý browser reload / navigation như thế nào?
SEB xử lý reload chủ yếu qua `SafeExamBrowser.Browser\BrowserWindow.cs` và `BrowserControl.cs`. Sự kiện `ReloadRequested` từ Keyboard (F5/Ctrl+R) hoặc JS sẽ gọi lệnh `RequestPageReload()`. Nếu được phép (dựa trên `WindowSettings.AllowReloading`), SEB sẽ gọi trực tiếp `Control.Reload()` của CefSharp/CefBrowser core. SEB không khởi động lại toàn bộ form browser.

### 2. SEB ngăn reload gây thoát exam/session như thế nào?
SEB không tái tạo các object quản lý Cef mà tận dụng CookieManager nội bộ (đã liên kết session với phiên thi thông qua `sessionMode`). Các request filter và rule của SEB vẫn nằm đó. Việc chỉ gọi `.Reload()` trên Cef browser control đảm bảo nó chỉ là một lệnh F5 thông thường, không phá vỡ ranh giới cửa sổ Kiosk/Lockdown.

### 3. SEB có tách browser state và exam session state không?
Có. Trạng thái phiên thi (Session verification, Quit policies) được quản lý riêng và không bị xóa bởi hành động tải lại nội dung web. Dữ liệu answer được cho là đồng bộ lên server hoặc lưu trong storage (localStorage/sessionStorage).

### 4. Bài học nào áp dụng cho TSE?
- Refresh ở TSE cũng chỉ nên gói gọn trong việc kích hoạt `.reload()` hoặc `executeJavaScript("location.reload();")` của JCEF Browser. Tuyệt đối không dispose hay tạo lại `TSEExamChildClient` hay `TSEBrowserPanel`.
- Cần có cơ chế "hỏi đáp hoặc chặn" như `RequestPageReload()` của SEB — tại TSE, chúng ta sẽ chặn nếu `finalSubmitInProgress == true`.

### 5. Điểm nào không nên copy từ SEB?
- SEB dùng Message Box hệ điều hành để hỏi "Bạn có muốn reload không?". Trên TSE, JCEF là một component nặng (heavyweight), nên OS popup có thể gặp lỗi bị đè (z-order bug). Vì thế TSE sẽ dùng một Overlay DOM nội bộ (HTML/JS) mỏng, đẹp để báo "Đang làm mới...".

## 3. Current Refresh Behavior
Nút `btnRefresh` trong `ExamHeaderBar.java` đã được tạo nhưng chưa gắn bất kỳ hàm Callback hay `ActionListener` nào. Khi nhấn vào, nút chưa có phản ứng.

## 4. Current Answer Collection Flow
TSE hiện dựa vào việc Java gọi vào JS: `executeJavaScript("collectTSEAnswers();")`. Hàm này (được web app định nghĩa) sẽ gom câu trả lời và bắn `window.cefQuery({request: 'SUBMIT_PAYLOAD:'...})` về lại Java. Java sẽ chặn gói này để ghi thành `autosave_payload.enc`.

## 5. Current Timer / Session Flow
- `sessionId`, `examId` được truyền từ Parent sang Child thông qua file `exam_context.enc`. Java nạp nó một lần và giữ cố định không đổi trong toàn bộ vòng đời ứng dụng Child.
- Timer đang được Java lấy tĩnh từ chuỗi "45:00" hoặc do JS tự đếm ngược. Trong mọi trường hợp, reload JCEF có thể làm reset JS timer, nhưng vì Java đang quản lý `sessionId`, chúng ta cần cẩn trọng. JS cần khôi phục lại timer thông qua Local/Session Storage hoặc liên lạc server. (Phần này sẽ áp dụng lưu timer vào sessionStorage).

## 6. Risks
- **Mất bài làm**: Khi `.reload()`, JS sẽ mất các câu học sinh vừa click. Phải lưu vào `sessionStorage` trước khi tải.
- **Mất cấu hình Input Mode**: Cơ chế gõ Tiếng Việt nội bộ cần được lưu và kích hoạt lại tự động sau khi trang load xong.

## 7. Proposed Safe Refresh Flow
Khi học sinh bấm Refresh:
1. Ghi log `[TSE_CONTROL] Refresh clicked.` và `[TSE_REFRESH] Refresh requested.`.
2. Kiểm tra `finalSubmitInProgress`, nếu `true` thì `return` (bỏ qua).
3. Java gọi `browserPanel.executeJavaScript("window.TSESafeRefresh.triggerSnapshotAndReload();")`.
4. Trong JS (`tse-safe-refresh.js`):
   - Gom snapshot của các thẻ `<input>`, `<textarea>`, `<select>` và `window.TSEInputMode` lưu vào `sessionStorage`.
   - Hiển thị màn đen mờ "Đang làm mới...".
   - Gọi `location.reload()`.
5. Khi JCEF load lại trang, luồng inject mặc định của Java sẽ chèn lại `tse-safe-refresh.js`.
6. JS tự động nhận diện `sessionStorage` có dữ liệu:
   - Nó chạy hàm khôi phục: điền lại các giá trị answer.
   - Gọi `window.cefQuery({request: 'TSE_LANG_SELECT:VIE'})` để đồng bộ nút Footer trên Java nếu cần (hoặc chỉ cần áp dụng lại bộ gõ).
   - Xóa overlay.
   - Xóa `sessionStorage`.
7. Ghi log `[TSE_REFRESH] Refresh completed.`.

## 8. Implementation Plan
- Sửa `ExamHeaderBar.java` để thêm `Runnable onRefresh`.
- Sửa `TSEExamChildClient.java` để inject `tse-safe-refresh.js` và bind luồng onRefresh.
- Viết mới `src/main/resources/tse/tse-safe-refresh.js` với logic snapshot + overlay + restore.

## 9. Test Plan
- Cố tình điền test, gõ tiếng Việt.
- Bấm Refresh và chờ xem JCEF có load lại mà không mất test state không.
- Test Submit sau khi refresh để đảm bảo CEFQuery chưa hỏng.

## 10. Acceptance Criteria
- Cấu trúc không mất Answer, không mất Input Mode, và không vỡ Layout Windows 11 Tray.
- Có log TSE_REFRESH và TSE_CONTROL rõ ràng.
- `sessionId` và cửa sổ không khởi động lại.
