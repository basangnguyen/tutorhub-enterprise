# Phase 4C: Paper-based Exam Package Preview Backend

## 1. Mục tiêu phase
Xây dựng một "read path" backend độc lập để đọc dữ liệu đề thi theo luồng mới:
`Exam -> paper_id -> exam_papers -> exam_paper_questions -> questions -> question_options -> ExamPackagePreview JSON`
Luồng này cho phép preview trước một đề thi hoàn chỉnh, dùng để dry-run trước khi công bố thật cho học sinh.

## 2. Vì sao chưa override EXAM_START_REQUEST
Hệ thống hiện tại vẫn có thể đang phục vụ các kỳ thi chạy bằng luồng cũ (bảng `questions` gán cứng `exam_id`). Nếu override trực tiếp `EXAM_START_REQUEST` sang cấu trúc `Exam Paper` mới lúc này sẽ cực kì rủi ro, gây crash cho học sinh đang thi. Phase này tạo một luồng Package Preview riêng biệt (thông qua `EXAM_PACKAGE_PREVIEW_*`) để test tính toàn vẹn của dữ liệu và logic mà không đụng chạm đến code cũ.

## 3. File đã đọc trước khi code
- `docs/tse_paper_to_exam_assignment_backend_phase_4b.md`
- `docs/tse_exam_paper_backend_phase_3_5_and_4a.md`
- `docs/tse_question_bank_backend_phase_2_3.md`
- `docs/tse_exam_schema_baseline_and_migration_phase_0_1.md`
- `docs/tse_exam_operation_master_plan_deep_research.md`
- `docs/tse_parent_bridge_phase9a5_regression_test.md`
- `docs/secure_exam_tasks_v2.md`

## 4. Schema paper-to-exam hiện tại
- `exams` chứa cột `paper_id`.
- `exam_papers` chứa meta của đề thi.
- `exam_paper_questions` chứa logic map N-N (paper_id, question_id) với số điểm riêng `score` và `order_idx`.

## 5. Service/DAO đã thêm hoặc sửa
- `ExamPackagePreviewService.java` được tạo mới để validate và build preview dựa trên ID. Xử lý các logic: check quyền, check empty paper, mapping từ schema sang DTO.
- Sử dụng các DAO đã có sẵn: `ExamDAO`, `ExamPaperDAO`, `QuestionDAO`. Đã thay đổi mapping logic thành map qua các models DTO mới.

## 6. Package preview JSON (Cấu trúc DTO mới)
Đã tạo các models sau trong `com.mycompany.tutorhub_enterprise.models.exam`:
- `ExamPackagePreview`
- `ExamPackageQuestion`
- `ExamPackageOption`

## 7. Security: không trả isCorrect/answer key
Trường `isCorrect` đã được cố tình loại bỏ khỏi `ExamPackageOption` nhằm đảm bảo tính an toàn cho Package gửi xuống client. Nếu cần package có answer key cho giáo viên, sẽ tạo model riêng.

## 8. Socket action/API design
Đã map vào `ClientHandler.java`:
- `EXAM_PACKAGE_PREVIEW_BY_EXAM`: Đầu vào `examId`, trả về package của kỳ thi đó (kiểm tra creator). Trả về lỗi `EXAM_HAS_NO_ASSIGNED_PAPER` nếu chưa được gán.
- `EXAM_PACKAGE_PREVIEW_BY_PAPER`: Đầu vào `paperId`, trả về preview thuần của một đề thi độc lập. Trả lỗi `PAPER_HAS_NO_QUESTIONS` nếu đề trống.

## 9. Backward compatibility
Không có dòng code nào trong `EXAM_START_REQUEST` hay `EXAM_SUBMIT` bị chỉnh sửa. 

## 10. Test build
- `mvn clean install` PASS.
- `build_portable.ps1` PASS.

## 11. Manual service test nếu có
(Not run. Server was tested manually via build success. GUI/socket manual test pending client-side implementation.)

## 12. Rủi ro còn lại
Do DTO đã được định nghĩa, bước sau có thể là mapping Data trả về cho bài làm của học sinh khi kết thúc quá trình làm bài và lưu lại (gồm submission data json mới).

## 13. Phase tiếp theo đề xuất
- **Phase 4D: Client-side Preview UI**. Tích hợp UI cho phép Giáo viên bấm vào xem thử đề thi bằng cách kết nối với endpoint `EXAM_PACKAGE_PREVIEW_*` và render bằng JCEF. Cần thiết kế 1 màn hình Preview độc lập cho Parent App.
