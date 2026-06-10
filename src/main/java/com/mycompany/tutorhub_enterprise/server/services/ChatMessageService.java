package com.mycompany.tutorhub_enterprise.server.services;

import com.mycompany.tutorhub_enterprise.server.dao.ChatDAO;

public final class ChatMessageService {

    private ChatMessageService() {
    }

    public static SendChatResult handleSendChat(int senderId, String senderIdentity, String payload) {
        SendChatPayload chatPayload = parsePayload(payload);
        if (!chatPayload.isValid()) {
            return SendChatResult.rejected("Invalid SEND_CHAT payload");
        }

        int serverMessageId = ChatDAO.findMessageIdByClientMessageId(
                chatPayload.conversationId(),
                senderId,
                chatPayload.clientMessageId()
        );

        boolean isNewMessage = serverMessageId <= 0;
        if (isNewMessage) {
            serverMessageId = ChatDAO.insertMessage(
                    chatPayload.conversationId(),
                    senderId,
                    chatPayload.messageType(),
                    chatPayload.content(),
                    chatPayload.clientMessageId()
            );
        }

        if (serverMessageId <= 0) {
            return SendChatResult.rejected("Cannot save chat message");
        }

        String safeSenderIdentity = senderIdentity == null || senderIdentity.trim().isEmpty()
                ? String.valueOf(senderId)
                : senderIdentity;
        return SendChatResult.accepted(chatPayload, serverMessageId, isNewMessage, "Gia su " + safeSenderIdentity);
    }

    static SendChatPayload parsePayload(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return SendChatPayload.invalid();
        }

        String[] parts = payload.split("\\|", 4);
        if (parts.length < 3) {
            return SendChatPayload.invalid();
        }

        try {
            int conversationId = Integer.parseInt(parts[0].trim());
            String messageType = isBlank(parts[1]) ? "TEXT" : parts[1].trim();
            if (parts.length >= 4) {
                return new SendChatPayload(conversationId, messageType, parts[2], parts[3], true);
            }
            return new SendChatPayload(conversationId, messageType, "", parts[2], true);
        } catch (NumberFormatException ex) {
            return SendChatPayload.invalid();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record SendChatPayload(
            int conversationId,
            String messageType,
            String clientMessageId,
            String content,
            boolean valid
    ) {
        static SendChatPayload invalid() {
            return new SendChatPayload(0, "TEXT", "", "", false);
        }

        boolean isValid() {
            return valid;
        }
    }

    public record SendChatResult(
            boolean accepted,
            SendChatPayload payload,
            int serverMessageId,
            boolean newMessage,
            String senderDisplayName,
            String errorMessage
    ) {
        static SendChatResult accepted(SendChatPayload payload, int serverMessageId, boolean newMessage, String senderDisplayName) {
            return new SendChatResult(true, payload, serverMessageId, newMessage, senderDisplayName, "");
        }

        static SendChatResult rejected(String errorMessage) {
            return new SendChatResult(false, SendChatPayload.invalid(), 0, false, "", errorMessage);
        }

        public String ackPayload() {
            return payload.conversationId() + "|" + payload.clientMessageId() + "|" + serverMessageId;
        }

        public String forwardPayload() {
            return payload.conversationId() + "|" + senderDisplayName + "|" + payload.content();
        }
    }
}
