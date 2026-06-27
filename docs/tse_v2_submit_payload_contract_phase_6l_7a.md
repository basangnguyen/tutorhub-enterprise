# TSE V2 Submit Payload Contract (Phase 6L / 7A)

## 1. Mục tiêu phase
Xây dựng Data Transfer Object (DTO) contract đại diện cho trạng thái làm bài hoàn chỉnh trước khi submit (`TSEV2SubmitPayload`, `TSEV2SubmitAnswerItem`) và service tạo, validate payload (`TSEV2SubmitPayloadService`). 

## 2. Vì sao chỉ là contract in-memory
Để đảm bảo an toàn tuyệt đối và phân tách rõ ràng (separation of concerns), phase này chỉ chuẩn bị payload trong RAM mà không ghi disk, không gửi network và không đụng tới EXAM_SUBMIT legacy. Điều này giúp test logic validation chặt chẽ trước khi thật sự encrypt và nộp dữ liệu.

## 3. Submit payload schema
- `payloadVersion`: String (ví dụ "1.0")
- `flow`: String ("PAPER_START_V2")
- `examId`: int
- `paperId`: int
- `attemptId`: String
- `packageHash`: String
- `questionCount`: int
- `answeredCount`: int
- `unansweredCount`: int
- `complete`: boolean
- `draftSnapshotHash`: String
- `payloadHash`: String (SHA-256 của toàn bộ DTO)
- `preparedAt`: String (ISO 8601)
- `answers`: List<TSEV2SubmitAnswerItem>

## 4. Submit answer item schema
- `questionId`: int
- `selectedOptionId`: int
- `answeredAt`: String

## 5. Validation rules
1. Kiểm tra context (`examId`, `paperId`, `packageHash`) khớp với Model gốc.
2. Kiểm tra `questionCount`, `answeredCount`, `unansweredCount` và cờ `complete` tính toán đúng.
3. Chống answer duplication (chuẩn hóa an toàn bằng cách bỏ qua dupes).
4. `selectedOptionId` bắt buộc phải là 1 option hợp lệ thuộc về `questionId` có trong package.
5. `questionId` phải tồn tại trong exam package.

## 6. Out-of-scope: backend submit / encryption / final submit / scoring
Phase này KHÔNG thực hiện:
- Submit HTTP POST tới Server.
- Sửa hàm legacy `EXAM_SUBMIT`.
- Mã hóa thành `submit_payload.enc`.
- Tính điểm hoặc tính đúng/sai.

## 7. Security validation
Payload được build strict. Function `validatePayloadSafe` chống rò rỉ dữ liệu bằng cách quét chuỗi JSON được tạo ra:
- `sessionToken`, `keyB64`, `password`, `passwordHash` tuyệt đối không được phép có mặt.
- Thông tin về đáp án đúng (`isCorrect`, `answerKey`, `correctOption`) tuyệt đối không được rò rỉ.
- Các keyword liên quan đến key thô (`plaintextJson`, `plaintext`) không được lọt vào.

## 8. Unit test result
Thực hiện 9 case test chuyên sâu cho `TSEV2SubmitPayloadServiceTest` xác nhận payload sinh ra, payload context mismatching error, answer duplication normalize, invalid option rejecting, SHA-256 hash ổn định và safe leak. Pass 9/9.

## 9. Maven build result
PASS: 127/127 tests passed. Cả project không bị ảnh hưởng.

## 10. Portable build result
PASS. Build `build_portable.ps1` thành công.

## 11. run_input_test status
PENDING - VM-only / skipped by current fast-track rule.

## 12. Rủi ro còn lại
Do payload chưa đi qua network nên chưa confirm limit size của payload JSON hoặc behaviour khi mất network lúc build payload. Phụ thuộc vào phase thực tế submit.

## 13. Phase tiếp theo đề xuất: encrypted submit payload dry-run
Tạo phase dry-run encrypt submit payload -> wrap vào AES-GCM và thử test kích thước ciphertext payload, mock lưu local file hoặc bypass tới backend mock server.
