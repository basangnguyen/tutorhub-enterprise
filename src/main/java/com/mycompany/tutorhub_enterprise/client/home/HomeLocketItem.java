package com.mycompany.tutorhub_enterprise.client.home;

public class HomeLocketItem {
    public String id;
    public String imageUrl;
    public String thumbnailUrl;
    public String caption;
    public String authorName;
    public String authorAvatar;
    public String authorInitials;
    public String timeText;
    public int likeCount;
    public int commentCount;
    public boolean likedByMe;
    public boolean canDelete;

    public HomeLocketItem() {
    }

    public HomeLocketItem(
            String id,
            String imageUrl,
            String thumbnailUrl,
            String caption,
            String authorName,
            String authorAvatar,
            String authorInitials,
            String timeText,
            int likeCount,
            int commentCount,
            boolean likedByMe,
            boolean canDelete
    ) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.caption = caption;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.authorInitials = authorInitials;
        this.timeText = timeText;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.likedByMe = likedByMe;
        this.canDelete = canDelete;
    }
}
