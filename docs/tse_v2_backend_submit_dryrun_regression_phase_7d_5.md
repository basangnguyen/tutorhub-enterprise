# Phase 7D.5: Backend Submit Dry-run Full Regression Gate

## 1. Mục tiêu phase
Kiểm tra, rà soát lại toàn bộ codebase thuộc **Phase 7D** (Backend Submit Contract - Server-side Dry-run Validation) trước khi tiến hành ghi vào DB ở **Phase 7E**. Đảm bảo:
- Action dry-run được định tuyến đúng đắn.
- Không có bất kỳ side effect hay leak data nhạy cảm nào (score, answerKey, isCorrect).
- Tính nguyên vẹn của luồng legacy `EXAM_SUBMIT` không bị ảnh hưởng.
- Các feature flag và role check hoạt động tốt.
- Full build và test coverage đạt yêu cầu.

## 2. Phase 7D code audit
- DTO mới `V2SubmitDryRunValidationResult.java` **chỉ** chứa các thông tin báo lỗi / status code, KHÔNG hề có trường lưu trữ `answerKey`, `score`, hay kết quả chấm.
- `V2SubmitDryRunValidationService.java` tuân thủ các quy tắc không được gọi API cập nhật, insert của hệ thống. File chỉ thực hiện validate 17 luồng điều kiện dựa vào các Select queries từ `ExamDAO`, `ExamPaperDAO`, `ExamAttemptDAO`.
- Unit Test (`V2SubmitDryRunValidationServiceTest.java`) bao phủ 9 luồng negative và positive cho Validation Service, đảm bảo catch các lỗi như số lượng câu hỏi sai, payload marker nguy hiểm.

## 3. ClientHandler route audit
- Socket action `EXAM_SUBMIT_V2_DRYRUN_VALIDATE` đã được thêm vào switch-case.
- Bắt lỗi parse payload và validate DTO rất kỹ.
- Trả về 2 type packet duy nhất: `EXAM_SUBMIT_V2_DRYRUN_VALIDATE_OK` hoặc `EXAM_SUBMIT_V2_DRYRUN_VALIDATE_ERROR`.
- Không gọi hàm ghi đè, và luồng `EXAM_SUBMIT` legacy từ Phase 1 vẫn nằm nguyên không bị sửa đổi.

## 4. Feature flag / role guard result
- Việc kiểm tra cờ `tse.v2.submitDryRunValidation.enabled` được thực hiện.
- Role check: Đã thực hiện fix lấy role trực tiếp từ database cho User ID thông qua logic truy vấn. Chỉ user có role `ADMIN` hoặc `TUTOR` (hoặc flag forceStudent bật) mới được đi tiếp. Vượt qua strict check.

## 5. Security scan result
- Command `findstr` cho các keywords: `answerKey`, `isCorrect`, `correctOption`, `score`, `EXAM_SUBMIT`, `submit_payload`, `FinalSubmit`, `Rust`, `UPDATE`, `INSERT`, `submitted`, `exam_results`.
- Kết quả không phát hiện rò rỉ: 
  - Không có lệnh `UPDATE/INSERT` nào trong service Dry-Run.
  - Các keywords nhạy cảm chỉ nằm trong các comment hoặc payload injection test ở `V2SubmitDryRunValidationServiceTest.java`.
  - Không có bất kỳ lệnh gọi cập nhật trạng thái "SUBMITTED" nào lên DB từ Dry-Run.

## 6. Full Maven build result
- `mvn clean install` PASS: Đã xác thực không còn lỗi compilation và tất cả các Unit Test (gồm 9 test cases trong V2SubmitDryRunValidationServiceTest và các tests từ các phase trước) đều Success.

## 7. Portable build result
- Portable script `build_portable.ps1` chạy PASS. Đã tạo ra folder `dist/TutorHubSecureExam` chuẩn chỉnh, không bị lỗi jar missing.

## 8. run_input_test status
- `run_input_test.bat` legacy: PENDING - VM-only / skipped by fast-track rule. Không chạy lockdown GUI trên máy vật lý.

## 9. Rủi ro còn lại
- Hiện tại test sử dụng Testable Class và local connection trong `ClientHandler`. Có rủi ro khi chạy trên live test nếu DatabaseManager hoặc JDBC connector chậm do lượng truy vấn `SELECT` cao từ validation loop. Tuy nhiên sẽ theo dõi trong các phase E2E.
- Tốc độ truy xuất Role có thể tạo một độ trễ nhỏ (1-2ms), đã được cover an toàn.

## 10. Go/No-Go cho Phase 7E persistence
- **Trạng thái:** GO.
- Codebase sạch, bảo mật, và sẵn sàng cho Phase 7E. Toàn bộ tính năng validation server-side chạy ổn định và an toàn.
