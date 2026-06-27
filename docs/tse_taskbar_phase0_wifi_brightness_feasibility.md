# Phase 0: WiFi & Brightness Feasibility & Security Policy Spike

**Author**: Antigravity AI Architect  
**Date**: 2026-06-14  
**Scope**: Research & Feasibility for Native WiFi & Brightness  

---

## 1. Mục tiêu Phase 0

Phase 0 đóng vai trò là "Feasibility Spike" (nghiên cứu khả thi) nhằm trả lời các câu hỏi kỹ thuật cốt lõi trước khi refactor kiến trúc Taskbar/Quick Settings:

1. **WiFi**: Làm sao để có tính năng scan và connect giống Safe Exam Browser (SEB) nhưng phù hợp với môi trường Java/JNA và đảm bảo an toàn tuyệt đối chống thoát lockdown?
2. **Brightness**: Làm sao để chỉnh độ sáng ổn định, giải quyết tình trạng "đọc được nhưng set không đổi", và phân biệt được internal display (laptop) với external monitor (desktop)?
3. **Security Policy**: Cần định nghĩa matrix phân quyền rõ ràng cho từng control (WiFi, Brightness, Volume, Battery, Clock, Exit) theo từng trạng thái (Login/Config, Exam).

Tài liệu này không chứa mã nguồn thực thi chính thức mà chỉ phân tích, đánh giá rủi ro, cung cấp lệnh test độc lập và kiến trúc đề xuất.

---

## 2. Baseline hiện tại của System Components

Trước khi nâng cấp, đây là hiện trạng baseline của hệ thống:

| Component | Status | Công nghệ hiện tại | Vấn đề / Hạn chế |
|---|---|---|---|
| **WiFi** | Read-only | `netsh wlan show interfaces` | Chậm (spawn cmd), phụ thuộc ngôn ngữ HĐH, không có connect, UI Exam hiển thị danh sách tĩnh. |
| **Brightness** | Unstable | PowerShell + WMI | Chậm (spawn PowerShell), chỉ hoạt động trên màn hình laptop, set() hay fail trên desktop. |
| **Volume** | Writable | JNA + CoreAudio (`IAudioEndpointVolume`) | COM leak (thiếu `CoUninitialize`), thiếu event callback (đang dùng polling). |
| **Battery** | Read-only | JNA + `GetSystemPowerStatus()` | Tốt, ổn định, nhưng đang dùng polling 2s (quá nhanh so với thực tế). |
| **Clock** | Missing | N/A | Hoàn toàn chưa có hiển thị giờ ở taskbar/tray. |

---

## 3. Feasibility WiFi thật như SEB

SEB sử dụng API UWP `Windows.Devices.WiFi` qua C# để scan và quản lý mạng, đồng thời có UI cho phép nhập password (`CredentialsRequired` event) để connect. Với TSE, việc tích hợp UWP qua Java khó khăn hơn, và việc cho nhập password trực tiếp trong exam có thể mở ra rủi ro security (phá lockdown, access captive portal).

### 3.1 Đánh giá các hướng tiếp cận WiFi trên Windows

| Option | Mô tả | Bảo mật | Giống SEB | Nhược điểm / Rủi ro | Đề xuất |
|---|---|---|---|---|---|
| **A: Read-only `netsh`** | Chỉ đọc trạng thái kết nối bằng `netsh` | Rất cao | Thấp | Chậm, phụ thuộc locale, không hiển thị danh sách mạng | Giữ tạm thời |
| **B: Scan-only `netsh`** | Quét danh sách bằng `netsh` | Rất cao | Trung bình | Vẫn chậm, regex phức tạp, UX không tốt | Không khuyên dùng |
| **C: Native WlanAPI (JNA)** | Dùng `wlanapi.dll` qua JNA để scan/get trạng thái | Cao | Cao | Cần define C-struct phức tạp trong JNA | Khuyên dùng (Scan) |
| **D: Profile Connect** | Dùng WlanAPI connect tới SSID đã lưu profile | Tốt | Rất cao | Thí sinh không thể tự nhập pass mạng mới | Khuyên dùng (Connect) |
| **E: Nhập pass trong UI** | Custom popup nhập WPA2 key | Kém | Giống 100% | Rủi ro lưu key, captive portal bypass lockdown | **Không khuyên dùng** |
| **F: Mở Network UI thật**| Spawn Windows native flyout | Rất Kém | 0% | Cho phép thí sinh mở full settings, VPN | **TUYỆT ĐỐI KHÔNG** |

### 3.2 Khảo sát Windows Native Wi-Fi API (WlanAPI)

Nếu muốn làm WiFi thật (Option C & D), cần tích hợp thư viện `wlanapi.dll` qua JNA. Các hàm bắt buộc:
- `WlanOpenHandle`: Mở session.
- `WlanEnumInterfaces`: Lấy danh sách adapter (card WiFi).
- `WlanScan`: Kích hoạt quét mạng (async).
- `WlanGetAvailableNetworkList`: Lấy danh sách SSID, signal strength sau khi scan.
- `WlanConnect`: Kết nối với cấu hình cụ thể (profile).
- `WlanCloseHandle`: Đóng session.

### 3.3 Kết luận Feasibility WiFi

1. **Phase gần nhất**: Nâng cấp lên **Native WlanAPI (Scan-only)** hoặc giữ **Read-only**. Không cho phép connect mới.
2. **Phase tương lai (nếu cần connect)**: Dùng **Option D (Profile Connect)** kết hợp **Whitelist**. Máy thi phải được cấu hình sẵn profile WiFi (lưu sẵn password trong HĐH). TSE chỉ gọi `WlanConnect` tới profile đó, không có UI nhập password.

---

## 4. Feasibility Brightness thật

Trường hợp "đọc được nhưng set không đổi" là một vấn đề kinh điển của Windows API.

### 4.1 Bản chất vấn đề Laptop vs Desktop
- **Internal Display (Laptop)**: Windows quản lý qua WMI (`WmiMonitorBrightness`). PowerShell/WMI có thể set dễ dàng.
- **External Monitor (Desktop)**: Windows KHÔNG quản lý qua WMI. Các hàm WMI sẽ trả về lỗi "Not Supported" hoặc set thành công (update biến) nhưng phần cứng màn hình không thay đổi. Để chỉnh sáng màn hình rời, phải dùng DDC/CI thông qua `SetMonitorBrightness` API trong `dxva2.dll`.

### 4.2 Đánh giá các hướng tiếp cận Brightness

| Option | Công nghệ | Ưu điểm | Nhược điểm / Rủi ro | Phù hợp |
|---|---|---|---|---|
| **A: WMI PowerShell** | `Get-WmiObject` | Dễ implement | Chậm, giật (spawn process), bị GPO chặn | Bỏ |
| **B: WMI qua COM/JNA** | JNA -> WMI COM | Nhanh, không spawn process | Vẫn chỉ hỗ trợ Laptop, COM phức tạp | Tốt cho Laptop |
| **C: Monitor Config API**| `SetMonitorBrightness` | Hỗ trợ màn rời (DDC/CI) | JNA phức tạp, một số màn hình không hỗ trợ DDC/CI | Tốt cho Desktop |
| **D: Adaptive (Khuyên dùng)**| Thử WMI -> Thử Monitor API -> Disable | Trải nghiệm tốt nhất, tự ẩn lỗi | Logic phức tạp | Đích đến lý tưởng |

### 4.3 Kết luận Feasibility Brightness

- Cần chuyển từ PowerShell sang WMI/COM native qua JNA (giống cách Volume đang làm).
- Phải áp dụng **Adaptive Strategy**: Khi khởi động, app thử query brightness. Nếu trả về lỗi hoặc timeout -> `supported = false`. UI phải disable thanh trượt và hiển thị chữ "Không hỗ trợ" để thí sinh không bị bối rối.
- Không nên giả vờ cho phép set() nếu phần cứng không đáp ứng.

---

## 5. Lệnh test thủ công Brightness ngoài app

Để xác minh thiết bị có hỗ trợ WMI Brightness hay không, yêu cầu user chạy thủ công các lệnh PowerShell sau (với quyền admin nếu cần):

```powershell
# 1. Đọc giá trị hiện tại
Get-CimInstance -Namespace root/wmi -ClassName WmiMonitorBrightness -ErrorAction SilentlyContinue | Select-Object CurrentBrightness

# 2. Thử thay đổi độ sáng thành 80% (Chỉ hoạt động trên màn hình hỗ trợ WMI/Laptop)
$brightnessParams = @{
    Namespace = 'root/wmi'
    Class = 'WmiMonitorBrightnessMethods'
    ErrorAction = 'SilentlyContinue'
}
(Get-WmiObject @brightnessParams).WmiSetBrightness(1, 80)

# 3. Đọc lại giá trị sau khi đổi
Get-CimInstance -Namespace root/wmi -ClassName WmiMonitorBrightness -ErrorAction SilentlyContinue | Select-Object CurrentBrightness
```

**Acceptance**: 
- Nếu kết quả trả về trống hoặc báo lỗi -> Thiết bị KHÔNG hỗ trợ WMI Brightness.
- Nếu giá trị cập nhật thành 80 nhưng màn hình KHÔNG SÁNG LÊN -> Thiết bị là Desktop/External Monitor không hỗ trợ WMI.
- **Hành động**: Trong cả hai trường hợp lỗi trên, TSE BrightnessController phải set `supported = false`.

---

## 6. Đề xuất Security Policy Matrix

Để quản lý trạng thái UI đồng nhất và bảo mật, cần xây dựng class `TSESecurityPolicy` kiểm soát quyền:

| Control | Mode `Login/Config` | Mode `Exam` (Lockdown) | Mode `Debug` | Ghi chú |
|---|---|---|---|---|
| **WiFi** | `scanOnly` / `readOnly` | `readOnly` | `scanOnly` | Cấm nhập password mọi nơi |
| **Brightness**| `writableIfSupported`| `writableIfSupported`| `writableIfSupported`| Tự động disable nếu không hỗ trợ |
| **Volume** | `writable` | `writable` | `writable` | |
| **Battery** | `readOnly` | `readOnly` | `readOnly` | |
| **Clock** | `show` | `show` (có thể `hide` qua cấu hình)| `show` | |
| **InputMode** | `enabled` | `enabled` | `enabled` | Chuyển đổi VIE/ENG |
| **Refresh** | `enabled` | `disabledDuringSubmit` | `enabled` | Tránh refresh hỏng bài làm |
| **Exit** | `allowed` | `blocked` | `allowed` | |

---

## 7. Kiến trúc Implementation Đề Xuất

Thay vì gọi HĐH trực tiếp từ UI (như hiện tại), kiến trúc sẽ được chuyển sang mô hình: **State -> Controller -> Service**.

### 7.1 Cấu trúc Class
1. `TSESecurityPolicy`: Chứa Matrix quyền ở trên.
2. `QuickSettingsSnapshot`: Immutable object chứa data (json-ready) cho UI render.
3. `QuickSettingsStateStore`: In-memory thread-safe state (Lưu Volume, Brightness, Wifi hiện tại).
4. `QuickSettingsController`: Orchestrator nhận lệnh từ UI (JCEF/JavaFX), gọi Service, cập nhật Store.
5. **Services (Singletons)**:
   - `BatteryService` (5s polling timer)
   - `ClockService` (60s polling timer)
   - `VolumeService` (JNA/COM, event-based hoặc caching)
   - `BrightnessService` (Adaptive JNA WMI/Monitor API)
   - `NetworkService` (JNA WlanAPI hoặc netsh scan)
6. **Bridges**: `ParentQuickSettingsBridge` và `ExamQuickSettingsBridge` (chỉ làm nhiệm vụ parse JSON và gọi Controller).

### 7.2 Lộ trình Implement (Thứ tự ưu tiên)
- **Bước 1**: Tạo `TSESecurityPolicy`, `QuickSettingsSnapshot`, `QuickSettingsStateStore`.
- **Bước 2**: Implement `ClockService` và `BatteryService` (Dễ, rủi ro thấp).
- **Bước 3**: Refactor `VolumeService` (Fix COM lifecycle - rất quan trọng).
- **Bước 4**: Refactor `NetworkService` (Chỉ làm Scan/Read-only để an toàn).
- **Bước 5**: Refactor `BrightnessService` (Thêm cơ chế Adaptive disable).
- **Bước 6**: Hợp nhất Bridge của Parent và Exam để dùng chung logic.
- **Bước 7**: UI Polish (CSS/JS) giống Windows 11.

---

## 8. Rủi ro của Phase

1. **JNA COM Leak**: Việc tái cấu trúc WMI và CoreAudio qua JNA phải làm cực cẩn thận với `CoInitialize` / `CoUninitialize` trên đúng luồng thực thi (dedicated thread) để tránh memory leak và treo ứng dụng.
2. **WlanAPI JNA Complexity**: Khai báo structs (WLAN_AVAILABLE_NETWORK_LIST, WLAN_INTERFACE_INFO) trong JNA rất dễ lỗi Memory Violation (C-struct padding). Cần fallback an toàn.
3. **Màn hình tối đen**: Lỗi WMI hoặc Monitor API chỉnh sai giá trị có thể làm màn hình thí sinh tối đen không khôi phục được. Phải có validate range `[0-100]`.

---

## 9. Kết luận: Nên implement gì trước?

**Mục tiêu gần nhất**: 
Khoan tích hợp UI hay WlanAPI phức tạp. 
1. **Ưu tiên 1**: Viết các class lõi Data (`TSESecurityPolicy`, `QuickSettingsSnapshot`, `QuickSettingsStateStore`) để định hình luồng dữ liệu.
2. **Ưu tiên 2**: Refactor `VolumeController` và `BatteryStatusProvider` thành Services chuẩn hóa, xử lý dứt điểm COM lifecycle.
3. **Ưu tiên 3**: Test thủ công Brightness theo hướng dẫn để chốt phương án Adaptive WMI.
4. WiFi sẽ giữ ở mức Read-only (hoặc Scan-only qua netsh) cho tới khi kiến trúc lõi hoàn thiện, vì WiFi Connect mang rủi ro bảo mật quá cao trong lockdown.
