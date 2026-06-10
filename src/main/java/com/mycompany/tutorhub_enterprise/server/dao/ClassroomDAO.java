package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.ClassroomGroupModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomLessonModel;
import com.mycompany.tutorhub_enterprise.models.ClassroomMemberModel;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ClassroomDAO {

    public boolean createClassroom(ClassroomGroupModel model) {
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try {
                int classroomId = insertClassroom(con, model);
                addOwnerMember(con, classroomId, model.getOwnerId());
                con.commit();
                model.setId(classroomId);
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] createClassroom failed: " + e.getMessage());
        }
        return false;
    }

    public boolean createLiveClassroom(ClassroomGroupModel classroom, ClassroomLessonModel lesson) {
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try {
                int classroomId = insertClassroom(con, classroom);
                classroom.setId(classroomId);
                addOwnerMember(con, classroomId, classroom.getOwnerId());

                prepareDefaultLiveLesson(classroom, lesson);
                lesson.setClassroomId(classroomId);
                int lessonId = insertLesson(con, lesson);
                lesson.setId(lessonId);

                String boardId = "LESSON_" + lessonId;
                updateLessonBoardId(con, lessonId, boardId);
                lesson.setBoardId(boardId);

                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] createLiveClassroom failed: " + e.getMessage());
        }
        return false;
    }

    public boolean createPublicLesson(ClassroomGroupModel classroom, ClassroomLessonModel lesson) {
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try {
                int classroomId = insertClassroom(con, classroom);
                classroom.setId(classroomId);
                addOwnerMember(con, classroomId, classroom.getOwnerId());

                prepareDefaultPublicLesson(classroom, lesson);
                lesson.setClassroomId(classroomId);
                int lessonId = insertLesson(con, lesson);
                lesson.setId(lessonId);

                String boardId = "LESSON_" + lessonId;
                updateLessonBoardId(con, lessonId, boardId);
                lesson.setBoardId(boardId);

                con.commit();
                return true;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] createPublicLesson failed: " + e.getMessage());
        }
        return false;
    }

    public List<ClassroomGroupModel> getClassroomsByUser(int userId) {
        List<ClassroomGroupModel> list = new ArrayList<>();
        String sql = "SELECT cg.* FROM classroom_groups cg " +
                     "JOIN classroom_members cm ON cg.id = cm.classroom_id " +
                     "WHERE cm.user_id = ? AND cg.status = 'ACTIVE' ORDER BY cg.created_at DESC";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClassroomGroupModel model = new ClassroomGroupModel();
                    model.setId(rs.getInt("id"));
                    model.setOwnerId(rs.getInt("owner_id"));
                    model.setName(rs.getString("name"));
                    model.setCoverImage(rs.getString("cover_image"));
                    model.setDescription(rs.getString("description"));
                    model.setOrganizationName(rs.getString("organization_name"));
                    model.setJoinCode(rs.getString("join_code"));
                    model.setStatus(rs.getString("status"));
                    model.setCreatedAt(rs.getTimestamp("created_at"));
                    list.add(model);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] getClassroomsByUser failed: " + e.getMessage());
        }
        return list;
    }

    public List<ClassroomLessonModel> getLessonsByUser(int userId) {
        List<ClassroomLessonModel> list = new ArrayList<>();
        String sql = "SELECT cl.*, cg.name AS classroom_name, cg.organization_name, cg.join_code, " +
                     "COALESCE(cm.member_status, 'APPROVED') AS member_status " +
                     "FROM classroom_lessons cl " +
                     "JOIN classroom_groups cg ON cg.id = cl.classroom_id " +
                     "JOIN classroom_members cm ON cm.classroom_id = cg.id " +
                     "WHERE cm.user_id = ? AND cg.status = 'ACTIVE' " +
                     "ORDER BY " +
                     "CASE WHEN cl.status = 'LIVE' THEN 0 WHEN cl.start_time >= CURRENT_TIMESTAMP THEN 1 ELSE 2 END, " +
                     "cl.start_time ASC, cl.created_at DESC";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapLesson(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] getLessonsByUser failed: " + e.getMessage());
        }
        return list;
    }

        public ClassroomGroupModel joinClassroomByCode(int userId, String rawCode) {
        String code = normalizeJoinCode(rawCode);
        if (code.isEmpty()) return null;
        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try {
                ClassroomGroupModel group = findClassroomGroupByCode(con, code);
                if (group == null) {
                    con.rollback();
                    return null;
                }
                addStudentMember(con, group.getId(), userId, "APPROVED");
                con.commit();
                return group;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] joinClassroomByCode failed: " + e.getMessage());
        }
        return null;
    }

    private ClassroomGroupModel findClassroomGroupByCode(Connection con, String code) throws SQLException {
        String sql = "SELECT * FROM classroom_groups WHERE status = 'ACTIVE' AND UPPER(join_code) = UPPER(?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ClassroomGroupModel model = new ClassroomGroupModel();
                    model.setId(rs.getInt("id"));
                    model.setOwnerId(rs.getInt("owner_id"));
                    model.setName(rs.getString("name"));
                    model.setCoverImage(rs.getString("cover_image"));
                    model.setDescription(rs.getString("description"));
                    model.setOrganizationName(rs.getString("organization_name"));
                    model.setJoinCode(rs.getString("join_code"));
                    model.setStatus(rs.getString("status"));
                    model.setCreatedAt(rs.getTimestamp("created_at"));
                    return model;
                }
            }
        }
        return null;
    }

    public ClassroomLessonModel joinPublicLessonByCode(int userId, String rawCode) {
        String code = normalizeJoinCode(rawCode);
        if (code.isEmpty()) {
            return null;
        }

        try (Connection con = DatabaseManager.getConnection()) {
            con.setAutoCommit(false);
            try {
                ClassroomLessonModel lesson = findPublicLessonByCode(con, code);
                if (lesson == null) {
                    con.rollback();
                    return null;
                }

                String desiredStatus = lesson.isLobbyEnabled() && lesson.getCreatedBy() != userId ? "WAITING" : "APPROVED";
                addStudentMember(con, lesson.getClassroomId(), userId, desiredStatus);
                lesson.setMemberStatus(getMemberStatus(con, lesson.getClassroomId(), userId));
                con.commit();
                return lesson;
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] joinPublicLessonByCode failed: " + e.getMessage());
        }
        return null;
    }

    public boolean createLesson(ClassroomLessonModel model) {
        try (Connection con = DatabaseManager.getConnection()) {
            int lessonId = insertLesson(con, model);
            model.setId(lessonId);
            if (model.getBoardId() == null || model.getBoardId().trim().isEmpty()) {
                String boardId = "LESSON_" + lessonId;
                updateLessonBoardId(con, lessonId, boardId);
                model.setBoardId(boardId);
            }
            return true;
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] createLesson failed: " + e.getMessage());
        }
        return false;
    }

    private int insertClassroom(Connection con, ClassroomGroupModel model) throws SQLException {
        String sql = "INSERT INTO classroom_groups (owner_id, name, cover_image, description, organization_name, join_code) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String joinCode = valueOrDefault(model.getJoinCode(), buildJoinCode());
            model.setJoinCode(joinCode);

            ps.setInt(1, model.getOwnerId());
            ps.setString(2, model.getName());
            ps.setString(3, valueOrEmpty(model.getCoverImage()));
            ps.setString(4, valueOrEmpty(model.getDescription()));
            ps.setString(5, valueOrDefault(model.getOrganizationName(), "My Account"));
            ps.setString(6, joinCode);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Cannot create classroom.");
    }

        public List<ClassroomMemberModel> getApprovedMembersForLesson(int lessonId) {
        List<ClassroomMemberModel> list = new ArrayList<>();
        String sql = "SELECT cl.id AS lesson_id, cm.classroom_id, cm.user_id, cm.role, " +
                     "COALESCE(cm.member_status, 'APPROVED') AS member_status, cm.joined_at, " +
                     "u.full_name, u.email " +
                     "FROM classroom_members cm " +
                     "JOIN classroom_lessons cl ON cl.classroom_id = cm.classroom_id " +
                     "JOIN users u ON u.id = cm.user_id " +
                     "WHERE cl.id = ? " +
                     "AND UPPER(COALESCE(cm.member_status, 'APPROVED')) = 'APPROVED' " +
                     "AND cm.role != 'OWNER' " +
                     "ORDER BY cm.joined_at ASC";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, lessonId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClassroomMemberModel model = new ClassroomMemberModel();
                    model.setLessonId(rs.getInt("lesson_id"));
                    model.setClassroomId(rs.getInt("classroom_id"));
                    model.setUserId(rs.getInt("user_id"));
                    model.setRole(rs.getString("role"));
                    model.setMemberStatus(rs.getString("member_status"));
                    model.setJoinedAt(rs.getTimestamp("joined_at"));
                    model.setFullName(rs.getString("full_name"));
                    model.setEmail(rs.getString("email"));
                    list.add(model);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] getApprovedMembersForLesson failed: " + e.getMessage());
        }
        return list;
    }
    public List<ClassroomMemberModel> getWaitingMembersForLesson(int lessonId, int teacherId) {
        List<ClassroomMemberModel> list = new ArrayList<>();
        String sql = "SELECT cl.id AS lesson_id, cm.classroom_id, cm.user_id, cm.role, " +
                     "COALESCE(cm.member_status, 'APPROVED') AS member_status, cm.joined_at, " +
                     "u.full_name, u.email " +
                     "FROM classroom_members cm " +
                     "JOIN classroom_lessons cl ON cl.classroom_id = cm.classroom_id " +
                     "JOIN users u ON u.id = cm.user_id " +
                     "WHERE cl.id = ? AND cl.created_by = ? " +
                     "AND UPPER(COALESCE(cm.member_status, 'APPROVED')) = 'WAITING' " +
                     "ORDER BY cm.joined_at ASC";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, lessonId);
            ps.setInt(2, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ClassroomMemberModel model = new ClassroomMemberModel();
                    model.setLessonId(rs.getInt("lesson_id"));
                    model.setClassroomId(rs.getInt("classroom_id"));
                    model.setUserId(rs.getInt("user_id"));
                    model.setRole(rs.getString("role"));
                    model.setMemberStatus(rs.getString("member_status"));
                    model.setJoinedAt(rs.getTimestamp("joined_at"));
                    model.setFullName(rs.getString("full_name"));
                    model.setEmail(rs.getString("email"));
                    list.add(model);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] getWaitingMembersForLesson failed: " + e.getMessage());
        }
        return list;
    }

    public ClassroomLessonModel approveLessonStudent(int lessonId, int studentId, int teacherId) {
        String sql = "UPDATE classroom_members cm SET member_status = 'APPROVED' " +
                     "FROM classroom_lessons cl " +
                     "WHERE cm.classroom_id = cl.classroom_id AND cl.id = ? AND cl.created_by = ? AND cm.user_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, lessonId);
            ps.setInt(2, teacherId);
            ps.setInt(3, studentId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ClassroomLessonModel lesson = getLessonForMember(lessonId, studentId);
                if (lesson != null) {
                    lesson.setMemberStatus("APPROVED");
                }
                return lesson;
            }
        } catch (SQLException e) {
            System.err.println("[ClassroomDAO] approveLessonStudent failed: " + e.getMessage());
        }
        return null;
    }

    private ClassroomLessonModel getLessonForMember(int lessonId, int userId) throws SQLException {
        String sql = "SELECT cl.*, cg.name AS classroom_name, cg.organization_name, cg.join_code, " +
                     "COALESCE(cm.member_status, 'APPROVED') AS member_status " +
                     "FROM classroom_lessons cl " +
                     "JOIN classroom_groups cg ON cg.id = cl.classroom_id " +
                     "JOIN classroom_members cm ON cm.classroom_id = cg.id " +
                     "WHERE cl.id = ? AND cm.user_id = ?";
        try (Connection con = DatabaseManager.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, lessonId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLesson(rs);
                }
            }
        }
        return null;
    }

    private void addOwnerMember(Connection con, int classroomId, int ownerId) throws SQLException {
        String sqlMember = "INSERT INTO classroom_members (classroom_id, user_id, role, member_status) VALUES (?, ?, ?, 'APPROVED') " +
                           "ON CONFLICT (classroom_id, user_id) DO NOTHING";
        try (PreparedStatement psMem = con.prepareStatement(sqlMember)) {
            psMem.setInt(1, classroomId);
            psMem.setInt(2, ownerId);
            psMem.setString(3, "OWNER");
            psMem.executeUpdate();
        }
    }

    private void addStudentMember(Connection con, int classroomId, int userId, String memberStatus) throws SQLException {
        String sqlMember = "INSERT INTO classroom_members (classroom_id, user_id, role, member_status) VALUES (?, ?, ?, ?) " +
                           "ON CONFLICT (classroom_id, user_id) DO UPDATE SET " +
                           "member_status = CASE " +
                           "WHEN classroom_members.member_status = 'APPROVED' THEN 'APPROVED' " +
                           "ELSE EXCLUDED.member_status END";
        try (PreparedStatement psMem = con.prepareStatement(sqlMember)) {
            psMem.setInt(1, classroomId);
            psMem.setInt(2, userId);
            psMem.setString(3, "STUDENT");
            psMem.setString(4, valueOrDefault(memberStatus, "APPROVED"));
            psMem.executeUpdate();
        }
    }

    private String getMemberStatus(Connection con, int classroomId, int userId) throws SQLException {
        String sql = "SELECT COALESCE(member_status, 'APPROVED') AS member_status FROM classroom_members WHERE classroom_id = ? AND user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, classroomId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("member_status");
                }
            }
        }
        return "APPROVED";
    }

    private ClassroomLessonModel findPublicLessonByCode(Connection con, String code) throws SQLException {
        String sql = "SELECT cl.*, cg.name AS classroom_name, cg.organization_name, cg.join_code " +
                     "FROM classroom_lessons cl " +
                     "JOIN classroom_groups cg ON cg.id = cl.classroom_id " +
                     "WHERE cg.status = 'ACTIVE' AND UPPER(cl.lesson_type) = 'PUBLIC' " +
                     "AND (UPPER(cg.join_code) = UPPER(?) OR UPPER(cl.board_id) = UPPER(?) OR CAST(cl.id AS TEXT) = ?) " +
                     "ORDER BY cl.start_time ASC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, code);
            ps.setString(3, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapLesson(rs);
                }
            }
        }
        return null;
    }

    private ClassroomLessonModel mapLesson(ResultSet rs) throws SQLException {
        ClassroomLessonModel model = new ClassroomLessonModel();
        model.setId(rs.getInt("id"));
        model.setClassroomId(rs.getInt("classroom_id"));
        model.setClassroomName(rs.getString("classroom_name"));
        model.setOrganizationName(rs.getString("organization_name"));
        model.setJoinCode(rs.getString("join_code"));
        model.setTitle(rs.getString("title"));
        model.setStartTime(rs.getTimestamp("start_time"));
        model.setDurationMinutes(rs.getInt("duration_minutes"));
        model.setSeatCount(rs.getInt("seat_count"));
        model.setStatus(rs.getString("status"));
        model.setBoardId(rs.getString("board_id"));
        model.setLessonType(rs.getString("lesson_type"));
        model.setStageLayout(rs.getString("stage_layout"));
        model.setLobbyEnabled(rs.getBoolean("lobby_enabled"));
        model.setAllowStudentDraw(rs.getBoolean("allow_student_draw"));
        model.setRecordingEnabled(rs.getBoolean("recording_enabled"));
        model.setCreatedBy(rs.getInt("created_by"));
        try {
            model.setMemberStatus(rs.getString("member_status"));
        } catch (SQLException ignored) {
            model.setMemberStatus("APPROVED");
        }
        model.setCreatedAt(rs.getTimestamp("created_at"));
        return model;
    }

    private int insertLesson(Connection con, ClassroomLessonModel model) throws SQLException {
        String sql = "INSERT INTO classroom_lessons " +
                "(classroom_id, title, start_time, duration_minutes, seat_count, status, board_id, lesson_type, stage_layout, lobby_enabled, allow_student_draw, recording_enabled, created_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, model.getClassroomId());
            ps.setString(2, valueOrDefault(model.getTitle(), "Live Lesson"));
            ps.setTimestamp(3, model.getStartTime() != null ? model.getStartTime() : new Timestamp(System.currentTimeMillis()));
            ps.setInt(4, model.getDurationMinutes() > 0 ? model.getDurationMinutes() : 40);
            ps.setInt(5, model.getSeatCount() > 0 ? model.getSeatCount() : 6);
            ps.setString(6, valueOrDefault(model.getStatus(), "SCHEDULED"));
            ps.setString(7, valueOrEmpty(model.getBoardId()));
            ps.setString(8, valueOrDefault(model.getLessonType(), "CLASSROOM"));
            ps.setString(9, valueOrDefault(model.getStageLayout(), "1V6"));
            ps.setBoolean(10, model.isLobbyEnabled());
            ps.setBoolean(11, model.isAllowStudentDraw());
            ps.setBoolean(12, model.isRecordingEnabled());
            ps.setInt(13, model.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Cannot create classroom lesson.");
    }

    private void prepareDefaultLiveLesson(ClassroomGroupModel classroom, ClassroomLessonModel lesson) {
        lesson.setTitle(valueOrDefault(lesson.getTitle(), classroom.getName()));
        lesson.setStartTime(lesson.getStartTime() != null ? lesson.getStartTime() : new Timestamp(System.currentTimeMillis()));
        lesson.setDurationMinutes(lesson.getDurationMinutes() > 0 ? lesson.getDurationMinutes() : 40);
        lesson.setSeatCount(lesson.getSeatCount() > 0 ? lesson.getSeatCount() : 6);
        lesson.setStatus(valueOrDefault(lesson.getStatus(), "LIVE"));
        lesson.setLessonType(valueOrDefault(lesson.getLessonType(), "CLASSROOM"));
        lesson.setStageLayout(valueOrDefault(lesson.getStageLayout(), "1V6"));
        lesson.setLobbyEnabled(true);
        lesson.setAllowStudentDraw(false);
        lesson.setRecordingEnabled(false);
        lesson.setCreatedBy(lesson.getCreatedBy() > 0 ? lesson.getCreatedBy() : classroom.getOwnerId());
    }

    private void prepareDefaultPublicLesson(ClassroomGroupModel classroom, ClassroomLessonModel lesson) {
        lesson.setTitle(valueOrDefault(lesson.getTitle(), classroom.getName()));
        lesson.setStartTime(lesson.getStartTime() != null ? lesson.getStartTime() : new Timestamp(System.currentTimeMillis()));
        lesson.setDurationMinutes(lesson.getDurationMinutes() > 0 ? lesson.getDurationMinutes() : 40);
        lesson.setSeatCount(lesson.getSeatCount() > 0 ? lesson.getSeatCount() : 6);
        lesson.setStatus(valueOrDefault(lesson.getStatus(), "SCHEDULED"));
        lesson.setLessonType(valueOrDefault(lesson.getLessonType(), "PUBLIC"));
        lesson.setStageLayout(valueOrDefault(lesson.getStageLayout(), "1V6"));
        lesson.setCreatedBy(lesson.getCreatedBy() > 0 ? lesson.getCreatedBy() : classroom.getOwnerId());
    }

    private void updateLessonBoardId(Connection con, int lessonId, String boardId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE classroom_lessons SET board_id = ? WHERE id = ?")) {
            ps.setString(1, boardId);
            ps.setInt(2, lessonId);
            ps.executeUpdate();
        }
    }

    private String buildJoinCode() {
        long value = Math.abs(System.nanoTime() % 1_000_000L);
        return String.format("CL%06d", value);
    }

    private String normalizeJoinCode(String rawCode) {
        if (rawCode == null) {
            return "";
        }
        String code = rawCode.trim();
        if (code.isEmpty()) {
            return "";
        }

        String lowerCode = code.toLowerCase();
        int queryIndex = lowerCode.indexOf("code=");
        if (queryIndex >= 0) {
            code = code.substring(queryIndex + 5);
            int end = code.indexOf('&');
            if (end >= 0) {
                code = code.substring(0, end);
            }
        } else if (code.contains("/")) {
            int lastSlash = code.lastIndexOf('/');
            code = code.substring(lastSlash + 1);
        }

        int queryTail = code.indexOf('?');
        if (queryTail >= 0) {
            code = code.substring(0, queryTail);
        }
        int hashTail = code.indexOf('#');
        if (hashTail >= 0) {
            code = code.substring(0, hashTail);
        }

        return code.trim();
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}


