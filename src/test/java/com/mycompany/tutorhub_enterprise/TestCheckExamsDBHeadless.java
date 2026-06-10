package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TestCheckExamsDBHeadless {
    public static void main(String[] args) {
        System.out.println("Kiểm tra DB trực tiếp...");
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn == null) {
                System.out.println("Lỗi: Không thể kết nối Database!");
                return;
            }
            
            String dbUrl = conn.getMetaData().getURL();
            // Mask password if any (JDBC URLs typically don't show password but we'll mask credentials if present)
            System.out.println("Kết nối DB thành công. URL: " + dbUrl.replaceAll("(?<=password=)[^&;]*", "****"));
            
            // 1. Tổng số dòng trong bảng exams
            try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM exams");
                 ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Tổng số dòng trong bảng exams: " + rs.getInt(1));
                }
            }
            
            // 2. COUNT theo từng status
            System.out.println("--- Số lượng exam theo status ---");
            try (PreparedStatement pst = conn.prepareStatement("SELECT status, COUNT(*) FROM exams GROUP BY status");
                 ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    System.out.println("Status: " + rs.getString("status") + " - Count: " + rs.getInt(2));
                }
            }
            
            // 3. 20 exam mới nhất
            System.out.println("--- 20 exam mới nhất ---");
            try (PreparedStatement pst = conn.prepareStatement("SELECT id, title, status, creator_id, duration_mins FROM exams ORDER BY id DESC LIMIT 20");
                 ResultSet rs = pst.executeQuery()) {
                boolean hasAny = false;
                while (rs.next()) {
                    hasAny = true;
                    System.out.printf("ID: %d | Status: %s | Title: %s | Creator: %d | Duration: %d%n",
                            rs.getInt("id"),
                            rs.getString("status"),
                            rs.getString("title"),
                            rs.getInt("creator_id"),
                            rs.getInt("duration_mins"));
                }
                if (!hasAny) {
                    System.out.println("Không có exam nào trong DB.");
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
