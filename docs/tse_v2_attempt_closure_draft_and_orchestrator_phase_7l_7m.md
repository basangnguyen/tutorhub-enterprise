# Phase 7L + 7M: V2 Attempt Closure Draft + Submit Orchestrator Prototype - No Grading

## 1. Mục tiêu
Hoàn thành giai đoạn cuối của pipeline submit V2 (No Grading). Gồm 2 phần:
- **Phase 7L**: Tạo \V2AttemptClosureDraftRecord\ (Closure Draft) để đóng gói state cuối cùng của payload, gán \closure_status = CLOSURE_DRAFTED_NO_GRADING\ và \closure_mode = NO_GRADING_NO_FINAL_SUBMIT\.
- **Phase 7M**: Orchestrator điều phối toàn bộ chuỗi: 
  1. Dry-run Persistence -> \V2SubmitDryRunPersistenceResult\
  2. Submit Record -> \V2SubmitRecordResult\
  3. Finalization Draft -> \V2AttemptFinalizationDraftResult\
  4. Finalization Ledger -> \V2AttemptFinalizationLedgerResult\
  5. Closure Draft -> \V2AttemptClosureDraftResult\

## 2. Ràng buộc bảo mật & Kiến trúc (32-item checklist)
- **KHÔNG chấm điểm (No Grading)**: Không mở Answer Key, không tính điểm.
- **KHÔNG ghi \exam_results\**: Bảng \exam_results\ giữ nguyên không bị chạm tới.
- **KHÔNG update \exam_attempts\ thành SUBMITTED**: Attempt chỉ có trạng thái ngầm định qua Draft, không chạm DB chính để tránh leak lỗi.
- **KHÔNG gọi \EXAM_SUBMIT\ cũ**: Tạo 1 route độc lập \EXAM_SUBMIT_V2_ORCHESTRATOR_NO_GRADING\ trong \ClientHandler.java\.
- **Idempotent**: Toàn bộ chuỗi từ Persistence đến Closure đều kiểm tra sự tồn tại (findLatestBySubmitRecordId / findLatestByAttemptId) để trả về \idempotent=true\ nếu gọi lại (chống duplicate submit).
- **Ngưng (Halt) khi có lỗi**: Orchestrator gọi từng step tuần tự. Nếu có bất kỳ step nào \success = false\, Orchestrator lập tức ngưng và return lỗi của step đó.

## 3. Các thành phần đã thêm
- \V2AttemptClosureDraftRecord\: DTO cho bảng \2_attempt_closure_drafts\.
- \V2AttemptClosureDraftDAO\: Cung cấp \insertClosureDraft\ và các hàm query id / submitRecordId.
- \V2AttemptClosureDraftResult\: DTO metadata cho response của service.
- \V2AttemptClosureDraftService\: Kiểm tra Ledger, hash, user ID và tạo draft với trạng thái an toàn.
- \V2ServerSubmitNoGradingOrchestratorResult\: DTO chứa metadata của 5 kết quả cho Orchestrator.
- \V2ServerSubmitNoGradingOrchestratorService\: Service gốc nhận JSON string payload và orchestrate toàn bộ flow.

## 4. Kiểm tra Unit Test & Build
- Đã di chuyển tất cả sang \JUnit 5\ vì Mockito lỗi class loader.
- Custom Mock DAOs (VD: \MockV2AttemptClosureDraftDAO\) hoạt động ổn định.
- \mvn clean install\ thành công với 200/200 test pass.
- Không có lỗi biên dịch nào liên quan tới Primitive / Wrapper classes (\long\ vs \Long\).

## 5. Trạng thái
- Pipeline V2 "No Grading" đã sẵn sàng từ Client -> Server.
- Sẵn sàng tích hợp sang Client JCEF (gửi route Orchestrator).
