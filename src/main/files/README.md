# TutorHub — Map Address Picker (MapLibre + OpenFreeMap + Goong + JCEF)

Bộ 3 file để nhúng dialog **chọn địa chỉ trên bản đồ** vào app Java Swing
đã có sẵn JCEF của bạn.

## Stack đã chọn

| Thành phần        | Công nghệ                          | Cần API key? |
|--------------------|-------------------------------------|--------------|
| Render bản đồ      | MapLibre GL JS 4.7.1 (CDN)          | Không        |
| Tile bản đồ        | OpenFreeMap (`tiles.openfreemap.org/styles/liberty`) | Không |
| Tìm kiếm / reverse geocode địa chỉ VN | Goong Geocoding + Place API (`rsapi.goong.io`) | **Có** — đăng ký free tại https://account.goong.io |

→ Đúng theo yêu cầu: bản đồ miễn phí hoàn toàn, chỉ phần tìm-kiếm-địa-chỉ
(thế mạnh của Goong cho địa chỉ VN) là cần key riêng.

## Cấu trúc file

```
resources/mappicker/map_picker.html      ← UI bản đồ (HTML/CSS/JS)
src/com/tutorhub/mappicker/
    MapAddressResult.java                ← POJO kết quả (address, lat, lng)
    MapPickerDialog.java                 ← JDialog dùng JCEF, cầu nối JS<->Java
    MapPickerDemo.java                   ← ví dụ wiring nút "Chọn địa chỉ"
```

**Quan trọng:** `map_picker.html` phải nằm trên classpath đúng đường dẫn
`/mappicker/map_picker.html`. Trong Maven/Gradle layout chuẩn, copy file
vào `src/main/resources/mappicker/map_picker.html`. Nếu bạn dùng cấu trúc
project khác, sửa hằng `HTML_RESOURCE_PATH` trong `MapPickerDialog.java`.

Đổi package `com.tutorhub.mappicker` thành package thật của app bạn nếu cần
(nhớ đổi luôn statement `package ...;` ở đầu mỗi file `.java`).

## Cách dùng (tích hợp vào form có sẵn)

```java
// cefApp = CefApp đã khởi tạo sẵn trong app của bạn (lõi JCEF bạn đang dùng)
JButton btnChonDiaChi = new JButton("📍 Chọn địa chỉ");
btnChonDiaChi.addActionListener(e -> {
    MapAddressResult result = MapPickerDialog.pickAddress(
            parentFrame, cefApp, "YOUR_GOONG_API_KEY");

    if (result != null) {
        txtDiaChi.setText(result.getAddress());
        this.lat = result.getLat();
        this.lng = result.getLng();
    }
    // null => người dùng bấm "Đóng", giữ nguyên giá trị cũ
});
```

### Chế độ sửa (preload địa chỉ cũ lên bản đồ)

```java
MapAddressResult initial = new MapAddressResult(diaChiCu, latCu, lngCu);
MapAddressResult result = MapPickerDialog.pickAddress(parentFrame, cefApp, apiKey, initial);
```

### Nếu app bạn dùng chung 1 `CefClient` cho toàn bộ app

Dùng overload nhận `CefClient` có sẵn — dialog sẽ KHÔNG dispose client này
khi đóng (bạn tự quản lý vòng đời):

```java
MapAddressResult result = MapPickerDialog.pickAddress(parentFrame, existingCefClient, apiKey, null);
```

## Cơ chế giao tiếp JS ↔ Java

- Phía JS gọi `window.cefQuery({request: 'MAP_SELECT:{...json...}', onSuccess, onFailure})`
  khi người dùng bấm **Xác nhận**, hoặc `window.cefQuery({request: 'MAP_CANCEL'})`
  khi bấm **Đóng**.
- Phía Java dùng `CefMessageRouter` với config mặc định (`cefQuery` /
  `cefQueryCancel` — đúng tên hàm JCEF tự inject vào `window`, không cần
  chỉnh gì thêm bên JS).
- `MapPickerDialog` tự parse chuỗi JSON đơn giản đó (không phụ thuộc thư
  viện JSON ngoài — tự viết 1 parser nhỏ chỉ đủ cho object phẳng
  `{"address":"...","lat":...,"lng":...}`).
- API key Goong được Java **inject vào file HTML** (thay placeholder
  `__GOONG_API_KEY__`) trước khi nạp vào `CefBrowser` qua `file://` URL —
  không cần chạy local web server.

## Lưu ý threading (rất quan trọng)

`CefMessageRouterHandler.onQuery(...)` được CEF gọi trên **luồng UI riêng
của trình duyệt**, không phải Swing EDT. Vì vậy mọi thao tác đóng dialog
(`dispose()`) đều được bọc trong `SwingUtilities.invokeLater(...)` —
đã xử lý sẵn trong `MapPickerDialog`, bạn không cần làm thêm gì.

## Lưu ý tương thích phiên bản JCEF

Có vài bản phân phối JCEF khác nhau (chromiumembedded/java-cef gốc,
JetBrains/jcef, wrapper `jcefmaven`...). Các method dùng trong
`MapPickerDialog` (`createBrowser`, `addMessageRouter`/`removeMessageRouter`,
`addLoadHandler`/`removeLoadHandler`, `CefMessageRouter.create(config, handler)`,
`browser.close(boolean)`, `cefClient.dispose()`) đã được kiểm tra khớp với
API JCEF chuẩn (`org.cef.*`). Nếu project bạn dùng 1 wrapper riêng có tên
method khác, chỉnh lại cho khớp — phần dọn dẹp resource trong `dispose()`
đã bọc try/catch nên một method bị thiếu sẽ không làm crash toàn bộ flow.

## Vì sao dùng rendering "windowed" (không offscreen)?

`cefClient.createBrowser(url, false, false)` — tham số thứ 2 (`isOffscreenRendered`)
là `false`, nghĩa là dùng **native windowed rendering**, cách phổ biến và ổn
định nhất khi nhúng JCEF trực tiếp vào `JDialog`/`JPanel` trong Swing.
Nếu app bạn đang dùng offscreen rendering (OSR) cho lý do khác (ví dụ cần
hiệu ứng trong suốt, overlay phức tạp...), đổi thành `true` và xử lý
`CefRenderHandler` tương ứng — nhưng với 1 dialog chọn địa chỉ đơn giản,
windowed rendering là đủ và nhẹ hơn.

## Đã sửa so với bản HTML nháp trước đó

1. **Bug `goTo()` chạy 2 lần**: bản cũ vừa đăng ký `map.once('idle', ...)`
   vừa gọi trực tiếp nếu `isStyleLoaded()` — có thể khiến marker được đặt
   2 lần. Đã sửa: chỉ chạy đúng 1 lần, dùng `map.loaded()` + `map.once('load', ...)`.
2. **Race condition khi gõ tìm kiếm nhanh**: thêm `AbortController` để hủy
   request autocomplete cũ khi có request mới — tránh kết quả cũ "đè" lên
   kết quả mới do trả về không theo thứ tự.
3. **Cảnh báo khi chưa cấu hình API key**: nếu Java quên inject
   `__GOONG_API_KEY__`, status bar sẽ hiện cảnh báo màu vàng ngay khi mở,
   thay vì để người dùng bấm tìm kiếm rồi mới thấy lỗi 403 khó hiểu.
4. **Thông báo lỗi rõ hơn**: khi Goong trả lỗi (key sai/hết quota), đọc
   `error_message` trong response JSON nếu có, thay vì chỉ hiện `HTTP 403`.
