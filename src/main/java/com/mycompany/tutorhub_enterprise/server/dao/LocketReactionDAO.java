package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LocketReactionDAO {

    public boolean toggleHeart(long postId, int userId) {
        String checkSql = "SELECT 1 FROM locket_reactions WHERE post_id = ? AND user_id = ? AND reaction_type = 'HEART'";
        boolean currentlyReacted = false;
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setLong(1, postId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                currentlyReacted = rs.next();
            }
            
            if (currentlyReacted) {
                // Delete reaction
                String delSql = "DELETE FROM locket_reactions WHERE post_id = ? AND user_id = ? AND reaction_type = 'HEART'";
                try (PreparedStatement delStmt = conn.prepareStatement(delSql)) {
                    delStmt.setLong(1, postId);
                    delStmt.setInt(2, userId);
                    delStmt.executeUpdate();
                }
                updateLikeCount(conn, postId, -1);
                return false; // Result state: not reacted
            } else {
                // Insert reaction
                String insSql = "INSERT INTO locket_reactions (post_id, user_id, reaction_type) VALUES (?, ?, 'HEART')";
                try (PreparedStatement insStmt = conn.prepareStatement(insSql)) {
                    insStmt.setLong(1, postId);
                    insStmt.setInt(2, userId);
                    insStmt.executeUpdate();
                }
                updateLikeCount(conn, postId, 1);
                return true; // Result state: reacted
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasReacted(long postId, int userId) {
        String sql = "SELECT 1 FROM locket_reactions WHERE post_id = ? AND user_id = ? AND reaction_type = 'HEART'";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int countByPost(long postId) {
        String sql = "SELECT COUNT(*) FROM locket_reactions WHERE post_id = ? AND reaction_type = 'HEART'";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void updateLikeCount(Connection conn, long postId, int delta) throws SQLException {
        String sql = "UPDATE locket_posts SET like_count = GREATEST(0, like_count + ?) WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, delta);
            pstmt.setLong(2, postId);
            pstmt.executeUpdate();
        }
    }
}
