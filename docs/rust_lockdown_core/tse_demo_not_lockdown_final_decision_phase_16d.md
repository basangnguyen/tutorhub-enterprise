# Phase 16D: Demo-Not-Lockdown Final Decision Gate

## Mục tiêu
Kiểm soát phân quyền khởi tạo môi trường thi thông qua `V2DemoNotLockdownFinalDecisionGateService`. Dựa trên kết quả tổng hợp của quá trình kiểm tra môi trường, gate này đưa ra quyết định có cho phép Demo hoạt động nhưng giới hạn không Lockdown hay không.

## Tiêu chí đánh giá Decision Gate
- **Cho phép (APPROVED_FOR_DEMO_NOT_LOCKDOWN_ONLY):**
  - Quá trình Security Scan Pass.
  - Probe hoàn tất và báo `PASS`.
  - Cờ Production Legacy (`tse.v2.defaultStudentSubmitV2.enabled`) phải là `false`.
  - Yêu cầu không được phép là "Desktop Demo" thật trừ khi được xác nhận là đang chạy trên VM (`vmEvidenceStatus`).
- **Từ chối / Block (BLOCKED_SECURITY_RISK / BLOCKED_PENDING_VM_EVIDENCE):**
  - Nếu cờ Legacy Production bật (gây rủi ro cho người dùng).
  - Yêu cầu thử nghiệm Desktop Lockdown thật nhưng không có bằng chứng VM.

## Tình trạng
Đã hoàn tất cấu hình Decision Gate. Gate hoạt động hoàn toàn ở chế độ Read-only. DTO trả về loại bỏ toàn bộ dữ liệu mang tính đáp án hay payload thực tế (như `payloadJson`, `answerKey`, `score`), chỉ truyền các meta an toàn.
