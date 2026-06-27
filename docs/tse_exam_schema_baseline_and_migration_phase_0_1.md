# Phase 0 + 1: Baseline Freeze & Schema Migration Foundation

## 1. Mục tiêu phase
Chuẩn hóa nền tảng database/migration trước khi làm Question Bank CRUD. 
Giai đoạn này tập trung vào:
- Audit schema hiện tại.
- Tạo bảng `schema_migrations` và các bảng dữ liệu additive cho quá trình vận hành kỳ thi.
- Không phá vỡ backward compatibility cho các tác vụ thi hiện tại.

## 2. Tài liệu đã đọc trước khi code
- `docs/tse_exam_operation_master_plan_deep_research.md`
- `docs/tse_exam_operation_architecture_research.md`
- `docs/secure_exam_tasks_v2.md`
- `docs/tse_parent_bridge_phase9a5_regression_test.md`

## 3. Bài học từ tse_parent_bridge_phase9a5_regression_test.md
- **Không được gọi JavaFX WebView / webEngine.executeScript sai thread.** Mọi tương tác giao diện JavaFX bắt buộc phải gọi qua `Platform.runLater()`. Việc gọi trên thread Swing `AWT-EventQueue-0` sẽ gây ra Exception `Not on FX application thread`.
- Không được làm Parent popup bị blank/trắng do nghẽn JS update bridge.
- Cần cẩn trọng trong bridge Parent/Exam, không phá vỡ liên kết.

## 4. Schema hiện tại
Bảng hiện tại:
- `exams`: Lưu cấu hình chính cho từng bài thi.
- `questions`: Đang thiết kế chưa chuẩn (phụ thuộc trực tiếp vào `exam_id`), phá vỡ tính tái sử dụng.
- `exam_sessions`: Ghi lại mỗi phiên thi, user_id, trạng thái (WAITING, SUBMITTED).
- `exam_answers`: Ghi lại câu trả lời và số điểm từng câu.
- `anticheat_events`: Ghi lại các sự kiện vi phạm an ninh.
- `question_bank_categories`: Quản lý danh mục ngân hàng câu hỏi.

## 5. Các nơi đang tạo schema
Hiện đang có hai nơi thực hiện khởi tạo schema, đó là:
1. `DatabaseManager.java` (Cột tĩnh kiểm tra các bảng chung và một số bảng exam).
2. `ExamDatabaseManager.java` (File riêng cho các module thi cử).

## 6. Rủi ro schema drift
Có rủi ro schema drift (lệch cấu trúc) khi cùng lúc cả `DatabaseManager.java` và `ExamDatabaseManager.java` đều thực thi câu lệnh `CREATE TABLE` đối với các bảng Exam. Điều này sẽ khiến cấu trúc bị phân mảnh và xung đột nếu không đồng bộ. Đã đề xuất phương án chuyển toàn bộ luồng tạo bảng exam sang `ExamDatabaseManager.java` và chỉ dùng `DatabaseManager` để quản lý connection, tuy nhiên trong phase này, vẫn tôn trọng trạng thái hiện tại (Backward Compatibility) và thêm bảng ở trạng thái Additive.

## 7. Migration table đã thêm
Đã thêm `schema_migrations` vào `ExamDatabaseManager.java`:
```sql
CREATE TABLE IF NOT EXISTS schema_migrations (
    id SERIAL PRIMARY KEY,
    module_name VARCHAR(100) NOT NULL,
    migration_key VARCHAR(150) NOT NULL,
    description TEXT,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(module_name, migration_key)
);
```

## 8. Bảng mới đã thêm theo kiểu additive
Đã bổ sung cấu trúc additive (Phase 1) vào `ExamDatabaseManager.java`:
- `question_banks`
- `exam_papers`
- `exam_paper_questions`
- `exam_assignments`
- `exam_attempts`
- `exam_results`
- `exam_audit_logs`

## 9. Backward compatibility
Tất cả các thay đổi đều có tính Additive (Chỉ tạo bảng mới bằng `CREATE TABLE IF NOT EXISTS`). Không thực hiện bất cứ câu lệnh `DROP` hay `ALTER TABLE` phá vỡ luồng tạo kỳ thi cũ, EXAM_START_REQUEST và EXAM_SUBMIT. Final submit vẫn hoạt động bình thường như cũ.

## 10. Test build
- **Maven build:** Đang chạy kiểm tra.
- **Portable build:** PENDING.

## 11. Test app
- **GUI test:** PENDING / Chưa chạy (Vì server và build chưa được gọi triệt để cho GUI manual test).

## 12. Rủi ro còn lại
- Sự trùng lặp logic cấu trúc database ở `DatabaseManager.java` cần được cấu trúc lại hoàn toàn ở các phase sau.
- Cần thực hiện code UI CRUD (Create, Read, Update, Delete) cho `question_banks` và `exam_papers` trước khi hoàn thiện kết nối UI với `questions`.

## 13. Phase tiếp theo đề xuất
Tiếp tục **Phase 2 & 3: Question Bank & Question Management (Backend + API)**:
- Xây dựng hệ thống API JSON/Socket RPC để lưu trữ `question_banks`.
- Viết các service quản lý câu hỏi độc lập.
