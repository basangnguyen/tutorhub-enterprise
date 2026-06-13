# Nghiên cứu chuyên sâu SEB (Safe Exam Browser) - Core System Components

Tài liệu này ghi lại các pattern và logic tham khảo sâu từ mã nguồn SEB (`D:\Ban_sao_du_an\seb-reference\SafeExamBrowser.SystemComponents`) để áp dụng vào TutorHub Secure Exam (TSE).

## 1. Năng lượng & Pin (Power Supply)
- **Kiến trúc SEB:** Nằm ở `SafeExamBrowser.SystemComponents\PowerSupply\PowerSupply.cs`.
  - Gọi WinForms API `System.Windows.Forms.SystemInformation.PowerStatus` (bên dưới là wrap của Win32 `GetSystemPowerStatus`).
  - Đọc `BatteryLifePercent` (phần trăm pin), `BatteryLifeRemaining` (thời gian còn lại) và `PowerLineStatus` (đang cắm sạc hay không).
  - Có vòng lặp `Timer` mỗi 5 giây (`FIVE_SECONDS = 5000`) để invoke sự kiện `StatusChanged` cập nhật UI.
- **Áp dụng TSE:** 
  - Vì TSE chạy Java, gọi Win32 API trực tiếp cần JNA (có thể gây crash).
  - Giải pháp tối ưu: Dùng PowerShell ngầm `Get-WmiObject Win32_Battery` hoặc `Get-CimInstance Win32_Battery` đọc định kỳ mỗi 30-60s. Dữ liệu gồm `EstimatedChargeRemaining` và `BatteryStatus`.

## 2. Âm thanh (Audio & Volume)
- **Kiến trúc SEB:** Nằm ở `SafeExamBrowser.SystemComponents\Audio\Audio.cs`.
  - Dùng thư viện `NAudio.CoreAudioApi` để giao tiếp với Windows Core Audio.
  - Sử dụng đối tượng `MMDevice` để set `AudioEndpointVolume.MasterVolumeLevelScalar`.
  - Có thể Mute, Unmute, SetVolume và theo dõi sự kiện thay đổi volume từ hệ thống (`OnVolumeNotification`).
- **Áp dụng TSE:**
  - Java mặc định không có API chuẩn để chỉnh Master Volume của toàn bộ OS. 
  - SEB là ứng dụng desktop quản lý toàn bộ hệ thống nên chỉnh Master Volume. Với TSE, để tránh phức tạp và rủi ro JNA, ta sẽ chỉ thực hiện **toggle mute nội bộ ứng dụng** (tắt âm JCEF) hoặc báo thông báo "System audio control unavailable in secure mode" nếu không cần thiết.

## 3. Ngôn ngữ & Bàn phím (Keyboard/Language)
- **Kiến trúc SEB:** Nằm ở `SafeExamBrowser.SystemComponents\Keyboard\Keyboard.cs`.
  - Dùng `System.Windows.Forms.InputLanguage.InstalledInputLanguages` để liệt kê và thay đổi layout bàn phím OS.
  - Ghi đè bàn phím để chuyển đổi giữa các culture (vd: EN, DE).
- **Áp dụng TSE:**
  - Yêu cầu của TSE chỉ là thay đổi **ngôn ngữ UI (Localization)** (VIE/ENG) cho nút bấm (Ví dụ: "Nộp Bài" -> "Submit"), KHÔNG thay đổi keyboard layout của OS.
  - Thiết kế `TSELanguageManager` với 1 Map tĩnh lưu trữ các khóa dịch thuật và trigger update toàn bộ JFrame khi ấn nút VIE/ENG.

## 4. Kiểm soát thoát ứng dụng (Quit/Lockdown)
- **Kiến trúc SEB:** 
  - Có Quit Password. Bắt các sự kiện đóng window (`Alt+F4`, nút X) và nếu đang trong Exam Session, sẽ block hoàn toàn trừ khi nhập đúng Quit Password.
- **Áp dụng TSE:**
  - TSE không dùng Quit Password cho người thi. Thay vào đó, dùng cờ trạng thái `isExamActive` hoặc `finalSubmitInProgress`. 
  - Nếu click nút Power ở góc dưới, kiểm tra nếu `isExamActive == true` thì hiện custom overlay "Bạn không thể thoát lúc này". Nếu false (đã nộp bài), gọi hàm dọn dẹp và thoát tiến trình Rust an toàn.

## 5. Mạng (Network/WiFi)
- **Kiến trúc SEB:** 
  - Dùng `Windows.Devices.WiFi.WiFiAdapter` (WinRT API). Cho phép dò danh sách WiFi, nhập mật khẩu và tự động kết nối lại (`ConnectAsync`).
- **Áp dụng TSE:**
  - Không triển khai picker chọn mạng vì rất phức tạp.
  - Chỉ cần đọc tên mạng (SSID) hiện tại qua `netsh wlan show interfaces` (parse dòng `SSID`) và ping tới server TutorHub để hiển thị UI trạng thái (Xanh lá = OK, Đỏ = Mất mạng).

## 6. Refresh / Reload Browser
- **Kiến trúc SEB:**
  - Có `checkBoxShowReloadWarning` cảnh báo khi ấn F5 để thí sinh không vô tình làm mất bài.
- **Áp dụng TSE:**
  - Khi ấn icon Refresh, gọi API nội bộ của ứng dụng để Autosave -> Chờ callback -> Gọi JCEF `reloadIgnoreCache()` hoặc reload trang. Không bao giờ reload trực tiếp ngay lập tức.
