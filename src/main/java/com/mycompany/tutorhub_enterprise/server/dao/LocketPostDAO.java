package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.locket.LocketPostModel;
import com.mycompany.tutorhub_enterprise.models.locket.LocketPostViewDTO;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocketPostDAO {

    public LocketPostModel findById(long postId) {
        String sql = "SELECT * FROM locket_posts WHERE id = ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToModel(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<LocketPostViewDTO> listGlobal(int currentUserId, int limit, long cursor) {
        List<LocketPostViewDTO> list = new ArrayList<>();
        // Use a cursor (id) for pagination if cursor > 0
        String sql = "SELECT p.*, u.full_name as author_name, u.avatar_url as author_avatar, " +
                     "EXISTS(SELECT 1 FROM locket_reactions r WHERE r.post_id = p.id AND r.user_id = ? AND r.reaction_type = 'HEART') as liked_by_me " +
                     "FROM locket_posts p " +
                     "JOIN users u ON p.user_id = u.id " +
                     "WHERE p.deleted_at IS NULL ";
        if (cursor > 0) {
            sql += "AND p.id < ? ";
        }
        sql += "ORDER BY p.created_at DESC, p.id DESC LIMIT ?";

        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIdx = 1;
            pstmt.setInt(paramIdx++, currentUserId);
            if (cursor > 0) {
                pstmt.setLong(paramIdx++, cursor);
            }
            pstmt.setInt(paramIdx++, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocketPostViewDTO dto = new LocketPostViewDTO();
                    dto.setId(rs.getLong("id"));
                    dto.setClassId(rs.getInt("class_id")); // Can be null/0 now
                    dto.setUserId(rs.getInt("user_id"));
                    dto.setAuthorName(rs.getString("author_name"));
                    dto.setAuthorAvatar(rs.getString("author_avatar"));
                    dto.setImageUrl(rs.getString("image_url"));
                    dto.setThumbnailUrl(rs.getString("thumbnail_url"));
                    dto.setCaption(rs.getString("caption"));
                    dto.setMediaType(rs.getString("media_type"));
                    dto.setLikeCount(rs.getInt("like_count"));
                    dto.setCommentCount(rs.getInt("comment_count"));
                    dto.setLikedByMe(rs.getBoolean("liked_by_me"));
                    
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    dto.setCreatedAt(createdAt != null ? createdAt.getTime() : System.currentTimeMillis());
                    dto.setTimeText(formatTimeText(dto.getCreatedAt()));
                    
                    boolean canDelete = (currentUserId == dto.getUserId()); // More permissions checked in service
                    dto.setCanDelete(canDelete);
                    
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public LocketPostModel createPostGlobal(int userId, String imageUrl, String thumbnailUrl, String caption) {
        String sql = "INSERT INTO locket_posts (class_id, user_id, image_url, thumbnail_url, caption) VALUES (NULL, ?, ?, ?, ?) RETURNING *";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, imageUrl);
            pstmt.setString(3, thumbnailUrl);
            pstmt.setString(4, caption);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToModel(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean softDeletePost(long postId, int deletedByUserId) {
        String sql = "UPDATE locket_posts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean toggleReaction(long postId, int userId, String reactionType) {
        String checkSql = "SELECT id FROM locket_reactions WHERE post_id = ? AND user_id = ? AND reaction_type = ?";
        String deleteSql = "DELETE FROM locket_reactions WHERE post_id = ? AND user_id = ? AND reaction_type = ?";
        String insertSql = "INSERT INTO locket_reactions (post_id, user_id, reaction_type) VALUES (?, ?, ?)";
        String updateCountSql = "UPDATE locket_posts SET like_count = (SELECT COUNT(*) FROM locket_reactions WHERE post_id = ?) WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            boolean isLiked = false;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setLong(1, postId);
                checkStmt.setInt(2, userId);
                checkStmt.setString(3, reactionType);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) isLiked = true;
                }
            }

            if (isLiked) {
                try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                    delStmt.setLong(1, postId);
                    delStmt.setInt(2, userId);
                    delStmt.setString(3, reactionType);
                    delStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                    insStmt.setLong(1, postId);
                    insStmt.setInt(2, userId);
                    insStmt.setString(3, reactionType);
                    insStmt.executeUpdate();
                }
            }

            try (PreparedStatement updateStmt = conn.prepareStatement(updateCountSql)) {
                updateStmt.setLong(1, postId);
                updateStmt.setLong(2, postId);
                updateStmt.executeUpdate();
            }
            return !isLiked;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean addComment(long postId, int userId, String content) {
        String insertSql = "INSERT INTO locket_comments (post_id, user_id, content) VALUES (?, ?, ?)";
        String updateCountSql = "UPDATE locket_posts SET comment_count = (SELECT COUNT(*) FROM locket_comments WHERE post_id = ?) WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setLong(1, postId);
                pstmt.setInt(2, userId);
                pstmt.setString(3, content);
                if (pstmt.executeUpdate() > 0) {
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateCountSql)) {
                        updateStmt.setLong(1, postId);
                        updateStmt.setLong(2, postId);
                        updateStmt.executeUpdate();
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> getComments(long postId) {
        List<String> comments = new ArrayList<>();
        String sql = "SELECT c.content, u.full_name as author_name, u.avatar_url as author_avatar " +
                     "FROM locket_comments c " +
                     "JOIN users u ON c.user_id = u.id " +
                     "WHERE c.post_id = ? AND c.deleted_at IS NULL " +
                     "ORDER BY c.created_at ASC";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, postId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String content = rs.getString("content");
                    String author = rs.getString("author_name");
                    String avatar = rs.getString("author_avatar");
                    if (avatar == null || avatar.isEmpty()) avatar = "https://img.icons8.com/color/48/test-account.png";
                    
                    // Format: author;;avatar;;content
                    comments.add(author + ";;" + avatar + ";;" + content);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return comments;
    }

    private LocketPostModel mapRowToModel(ResultSet rs) throws SQLException {
        return new LocketPostModel(
            rs.getLong("id"),
            rs.getInt("class_id"),
            rs.getInt("user_id"),
            rs.getString("image_url"),
            rs.getString("thumbnail_url"),
            rs.getString("caption"),
            rs.getString("media_type"),
            rs.getInt("like_count"),
            rs.getInt("comment_count"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("updated_at"),
            rs.getTimestamp("deleted_at")
        );
    }

    private String formatTimeText(long timeMillis) {
        long diff = System.currentTimeMillis() - timeMillis;
        if (diff < 60000) return "Vừa xong";
        if (diff < 3600000) return (diff / 60000) + " phút trước";
        if (diff < 86400000) return (diff / 3600000) + " giờ trước";
        return (diff / 86400000) + " ngày trước";
    }
}
