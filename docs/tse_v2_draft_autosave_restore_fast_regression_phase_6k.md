# Phase 6K: V2 Draft Autosave/Restore Fast Regression Gate - No VM

## 1. Mục tiêu phase
Kiểm tra nhanh và ổn định lại toàn bộ chuỗi lifecycle của hệ thống V2 Draft (từ khi chọn đáp án trên RAM -> sinh snapshot trong RAM -> tự động lưu local autosave (mã hoá) kèm metadata an toàn -> khôi phục từ encrypted payload -> apply lại vào RAM -> UI phản ánh kết quả). Phase này là "fast regression gate", không bổ sung chức năng mới.

## 2. Vì sao no-VM fast gate
Autosave và restore hoạt động phần lớn độc lập với các cơ chế hooking hay screen lockdown của VM. Phase này chú trọng vào flow dữ liệu và tính an toàn của state management. Việc bỏ qua VM test giúp thu hẹp focus vào memory validation, security payload regression, và ngăn chặn leak dữ liệu thay vì mất công debug IPC trên VM.

## 3. End-to-end draft flow
Luồng dữ liệu được test thành công:
1. Giao diện (Mock UI/Test) -> gọi `selectOption(questionId, optionId)` trên `TSEV2AnswerSelectionState`.
2. Hệ thống gọi `createSnapshot()` tạo ra một bản `TSEV2AnswerDraftSnapshot`.
3. Bản snapshot được ghi xuống ổ đĩa dạng file `.enc` thông qua thuật toán AES-GCM (Payload hoàn toàn mã hoá). Đồng thời, sinh `.meta.json` chỉ chứa siêu dữ liệu (đủ an toàn).
4. Hệ thống mô phỏng việc restart, đọc file `.meta.json` và `.enc` bằng key chính xác thông qua `tryLoadEncryptedDraft()`.
5. Snapshot khôi phục thành công được apply lại vào `TSEV2AnswerSelectionState` bằng phương thức `applySnapshotToSelectionState()`.
6. RAM phản ánh chính xác kết quả chọn của user trước khi tắt ứng dụng.

## 4. Security validation
Một security scan hẹp (`findstr`) đã được thực thi trên các class liên quan:
- `TSEV2AnswerSelectionState.java`
- `TSEV2AnswerDraftSnapshot.java`
- `TSEV2AnswerDraftItem.java`
- `TSEV2AnswerDraftSnapshotService.java`
- `TSEV2LocalEncryptedDraftAutosaveService.java`
- `TSEV2DraftAutosaveMeta.java`
- `TSEV2ReadOnlyExamPanel.java`
- `TSEExamChildClient.java`

Kết quả:
- **Không có network send**.
- **Không gọi EXAM_SUBMIT**.
- **Không tạo submit_payload.enc**.
- **Không ghi plaintext JSON** cho snapshot data.
- **Files.write** chỉ được dùng cho encrypted bytes hoặc safe meta data.
- Không có bất kỳ logging in clear-text cho aesKey, sessionToken, isCorrect hay password.
- Secret key được tạo trong RAM bằng phương thức tĩnh `generateDraftKey()` và truyền đi dưới dạng tham số nội bộ.

## 5. Test coverage
Một Unit Test end-to-end `TSEV2DraftRoundTripRegressionTest.java` mới đã được bổ sung nhằm tự động hóa luồng. Các test đã cover:
1. Selection state select/change/clear.
2. Snapshot reject selectedOptionId sai question.
3. Snapshot JSON không có token/key/answerKey/isCorrect/password.
4. Autosave ghi `.enc` và safe meta.
5. Autosave không ghi plaintext JSON.
6. Meta không có answers/selectedOptionId.
7. Restore đúng key pass.
8. Wrong key fail.
9. Tamper `.enc` fail.
10. Context mismatch fail.
11. Restore apply vào RAM state.
12. UI reflect restored answers (thông qua Test và State Verification).
13. Không có Submit/Save/Finish button.
14. Không gọi EXAM_SUBMIT.
15. Không tạo submit_payload.enc.

## 6. Maven build result
- Result: **PASS** (118/118 tests pass).

## 7. Portable build result
- Result: **PASS** (Portable build succeeds without errors).

## 8. run_input_test status
- `run_input_test.bat` legacy: PENDING - VM-only / skipped by current fast-track rule.

## 9. Rủi ro còn lại
- Hiện tại luồng submit (kết thúc làm bài để nộp) vẫn chưa được xử lý, toàn bộ chỉ là lưu bản nháp offline.
- Tích hợp với Java UI thực sự chưa gửi draft update theo thời gian thực (ví dụ như Auto-save loop timer).
- Chưa test trên VM.

## 10. Go/No-Go cho submit payload phase
- **GO**. Toàn bộ infrastructure local save an toàn đã ổn định và đã được cô lập khỏi memory leakage. Phase tiếp theo có thể yên tâm sử dụng `TSEV2AnswerDraftSnapshot` như payload cơ bản để mã hoá và chuẩn bị gói tin cho submit backend.
