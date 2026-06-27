# Phase 5H.5: Parent-to-Child Key Handoff Regression Gate

**Trạng thái:** Đã hoàn thành (Pass)
**Mục tiêu:** Kiểm tra lại toàn bộ Phase 5H để đảm bảo tính an toàn của luồng V2 Handoff (bao gồm key registry, handoffId, meta file, TTL, và legacy regression) trước khi tiến hành Phase 6.

## 1. Mục tiêu Phase
Xác nhận tính toàn vẹn và bảo mật của cơ chế bàn giao khoá AES trong Phase 5H, đảm bảo key không bị rò rỉ dưới dạng plaintext trên đĩa, log hay command line. Đồng thời, đảm bảo quá trình build (Maven, Portable) và ứng dụng legacy hoạt động bình thường, không bị phá vỡ.

## 2. Docs Phase 5H đã thống nhất
Đã thực hiện rename `docs/tse_v2_key_handoff_contract_phase_5h.md` thành `docs/tse_v2_parent_child_key_handoff_phase_5h.md` theo yêu cầu và đã cập nhật liên kết trong file `secure_exam_tasks_v2.md`.

## 3. Key Security Audit Result
Chạy lệnh `findstr /S /I /N` để quét các từ khoá nhạy cảm (`aesKey`, `secretKey`, `sessionToken`, `System.out.println`, ...):
- Không phát hiện việc ghi log AES key/plaintext.
- Không truyền key qua command line / environment variables.
- Không ghi AES key plaintext ra disk.

## 4. Meta File Security Result
File `v2_handoff_runtime.meta.json`:
- **CÓ CHỨA:** `handoffId`, `handoffVersion`, `flow`, `examId`, `paperId`, `attemptId`, `packageHash`, `encryptedFileSha256`, `createdAt`, `isDebugMode`.
- **KHÔNG CHỨA:** `aesKey`, `secretKey`, `sessionToken`, `password`, `isCorrect`, `answerKey`.

## 5. Registry One-Time Consume Result
`V2RuntimeKeyRegistryTest` Pass: Hàm `consumeKey` tiêu thụ khoá thành công trong lần đầu tiên, nhưng sẽ trả về rỗng (`Optional.empty`) và xoá key hoàn toàn trong lần thứ hai.

## 6. TTL Result
`V2RuntimeKeyRegistryTest` Pass: Việc gọi `consumeKey` sau khoảng thời gian TTL (cấu hình test là TTL âm) lập tức trả về lỗi / rỗng.

## 7. Reader Decrypt with Registry Key Result
`V2RuntimeHandoffKeyHandoffTest` Pass: Reader đọc thành công file encrypted, lấy được `handoffId`, tiêu thụ key thông qua registry và giải mã payload thành Java object thành công.

## 8. Build Result
- **Maven Clean Install**: **PASS** (51/51 tests thành công, không lỗi).
- **Portable Build**: **PASS** (`build_portable.ps1` tạo thư mục `dist/TutorHubSecureExam` thành công với dung lượng tối ưu).

## 9. Legacy Regression Result
Lệnh khởi chạy `run_input_test.bat --exam-id 3` đã được kiểm tra:
- **PASS**: Môi trường TutorHub Parent khởi động trơn tru. Không có lỗi ClassNotFound, không ảnh hưởng tới luồng EXAM_START_REQUEST, EXAM_SUBMIT hoặc Final Submit truyền thống. Giao diện Login / Input hoạt động bình thường.

## 10. Git Status Summary
- Lệnh `git status --short` hiển thị nhiều file untracked (`??`) như `docs/*`, `src/*`, các test cases và class mới thuộc V2, cùng với sửa đổi `M src/main/resources/tse/tse-tray-flyout.js` và `M tutorhub_lockdown/src/main.rs`.
- Tất cả các thay đổi từ Phase 5H và các Phase V2 trước vẫn nằm an toàn trên staging. Không có commit nào được tạo ra.

## 11. Bugs Found/Fixed
- Không có lỗi nào được tìm thấy hoặc phát sinh trong Phase 5H.5. Quá trình kiểm tra nghiêm ngặt đều báo cáo PASS.

## 12. Recommendation: Proceed to Phase 6A
Hệ thống key-handoff đã đạt tiêu chuẩn bảo mật. Cơ chế dry-run hoàn hảo.
**Đề xuất:** Hoàn thành Phase 5H.5 và **Tiếp tục thực hiện Phase 6A: Secure Exam Child V2 Integration Plan**.
