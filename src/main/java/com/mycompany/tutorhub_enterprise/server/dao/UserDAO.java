package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.UserInfo;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import com.mycompany.tutorhub_enterprise.server.ClientHandler;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    // 1. Tìm kiếm người dùng thật
    public static List<UserInfo> searchUsers(String keyword, int currentUserId) {
        List<UserInfo> result = new ArrayList<>();
        String sql = """
            SELECT u.id, u.full_name, u.email, u.avatar_url,
                   f.status as friend_status, f.requester_id
            FROM users u
            LEFT JOIN friendships f ON (f.requester_id = u.id AND f.receiver_id = ?) 
                                    OR (f.requester_id = ? AND f.receiver_id = u.id)
            WHERE (u.email ILIKE ? OR u.full_name ILIKE ?) AND u.id != ?
            LIMIT 10
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, currentUserId);
            pst.setInt(2, currentUserId);
            pst.setString(3, "%" + keyword + "%");
            pst.setString(4, "%" + keyword + "%");
            pst.setInt(5, currentUserId);
            
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                UserInfo info = new UserInfo();
                info.userId = rs.getInt("id");
                info.fullName = rs.getString("full_name");
                info.email = rs.getString("email");
                info.avatarUrl = rs.getString("avatar_url");
                
                // Trạng thái online realtime
                info.isOnline = ClientHandler.isUserOnline(info.email); 

                // Xử lý trạng thái kết bạn
                String fStatus = rs.getString("friend_status");
                int reqId = rs.getInt("requester_id");
                if (fStatus == null) {
                    info.friendshipStatus = "NONE";
                } else if ("ACCEPTED".equals(fStatus)) {
                    info.friendshipStatus = "FRIEND";
                } else if ("PENDING".equals(fStatus)) {
                    info.friendshipStatus = (reqId == currentUserId) ? "PENDING_SENT" : "PENDING_RECEIVED";
                }
                result.add(info);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    // 2. Gửi lời mời kết bạn
    public static boolean sendFriendRequest(int senderId, int receiverId) {
        String sql = "INSERT INTO friendships (requester_id, receiver_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, senderId);
            pst.setInt(2, receiverId);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }

    // 3. Chấp nhận kết bạn
    public static boolean acceptFriendRequest(int senderId, int receiverId) {
        String sql = "UPDATE friendships SET status = 'ACCEPTED', updated_at = CURRENT_TIMESTAMP WHERE requester_id = ? AND receiver_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, senderId); // Người gửi lời mời
            pst.setInt(2, receiverId); // Mình (Người nhận)
            return pst.executeUpdate() > 0;
        } catch (SQLException e) { return false; }
    }
}