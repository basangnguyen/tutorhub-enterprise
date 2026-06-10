package com.mycompany.tutorhub_enterprise.server.dao;

import com.mycompany.tutorhub_enterprise.models.ConversationInfo;
import com.mycompany.tutorhub_enterprise.models.Message;
import com.mycompany.tutorhub_enterprise.server.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ChatDAO {

    public static List<ConversationInfo> getConversationList(int currentUserId) {
        List<ConversationInfo> list = new ArrayList<>();
        String sql = """
            SELECT c.id as convo_id, c.is_group, c.title,
                   u.full_name as other_name, u.avatar_url as other_avatar,
                   m.content as last_msg, m.sent_at as last_time,
                   COALESCE(unread.unread_count, 0) as unread_count
            FROM conversations c
            JOIN conversation_participants cp ON c.id = cp.conversation_id
            JOIN users u ON cp.user_id = u.id
            LEFT JOIN LATERAL (
                SELECT content, sent_at
                FROM messages
                WHERE conversation_id = c.id
                ORDER BY sent_at DESC
                LIMIT 1
            ) m ON true
            LEFT JOIN LATERAL (
                SELECT COUNT(*) as unread_count
                FROM messages unread_msg
                WHERE unread_msg.conversation_id = c.id
                  AND unread_msg.sender_id <> ?
                  AND COALESCE(unread_msg.is_read, false) = false
            ) unread ON true
            WHERE c.id IN (SELECT conversation_id FROM conversation_participants WHERE user_id = ?)
              AND u.id <> ?
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, currentUserId);
            pst.setInt(2, currentUserId);
            pst.setInt(3, currentUserId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    ConversationInfo info = new ConversationInfo();
                    info.conversationId = rs.getInt("convo_id");
                    info.displayName = rs.getBoolean("is_group") ? rs.getString("title") : rs.getString("other_name");
                    info.avatarUrl = rs.getString("other_avatar");
                    info.lastMessage = rs.getString("last_msg");
                    info.lastMessageTime = rs.getTimestamp("last_time");
                    info.unreadCount = rs.getInt("unread_count");
                    info.isOnline = true; // TODO: replace with real presence state.
                    list.add(info);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Cannot load chat conversation list: " + e.getMessage());
        }
        return list;
    }

    public static List<Message> getMessages(int conversationId, int currentUserId) {
        List<Message> messages = new ArrayList<>();
        String sql = """
            SELECT m.id, m.client_message_id, m.content, m.sent_at, m.message_type, m.is_read,
                   CASE WHEN m.sender_id = ? THEN 'ME' ELSE 'OTHER' END as sender_type
            FROM messages m
            WHERE m.conversation_id = ?
            ORDER BY m.sent_at ASC
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, currentUserId);
            pst.setInt(2, conversationId);

            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    Message msg = new Message();
                    msg.messageId = rs.getInt("id");
                    msg.serverMessageId = msg.messageId;
                    msg.clientMessageId = rs.getString("client_message_id");
                    msg.conversationId = conversationId;
                    msg.content = rs.getString("content");
                    msg.sentAt = rs.getTimestamp("sent_at");
                    msg.messageType = rs.getString("message_type");
                    msg.senderType = rs.getString("sender_type");
                    msg.isRead = rs.getBoolean("is_read");
                    messages.add(msg);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Cannot load chat messages: " + e.getMessage());
        }
        return messages;
    }

    public static List<Integer> getConversationMembers(int conversationId) {
        List<Integer> members = new ArrayList<>();
        String sql = "SELECT user_id FROM conversation_participants WHERE conversation_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, conversationId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getInt("user_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Cannot load conversation members: " + e.getMessage());
        }
        return members;
    }

    public static int findMessageIdByClientMessageId(int conversationId, int senderId, String clientMessageId) {
        if (clientMessageId == null || clientMessageId.trim().isEmpty()) {
            return 0;
        }

        String sql = "SELECT id FROM messages WHERE conversation_id = ? AND sender_id = ? AND client_message_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, conversationId);
            pst.setInt(2, senderId);
            pst.setString(3, clientMessageId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Cannot find chat message by client id: " + e.getMessage());
        }
        return 0;
    }

    public static int insertMessage(int conversationId, int senderId, String messageType, String content, String clientMessageId) {
        String sql = "INSERT INTO messages (conversation_id, sender_id, message_type, content, is_read, client_message_id) VALUES (?, ?, ?, ?, false, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pst.setInt(1, conversationId);
            pst.setInt(2, senderId);
            pst.setString(3, messageType);
            pst.setString(4, content);
            pst.setString(5, clientMessageId == null || clientMessageId.trim().isEmpty() ? null : clientMessageId);
            pst.executeUpdate();

            try (ResultSet keys = pst.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DB ERROR] Cannot insert chat message: " + e.getMessage());
        }
        return 0;
    }
}
