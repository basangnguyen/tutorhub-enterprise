package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.CalendarPollModel;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CalendarPollDAO {
    
    // Hàm lưu Cuộc khảo sát mới vào CSDL
    public boolean insertPoll(CalendarPollModel poll) {
        String sql = "INSERT INTO calendar_polls (tutor_id, title, description, date_list, start_time, end_time, unique_code) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, poll.getTutorId());
            pstmt.setString(2, poll.getTitle());
            pstmt.setString(3, poll.getDescription());
            pstmt.setString(4, poll.getDateList());
            pstmt.setTime(5, java.sql.Time.valueOf(poll.getStartTime()));
            pstmt.setTime(6, java.sql.Time.valueOf(poll.getEndTime()));
            pstmt.setString(7, poll.getUniqueCode());
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi tạo Khảo sát: " + e.getMessage());
            return false;
        }
    }
    // 1. Hàm lấy thông tin Cuộc khảo sát dựa vào Mã Code (VD: 1734DB)
    public CalendarPollModel getPollByCode(String uniqueCode) {
        String sql = "SELECT * FROM calendar_polls WHERE unique_code = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, uniqueCode);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                CalendarPollModel poll = new CalendarPollModel();
                poll.setPollId(rs.getInt("poll_id"));
                poll.setTutorId(rs.getInt("tutor_id"));
                poll.setTitle(rs.getString("title"));
                poll.setDescription(rs.getString("description"));
                poll.setDateList(rs.getString("date_list"));
                poll.setStartTime(rs.getTime("start_time").toLocalTime());
                poll.setEndTime(rs.getTime("end_time").toLocalTime());
                poll.setUniqueCode(rs.getString("unique_code"));
                return poll;
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lấy thông tin Khảo sát: " + e.getMessage());
        }
        return null; // Trả về null nếu không tìm thấy mã
    }

    // 2. Hàm lưu mốc thời gian rảnh Học viên vừa bôi đen vào Database
    public boolean insertVote(int pollId, String voterName, String voterEmail, String availableSlotsJson) {
        String sql = "INSERT INTO calendar_poll_votes (poll_id, voter_name, voter_email, available_slots) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, pollId);
            pstmt.setString(2, voterName);
            pstmt.setString(3, voterEmail);
            pstmt.setString(4, availableSlotsJson);
            
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lưu lượt Bình chọn: " + e.getMessage());
            return false;
        }
    }

    // 3. Hàm lấy tất cả khảo sát của một gia sư (để hiển thị trên lịch)
    public java.util.List<CalendarPollModel> getPollsByTutor(int tutorId) {
        java.util.List<CalendarPollModel> polls = new java.util.ArrayList<>();
        String sql = "SELECT * FROM calendar_polls WHERE tutor_id = ? ORDER BY poll_id DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, tutorId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                CalendarPollModel poll = new CalendarPollModel();
                poll.setPollId(rs.getInt("poll_id"));
                poll.setTutorId(rs.getInt("tutor_id"));
                poll.setTitle(rs.getString("title"));
                poll.setDescription(rs.getString("description"));
                poll.setDateList(rs.getString("date_list"));
                poll.setStartTime(rs.getTime("start_time").toLocalTime());
                poll.setEndTime(rs.getTime("end_time").toLocalTime());
                poll.setUniqueCode(rs.getString("unique_code"));
                polls.add(poll);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi lấy danh sách Khảo sát: " + e.getMessage());
        }
        return polls;
    }

    public boolean deletePoll(int pollId) {
        String sql = "DELETE FROM calendar_polls WHERE poll_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, pollId);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi xóa Khảo sát: " + e.getMessage());
            return false;
        }
    }
}