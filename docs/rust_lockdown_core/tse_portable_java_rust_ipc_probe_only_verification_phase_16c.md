# Phase 16C: Portable Java/Rust IPC Probe-Only Verification

## Mục tiêu
Xác thực module Java `V2RustPortableIpcProbeOnlyVerificationService` chỉ giao tiếp với tập tin đóng gói `rust-core.exe` bằng cơ chế IPC và tham số `--probe`, loại bỏ hoàn toàn khả năng vô tình kích hoạt lockdown hay demo.

## Các yêu cầu đã hoàn tất
- **Action Gate:** Tạo thành công `EXAM_SUBMIT_V2_RUST_PORTABLE_IPC_PROBE_ONLY_VERIFY`.
- **Cờ cấm:** `tse.v2.rustPortableIpcProbeOnlyVerification.enabled` mặc định false.
- **ProcessBuilder Timeout:** Thiết lập timeout ngắn (5s) nhằm đảm bảo không có ghost process.
- **Tham số thực thi:** Chỉ gọi `"--probe"`.
- **Xử lý thiếu file:** Nếu không tìm thấy `rust-core.exe`, hệ thống trả về mã `PENDING_PACKAGING_DECISION` hoặc `RUST_CORE_NOT_FOUND`.

## Đánh giá an toàn
Java IPC hoạt động chính xác trong giới hạn đã định, không rò rỉ quyền Admin và xử lý dọn dẹp tiến trình trong trường hợp TimeOut. Không ảnh hưởng API Submit Production hiện hành.
