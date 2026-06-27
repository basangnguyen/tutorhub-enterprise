package com.mycompany.tutorhub_enterprise.models.locket;

public class LocketPostViewDTO {
    private long id;
    private transient int classId;
    private int userId;
    private String authorName;
    private String authorAvatar;
    private String imageUrl;
    private String thumbnailUrl;
    private String caption;
    private String mediaType;
    private int likeCount;
    private int commentCount;
    private boolean likedByMe;
    private boolean canDelete;
    private long createdAt;
    private String timeText;

    public LocketPostViewDTO() {}

    public LocketPostViewDTO(long id, int classId, int userId, String authorName, String authorAvatar, String imageUrl, String thumbnailUrl, String caption, String mediaType, int likeCount, int commentCount, boolean likedByMe, boolean canDelete, long createdAt, String timeText) {
        this.id = id;
        this.classId = classId;
        this.userId = userId;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.caption = caption;
        this.mediaType = mediaType;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.likedByMe = likedByMe;
        this.canDelete = canDelete;
        this.createdAt = createdAt;
        this.timeText = timeText;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public boolean isLikedByMe() { return likedByMe; }
    public void setLikedByMe(boolean likedByMe) { this.likedByMe = likedByMe; }
    public boolean isCanDelete() { return canDelete; }
    public void setCanDelete(boolean canDelete) { this.canDelete = canDelete; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getTimeText() { return timeText; }
    public void setTimeText(String timeText) { this.timeText = timeText; }
}
