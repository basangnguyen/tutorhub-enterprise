package com.mycompany.tutorhub_enterprise.models;
import java.io.Serializable;

public class UserInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    public int userId;
    public String fullName;
    public String email;
    public String avatarUrl;
    public boolean isOnline;
    public String friendshipStatus; // "NONE", "PENDING_SENT", "PENDING_RECEIVED", "FRIEND"

    public UserInfo() {}
}