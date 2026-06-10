package com.mycompany.tutorhub_enterprise;

import com.mycompany.tutorhub_enterprise.client.dao.LocalDatabaseManager;
import com.mycompany.tutorhub_enterprise.models.Message;
import com.mycompany.tutorhub_enterprise.models.MessageStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatOutboxSmokeTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void configureTempDatabase() throws Exception {
        resetLocalDatabaseSingleton();
        System.setProperty("tutorhub.local.db.path", tempDir.resolve("local_chat.db").toString());
    }

    @AfterEach
    void cleanup() throws Exception {
        resetLocalDatabaseSingleton();
        System.clearProperty("tutorhub.local.db.path");
    }

    @Test
    void pendingMessageSurvivesServerSyncAndAckTargetsClientId() {
        LocalDatabaseManager db = LocalDatabaseManager.getInstance();
        String conversationId = "101";
        String clientMessageId = "11111111-1111-4111-8111-111111111111";

        Message pending = outgoingMessage(conversationId, clientMessageId, "offline text");
        db.saveMessage(conversationId, pending, MessageStatus.PENDING_SEND.name());

        assertEquals(1, db.getPendingMessages(conversationId, 10).size());

        Message serverMessage = serverMessage(conversationId, 77, "server history");
        db.saveAllMessages(conversationId, List.of(serverMessage));

        assertEquals(1, db.getPendingMessages(conversationId, 10).size());
        assertEquals(2, db.getMessages(conversationId, 10, 0).size());

        db.markMessageAsSent(conversationId, clientMessageId, 501);

        assertEquals(0, db.getPendingMessages(conversationId, 10).size());
        Message restored = db.getMessages(conversationId, 10, 0).stream()
                .filter(msg -> clientMessageId.equals(msg.clientMessageId))
                .findFirst()
                .orElseThrow();
        assertEquals(MessageStatus.SENT.name(), restored.status);
        assertEquals(501, restored.serverMessageId);
    }

    @Test
    void serverEchoWithSameClientMessageIdReplacesPendingInsteadOfDuplicatingIt() {
        LocalDatabaseManager db = LocalDatabaseManager.getInstance();
        String conversationId = "202";
        String clientMessageId = "22222222-2222-4222-8222-222222222222";

        Message pending = outgoingMessage(conversationId, clientMessageId, "retry me");
        db.saveMessage(conversationId, pending, MessageStatus.PENDING_SEND.name());

        Message serverEcho = outgoingMessage(conversationId, clientMessageId, "retry me");
        serverEcho.markSent(900);

        db.saveAllMessages(conversationId, List.of(serverEcho));

        List<Message> messages = db.getMessages(conversationId, 10, 0);
        assertEquals(1, messages.size());
        assertEquals(0, db.getPendingMessages(conversationId, 10).size());
        assertEquals(900, messages.get(0).serverMessageId);
        assertEquals(MessageStatus.SENT.name(), messages.get(0).status);
    }

    @Test
    void readReceiptIsPersistedInLocalCache() {
        LocalDatabaseManager db = LocalDatabaseManager.getInstance();
        String conversationId = "303";
        String clientMessageId = "33333333-3333-4333-8333-333333333333";

        Message sent = outgoingMessage(conversationId, clientMessageId, "read me");
        sent.setDeliveryStatus(MessageStatus.SENT);
        db.saveMessage(conversationId, sent, MessageStatus.SENT.name());

        assertEquals(0, db.getMessages(conversationId, 10, 0).stream().filter(msg -> msg.isRead).count());

        db.markOutgoingMessagesAsRead(conversationId);

        Message restored = db.getMessages(conversationId, 10, 0).get(0);
        assertTrue(restored.isRead);
        assertEquals(MessageStatus.READ.name(), restored.status);
    }

    @Test
    void deliveryReceiptIsPersistedWithoutOverridingReadReceipt() {
        LocalDatabaseManager db = LocalDatabaseManager.getInstance();
        String conversationId = "304";
        String clientMessageId = "33333333-3333-4333-8333-333333333334";

        Message sent = outgoingMessage(conversationId, clientMessageId, "deliver me");
        sent.markSent(701);
        db.saveMessage(conversationId, sent, MessageStatus.SENT.name());

        db.markMessageAsDelivered(conversationId, clientMessageId, 701);

        Message delivered = db.getMessages(conversationId, 10, 0).get(0);
        assertEquals(MessageStatus.DELIVERED.name(), delivered.status);
        assertEquals(701, delivered.serverMessageId);

        db.markOutgoingMessagesAsRead(conversationId);
        db.markMessageAsDelivered(conversationId, clientMessageId, 701);

        Message read = db.getMessages(conversationId, 10, 0).get(0);
        assertTrue(read.isRead);
        assertEquals(MessageStatus.READ.name(), read.status);
    }

    @Test
    void sourceContainsServerSideIdempotencyForRetry() throws Exception {
        String server = java.nio.file.Files.readString(Path.of("src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java"));
        String database = java.nio.file.Files.readString(Path.of("src/main/java/com/mycompany/tutorhub_enterprise/server/DatabaseManager.java"));
        String chatDao = java.nio.file.Files.readString(Path.of("src/main/java/com/mycompany/tutorhub_enterprise/server/dao/ChatDAO.java"));

        assertTrue(database.contains("client_message_id"));
        assertTrue(database.contains("ux_messages_client_message_id"));
        assertTrue(server.contains("ChatMessageService.handleSendChat"));
        assertTrue(server.contains("SEND_CHAT_ACK"));
        assertTrue(server.contains("MESSAGE_DELIVERED_ACK"));
        assertTrue(chatDao.contains("SELECT id FROM messages WHERE conversation_id = ? AND sender_id = ? AND client_message_id = ?"));
        assertTrue(chatDao.contains("INSERT INTO messages (conversation_id, sender_id, message_type, content, is_read, client_message_id)"));
    }

    private static Message outgoingMessage(String conversationId, String clientMessageId, String content) {
        Message message = new Message();
        message.conversationId = Integer.parseInt(conversationId);
        message.senderId = 7;
        message.senderName = "Tester";
        message.senderType = "ME";
        message.messageType = "TEXT";
        message.clientMessageId = clientMessageId;
        message.content = content;
        message.markPendingSend();
        return message;
    }

    private static Message serverMessage(String conversationId, int serverMessageId, String content) {
        Message message = new Message();
        message.conversationId = Integer.parseInt(conversationId);
        message.senderId = 8;
        message.senderName = "Teacher";
        message.senderType = "OTHER";
        message.messageType = "TEXT";
        message.serverMessageId = serverMessageId;
        message.messageId = serverMessageId;
        message.content = content;
        message.setDeliveryStatus(MessageStatus.SENT);
        return message;
    }

    private static void resetLocalDatabaseSingleton() throws Exception {
        Field instanceField = LocalDatabaseManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        Object existing = instanceField.get(null);
        if (existing != null) {
            Field connectionField = LocalDatabaseManager.class.getDeclaredField("connection");
            connectionField.setAccessible(true);
            Connection connection = (Connection) connectionField.get(existing);
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
        instanceField.set(null, null);
    }
}
