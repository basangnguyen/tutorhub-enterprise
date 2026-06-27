# Phase 16A: Physical-Machine Safe Probe Gate

## Mục tiêu
Đảm bảo khi chạy TutorHub V2 Submit trên máy thật (physical machine), hệ thống chỉ được phép hoạt động ở chế độ thăm dò (probe-only).

## Cấu trúc cổng Gate
- **Tên cổng:** V2RustPhysicalMachineSafeProbeGate
- **Cờ kích hoạt:** `tse.v2.rustPhysicalMachineSafeProbeGate.enabled` (mặc định false)
- **Hành động API:** `EXAM_SUBMIT_V2_RUST_PHYSICAL_MACHINE_SAFE_PROBE_GATE`

## Quy tắc an toàn (Physical Machine)
1. Phát hiện máy thật sẽ hardcode `physicalMachineDetected = true` do scope của phase.
2. `desktopDemoAllowed` bắt buộc phải là `false`. Tuyệt đối không gọi `--desktop-demo-safe` nếu không phải là VM.
3. Chế độ `probeOnlyAllowed` là `true`.
4. Gate sẽ chặn (BLOCKED) nếu cờ `tse.v2.defaultStudentSubmitV2.enabled` của legacy production đang bật.

## Đánh giá
Cổng gate được triển khai đúng giới hạn read-only/diagnostic. Không sử dụng SetWindowsHookEx, không ảnh hưởng production.
