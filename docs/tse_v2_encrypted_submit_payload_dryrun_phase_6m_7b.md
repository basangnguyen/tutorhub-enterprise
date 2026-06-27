# Encrypted Submit Payload Dry-run - Debug Only (Phase 6M / 7B)

## 1. Mục tiêu phase
Thử nghiệm đóng gói payload in-memory (từ Phase 6L/7A) thành một file mã hóa AES-GCM (submit dry-run).

## 2. Vì sao debug-only
Phase này tập trung vào bài toán bảo mật và data serialization. Việc debug-only nhằm tách biệt hoàn toàn việc tạo file (I/O, Crypto) với việc đẩy file lên mạng (Network) và cập nhật backend (Database, Final Submit). Tách biệt logic để dễ unit test mà không sợ ảnh hưởng legacy flow.

## 3. Submit dry-run encrypted file design
- Khóa (Key): Dùng `SecretKey` tự sinh (AES-256) chỉ lưu trong RAM cho phiên chạy dry-run. Không có persistence.
- Output: 2 file `v2_submit_payload_dryrun.enc` và `v2_submit_payload_dryrun.meta.json`.
- Định dạng `.enc`: JSON wrapper chứa `alg`, `iv`, và `ciphertext` base64 từ GCM encrypt của raw payload JSON.

## 4. Meta schema
Lưu vào `v2_submit_payload_dryrun.meta.json`.
- `schemaVersion`: "1.0"
- `flow`: "PAPER_START_V2"
- `mode`: "DEBUG_DRY_RUN"
- `examId`, `paperId`, `attemptId`, `packageHash`
- `questionCount`, `answeredCount`, `unansweredCount`, `complete`
- `payloadHash`: SHA-256 từ `TSEV2SubmitPayloadService`
- `encryptedFileSha256`: Hash SHA-256 của toàn bộ file `.enc` đã ghi.
- `encFileName`: "v2_submit_payload_dryrun.enc"
- `createdAt`, `updatedAt`

## 5. Key strategy RAM-only
Chưa áp dụng chính sách persistence key nào. Key bị huỷ sau khi session kết thúc. Code không ghi key ra disk, không log, không gửi kèm meta.

## 6. Validation rules
- Khi save: Yêu cầu payload hợp lệ, cấm leak token/plaintext/correctOption/password vào payload raw hoặc meta.
- Khi load: Verify `encryptedFileSha256` của file vật lý. Decrypt bằng RAM key. Xác minh meta khớp hoàn toàn payload.

## 7. Out-of-scope: backend submit / EXAM_SUBMIT / Final Submit / scoring
Phase này chưa đụng gì đến server backend, chưa ghi điểm số, không chấm bài. Mọi logic EXAM_SUBMIT legacy, JCEF hay nút Final Submit cũ đều không thay đổi.

## 8. Security validation
Chống data leak ở 3 điểm:
1. JSON Payload sinh ra cấm chứa key/plaintext.
2. JSON Meta sinh ra cấm chứa password/score/key.
3. Không ghi raw payload text hay key bằng `Files.write` - chỉ ghi byte sau khi mã hóa.

## 9. Unit test result
`TSEV2EncryptedSubmitDryRunServiceTest`: Đã phủ 6 case quan trọng: sinh 2 file, không leak data trong file meta, `.enc` vô nghĩa với con người, load lại thành công, hash mismatch fail, và wrong key fail. Pass 6/6.

## 10. Maven build result
PASS: 133 tests passed, 0 failures.

## 11. Portable build result
PASS: Successful.

## 12. run_input_test status
PENDING - VM-only / skipped by fast-track rule.

## 13. Rủi ro còn lại
Do file chỉ lưu trên thư mục tạm của người dùng (`.tutorhub-secure-exam/debug/v2-submit-dryrun`), chưa được truyền đi thật, nên chưa có đánh giá latency I/O hay giới hạn memory khi payload rất lớn. Nhưng GCM stream an toàn.

## 14. Phase tiếp theo đề xuất
Triển khai Phase 7C: Back-end API dry-run (nhận encrypted submission dry-run) hoặc triển khai V2 Handoff & Key persistence strategy nếu cần kết nối Rust.
