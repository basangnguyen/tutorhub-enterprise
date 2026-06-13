# TSE Brightness Control Plan

## 1. Scope
TSE sẽ bổ sung khả năng điều chỉnh độ sáng màn hình (Brightness) từ thanh Quick Settings dưới System Tray. Khác với giao diện UI thuần túy, slider này sẽ trực tiếp giao tiếp với phần cứng máy tính/Windows OS để thay đổi độ sáng thực tế. Nếu thiết bị không hỗ trợ, UI sẽ bị vô hiệu hóa rõ ràng.

## 2. SEB Brightness / System Control Research Notes

### 1. Đã tìm trong file nào của SEB?
Tôi đã quét toàn bộ mã nguồn thư mục `D:\Ban_sao_du_an\seb-reference` với các từ khóa `brightness`, `dxva2`, `WmiMonitorBrightness`, `MonitorConfiguration`, `DDC`, `monitor`, `display`. Kết quả: SEB **không hề có** bất kỳ đoạn mã nào liên quan đến điều chỉnh độ sáng. Các từ khóa `monitor` xuất hiện trong SEB chỉ mang nghĩa "giám sát" (VD: `IDesktopMonitor` giám sát việc nhảy Desktop, `checkBoxMonitorProcesses` giám sát process lạ).

### 2. SEB có brightness control không?
Không. Safe Exam Browser v3.10.1 không hỗ trợ chỉnh độ sáng màn hình từ giao diện Kiosk của nó.

### 3. SEB xử lý system control tương tự ra sao?
Dù không có Brightness, SEB có xử lý Audio, Power và WiFi. Nguyên tắc chung của SEB:
- Dùng thư viện Native/PInvoke (VD: `SafeExamBrowser.WindowsApi`) để gọi trực tiếp các Win32 API.
- Dùng WMI (vd. qua `ManagementObjectSearcher` trong C#) để truy vấn thông tin phần cứng.
- Chạy ngầm trong background thread.

### 4. Bài học áp dụng cho TSE
- **Bảo toàn Secure Desktop**: Gọi WMI/Windows API ngầm thông qua Java ProcessBuilder.
- **Tách biệt Model**: Thiết kế rõ ràng Controller chuyên biệt (`TSEBrightnessController`) để gọi Native, không trộn lẫn vào UI code.

### 5. Điểm không copy từ SEB
- Tính năng Brightness không hề có trong SEB, nên tôi sẽ phải xây dựng từ đầu cho TSE để mang lại UX Windows 11 thật sự.

## 3. Windows Brightness APIs Studied
Windows quản lý độ sáng theo hai luồng:
1. **Laptop / Internal Displays**: Chuẩn WMI thông qua `root\wmi` và class `WmiMonitorBrightness` (đọc) / `WmiMonitorBrightnessMethods` (ghi).
2. **Desktop / External Monitors**: Màn hình rời dùng DDC/CI qua cáp HDMI/DP với API `dxva2.dll`. Cấu trúc `PHYSICAL_MONITOR` và các hàm `GetMonitorBrightness`, `SetMonitorBrightness`.

## 4. WMI Internal Display Strategy
Dùng WMI PowerShell snippet chạy ẩn qua Java `ProcessBuilder`:
- Get: `Get-CimInstance -Namespace root/wmi -ClassName WmiMonitorBrightness | Select-Object -ExpandProperty CurrentBrightness`
- Set: `(Get-WmiObject -Namespace root/wmi -Class WmiMonitorBrightnessMethods).WmiSetBrightness(1, <percent>)`
Ưu điểm: Đáng tin cậy nhất cho laptop, không cần JNA phức tạp.

## 5. DDC-CI / External Monitor Strategy
Gọi `dxva2.dll` qua JNA. Tuy nhiên vì nguy cơ tương thích và có thể gây treo máy, phương án là ưu tiên WMI. Nếu WMI unsupported, có thể thử DDC/CI thông qua thư viện JNA nếu có sẵn, hoặc báo rõ "Không hỗ trợ" để giữ an toàn tuyệt đối cho Secure Exam.

## 6. PowerShell Hidden Process Strategy
Vì viết WMI trực tiếp bằng JNA COM cực kỳ dễ lỗi bộ nhớ, TSE sẽ dùng **PowerShell Hidden Process**:
- Lệnh: `powershell.exe -NoProfile -NonInteractive -WindowStyle Hidden -Command "..."`
- Có timeout xử lý ngầm trong thread để tránh kẹt.

## 7. Current Quick Settings Integration
TSE đã có Quick Settings DOM (`tse-tray-flyout.js`).
- JCEF gửi `TSE_BRIGHTNESS_GET` -> Java trả về % (hoặc -1).
- Nếu `-1`, disabled slider. Nếu >= 0, hiển thị value.
- Kéo slider -> throttle 300ms -> gửi `TSE_BRIGHTNESS_SET:<value>`.

## 8. Implementation Architecture
1. **Model**: `TSEBrightnessStatus.java`.
2. **Controller**: `TSEBrightnessController.java` quản lý gọi background WMI.
3. **Bridge**: Cập nhật CEF Message Router trong `TSEExamChildClient.java` để hứng lệnh độ sáng.

## 9. Security Restrictions
- Tuyệt đối dùng ProcessBuilder giấu cửa sổ (WindowStyle Hidden, hoặc Java mặc định chạy process không cmd host).
- Không yêu cầu UAC Admin.

## 10. Test Plan
1. Build `run.bat`.
2. Chạy trên Laptop (để test WMI có hoạt động set thật không).
3. Chạy trên Desktop (để xem Unsupported có báo và disabled chuẩn không).
4. Đảm bảo Safe Refresh/Final Submit hoạt động bình thường.

## 11. Acceptance Criteria
- Slider có tác dụng độ sáng thực tế hoặc vô hiệu hóa báo lỗi. Không giả mạo UI.
- Log Java ghi đầy đủ hành vi.
