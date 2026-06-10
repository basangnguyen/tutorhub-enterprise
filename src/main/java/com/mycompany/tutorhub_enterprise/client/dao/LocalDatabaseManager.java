package com.mycompany.tutorhub_enterprise.client.dao;

import com.mycompany.tutorhub_enterprise.models.Message;
import com.mycompany.tutorhub_enterprise.models.ConversationInfo;
import com.mycompany.tutorhub_enterprise.models.MessageStatus;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalDatabaseManager {

    private static final String LOCAL_DB_PATH_PROPERTY = "tutorhub.local.db.path";
    private static LocalDatabaseManager instance;
    private Connection connection;

    private LocalDatabaseManager() {
        initDatabase();
    }

    public static synchronized LocalDatabaseManager getInstance() {
        if (instance == null) {
            instance = new LocalDatabaseManager();
        }
        return instance;
    }

    private void initDatabase() {
        try {
            // Lấy thư mục AppData hoặc home
            String appData = System.getenv("APPDATA");
            if (appData == null) {
                appData = System.getProperty("user.home");
            }
            File dir = new File(appData, "TutorHub_Enterprise");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String dbPath = resolveDatabasePath(dir);
            String url = "jdbc:sqlite:" + dbPath;

            connection = DriverManager.getConnection(url);

            // Tạo bảng Tin nhắn
            String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "conversationId TEXT NOT NULL," +
                    "senderId TEXT NOT NULL," +
                    "senderName TEXT," +
                    "senderType TEXT," +
                    "messageType TEXT," +
                    "clientMessageId TEXT," +
                    "serverMessageId INTEGER DEFAULT 0," +
                    "content TEXT," +
                    "sentAt TEXT," +
                    "status TEXT DEFAULT 'SENT'," + // PENDING_SEND, SENT, DELIVERED, READ
                    "isRead INTEGER DEFAULT 0," +
                    "localTimestamp INTEGER" + 
                    ");";
            
            // Bảng Hội thoại để cache danh sách bên trái (Tùy chọn)
            String createConversationsTable = "CREATE TABLE IF NOT EXISTS conversations (" +
                    "conversationId TEXT PRIMARY KEY," +
                    "name TEXT," +
                    "lastMessage TEXT," +
                    "lastMessageTime TEXT," +
                    "unreadCount INTEGER," +
                    "avatarUrl TEXT," +
                    "isOnline INTEGER" +
                    ");";

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createMessagesTable);
                stmt.execute(createConversationsTable);
            }
            addColumnIfMissing("messages", "clientMessageId", "TEXT");
            addColumnIfMissing("messages", "serverMessageId", "INTEGER DEFAULT 0");
            addColumnIfMissing("messages", "isRead", "INTEGER DEFAULT 0");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String resolveDatabasePath(File defaultDir) {
        String configuredPath = System.getProperty(LOCAL_DB_PATH_PROPERTY);
        if (!isBlank(configuredPath)) {
            File dbFile = new File(configuredPath.trim());
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            return dbFile.getAbsolutePath();
        }
        return new File(defaultDir, "local_chat.db").getAbsolutePath();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDefinition) throws SQLException {
        if (connection == null) return;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return;
                }
            }
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    // ==========================================
    // CÁC HÀM XỬ LÝ TIN NHẮN (MESSAGES)
    // ==========================================

    public void saveMessage(String conversationId, Message m, String status) {
        if (connection == null) return;
        MessageStatus deliveryStatus = m.isRead ? MessageStatus.READ : MessageStatus.from(status);
        String sql = "INSERT INTO messages (conversationId, senderId, senderName, senderType, messageType, clientMessageId, serverMessageId, content, sentAt, status, isRead, localTimestamp) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, conversationId);
            pstmt.setString(2, String.valueOf(m.senderId));
            pstmt.setString(3, m.senderName);
            pstmt.setString(4, m.senderType);
            pstmt.setString(5, m.messageType);
            pstmt.setString(6, m.clientMessageId);
            pstmt.setInt(7, m.serverMessageId > 0 ? m.serverMessageId : m.messageId);
            pstmt.setString(8, m.content);
            pstmt.setString(9, m.sentAt != null ? m.sentAt.toString() : "");
            pstmt.setString(10, deliveryStatus.name());
            pstmt.setInt(11, deliveryStatus == MessageStatus.READ ? 1 : 0);
            pstmt.setLong(12, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Message> getMessages(String conversationId, int limit, int offset) {
        List<Message> list = new ArrayList<>();
        if (connection == null) return list;
        
        // Lấy tin nhắn theo thời gian (cũ nhất ở trên cùng khi render)
        // Cần lấy `limit` tin nhắn mới nhất rồi đảo ngược lại, nhưng trong SQL thì ta ORDER BY DESC lấy limit, sau đó đảo List.
        String sql = "SELECT * FROM messages WHERE conversationId = ? ORDER BY localTimestamp DESC LIMIT ? OFFSET ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, conversationId);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Message m = readMessage(rs);
                // TODO: Parsing Date is tricky for sentAt. We'll skip exact Date mapping for this demo or parse string.
                
                list.add(0, m); // Add vào đầu danh sách để list đúng thứ tự thời gian
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Message> getPendingMessages(int limit) {
        List<Message> list = new ArrayList<>();
        if (connection == null) return list;

        String sql = "SELECT * FROM messages WHERE status = ? "
                + "AND senderType = 'ME' "
                + "AND clientMessageId IS NOT NULL AND TRIM(clientMessageId) <> '' "
                + "ORDER BY localTimestamp ASC LIMIT ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, MessageStatus.PENDING_SEND.name());
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(readMessage(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Message> getPendingMessages(String conversationId, int limit) {
        List<Message> list = new ArrayList<>();
        if (connection == null) return list;

        String sql = "SELECT * FROM messages WHERE conversationId = ? AND status = ? "
                + "AND senderType = 'ME' "
                + "AND clientMessageId IS NOT NULL AND TRIM(clientMessageId) <> '' "
                + "ORDER BY localTimestamp ASC LIMIT ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, conversationId);
            pstmt.setString(2, MessageStatus.PENDING_SEND.name());
            pstmt.setInt(3, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(readMessage(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public void markMessagesAsSent(String conversationId) {
        if (connection == null) return;
        String sql = "UPDATE messages SET status = ? WHERE conversationId = ? AND status = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, MessageStatus.SENT.name());
            pstmt.setString(2, conversationId);
            pstmt.setString(3, MessageStatus.PENDING_SEND.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void markMessageAsSent(String conversationId, String clientMessageId, int serverMessageId) {
        if (connection == null) return;
        if (clientMessageId == null || clientMessageId.trim().isEmpty()) {
            markMessagesAsSent(conversationId);
            return;
        }

        String sql = "UPDATE messages SET status = ?, serverMessageId = ? "
                + "WHERE conversationId = ? AND clientMessageId = ? AND status NOT IN (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, MessageStatus.SENT.name());
            pstmt.setInt(2, serverMessageId);
            pstmt.setString(3, conversationId);
            pstmt.setString(4, clientMessageId);
            pstmt.setString(5, MessageStatus.READ.name());
            pstmt.setString(6, MessageStatus.DELIVERED.name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void markMessageAsDelivered(String conversationId, String clientMessageId, int serverMessageId) {
        if (connection == null) return;
        if (isBlank(clientMessageId) && serverMessageId <= 0) {
            return;
        }

        String sql = "UPDATE messages SET status = ?, serverMessageId = CASE WHEN ? > 0 THEN ? ELSE serverMessageId END "
                + "WHERE conversationId = ? AND senderType = 'ME' AND isRead = 0 "
                + "AND status <> ? AND (clientMessageId = ? OR (? > 0 AND serverMessageId = ?))";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, MessageStatus.DELIVERED.name());
            pstmt.setInt(2, serverMessageId);
            pstmt.setInt(3, serverMessageId);
            pstmt.setString(4, conversationId);
            pstmt.setString(5, MessageStatus.READ.name());
            pstmt.setString(6, clientMessageId);
            pstmt.setInt(7, serverMessageId);
            pstmt.setInt(8, serverMessageId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void markOutgoingMessagesAsRead(String conversationId) {
        if (connection == null) return;
        String sql = "UPDATE messages SET isRead = 1, status = ? WHERE conversationId = ? AND senderType = 'ME' AND isRead = 0";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, MessageStatus.READ.name());
            pstmt.setString(2, conversationId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void saveAllMessages(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;
        List<Message> pendingMessages = getPendingMessages(conversationId, 200);
        
        // Xóa cũ lưu mới (Cách an toàn nhất cho cache, hoặc dùng UPSERT)
        clearMessages(conversationId);
        
        for (Message m : messages) {
            saveMessage(conversationId, m, m.getDeliveryStatus().name());
        }
        for (Message pending : pendingMessages) {
            if (!hasClientMessage(messages, pending.clientMessageId)) {
                saveMessage(conversationId, pending, pending.getDeliveryStatus().name());
            }
        }
    }
    
    private boolean hasClientMessage(List<Message> messages, String clientMessageId) {
        if (clientMessageId == null || clientMessageId.trim().isEmpty()) {
            return false;
        }
        for (Message message : messages) {
            if (clientMessageId.equals(message.clientMessageId)) {
                return true;
            }
        }
        return false;
    }

    private Message readMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.conversationId = parseIntOrDefault(rs.getString("conversationId"), 0);
        m.senderId = rs.getInt("senderId");
        m.senderName = rs.getString("senderName");
        m.senderType = rs.getString("senderType");
        m.messageType = rs.getString("messageType");
        m.clientMessageId = rs.getString("clientMessageId");
        m.serverMessageId = rs.getInt("serverMessageId");
        m.messageId = m.serverMessageId;
        m.content = rs.getString("content");
        MessageStatus deliveryStatus = MessageStatus.from(rs.getString("status"));
        boolean isRead = rs.getInt("isRead") == 1;
        m.setDeliveryStatus(isRead ? MessageStatus.READ : deliveryStatus);
        return m;
    }

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private void clearMessages(String conversationId) {
        if (connection == null) return;
        String sql = "DELETE FROM messages WHERE conversationId = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, conversationId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

