package com.mycompany.tutorhub_enterprise.models;

import java.io.Serializable;
import java.util.Date;

public class ConversationInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    public int conversationId;
    public String displayName; // Tên người chat cùng hoặc tên nhóm
    public String avatarUrl;
    public String lastMessage;
    public Date lastMessageTime;
    public int unreadCount;
    public boolean isOnline;
    public boolean isPriority;

    public ConversationInfo() {}
}