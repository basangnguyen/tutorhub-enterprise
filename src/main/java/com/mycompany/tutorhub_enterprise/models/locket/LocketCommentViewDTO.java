package com.mycompany.tutorhub_enterprise.models.locket;

public class LocketCommentViewDTO {
    private long id;
    private long postId;
    private int userId;
    private String authorName;
    private String authorAvatar;
    private String content;
    private long createdAt;
    private boolean canDelete;

    public LocketCommentViewDTO() {}

    public LocketCommentViewDTO(long id, long postId, int userId, String authorName, String authorAvatar, String content, long createdAt, boolean canDelete) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.content = content;
        this.createdAt = createdAt;
        this.canDelete = canDelete;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPostId() { return postId; }
    public void setPostId(long postId) { this.postId = postId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
}
