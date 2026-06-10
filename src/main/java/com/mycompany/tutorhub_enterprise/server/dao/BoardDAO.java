package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class BoardDAO {

    // 1. Hàm lưu Bảng vẽ mới vào Database
    public static boolean saveBoard(int tutorId, String title, String className, String base64Thumbnail) {
        String boardId = "BW_" + System.currentTimeMillis(); 
        String sql = "INSERT INTO blackboards (board_id, tutor_id, title, class_name, thumbnail_base64) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, boardId);
            stmt.setInt(2, tutorId);
            stmt.setString(3, title);
            stmt.setString(4, className);
            stmt.setString(5, base64Thumbnail);
            
            return stmt.executeUpdate() > 0;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Hàm lấy danh sách Bảng vẽ định dạng theo chuẩn chuỗi "BW_...|Title|...;;"
    public static String getUserBoards(int tutorId) {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT * FROM blackboards WHERE tutor_id = ? ORDER BY last_modified DESC";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, tutorId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                sb.append(rs.getString("board_id")).append("|")
                  .append(rs.getString("title")).append("|")
                  .append(rs.getString("class_name")).append("|")
                  .append(rs.getString("last_modified")).append("|")
                  .append(rs.getString("size_mb")).append("|")
                  .append(rs.getInt("pages")).append("|")
                  .append(rs.getBoolean("is_current")).append("|")
                  .append(rs.getString("thumbnail_base64")).append(";;");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return sb.length() > 0 ? sb.toString() : "EMPTY";
    }
}