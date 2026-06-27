# Phase 9B.8: Brightness JS Binding / Resource Load / Outside Click Fix

## 1. Vấn đề đã giải quyết
- **Parent Quick Settings (WebView)**: 
  - Thêm log khởi tạo version.
  - Sửa lỗi không gọi được `javaApp.setBrightnessCommand` bằng cách dùng hàm `bindBrightnessSlider()`. Hàm này remove các inline events `oninput`/`onchange` và thay bằng `addEventListener`.
  - Thêm `stopPropagation()` cho sự kiện click, mousedown trên slider và background panel để chặn Java AWT `outsideClickListener` đóng popup khi đang tương tác kéo thả.
  
- **Exam Quick Settings (JCEF)**: 
  - Thêm log khởi tạo version.
  - Áp dụng kỹ thuật tương tự: tạo `bindExamBrightnessSlider()` gọi `addEventListener` thay cho việc dùng binding trung gian.
  - Thêm các log tracing cho `cefQuery` gọi string command `TSE_BRIGHTNESS_SET:percent`.

- **QuickSettingsController**: 
  - Bổ sung implementation gọi `.refreshNow()` cho Brightness và Volume vào `requestRefresh()` thay vì chỉ dùng skeleton method `// TODO: Call native services`.

- **BrightnessService**: 
  - Bổ sung bước verify lại giá trị bằng `getBrightnessInternal` ngay sau khi ghi thành công giá trị xuống WMI để chắc chắn brightness đã được cập nhật.

## 2. Kết quả Build
- Đã chạy `mvn clean install` thành công sau khi sửa tham số trong `updateVolume`.
- Đã chạy `build_portable.ps1` thành công.
- Binaries đã được cập nhật tại `dist/TutorHubSecureExam/`.

## 3. Cách Test
Chạy script test:
```powershell
cd dist\TutorHubSecureExam
.\run_input_test.bat --exam-id 3
```

- Mở Parent Quick Settings -> Kéo Brightness -> Xác nhận màn hình thay đổi sáng/tối và không bị đóng popup.
- Vào màn hình Exam JCEF -> Mở tray -> Kéo Brightness -> Xác nhận màn hình thay đổi sáng/tối.
