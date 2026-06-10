package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ClassDAO {
    
    public static List<String> getAvailableClasses() {
        List<String> classesList = new ArrayList<>();
        // ĐÃ FIX: Chuyển 'AVAILABLE' thành 'OPEN' để khớp với Database trên Neon
        String sql = "SELECT class_code, title, salary, location, schedule_desc, requirement, tag_text, tag_color " +
                     "FROM classes WHERE status = 'OPEN' ORDER BY created_at ASC";
                     
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
             
            while (rs.next()) {
                String formattedSalary = String.format("%,.0fđ/buổi", rs.getDouble("salary")).replace(",", ".");
                String classData = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
                        rs.getString("class_code"), rs.getString("title"), formattedSalary,
                        rs.getString("location"), rs.getString("schedule_desc"),
                        rs.getString("requirement"), rs.getString("tag_text"), rs.getString("tag_color"));
                classesList.add(classData);
            }
            System.out.println("[DAO] Đã lấy được: " + classesList.size() + " lớp học từ Database.");
        } catch (SQLException e) {
            System.err.println("[DAO LỖI] Truy vấn danh sách lớp: " + e.getMessage());
        }
        return classesList;
    }

    public static boolean insertClass(String classCode, String subject, String tuition, String location, String title, String description) {
        // ĐÃ FIX: Khi tạo lớp mới mặc định status là 'OPEN'
        String sql = "INSERT INTO classes (class_code, title, salary, location, schedule_desc, requirement, tag_text, tag_color, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'OPEN')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
             
            double salary = 0;
            try { salary = Double.parseDouble(tuition.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}

            pst.setString(1, classCode);
            pst.setString(2, subject);       
            pst.setDouble(3, salary);        
            pst.setString(4, location);      
            pst.setString(5, "Thứ 2,4,6");   
            pst.setString(6, title + " - " + description); 
            pst.setString(7, "MỚI");         
            pst.setString(8, "#10B981");     
            
            int affectedRows = pst.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("[DAO] Đã tạo thành công lớp mới: " + classCode);
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("[DAO LỖI] Lỗi tạo lớp mới: " + e.getMessage());
            return false;
        }
    }

    public static boolean updateClassStatus(String classCode, int tutorId) {
        // ĐÃ FIX: Cập nhật điều kiện WHERE status = 'OPEN' thay vì 'AVAILABLE'
        String sql = "UPDATE classes SET status = 'TAKEN', tutor_id = ? WHERE class_code = ? AND status = 'OPEN'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, tutorId);
            pst.setString(2, classCode);
            return pst.executeUpdate() > 0; 
        } catch (SQLException e) {
            System.err.println("[DAO LỖI] Lỗi chốt đơn: " + e.getMessage());
            return false;
        }
    }
}