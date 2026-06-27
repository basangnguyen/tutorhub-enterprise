# Phase 16B: Rust Probe / Cargo Verification

## Mục tiêu
Đảm bảo Rust Core (TutorHub Lockdown) có khả năng thực thi ở chế độ thăm dò (`--probe`) trên máy thật, mà không can thiệp vào UI, không khóa màn hình, không đăng ký hook hay gây gián đoạn cho người dùng.

## Xác minh thực thi (Terminal)
- **`cargo build`**: Hoàn thành, không lỗi biên dịch.
- **`cargo test`**: Hoàn thành, 0 tests failed.
- **`cargo run -- --probe`**: Hoàn thành, trả về JSON chuẩn xác định môi trường là physical machine, desktop demo = false.

## Quy định an toàn
> [!IMPORTANT]
> Cấm tuyệt đối chạy `cargo run -- --desktop-demo-safe` hay file thực thi với tham số `desktop-demo-safe` trên Phase 16, do phase này là môi trường máy thật.
> Không thực thi API như `SetWindowsHookEx`, `CreateDesktopW`, `SwitchDesktop` ở chế độ này.

## Kết quả
Rust module chỉ hoạt động như một công cụ cung cấp telemetry/metadata về khả năng của hệ thống.
