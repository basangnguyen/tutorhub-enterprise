package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.TutorScheduleModel;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TutorScheduleDAO {

    // Lấy toàn bộ lịch trình (Có thể truyền thêm tham số userId vào đây nếu muốn lọc theo từng gia sư)
    public List<TutorScheduleModel> getSchedulesByTutor(int userId) {
        List<TutorScheduleModel> list = new ArrayList<>();
        String sql = "SELECT * FROM tutor_schedules WHERE user_id = ?";
        
        try (Connection conn = DatabaseManager.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TutorScheduleModel(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("title"),
                        rs.getInt("day_of_week"),
                        rs.getTime("start_time"),
                        rs.getTime("end_time"),
                        rs.getString("location"),
                        rs.getString("category"),
                        rs.getString("color_code")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    // Thêm hàm này vào TutorScheduleDAO.java
    public boolean updateScheduleTime(int scheduleId, int newDayOfWeek, Time newStartTime, Time newEndTime) {
        String sql = "UPDATE tutor_schedules SET day_of_week = ?, start_time = ?, end_time = ? WHERE id = ?";
        
        try (Connection conn = DatabaseManager.getConnection(); 
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, newDayOfWeek);
            ps.setTime(2, newStartTime);
            ps.setTime(3, newEndTime);
            ps.setInt(4, scheduleId);
            
            int rowsAffected = ps.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // Thêm vào TutorScheduleDAO.java
    public boolean insertSchedule(int userId, String title, int dayOfWeek, java.sql.Time startTime, java.sql.Time endTime, String location, String category, String colorCode) {
        String sql = "INSERT INTO tutor_schedules (user_id, title, day_of_week, start_time, end_time, location, category, color_code) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (java.sql.Connection conn = com.mycompany.tutorhub_enterprise.server.DatabaseManager.getConnection(); 
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setInt(3, dayOfWeek);
            ps.setTime(4, startTime);
            ps.setTime(5, endTime);
            ps.setString(6, location);
            ps.setString(7, category);
            ps.setString(8, colorCode);
            
            return ps.executeUpdate() > 0;
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}