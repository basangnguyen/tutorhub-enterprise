package com.mycompany.tutorhub_enterprise.client.services;

import com.mycompany.tutorhub_enterprise.client.NetworkManager;
import com.mycompany.tutorhub_enterprise.client.dao.LocalDatabaseManager;
import com.mycompany.tutorhub_enterprise.models.Message;
import com.mycompany.tutorhub_enterprise.models.MessageStatus;
import com.mycompany.tutorhub_enterprise.models.Packet;

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageSyncService {

    public interface PacketSender {
        boolean isConnected();
        void send(Packet packet) throws Exception;
    }

    private static MessageSyncService instance;

    private final LocalDatabaseManager localDatabase;
    private final PacketSender packetSender;
    private javax.swing.Timer pendingRetryTimer;
    private volatile boolean pendingRetryRunning = false;

    public MessageSyncService(LocalDatabaseManager localDatabase, PacketSender packetSender) {
        this.localDatabase = localDatabase;
        this.packetSender = packetSender;
    }

    public static synchronized MessageSyncService getInstance() {
        if (instance == null) {
            instance = new MessageSyncService(LocalDatabaseManager.getInstance(), new NetworkPacketSender());
        }
        return instance;
    }

    public void startPendingRetryTimer() {
        if (pendingRetryTimer != null && pendingRetryTimer.isRunning()) {
            return;
        }
        pendingRetryTimer = new javax.swing.Timer(8000, e -> retryPendingMessagesAsync());
        pendingRetryTimer.setInitialDelay(3000);
        pendingRetryTimer.start();
    }

    public void stopPendingRetryTimer() {
        if (pendingRetryTimer != null) {
            pendingRetryTimer.stop();
        }
    }

    public List<Message> getCachedMessages(String conversationId, int limit, int offset) {
        return localDatabase.getMessages(conversationId, limit, offset);
    }

    public void saveOutgoingMessage(int conversationId, Message message) {
        if (message == null) {
            return;
        }
        message.conversationId = conversationId;
        localDatabase.saveMessage(String.valueOf(conversationId), message, message.getDeliveryStatus().name());
    }

    public void saveIncomingMessage(String conversationId, Message message) {
        if (message == null) {
            return;
        }
        message.conversationId = parseIntOrDefault(conversationId, message.conversationId);
        message.setDeliveryStatus(MessageStatus.SENT);
        localDatabase.saveMessage(conversationId, message, message.getDeliveryStatus().name());
    }

    public void markMessageAsSent(String conversationId, String clientMessageId, int serverMessageId) {
        localDatabase.markMessageAsSent(conversationId, clientMessageId, serverMessageId);
    }

    public void markMessageAsDelivered(String conversationId, String clientMessageId, int serverMessageId) {
        localDatabase.markMessageAsDelivered(conversationId, clientMessageId, serverMessageId);
    }

    public void markConversationAsRead(String conversationId) {
        localDatabase.markOutgoingMessagesAsRead(conversationId);
    }

    public List<Message> mergeServerMessages(String conversationId, List<Message> serverMessages) {
        List<Message> safeServerMessages = serverMessages == null ? Collections.emptyList() : serverMessages;
        List<Message> displayMessages = new ArrayList<>(safeServerMessages);

        List<Message> pendingMessages = localDatabase.getPendingMessages(conversationId, 200);
        if (pendingMessages != null && !pendingMessages.isEmpty()) {
            for (Message pending : pendingMessages) {
                if (!hasClientMessage(displayMessages, pending.clientMessageId)) {
                    displayMessages.add(pending);
                }
            }
        }

        localDatabase.saveAllMessages(conversationId, safeServerMessages);
        return displayMessages;
    }

    public void sendChatMessage(int conversationId, Message message) throws Exception {
        packetSender.send(new Packet("SEND_CHAT", buildSendChatPayload(conversationId, message)));
    }

    public String buildSendChatPayload(int conversationId, Message message) {
        String messageType = getSendMessageType(message);
        String clientMessageId = message == null || message.clientMessageId == null ? "" : message.clientMessageId;
        String content = message == null || message.content == null ? "" : message.content;
        return conversationId + "|" + messageType + "|" + clientMessageId + "|" + content;
    }

    int retryPendingMessagesOnce(int limit) {
        if (!packetSender.isConnected()) {
            return 0;
        }

        int sentCount = 0;
        List<Message> pendingMessages = localDatabase.getPendingMessages(limit);
        for (Message message : pendingMessages) {
            if (!packetSender.isConnected()) {
                break;
            }
            try {
                sendChatMessage(message.conversationId, message);
                sentCount++;
            } catch (Exception ex) {
                break;
            }
        }
        return sentCount;
    }

    private void retryPendingMessagesAsync() {
        if (pendingRetryRunning || !packetSender.isConnected()) {
            return;
        }
        pendingRetryRunning = true;

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() {
                return retryPendingMessagesOnce(25);
            }

            @Override
            protected void done() {
                pendingRetryRunning = false;
            }
        }.execute();
    }

    private String getSendMessageType(Message message) {
        if (message == null || message.content == null) {
            return "TEXT";
        }
        if (message.content.startsWith("[IMG_URL]")) {
            return "IMAGE";
        }
        return "TEXT";
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

    private int parseIntOrDefault(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static class NetworkPacketSender implements PacketSender {
        @Override
        public boolean isConnected() {
            return NetworkManager.getInstance().isConnected();
        }

        @Override
        public void send(Packet packet) throws Exception {
            NetworkManager.getInstance().sendPacket(packet);
        }
    }
}
