# TSE WiFi & Battery Indicator Plan

## 1. Scope
Phân tích cách Safe Exam Browser (SEB) triển khai Network/WiFi selector và Battery indicator ở taskbar. Đưa ra đề xuất triển khai tính năng tương đương cho TutorHub Secure Exam (TSE) đảm bảo an toàn, không mở các công cụ OS và hoạt động trơn tru trong JCEF.

## 2. SEB Reference Findings

Qua phân tích mã nguồn SEB (`SafeExamBrowser.SystemComponents` và `SafeExamBrowser.UserInterface.Desktop`):

### 3. What SEB Does for WiFi
- SEB có một nút `NetworkControl` ở Taskbar/Action Center.
- Khi bấm, nó mở ra một **Popup UI nội bộ** chứa danh sách các mạng WiFi.
- Nguồn dữ liệu: Gọi API WinRT `Windows.Devices.WiFi.WiFiAdapter` để scan và lấy danh sách `WiFiAvailableNetwork`.
- SEB **cho phép kết nối WiFi**:
  - Nếu mạng đã lưu/không pass: Gọi `adapter.ConnectAsync(network, WiFiReconnectionKind.Automatic)`.
  - Nếu mạng cần pass: Hiện ô nhập password (thông qua event `CredentialsRequired`), sau đó truyền mật khẩu vào `ConnectAsync`.
- SEB **KHÔNG** mở Windows Settings hay Windows Network Flyout. Mọi thứ là giao diện tĩnh được vẽ lại bằng WPF.

### 4. What SEB Does for Battery
- SEB có `PowerSupplyControl.xaml.cs`.
- Nguồn dữ liệu: Gọi API .NET `System.Windows.Forms.SystemInformation.PowerStatus` (bên dưới .NET gọi API C++ Win32 `GetSystemPowerStatus`).
- Hiển thị UI: 
  - Đang sạc hay dùng pin (Icon thay đổi).
  - Phần trăm pin (Dựa vào `BatteryLifePercent`).
  - Cảnh báo khi pin yếu (BatteryChargeStatus.Critical/Low).

---

## 5. Current TSE Footer Status
- Hiện tại TSE có `ExamFooterStatusBar.java` chứa các nút giả lập UI chưa có logic mạng và pin thực sự.
- Các nút đã block thoát hệ thống thành công. 
- Ngôn ngữ/Telex toggle đang chia sẻ không gian với khu vực này.

---

## 6. WiFi Selector Proposal
Vì TSE là ứng dụng Java, không thể gọi trực tiếp WinRT API dễ dàng như SEB (C#). Nếu cố viết JNA map sang `wlanapi.dll` (WlanEnumInterfaces, WlanGetAvailableNetworkList) sẽ rất phức tạp với nested struct C/C++ và dễ làm Crash JVM. 

**Đề xuất cho TSE:**
- **UI:** Làm y hệt SEB. Click vào nút WiFi -> Hiển thị 1 JPanel Overlay trượt từ dưới lên chứa danh sách mạng.
- **Data (Option C - Netsh):** 
  - Lấy trạng thái kết nối: `netsh wlan show interfaces`
  - Quét mạng: `netsh wlan show networks mode=bssid` (Parse SSID, Signal). An toàn tuyệt đối, không crash JVM.
- **Connection (Option D for now):** 
  - Trong milestone này, để đảm bảo an toàn không rủi ro, ta sẽ làm **Option D**: Chỉ làm WiFi List + Status (Hiển thị danh sách mạng có sẵn).
  - Khi user click vào mạng, ta báo: "TSE Network Control đang trong chế độ Chỉ Đọc. Vui lòng kết nối WiFi từ ngoài Launcher trước khi thi."
  - Lý do: Connect WiFi mới trên Windows qua CLI đòi hỏi phải tạo file XML Profile và pass plaintext rất lằng nhằng và tiềm ẩn lỗi Security/OS. Hướng SEB gọi thẳng WinRT API rất mượt nhưng Java không có thư viện chuẩn. Do đó tạm thời hiển thị danh sách để thí sinh biết máy có thấy sóng hay không.

## 7. Battery Indicator Proposal
- **Option B (JNA/Win32):** Project của chúng ta đã có thư viện `jna-platform` trong `pom.xml`.
- Rất may, JNA Platform có sẵn `com.sun.jna.platform.win32.Kernel32.INSTANCE.GetSystemPowerStatus(new Kernel32.SYSTEM_POWER_STATUS())`. 
- Nó tương đương 100% với cách SEB gọi `SystemInformation.PowerStatus`.
- **UI:** Đọc `ACLineStatus` (Cắm sạc hay không) và `BatteryLifePercent`. Đổi icon pin và tooltips ở footer.

## 8. Security Restrictions
- KHÔNG MỞ Windows Network UI.
- KHÔNG gọi lệnh làm hiện command prompt (dùng ProcessBuilder ẩn cửa sổ).
- Overlay WiFi List phải đè lên trên CEF browser mà không dùng Modal Dialog (chỉ set visible JPanel).

## 9. Implementation Plan

1. **Battery Module:**
   - Cập nhật `ExamFooterStatusBar.java`.
   - Tạo Thread 10s/lần gọi `Kernel32.INSTANCE.GetSystemPowerStatus`.
   - Update giao diện Battery (Percentage, Charging/Discharging).

2. **Network/WiFi Status Module:**
   - Tạo `TSENetworkStatusProvider.java` chuyên chạy ngầm `netsh wlan show interfaces` (mỗi 10s) lấy SSID hiện tại và chất lượng kết nối.
   - Update icon WiFi trên Footer (Connected, No Internet, No WiFi).

3. **WiFi Selector Overlay (UI):**
   - Tạo `TSEWiFiOverlayPanel.java` (kế thừa JPanel). Khi click icon WiFi, overlay hiện lên.
   - Overlay có nút "Quét mạng". Khi ấn, chạy ngầm `netsh wlan show networks` và load danh sách SSID + Signal strength.
   - Thêm nút Close.
   - Không cho phép nhập pass/connect để giữ độ ổn định (Status + List only).

## 10. Test Plan
- Rút sạc laptop: Icon pin thay đổi.
- Tắt WiFi: Icon mạng đổi thành "Disconnected".
- Bật WiFi: Overlay hiển thị đúng các mạng xung quanh.
- Không gây thanh đen, không crash JCEF, Final Submit không bị ảnh hưởng.

## 11. Risks and Limitations
- Lệnh `netsh` trên một số máy ảo (VM) không có card WiFi sẽ trả về "The Wireless AutoConfig Service (wlansvc) is not running". Code parse cần handle lỗi này êm ái bằng cách trả về mảng rỗng và báo "Không tìm thấy card WiFi" (giống cơ chế SEB).
- Quét mạng WiFi (scan) mất từ 2-4 giây, cần hiện Progress "Đang tìm mạng..." để không bị đơ giao diện.
