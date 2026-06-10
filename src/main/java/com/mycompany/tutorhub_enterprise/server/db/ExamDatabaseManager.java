package com.mycompany.tutorhub_enterprise.server.db;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import java.sql.Connection;
import java.sql.Statement;

public class ExamDatabaseManager {

    static {
        try (Connection conn = DatabaseManager.getConnection(); Statement st = conn.createStatement()) {
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
