# Phase 12: VM Test-Profile Activation Pack & Final Release Prep

## Tổng Quan
Phase 12 đánh dấu bước chuẩn bị cuối cùng (Preparation) cho tính năng Student Submit V2 trước khi có thể được Release. Các công việc trong phase này tập trung hoàn toàn vào việc xây dựng tài liệu, bộ công cụ kích hoạt test cho VM và audit dọn dẹp các debug scripts tồn đọng.

**Quan trọng**: Phase 12 tuân thủ tuyệt đối nguyên tắc KHÔNG bật V2 mặc định trên production, KHÔNG sửa UI, KHÔNG sửa NetworkTSEExamService và KHÔNG làm lộ lọt dữ liệu.

## Chi tiết các Phase con đã thực hiện

### 12A: VM Test-Profile Activation Pack
- Đã tạo tài liệu hướng dẫn kích hoạt profile VM: `docs/vm_test_profile/tse_v2_submit_vm_activation_profile.md`
- Đã cung cấp file cấu hình mẫu chỉ dùng cho VM/Test: `docs/vm_test_profile/v2_submit_vm_test_profile.sample.properties`

### 12B: Portable EXAM_SUBMIT Smoke Test Harness
- Đã tạo kịch bản Smoke Test chi tiết để kiểm tra bản Portable: `docs/vm_test_profile/tse_v2_portable_exam_submit_smoke_test.md`
- Đã cung cấp script chạy Portable với các tham số V2 (chỉ để tham khảo/sample): `scripts/run_v2_submit_vm_smoke_sample.ps1`
- Trạng thái `run_input_test.bat`: **PENDING / NOT RUN** (script not found or VM-only).

### 12C: Final Release Checklist Gate
- Đã tạo `V2FinalReleaseChecklistGateService` và `V2FinalReleaseChecklistGateResult`.
- Service này đóng vai trò chốt chặn kiểm tra tất cả các Sub-gates, sự toàn vẹn của Legacy Route và sự tồn tại của các tài liệu Audit/Smoke Test.
- Gate route diagnostics đã được tích hợp qua action `EXAM_SUBMIT_V2_FINAL_RELEASE_CHECKLIST_GATE` (read-only, an toàn tuyệt đối).

### 12D: Debug Script / Repo Hygiene Audit
- Đã rà soát lại khoảng 80+ file Python debug scripts (dùng cho code patching).
- Đã tạo bản báo cáo Audit phân loại các script thành các nhóm (Fix, Patch, Replace, Test/Inject).
- Tài liệu audit bao gồm danh sách đầy đủ (Appendix A) mà không làm rác repo.
- Không dùng lệnh `git clean`, giữ nguyên các debug scripts để user xoá thủ công nếu muốn.

## Kết quả kiểm thử & Security Scan
- **Maven Clean Install**: 552 Tests PASS, 0 Failures, 0 Errors.
- **Portable Build**: PASS. Các file CEF binaries được copy đầy đủ vào `dist`.
- **Security Scan**: PASS. Không tìm thấy mã lộ lọt `payloadJson`, `score`, hay thao tác nguy hiểm như xoá/ghi dữ liệu ở các Gate Service.
- **Production Safety**: `tse.v2.defaultStudentSubmitV2.enabled=false` được đảm bảo ở trạng thái an toàn.
