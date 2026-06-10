package com.mycompany.tutorhub_enterprise.models;

import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public int messageId;
    public String clientMessageId;
    public int serverMessageId;
    public int conversationId;
    public String senderType; // "ME" or "OTHER"
    public int senderId;
    public String senderName;
    public String messageType; // "TEXT", "FILE", "IMAGE"
    public String content;
    public String fileName;
    public String fileSize;
    public Date sentAt;
    public boolean isRead;
    public String status = MessageStatus.SENT.name(); // Kept as String for backward-compatible serialization.

    public Message() {}

    public MessageStatus getDeliveryStatus() {
        if (isRead) {
            return MessageStatus.READ;
        }
        return MessageStatus.from(status);
    }

    public void setDeliveryStatus(MessageStatus deliveryStatus) {
        MessageStatus normalized = deliveryStatus == null ? MessageStatus.SENT : deliveryStatus;
        this.status = normalized.name();
        this.isRead = normalized == MessageStatus.READ;
    }

    public boolean isPendingSend() {
        return MessageStatus.PENDING_SEND == getDeliveryStatus();
    }

    public boolean isReadStatus() {
        return MessageStatus.READ == getDeliveryStatus();
    }

    public boolean isDeliveredStatus() {
        return MessageStatus.DELIVERED == getDeliveryStatus();
    }

    public void markPendingSend() {
        setDeliveryStatus(MessageStatus.PENDING_SEND);
    }

    public void markSent(int serverMessageId) {
        if (serverMessageId > 0) {
            this.serverMessageId = serverMessageId;
            this.messageId = serverMessageId;
        }
        if (!isReadStatus() && !isDeliveredStatus()) {
            setDeliveryStatus(MessageStatus.SENT);
        }
    }

    public void markDelivered(int serverMessageId) {
        if (serverMessageId > 0) {
            this.serverMessageId = serverMessageId;
            this.messageId = serverMessageId;
        }
        if (!isReadStatus()) {
            setDeliveryStatus(MessageStatus.DELIVERED);
        }
    }

    public void markRead() {
        setDeliveryStatus(MessageStatus.READ);
    }
}
