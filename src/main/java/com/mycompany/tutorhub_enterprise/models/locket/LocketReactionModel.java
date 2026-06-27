package com.mycompany.tutorhub_enterprise.models.locket;

import java.sql.Timestamp;

public class LocketReactionModel {
    private long id;
    private long postId;
    private int userId;
    private String reactionType;
    private Timestamp createdAt;

    public LocketReactionModel() {}

    public LocketReactionModel(long id, long postId, int userId, String reactionType, Timestamp createdAt) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.reactionType = reactionType;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPostId() { return postId; }
    public void setPostId(long postId) { this.postId = postId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getReactionType() { return reactionType; }
    public void setReactionType(String reactionType) { this.reactionType = reactionType; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
