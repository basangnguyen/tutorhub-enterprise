package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.exam.QuestionBank;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class QuestionBankDAO {

    public static int createQuestionBank(String name, String description, int creatorId) {
        String sql = "INSERT INTO question_banks (name, description, creator_id) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, name);
            pst.setString(2, description);
            pst.setInt(3, creatorId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<QuestionBank> listQuestionBanksByCreator(int creatorId) {
        List<QuestionBank> list = new ArrayList<>();
        String sql = "SELECT * FROM question_banks WHERE creator_id = ? ORDER BY id DESC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, creatorId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                QuestionBank qb = new QuestionBank();
                qb.id = rs.getInt("id");
                qb.name = rs.getString("name");
                qb.description = rs.getString("description");
                qb.creatorId = rs.getInt("creator_id");
                qb.createdAt = rs.getTimestamp("created_at");
                qb.updatedAt = rs.getTimestamp("updated_at");
                list.add(qb);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static QuestionBank getQuestionBankById(int bankId) {
        String sql = "SELECT * FROM question_banks WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, bankId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                QuestionBank qb = new QuestionBank();
                qb.id = rs.getInt("id");
                qb.name = rs.getString("name");
                qb.description = rs.getString("description");
                qb.creatorId = rs.getInt("creator_id");
                qb.createdAt = rs.getTimestamp("created_at");
                qb.updatedAt = rs.getTimestamp("updated_at");
                return qb;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateQuestionBank(int bankId, String name, String description) {
        String sql = "UPDATE question_banks SET name = ?, description = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, name);
            pst.setString(2, description);
            pst.setInt(3, bankId);
            return pst.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean deleteQuestionBank(int bankId) {
        // Safe delete would need a status or is_deleted column. 
        // For now, only delete if there are no questions (enforced by DB constraints if we remove CASCADE later, but currently CASCADE is ON for some tables).
        // Best approach is a simple delete.
        String sql = "DELETE FROM question_banks WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, bankId);
            return pst.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
