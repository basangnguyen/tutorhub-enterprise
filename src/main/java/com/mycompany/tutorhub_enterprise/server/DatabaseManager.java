package com.mycompany.tutorhub_enterprise.server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt; 

public class DatabaseManager {
    public static final int AUTH_FAILED = -1;
    public static final int AUTH_DATABASE_ERROR = -2;
    public static final int AUTH_INVALID_PASSWORD_HASH = -3;
    
    private static final String DB_URL = ServerConfig.get(
            "TUTORHUB_DB_URL",
            "tutorhub.db.url",
            "jdbc:postgresql://HOST:PORT/DB_NAME"
    );
    private static final String DB_USER = ServerConfig.get(
            "TUTORHUB_DB_USER",
            "tutorhub.db.user",
            "neondb_owner"
    );
    private static final String DB_PASS = ServerConfig.get(
            "TUTORHUB_DB_PASSWORD",
            "tutorhub.db.password",
            "your_db_password"
    );

    // TỰ ĐỘNG VÁ LỖI CẤU TRÚC BẢNG NẾU NHƯ DATABASE CỦA BẠN BỊ THIẾU CỘT (CHỐNG SẬP UI)
    static {
        try (Connection conn = getConnection(); java.sql.Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS cv_url TEXT");
            st.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS ekyc_front_url TEXT");
            st.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS ekyc_back_url TEXT");
            st.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(50)");
            st.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_normalized VARCHAR(32)");
            st.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT FALSE");
            st.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMP");
            st.execute("CREATE TABLE IF NOT EXISTS tutor_experiences (id SERIAL PRIMARY KEY, user_id INTEGER, duration VARCHAR(100), location VARCHAR(255), description TEXT, status VARCHAR(50) DEFAULT 'Chờ duyệt', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS reels (id SERIAL PRIMARY KEY, user_id INTEGER, video_url TEXT, caption TEXT, hashtags VARCHAR(255), likes INTEGER DEFAULT 0, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("ALTER TABLE reels ADD COLUMN IF NOT EXISTS location VARCHAR(255)");
            st.execute("ALTER TABLE reels ADD COLUMN IF NOT EXISTS product_link TEXT");
            st.execute("CREATE TABLE IF NOT EXISTS reel_comments (id SERIAL PRIMARY KEY, reel_id INTEGER, user_id INTEGER, content TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS drive_file_stars (file_id INTEGER, user_id INTEGER, PRIMARY KEY (file_id, user_id))");
            st.execute("CREATE TABLE IF NOT EXISTS drive_file_versions (version_id SERIAL PRIMARY KEY, file_id INTEGER, file_url TEXT, source_location VARCHAR(50), version_number INTEGER, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS drive_file_shares (file_id INTEGER, shared_with_user_id INTEGER, permission VARCHAR(20), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (file_id, shared_with_user_id))");
            st.execute("CREATE TABLE IF NOT EXISTS locket_videos (id SERIAL PRIMARY KEY, user_id INTEGER, media_url TEXT, title TEXT, media_type VARCHAR(50), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            // --- TUTORHUB SECURE EXAM MODULE TABLES ---
            st.execute("CREATE TABLE IF NOT EXISTS exams (" +
                    "id SERIAL PRIMARY KEY, " +
                    "creator_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "description TEXT, " +
                    "duration_mins INTEGER NOT NULL DEFAULT 60, " +
                    "open_at TIMESTAMP, " +
                    "close_at TIMESTAMP, " +
                    "security_config JSONB DEFAULT '{}'::jsonb, " +
                    "status VARCHAR(20) DEFAULT 'DRAFT', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            st.execute("CREATE TABLE IF NOT EXISTS questions (" +
                    "id SERIAL PRIMARY KEY, " +
                    "exam_id INTEGER REFERENCES exams(id) ON DELETE CASCADE, " +
                    "question_type VARCHAR(20) NOT NULL DEFAULT 'MCQ', " +
                    "category VARCHAR(100), " +
                    "difficulty VARCHAR(10) DEFAULT 'MEDIUM', " +
                    "points FLOAT DEFAULT 1.0, " +
                    "content JSONB NOT NULL, " +
                    "explanation TEXT, " +
                    "sort_order INTEGER DEFAULT 0, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            st.execute("CREATE TABLE IF NOT EXISTS exam_sessions (" +
                    "id SERIAL PRIMARY KEY, " +
                    "exam_id INTEGER NOT NULL REFERENCES exams(id) ON DELETE CASCADE, " +
                    "user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, " +
                    "status VARCHAR(20) DEFAULT 'WAITING', " +
                    "started_at TIMESTAMP, " +
                    "submitted_at TIMESTAMP, " +
                    "duration_used INTEGER, " +
                    "total_score FLOAT, " +
                    "max_score FLOAT, " +
                    "auto_graded BOOLEAN DEFAULT FALSE, " +
                    "violation_count INTEGER DEFAULT 0, " +
                    "trust_score_avg FLOAT DEFAULT 1.0, " +
                    "tek_hash VARCHAR(64), " +
                    "client_info JSONB, " +
                    "question_order JSONB, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(exam_id, user_id))");

            st.execute("CREATE TABLE IF NOT EXISTS exam_answers (" +
                    "id SERIAL PRIMARY KEY, " +
                    "session_id INTEGER NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE, " +
                    "question_id INTEGER NOT NULL REFERENCES questions(id) ON DELETE CASCADE, " +
                    "answer_data JSONB, " +
                    "is_correct BOOLEAN, " +
                    "score FLOAT, " +
                    "time_spent_sec INTEGER, " +
                    "change_count INTEGER DEFAULT 0, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(session_id, question_id))");

            st.execute("CREATE TABLE IF NOT EXISTS anticheat_events (" +
                    "id BIGSERIAL PRIMARY KEY, " +
                    "session_id INTEGER NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE, " +
                    "event_type VARCHAR(30) NOT NULL, " +
                    "severity VARCHAR(10) DEFAULT 'LOW', " +
                    "details JSONB, " +
                    "evidence_url TEXT, " +
                    "trust_score FLOAT, " +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            
            // Note: Indices for anticheat_events can be added if needed:
            // st.execute("CREATE INDEX IF NOT EXISTS idx_anticheat_session ON anticheat_events(session_id)");
            // st.execute("CREATE INDEX IF NOT EXISTS idx_anticheat_type ON anticheat_events(event_type)");
            // ------------------------------------------
            // --- CẤU TRÚC BẢNG MỚI CHO TÍNH NĂNG QUẢN LÝ LỚP HỌC (CLASSROOM) ---
            st.execute("CREATE TABLE IF NOT EXISTS classroom_groups (id SERIAL PRIMARY KEY, owner_id INTEGER, name VARCHAR(255), cover_image TEXT, description TEXT, status VARCHAR(50) DEFAULT 'ACTIVE', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS classroom_members (classroom_id INTEGER, user_id INTEGER, role VARCHAR(50) DEFAULT 'STUDENT', joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (classroom_id, user_id))");
            st.execute("CREATE TABLE IF NOT EXISTS classroom_lessons (id SERIAL PRIMARY KEY, classroom_id INTEGER, title VARCHAR(255), start_time TIMESTAMP, duration_minutes INTEGER, seat_count INTEGER DEFAULT 1, status VARCHAR(50) DEFAULT 'SCHEDULED', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            st.execute("ALTER TABLE classroom_groups ADD COLUMN IF NOT EXISTS organization_name VARCHAR(255) DEFAULT 'My Account'");
            st.execute("ALTER TABLE classroom_groups ADD COLUMN IF NOT EXISTS join_code VARCHAR(50)");
            st.execute("ALTER TABLE classroom_lessons ADD COLUMN IF NOT EXISTS board_id VARCHAR(100)");
            st.execute("ALTER TABLE classroom_lessons ADD COLUMN IF NOT EXISTS lesson_type VARCHAR(50) DEFAULT 'CLASSROOM'");
            st.execute("ALTER TABLE classroom_lessons ADD COLUMN IF NOT EXISTS stage_layout VARCHAR(20) DEFAULT '1V6'");
            st.execute("ALTER TABLE classroom_lessons ADD COLUMN IF NOT EXISTS lobby_enabled BOOLEAN DEFAULT TRUE");
            st.execute("ALTER TABLE classroom_lessons ADD COLUMN IF NOT EXISTS allow_student_draw BOOLEAN DEFAULT FALSE");
            st.execute("ALTER TABLE classroom_lessons ADD COLUMN IF NOT EXISTS recording_enabled BOOLEAN DEFAULT FALSE");
            st.execute("ALTER TABLE classroom_lessons ADD COLUMN IF NOT EXISTS created_by INTEGER");
            st.execute("ALTER TABLE classroom_members ADD COLUMN IF NOT EXISTS member_status VARCHAR(50) DEFAULT 'APPROVED'");
            st.execute("UPDATE classroom_members SET member_status = 'APPROVED' WHERE member_status IS NULL");
            st.execute("ALTER TABLE messages ADD COLUMN IF NOT EXISTS client_message_id TEXT");
            st.execute("CREATE UNIQUE INDEX IF NOT EXISTS ux_messages_client_message_id ON messages(conversation_id, sender_id, client_message_id) WHERE client_message_id IS NOT NULL AND client_message_id <> ''");
            
            System.out.println("[DB] Đã kiểm tra và vá lỗi cấu trúc bảng thành công!");
        } catch (Exception e) {
            System.err.println("[DB ERROR] Database schema check failed: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        try { Class.forName("org.postgresql.Driver"); } 
        catch (ClassNotFoundException e) { System.err.println("[DB LỖI] Không tìm thấy Driver."); }
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static boolean isEmailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email); ResultSet rs = pstmt.executeQuery(); return rs.next(); 
        } catch (SQLException e) { return true; }
    }

    public static boolean registerUser(String email, String rawPassword, String fullName, String role) {
        String sql = "INSERT INTO users (email, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        String hashedPass = BCrypt.hashpw(rawPassword, BCrypt.gensalt(12));
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email); pstmt.setString(2, hashedPass); pstmt.setString(3, fullName); pstmt.setString(4, role);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static void updateLastSeen(int userId) {
        String sql = "UPDATE users SET last_seen = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId); pst.executeUpdate();
        } catch (SQLException e) {}
    }
    
    public static int authenticateByEmail(String email, String inputPassword) {
        String sql = "SELECT id, password_hash, role FROM users WHERE email = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email); ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String dbHash = rs.getString("password_hash");
                if (ServerConfig.isBlank(dbHash)) {
                    System.err.println("[AUTH ERROR] User has empty password hash.");
                    return AUTH_INVALID_PASSWORD_HASH;
                }
                try {
                    if (BCrypt.checkpw(inputPassword, dbHash)) { return rs.getInt("id"); }
                    return AUTH_FAILED;
                } catch (IllegalArgumentException e) {
                    System.err.println("[AUTH ERROR] User password hash is not BCrypt-compatible: " + e.getMessage());
                    return AUTH_INVALID_PASSWORD_HASH;
                }
            } else { return AUTH_FAILED; }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] authenticateByEmail failed: " + e.getMessage());
            return AUTH_DATABASE_ERROR;
        }
    }

    public static int findUserIdByPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        if (ServerConfig.isBlank(normalizedPhone)) {
            return -1;
        }

        String indexedSql = "SELECT id FROM users WHERE phone_normalized = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(indexedSql)) {
            pstmt.setString(1, normalizedPhone);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException ignored) {}

        String fallbackSql = "SELECT id, phone FROM users WHERE phone IS NOT NULL AND TRIM(phone) <> ''";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(fallbackSql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String dbPhone = rs.getString("phone");
                if (normalizedPhone.equals(normalizePhone(dbPhone))) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException ignored) {}

        return -1;
    }

    public static int findVerifiedUserIdByPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        if (ServerConfig.isBlank(normalizedPhone)) {
            return -1;
        }

        String sql = "SELECT id FROM users WHERE phone_normalized = ? AND phone_verified = TRUE";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizedPhone);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException ignored) {}

        return -1;
    }

    public static boolean isPhoneLinkedToUser(int userId, String phone) {
        String normalizedPhone = normalizePhone(phone);
        if (ServerConfig.isBlank(normalizedPhone)) {
            return false;
        }

        String sql = "SELECT phone_normalized, phone FROM users WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedNormalized = rs.getString("phone_normalized");
                if (normalizedPhone.equals(storedNormalized)) {
                    return true;
                }
                return normalizedPhone.equals(normalizePhone(rs.getString("phone")));
            }
        } catch (SQLException ignored) {}

        return false;
    }

    public static boolean markPhoneVerified(int userId, String phone) {
        String normalizedPhone = normalizePhone(phone);
        String sql = "UPDATE users SET phone_normalized = ?, phone_verified = TRUE, phone_verified_at = CURRENT_TIMESTAMP WHERE id = ? AND (phone_normalized = ? OR phone_normalized IS NULL OR phone_normalized = '')";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizedPhone);
            pstmt.setInt(2, userId);
            pstmt.setString(3, normalizedPhone);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String value = phone.trim().replaceAll("[^0-9+]", "");
        if (value.startsWith("+84")) {
            return "84" + value.substring(3);
        }
        if (value.startsWith("0") && value.length() >= 10) {
            return "84" + value.substring(1);
        }
        return value;
    }

    public static boolean resetPassword(String email, String newRawPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE email = ?";
        String hashedPass = BCrypt.hashpw(newRawPassword, BCrypt.gensalt(12));
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashedPass); pstmt.setString(2, email); return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static boolean updateAvatar(int userId, String base64Data) {
        String sql = "UPDATE users SET avatar_url = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, base64Data); pst.setInt(2, userId); return pst.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    public static String getAvatar(int userId) {
        String sql = "SELECT avatar_url FROM users WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId); ResultSet rs = pst.executeQuery();
            if (rs.next()) { String avatarData = rs.getString("avatar_url"); if (avatarData != null && !avatarData.trim().isEmpty()) return avatarData; }
        } catch (SQLException e) { }
        return "NO_AVATAR";
    }

    public static java.util.List<String> getAllTutors() {
        java.util.List<String> list = new java.util.ArrayList<>();
        String sql = "SELECT id, full_name, email, avatar_url, phone, subject, location, status, performance_score, classes_taught, rating FROM users WHERE role = 'TUTOR' ORDER BY id DESC";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("full_name");
                String email = rs.getString("email");
                String avatarUrl = rs.getString("avatar_url");

                String avatarBase64 = "DEFAULT";
                if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                    java.io.File f = new java.io.File(avatarUrl);
                    if (f.exists()) { byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath()); avatarBase64 = java.util.Base64.getEncoder().encodeToString(bytes); }
                }

                String phone = rs.getString("phone") != null ? rs.getString("phone") : "Chưa cập nhật";
                String subject = rs.getString("subject") != null ? rs.getString("subject") : "Chưa phân công";
                String location = rs.getString("location") != null ? rs.getString("location") : "Chưa cập nhật";
                String status = rs.getString("status") != null ? rs.getString("status") : "Chờ duyệt";
                
                int perfScore = rs.getInt("performance_score"); int classesTaught = rs.getInt("classes_taught"); double rating = rs.getDouble("rating");
                String performance = perfScore + "|" + classesTaught + "|" + rating;
                String col0 = avatarBase64 + "|" + name + "|" + email + "|" + phone;
                String tutorId = "GS" + id;
                list.add(col0 + ";;" + tutorId + ";;" + subject + ";;" + location + ";;" + performance + ";;" + status);
            }
            rs.close();
        } catch (Exception e) {}
        return list;
    }
    
    // --- LẤY CHI TIẾT HỒ SƠ 1 GIA SƯ (BẢO VỆ CHỐNG SẬP DATABASE VÀ CHUYỂN MÃ ẢNH eKYC) ---
    public static String getFullProfile(int userId) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String id = "GS" + rs.getInt("id");
                String email = rs.getString("email") != null ? rs.getString("email") : "";
                String name = rs.getString("full_name") != null ? rs.getString("full_name") : "";
                String dob = rs.getString("dob") != null ? rs.getString("dob") : "";
                String gender = rs.getString("gender") != null ? rs.getString("gender") : "Nam";
                String phone = rs.getString("phone") != null ? rs.getString("phone") : "";
                String address = rs.getString("address") != null ? rs.getString("address") : "";
                String location = rs.getString("location") != null ? rs.getString("location") : "Toàn quốc";
                String subject = rs.getString("subject") != null ? rs.getString("subject") : "";
                String bio = rs.getString("bio") != null ? rs.getString("bio").replace("\n", "\\n") : "";
                
                String cvName = "";
                String ekycFrontB64 = "";
                String ekycBackB64 = "";
                
                try {
                    String cvUrl = rs.getString("cv_url");
                    if (cvUrl != null && !cvUrl.isEmpty()) cvName = new java.io.File(cvUrl).getName();
                    
                    String frontUrl = rs.getString("ekyc_front_url");
                    if (frontUrl != null && !frontUrl.isEmpty()) {
                        java.io.File f1 = new java.io.File(frontUrl);
                        if (f1.exists()) ekycFrontB64 = java.util.Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(f1.toPath()));
                    }
                    
                    String backUrl = rs.getString("ekyc_back_url");
                    if (backUrl != null && !backUrl.isEmpty()) {
                        java.io.File f2 = new java.io.File(backUrl);
                        if (f2.exists()) ekycBackB64 = java.util.Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(f2.toPath()));
                    }
                } catch (Exception ex) { } // Chống sập do thiếu bảng
                
                boolean phoneVerified = false;
                try {
                    phoneVerified = rs.getBoolean("phone_verified");
                } catch (Exception ignored) {}

                return id + ";;" + email + ";;" + name + ";;" + dob + ";;" + gender + ";;" + phone + ";;" + address + ";;" + location + ";;" + subject + ";;" + bio + ";;" + cvName + ";;" + ekycFrontB64 + ";;" + ekycBackB64 + ";;" + phoneVerified;
            }
        } catch (Exception e) { e.printStackTrace(); }
        return "";
    }

    public static boolean updateProfile(int userId, String name, String dob, String gender, String phone, String address, String location, String subject, String bio) {
        String sql = "UPDATE users SET full_name = ?, dob = ?, gender = ?, phone = ?, phone_normalized = ?, phone_verified = CASE WHEN COALESCE(phone_normalized, '') = ? THEN phone_verified ELSE FALSE END, phone_verified_at = CASE WHEN COALESCE(phone_normalized, '') = ? THEN phone_verified_at ELSE NULL END, address = ?, location = ?, subject = ?, bio = ? WHERE id = ?";
        String normalizedPhone = normalizePhone(phone);
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, name); pst.setString(2, dob); pst.setString(3, gender); pst.setString(4, phone);
            pst.setString(5, normalizedPhone); pst.setString(6, normalizedPhone); pst.setString(7, normalizedPhone); pst.setString(8, address); pst.setString(9, location); pst.setString(10, subject); pst.setString(11, bio); pst.setInt(12, userId);
            return pst.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    public static boolean insertDegree(int userId, String name, String major, String uni, String year, String fileUrl) {
        String sql = "INSERT INTO tutor_degrees (user_id, degree_name, major, university, graduation_year, file_url) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId); pst.setString(2, name); pst.setString(3, major); 
            pst.setString(4, uni); pst.setString(5, year); pst.setString(6, fileUrl);
            return pst.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    public static boolean insertCertificate(int userId, String name, String provider, String issueDate, String expiryDate, String fileUrl) {
        String sql = "INSERT INTO tutor_certificates (user_id, cert_name, provider, issue_date, expiry_date, file_url) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId); pst.setString(2, name); pst.setString(3, provider); 
            pst.setString(4, issueDate); pst.setString(5, expiryDate); pst.setString(6, fileUrl);
            return pst.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    public static java.util.List<String> getDegrees(int userId) {
        java.util.List<String> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM tutor_degrees WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                String name = rs.getString("degree_name"); String major = rs.getString("major");
                String uni = rs.getString("university"); String year = rs.getString("graduation_year");
                String fileUrl = rs.getString("file_url"); String status = rs.getString("status");
                String fileName = new java.io.File(fileUrl).getName();
                list.add(name + "|" + major + "|" + uni + "|" + year + "|" + fileName + "|" + status);
            }
        } catch (Exception e) { }
        return list;
    }

    public static java.util.List<String> getCertificates(int userId) {
        java.util.List<String> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM tutor_certificates WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                String name = rs.getString("cert_name"); String provider = rs.getString("provider");
                String issue = rs.getString("issue_date"); String exp = rs.getString("expiry_date");
                String fileUrl = rs.getString("file_url"); String status = rs.getString("status");
                String fileName = new java.io.File(fileUrl).getName();
                list.add(name + "|" + provider + "|" + issue + "|" + exp + "|" + fileName + "|" + status);
            }
        } catch (Exception e) { }
        return list;
    }

    public static boolean updateCV(int userId, String fileUrl) {
        String sql = "UPDATE users SET cv_url = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, fileUrl); pst.setInt(2, userId); return pst.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }
    
    public static boolean updateEkyc(int userId, String frontUrl, String backUrl) {
        String sql = "UPDATE users SET ekyc_front_url = ?, ekyc_back_url = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, frontUrl); pst.setString(2, backUrl); pst.setInt(3, userId); return pst.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    public static boolean insertExperience(int userId, String duration, String location, String description) {
        String sql = "INSERT INTO tutor_experiences (user_id, duration, location, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId); pst.setString(2, duration); pst.setString(3, location); pst.setString(4, description);
            return pst.executeUpdate() > 0;
        } catch (Exception e) { return false; }
    }

    public static java.util.List<String> getExperiences(int userId) {
        java.util.List<String> list = new java.util.ArrayList<>();
        String sql = "SELECT * FROM tutor_experiences WHERE user_id = ? ORDER BY id DESC";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId); ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                list.add(rs.getString("duration") + "|" + rs.getString("location") + "|" + rs.getString("description").replace("\n", " ") + "|" + rs.getString("status"));
            }
        } catch (Exception e) { }
        return list;
    }

    static {
        try (Connection conn = getConnection(); java.sql.Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE reels ADD COLUMN IF NOT EXISTS location VARCHAR(255)");
            st.execute("ALTER TABLE reels ADD COLUMN IF NOT EXISTS product_link VARCHAR(255)");
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static boolean insertReel(int userId, String videoUrl, String caption, String hashtags, String location, String productLink) {
        String sql = "INSERT INTO reels (user_id, video_url, caption, hashtags, location, product_link) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, videoUrl);
            ps.setString(3, caption);
            ps.setString(4, hashtags);
            ps.setString(5, location);
            ps.setString(6, productLink);
            return ps.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static boolean likeReel(int reelId, int userId) {
        String checkSql = "SELECT 1 FROM reel_likes WHERE reel_id = ? AND user_id = ?";
        boolean isLiked = false;
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(checkSql)) {
            pst.setInt(1, reelId);
            pst.setInt(2, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                isLiked = true;
            }
        } catch (Exception e) { return false; }

        if (isLiked) {
            String delSql = "DELETE FROM reel_likes WHERE reel_id = ? AND user_id = ?";
            String upSql = "UPDATE reels SET likes = GREATEST(COALESCE(likes, 0) - 1, 0) WHERE id = ?";
            try (Connection conn = getConnection(); 
                 PreparedStatement pstDel = conn.prepareStatement(delSql);
                 PreparedStatement pstUp = conn.prepareStatement(upSql)) {
                pstDel.setInt(1, reelId); pstDel.setInt(2, userId);
                pstDel.executeUpdate();
                pstUp.setInt(1, reelId);
                pstUp.executeUpdate();
                return true;
            } catch (Exception e) { return false; }
        } else {
            String insSql = "INSERT INTO reel_likes (reel_id, user_id) VALUES (?, ?)";
            String upSql = "UPDATE reels SET likes = COALESCE(likes, 0) + 1 WHERE id = ?";
            try (Connection conn = getConnection(); 
                 PreparedStatement pstIns = conn.prepareStatement(insSql);
                 PreparedStatement pstUp = conn.prepareStatement(upSql)) {
                pstIns.setInt(1, reelId); pstIns.setInt(2, userId);
                pstIns.executeUpdate();
                pstUp.setInt(1, reelId);
                pstUp.executeUpdate();
                return true;
            } catch (Exception e) { return false; }
        }
    }

    public static java.util.List<String> getReels(int currentUserId) {
        java.util.List<String> list = new java.util.ArrayList<>();
        String sql = "SELECT r.*, u.full_name, u.avatar_url, " +
                     "(SELECT 1 FROM reel_likes rl WHERE rl.reel_id = r.id AND rl.user_id = ?) as is_liked, " +
                     "(SELECT COUNT(*) FROM reel_comments rc WHERE rc.reel_id = r.id) as comment_count " +
                     "FROM reels r LEFT JOIN users u ON r.user_id = u.id ORDER BY r.id DESC LIMIT 50";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, currentUserId);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                int id = rs.getInt("id");
                String videoUrl = rs.getString("video_url");
                String caption = rs.getString("caption");
                String hashtags = rs.getString("hashtags");
                int likes = rs.getInt("likes");
                String fullName = rs.getString("full_name");
                String avatarUrl = rs.getString("avatar_url");
                int isLiked = rs.getInt("is_liked");
                int commentCount = rs.getInt("comment_count");
                
                String location = rs.getString("location");
                if (location == null || location.trim().isEmpty() || location.equals("null")) location = "";
                
                String productLink = rs.getString("product_link");
                if (productLink == null || productLink.trim().isEmpty() || productLink.equals("null")) productLink = "";
                
                String avatarBase64 = "DEFAULT";
                if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                    java.io.File f = new java.io.File(avatarUrl);
                    if (f.exists()) {
                        byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                        avatarBase64 = java.util.Base64.getEncoder().encodeToString(bytes);
                    }
                }
                
                list.add(id + ";;" + videoUrl + ";;" + caption + ";;" + hashtags + ";;" + likes + ";;" + fullName + ";;" + avatarBase64 + ";;" + isLiked + ";;" + commentCount + ";;" + location + ";;" + productLink);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static boolean insertReelComment(int reelId, int userId, String content) {
        String sql = "INSERT INTO reel_comments (reel_id, user_id, content) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, reelId); pst.setInt(2, userId); pst.setString(3, content);
            return pst.executeUpdate() > 0;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public static java.util.List<String> getReelComments(int reelId) {
        java.util.List<String> list = new java.util.ArrayList<>();
        String sql = "SELECT c.*, u.full_name, u.avatar_url FROM reel_comments c JOIN users u ON c.user_id = u.id WHERE c.reel_id = ? ORDER BY c.id ASC";
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, reelId);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                String fullName = rs.getString("full_name");
                String content = rs.getString("content");
                String avatarUrl = rs.getString("avatar_url");
                java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                long time = createdAt != null ? createdAt.getTime() : System.currentTimeMillis();
                
                String avatarBase64 = "DEFAULT";
                if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                    java.io.File f = new java.io.File(avatarUrl);
                    if (f.exists()) {
                        byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
                        avatarBase64 = java.util.Base64.getEncoder().encodeToString(bytes);
                    }
                }
                list.add(fullName + ";;" + content + ";;" + avatarBase64 + ";;" + time);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    public static boolean insertLocket(int userId, String mediaUrl, String title, String mediaType) {
        String sql = "INSERT INTO locket_videos (user_id, media_url, title, media_type) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.setString(2, mediaUrl);
            pst.setString(3, title);
            pst.setString(4, mediaType);
            pst.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("[DB LỖI] insertLocket: " + e.getMessage());
            return false;
        }
    }

    public static java.util.List<String> getLocketVideos() {
        String sql = "SELECT lv.id, lv.media_url, lv.title, lv.media_type, u.full_name, u.avatar_url, lv.created_at "
                   + "FROM locket_videos lv "
                   + "JOIN users u ON u.id = lv.user_id "
                   + "ORDER BY lv.created_at DESC";
        java.util.List<String> result = new java.util.ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String url = rs.getString("media_url");
                String t = rs.getString("title");
                String mt = rs.getString("media_type");
                String author = rs.getString("full_name");
                String avatarPath = rs.getString("avatar_url");
                String avatarB64 = "";
                if (avatarPath != null && !avatarPath.isEmpty()) {
                    java.io.File f = new java.io.File(avatarPath);
                    if (f.exists()) {
                        try { avatarB64 = java.util.Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(f.toPath())); }
                        catch (Exception ignore) {}
                    }
                }
                result.add(id + ";;" + url + ";;" + t + ";;" + mt + ";;" + author + ";;" + avatarB64);
            }
        } catch (Exception e) {
            System.err.println("[DB LỖI] getLocketVideos: " + e.getMessage());
        }
        return result;
    }

    public static void deleteLocket(int id, int userId) {
        String sql = "DELETE FROM locket_videos WHERE id = ? AND user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.setInt(2, userId);
            pst.executeUpdate();
        } catch (Exception e) {
            System.err.println("[DB LỖI] deleteLocket: " + e.getMessage());
        }
    }
}
