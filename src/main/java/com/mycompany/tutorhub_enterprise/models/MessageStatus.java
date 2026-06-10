package com.mycompany.tutorhub_enterprise.models;

import java.io.Serializable;

public enum MessageStatus implements Serializable {
    PENDING_SEND,
    SENT,
    DELIVERED,
    READ;

    public static MessageStatus from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SENT;
        }
        try {
            return MessageStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SENT;
        }
    }
}
