package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.models.locket.*;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.dao.LocketCommentDAO;
import com.mycompany.tutorhub_enterprise.server.dao.LocketPostDAO;
import com.mycompany.tutorhub_enterprise.server.dao.LocketReactionDAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LocketService {

    private static final LocketPostDAO postDAO = new LocketPostDAO();
    private static final LocketReactionDAO reactionDAO = new LocketReactionDAO();
    private static final LocketCommentDAO commentDAO = new LocketCommentDAO();

    // -- Permission Checks --

    private static boolean isAdmin(int userId) {
        // Implement admin check if role exists. For now, just false.
        return false;
    }

    private static boolean canManagePost(int userId, LocketPostModel post) {
        if (post.getUserId() == userId) return true; // Owner of post
        return isAdmin(userId); // Admin
    }

    // -- Posts --

    public static List<LocketPostViewDTO> listPosts(int currentUserId, int limit, long cursor) {
        return postDAO.listGlobal(currentUserId, limit, cursor);
    }

    public static LocketPostModel createPost(int userId, String imageUrl, String thumbnailUrl, String caption) {
        String safeCaption = "";
        if (caption != null) {
            safeCaption = caption.trim();
            if (safeCaption.length() > 150) {
                safeCaption = safeCaption.substring(0, 150);
            }
        }
        return postDAO.createPostGlobal(userId, imageUrl, thumbnailUrl, safeCaption);
    }

    public static boolean deletePost(long postId, int userId) {
        LocketPostModel post = postDAO.findById(postId);
        if (post == null) return false;
        if (!canManagePost(userId, post)) {
            throw new SecurityException("User does not have permission to delete this post.");
        }
        return postDAO.softDeletePost(postId, userId);
    }

    // -- Reactions --

    public static boolean toggleReaction(long postId, int userId) {
        LocketPostModel post = postDAO.findById(postId);
        if (post == null) throw new IllegalArgumentException("Post not found.");
        return reactionDAO.toggleHeart(postId, userId);
    }

    // -- Comments --

    public static List<LocketCommentViewDTO> listComments(long postId, int currentUserId, int limit, long cursor) {
        LocketPostModel post = postDAO.findById(postId);
        if (post == null) throw new IllegalArgumentException("Post not found.");
        return commentDAO.listByPostId(postId, currentUserId, limit, cursor);
    }

    public static LocketCommentModel createComment(long postId, int userId, String content) {
        LocketPostModel post = postDAO.findById(postId);
        if (post == null) throw new IllegalArgumentException("Post not found.");
        return commentDAO.createComment(postId, userId, content);
    }

    public static boolean deleteComment(long commentId, int userId) {
        long postId = -1;
        int commentOwnerId = -1;
        
        String sql = "SELECT post_id, user_id FROM locket_comments WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, commentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    postId = rs.getLong("post_id");
                    commentOwnerId = rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (postId == -1) return false;

        LocketPostModel post = postDAO.findById(postId);
        if (userId == commentOwnerId || (post != null && post.getUserId() == userId) || isAdmin(userId)) {
            return commentDAO.softDeleteComment(commentId, userId);
        } else {
            throw new SecurityException("User does not have permission to delete this comment.");
        }
    }
}
