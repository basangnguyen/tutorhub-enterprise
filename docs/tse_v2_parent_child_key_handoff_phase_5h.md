# Phase 5H: Parent-to-Child Key Handoff Contract + In-memory Key Registry

**Trạng thái:** Đã hoàn thành (Pass)
**Mục tiêu:** Thiết kế cơ chế bàn giao AES key an toàn giữa Parent và Child cho luồng thi V2.
**Người thực hiện:** Antigravity

## Chi tiết kỹ thuật

1. **V2RuntimeKeyRegistry (In-memory Storage)**
   - Lưu trữ AES key base64 với TTL và `purpose`.
   - Thread-safe thông qua `ConcurrentHashMap`.
   - Single-consume: Key bị xoá ngay khi được tiêu thụ (consume).
   - Tự động từ chối key nếu đã hết hạn TTL.

2. **V2ChildLaunchDescriptor (Data Transfer Object)**
   - Đóng gói an toàn các thông tin bàn giao cho Child: `metaPath`, `encPath`, và `handoffId`.
   - Không chứa AES key plaintext.

3. **V2RuntimeHandoffDryRunCoordinator (Điều phối)**
   - Sinh AES key an toàn.
   - Ghi danh AES key vào `V2RuntimeKeyRegistry` và nhận lại `handoffId`.
   - Truyền `handoffId` vào hàm sinh `v2_handoff_runtime.meta.json`.
   - Mã hoá payload V2 bằng AES key.

4. **Security Update (Bảo mật)**
   - Đảm bảo các thuộc tính nhạy cảm như `isCorrect`, `answerKey`, `password`, v.v... không xuất hiện trong plaintext của file `.enc`.
   - Đảm bảo `.meta.json` không rò rỉ key và token.
   - Không ghi key ra disk, không truyền qua command line arguments, không log plaintext key.

## Xác nhận quy tắc Gate bắt buộc
- **Phase 5G Pass:** Đã xác nhận cơ chế Verify Hash và Decrypt JSON hoạt động tốt.
- Không sửa file legacy `EXAM_START_REQUEST`, `EXAM_SUBMIT`, `submit_payload.enc`.
- Không sửa Rust, JCEF, Quick Settings, Parent Bridge.
- Không launch Secure Exam Child.
- Chỉ tạo prototype dry-run.

## Kết quả Test
- `V2RuntimeKeyRegistryTest.java`: Pass 100%. Xác nhận consume only once và TTL expiration.
- `V2RuntimeHandoffKeyHandoffTest.java`: Pass 100%. Xác nhận quy trình end-to-end dry-run: mã hoá → đăng ký key → tiêu thụ key → verify hash → giải mã.
- Lệnh `mvn clean install` và build portable thành công không lỗi.

## Rủi ro còn lại
- Hiện tại, luồng IPC để thực sự truyền key sang tiến trình Java Child vẫn chưa được cài đặt. Điều này sẽ được làm trong Phase 6.

## Đề xuất Phase tiếp theo
- **Phase 6: Integration into Secure Exam Child.** Tích hợp cơ chế V2 Handoff vào Secure Exam Child, kết nối Parent và Child, thiết lập đường truyền key qua secure pipe / stdin và trigger render bài thi.
