package com.mycompany.tutorhub_enterprise.client.home;

public class HomeBannerItem {
    public String id;
    public String imageUrl;
    public String title;
    public String subtitle;
    public String ctaText;

    public HomeBannerItem() {
    }

    public HomeBannerItem(String id, String imageUrl, String title, String subtitle, String ctaText) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.title = title;
        this.subtitle = subtitle;
        this.ctaText = ctaText;
    }
}
