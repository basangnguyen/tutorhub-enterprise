package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.locket.LocketCommentModel;
import com.mycompany.tutorhub_enterprise.models.locket.LocketCommentViewDTO;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocketCommentDAO {

    public List<LocketCommentViewDTO> listByPostId(long postId, int currentUserId, int limit, long cursor) {
        List<LocketCommentViewDTO> list = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name as author_name, u.avatar_url as author_avatar " +
                     "FROM locket_comments c " +
                     "JOIN users u ON c.user_id = u.id " +
                     "WHERE c.post_id = ? AND c.deleted_at IS NULL ";
        if (cursor > 0) {
            sql += "AND c.id > ? "; // ASC ordering typical for comments
        }
        sql += "ORDER BY c.created_at ASC, c.id ASC LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIdx = 1;
            pstmt.setLong(paramIdx++, postId);
            if (cursor > 0) {
                pstmt.setLong(paramIdx++, cursor);
            }
            pstmt.setInt(paramIdx++, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocketCommentViewDTO dto = new LocketCommentViewDTO();
                    dto.setId(rs.getLong("id"));
                    dto.setPostId(rs.getLong("post_id"));
                    dto.setUserId(rs.getInt("user_id"));
                    dto.setAuthorName(rs.getString("author_name"));
                    dto.setAuthorAvatar(rs.getString("author_avatar"));
                    dto.setContent(rs.getString("content"));
                    
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    dto.setCreatedAt(createdAt != null ? createdAt.getTime() : System.currentTimeMillis());
                    
                    boolean canDelete = (currentUserId == dto.getUserId());
                    dto.setCanDelete(canDelete);
                    
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public LocketCommentModel createComment(long postId, int userId, String content) {
        String sql = "INSERT INTO locket_comments (post_id, user_id, content) VALUES (?, ?, ?) RETURNING *";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            pstmt.setInt(2, userId);
            pstmt.setString(3, content);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    updateCommentCount(conn, postId, 1);
                    return mapRowToModel(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean softDeleteComment(long commentId, int deletedByUserId) {
        // Find post id to update count
        long postId = -1;
        String findSql = "SELECT post_id FROM locket_comments WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement findStmt = conn.prepareStatement(findSql)) {
            findStmt.setLong(1, commentId);
            try (ResultSet rs = findStmt.executeQuery()) {
                if (rs.next()) {
                    postId = rs.getLong("post_id");
                }
            }
            if (postId != -1) {
                String sql = "UPDATE locket_comments SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, commentId);
                    int updated = pstmt.executeUpdate();
                    if (updated > 0) {
                        updateCommentCount(conn, postId, -1);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int countByPost(long postId) {
        String sql = "SELECT COUNT(*) FROM locket_comments WHERE post_id = ? AND deleted_at IS NULL";
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

    private void updateCommentCount(Connection conn, long postId, int delta) throws SQLException {
        String sql = "UPDATE locket_posts SET comment_count = GREATEST(0, comment_count + ?) WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, delta);
            pstmt.setLong(2, postId);
            pstmt.executeUpdate();
        }
    }

    private LocketCommentModel mapRowToModel(ResultSet rs) throws SQLException {
        return new LocketCommentModel(
            rs.getLong("id"),
            rs.getLong("post_id"),
            rs.getInt("user_id"),
            rs.getString("content"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at"),
            rs.getTimestamp("deleted_at")
        );
    }
}
