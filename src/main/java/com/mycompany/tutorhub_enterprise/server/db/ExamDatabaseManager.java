package com.mycompany.tutorhub_enterprise.server.db;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import java.sql.Connection;
import java.sql.Statement;

public class ExamDatabaseManager {

    private static void safeAddColumn(Statement st, String table, String columnDef) {
        try {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + columnDef);
        } catch (java.sql.SQLException e) {
            String msg = e.getMessage().toLowerCase();
            if (!msg.contains("duplicate column name") && !msg.contains("already exists")) {
                System.err.println("[DB WARNING] Could not add column " + columnDef + " to " + table + ": " + e.getMessage());
            }
        }
    }

    static {
        try (Connection conn = DatabaseManager.getConnection(); Statement st = conn.createStatement()) {
            // 0. Bảng schema_migrations
            st.execute("CREATE TABLE IF NOT EXISTS schema_migrations (" +
                "id SERIAL PRIMARY KEY, " +
                "module_name VARCHAR(100) NOT NULL, " +
                "migration_key VARCHAR(150) NOT NULL, " +
                "description TEXT, " +
                "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE(module_name, migration_key)" +
                ")");

            // 1. Bảng exams
            st.execute("CREATE TABLE IF NOT EXISTS exams (" +
                "id SERIAL PRIMARY KEY, " +
                "creator_id INT NOT NULL, " + // Bỏ REFERENCES tạm thời để tránh lỗi nếu bảng users chưa khởi tạo
                "title VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "duration_mins INT NOT NULL DEFAULT 60, " +
                "open_at TIMESTAMP, " +
                "close_at TIMESTAMP, " +
                "security_config TEXT DEFAULT '{" +
                "\"shuffle_questions\": true, \"shuffle_options\": true, \"secure_mode\": false, " +
                "\"require_webcam\": false, \"require_mic\": false, \"max_violations\": 3, " +
                "\"security_level\": \"medium\", \"blocked_processes\": [\"TeamViewer\",\"AnyDesk\",\"obs64\"], " +
                "\"allow_calculator\": false, \"allow_notepad\": false" +
                "}', " +
                "status VARCHAR(20) DEFAULT 'DRAFT', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
            try { st.execute("ALTER TABLE exams ADD COLUMN paper_id INT"); } catch (Exception ignore) {}

            // 2. Bảng questions
            st.execute("CREATE TABLE IF NOT EXISTS questions (" +
                "id SERIAL PRIMARY KEY, " +
                "exam_id INT REFERENCES exams(id) ON DELETE CASCADE, " +
                "question_type VARCHAR(20) NOT NULL DEFAULT 'MCQ', " +
                "category VARCHAR(100), " +
                "difficulty VARCHAR(10) DEFAULT 'MEDIUM', " +
                "points FLOAT DEFAULT 1.0, " +
                "content TEXT NOT NULL, " + // Sử dụng TEXT chứa JSON để tương thích rộng
                "explanation TEXT, " +
                "sort_order INT DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            // 3. Bảng exam_sessions
            st.execute("CREATE TABLE IF NOT EXISTS exam_sessions (" +
                "id SERIAL PRIMARY KEY, " +
                "exam_id INT NOT NULL REFERENCES exams(id), " +
                "user_id INT NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'WAITING', " +
                "started_at TIMESTAMP, " +
                "submitted_at TIMESTAMP, " +
                "duration_used INT, " +
                "total_score FLOAT, " +
                "max_score FLOAT, " +
                "auto_graded BOOLEAN DEFAULT FALSE, " +
                "violation_count INT DEFAULT 0, " +
                "trust_score_avg FLOAT DEFAULT 1.0, " +
                "tek_hash VARCHAR(64), " +
                "client_info TEXT, " +
                "question_order TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE(exam_id, user_id)" +
                ")");

            // 4. Bảng exam_answers
            st.execute("CREATE TABLE IF NOT EXISTS exam_answers (" +
                "id SERIAL PRIMARY KEY, " +
                "session_id INT NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE, " +
                "question_id INT NOT NULL REFERENCES questions(id), " +
                "answer_data TEXT, " +
                "is_correct BOOLEAN, " +
                "score FLOAT, " +
                "time_spent_sec INT, " +
                "change_count INT DEFAULT 0, " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE(session_id, question_id)" +
                ")");

            // 5. Bảng anticheat_events
            st.execute("CREATE TABLE IF NOT EXISTS anticheat_events (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "session_id INT NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE, " +
                "event_type VARCHAR(30) NOT NULL, " +
                "severity VARCHAR(10) DEFAULT 'LOW', " +
                "details TEXT, " +
                "evidence_url TEXT, " +
                "trust_score FLOAT, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            st.execute("CREATE INDEX IF NOT EXISTS idx_anticheat_session ON anticheat_events(session_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_anticheat_type ON anticheat_events(event_type)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_anticheat_severity ON anticheat_events(severity)");

            // 6. Bảng question_bank_categories
            st.execute("CREATE TABLE IF NOT EXISTS question_bank_categories (" +
                "id SERIAL PRIMARY KEY, " +
                "creator_id INT NOT NULL, " +
                "name VARCHAR(100) NOT NULL, " +
                "parent_id INT REFERENCES question_bank_categories(id), " +
                "description TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            // 7. Bảng question_banks (Additive cho Phase 1 & 2)
            st.execute("CREATE TABLE IF NOT EXISTS question_banks (" +
                "id SERIAL PRIMARY KEY, " +
                "name VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "creator_id INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
            // Migration for Phase 2: Add columns to question_banks if they don't exist
            try { st.execute("ALTER TABLE question_banks ADD COLUMN description TEXT"); } catch (Exception ignored) {}
            try { st.execute("ALTER TABLE question_banks ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"); } catch (Exception ignored) {}

            // Migration for Phase 2: Add columns to questions
            try { st.execute("ALTER TABLE questions ADD COLUMN bank_id INT REFERENCES question_banks(id) ON DELETE CASCADE"); } catch (Exception ignored) {}
            try { st.execute("ALTER TABLE questions ADD COLUMN creator_id INT"); } catch (Exception ignored) {}
            try { st.execute("ALTER TABLE questions ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"); } catch (Exception ignored) {}

            // Bảng question_options (Additive cho Phase 2)
            st.execute("CREATE TABLE IF NOT EXISTS question_options (" +
                "id SERIAL PRIMARY KEY, " +
                "question_id INT NOT NULL REFERENCES questions(id) ON DELETE CASCADE, " +
                "option_label VARCHAR(10), " +
                "content TEXT NOT NULL, " +
                "is_correct BOOLEAN DEFAULT FALSE, " +
                "order_index INT DEFAULT 0" +
                ")");

            // 8. Bảng exam_papers (Additive cho Phase 1)
            st.execute("CREATE TABLE IF NOT EXISTS exam_papers (" +
                "id SERIAL PRIMARY KEY, " +
                "title VARCHAR(255) NOT NULL, " +
                "total_score FLOAT DEFAULT 0, " +
                "creator_id INT NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
            try { st.execute("ALTER TABLE exam_papers ADD COLUMN description TEXT"); } catch (Exception ignore) {}
            try { st.execute("ALTER TABLE exam_papers ADD COLUMN status VARCHAR(50) DEFAULT 'DRAFT'"); } catch (Exception ignore) {}
            try { st.execute("ALTER TABLE exam_papers ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"); } catch (Exception ignore) {}

            // 9. Bảng exam_paper_questions (Additive cho Phase 1)
            st.execute("CREATE TABLE IF NOT EXISTS exam_paper_questions (" +
                "paper_id INT REFERENCES exam_papers(id) ON DELETE CASCADE, " +
                "question_id INT REFERENCES questions(id) ON DELETE CASCADE, " +
                "order_idx INT DEFAULT 0, " +
                "points FLOAT DEFAULT 1.0, " +
                "PRIMARY KEY (paper_id, question_id)" +
                ")");
            try { st.execute("ALTER TABLE exam_paper_questions ADD COLUMN is_required BOOLEAN DEFAULT TRUE"); } catch (Exception ignore) {}
            try { st.execute("ALTER TABLE exam_paper_questions ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"); } catch (Exception ignore) {}

            // 10. Bảng exam_attempts (Additive cho Phase 1 & 5C)
            st.execute("CREATE TABLE IF NOT EXISTS exam_attempts (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "exam_id INT REFERENCES exams(id) ON DELETE CASCADE, " +
                "user_id INT NOT NULL, " +
                "status VARCHAR(50) DEFAULT 'IN_PROGRESS', " +
                "started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "submitted_at TIMESTAMP, " +
                "final_score FLOAT" +
                ")");
            safeAddColumn(st, "exam_attempts", "paper_id INT");
            safeAddColumn(st, "exam_attempts", "attempt_no INT DEFAULT 1");
            safeAddColumn(st, "exam_attempts", "deadline_at TIMESTAMP");
            safeAddColumn(st, "exam_attempts", "session_token_hash VARCHAR(128)");
            safeAddColumn(st, "exam_attempts", "client_nonce VARCHAR(64)");
            safeAddColumn(st, "exam_attempts", "package_hash VARCHAR(128)");
            safeAddColumn(st, "exam_attempts", "client_info_json TEXT");
            safeAddColumn(st, "exam_attempts", "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

            // 11. Bảng exam_assignments (Additive cho Phase 1)
            st.execute("CREATE TABLE IF NOT EXISTS exam_assignments (" +
                "exam_id INT REFERENCES exams(id) ON DELETE CASCADE, " +
                "user_id INT NOT NULL, " +
                "assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "PRIMARY KEY (exam_id, user_id)" +
                ")");

            // 12. Bảng exam_results (Additive cho Phase 1)
            st.execute("CREATE TABLE IF NOT EXISTS exam_results (" +
                "id SERIAL PRIMARY KEY, " +
                "attempt_id VARCHAR(36) REFERENCES exam_attempts(id) ON DELETE CASCADE, " +
                "total_score FLOAT, " +
                "graded_by INT, " +
                "graded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            // 13. Bảng exam_audit_logs (Additive cho Phase 1)
            st.execute("CREATE TABLE IF NOT EXISTS exam_audit_logs (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "user_id INT, " +
                "attempt_id VARCHAR(36), " +
                "action VARCHAR(255) NOT NULL, " +
                "details TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");



            System.out.println("[DB] Đã kiểm tra và tạo cấu trúc bảng thi cử (Exam Module) thành công!");
        } catch (Exception e) {
            System.err.println("[DB ERROR] ExamDatabaseManager schema check failed: " + e.getMessage());
        }
    }
    
    // Gọi hàm này lúc khởi động Server để kích hoạt khối static {}
    public static void initialize() {
        // Do nothing, static block handles it
    }
}
