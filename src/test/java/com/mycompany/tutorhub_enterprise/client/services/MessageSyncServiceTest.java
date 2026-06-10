package com.mycompany.tutorhub_enterprise.client.services;

import com.mycompany.tutorhub_enterprise.client.dao.LocalDatabaseManager;
import com.mycompany.tutorhub_enterprise.models.Message;
import com.mycompany.tutorhub_enterprise.models.MessageStatus;
import com.mycompany.tutorhub_enterprise.models.Packet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageSyncServiceTest {

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
    void retryPendingMessagesUsesCachedConversationAndClientMessageId() {
        LocalDatabaseManager db = LocalDatabaseManager.getInstance();
        RecordingPacketSender sender = new RecordingPacketSender();
        MessageSyncService service = new MessageSyncService(db, sender);

        Message message = outgoingMessage("44444444-4444-4444-8444-444444444444", "hello offline");
        service.saveOutgoingMessage(404, message);

        int sentCount = service.retryPendingMessagesOnce(10);

        assertEquals(1, sentCount);
        assertEquals(1, sender.sentPackets.size());
        assertEquals("SEND_CHAT", sender.sentPackets.get(0).action);
        assertEquals("404|TEXT|44444444-4444-4444-8444-444444444444|hello offline", sender.sentPackets.get(0).payload);
    }

    @Test
    void mergeServerMessagesKeepsUnackedPendingMessagesVisible() {
        LocalDatabaseManager db = LocalDatabaseManager.getInstance();
        MessageSyncService service = new MessageSyncService(db, new RecordingPacketSender());
        String pendingClientId = "55555555-5555-4555-8555-555555555555";

        service.saveOutgoingMessage(505, outgoingMessage(pendingClientId, "still pending"));

        Message serverMessage = new Message();
        serverMessage.conversationId = 505;
        serverMessage.senderId = 9;
        serverMessage.senderType = "OTHER";
        serverMessage.messageType = "TEXT";
        serverMessage.content = "server history";
        serverMessage.setDeliveryStatus(MessageStatus.SENT);

        List<Message> merged = service.mergeServerMessages("505", List.of(serverMessage));

        assertEquals(2, merged.size());
        assertTrue(merged.stream().anyMatch(message -> pendingClientId.equals(message.clientMessageId)));
        assertEquals(1, db.getPendingMessages("505", 10).size());
    }

    @Test
    void deliveredAckUpdatesCachedOutgoingMessage() {
        LocalDatabaseManager db = LocalDatabaseManager.getInstance();
        MessageSyncService service = new MessageSyncService(db, new RecordingPacketSender());
        String clientMessageId = "66666666-6666-4666-8666-666666666666";

        Message message = outgoingMessage(clientMessageId, "delivered");
        message.markSent(606);
        service.saveOutgoingMessage(606, message);

        service.markMessageAsDelivered("606", clientMessageId, 606);

        Message restored = db.getMessages("606", 10, 0).get(0);
        assertEquals(MessageStatus.DELIVERED.name(), restored.status);
    }

    private static Message outgoingMessage(String clientMessageId, String content) {
        Message message = new Message();
        message.senderId = 7;
        message.senderName = "Tester";
        message.senderType = "ME";
        message.messageType = "TEXT";
        message.clientMessageId = clientMessageId;
        message.content = content;
        message.markPendingSend();
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

    private static class RecordingPacketSender implements MessageSyncService.PacketSender {
        private final List<Packet> sentPackets = new ArrayList<>();
        private boolean connected = true;

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void send(Packet packet) {
            sentPackets.add(packet);
        }
    }
}
