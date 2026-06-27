package com.mycompany.tutorhub_enterprise.client.home;

import java.util.ArrayList;
import java.util.List;

public class HomeSocialState {
    public List<HomeBannerItem> banners;
    public List<HomeLocketItem> locketItems;
    public boolean locketCanPost;

    public HomeSocialState() {
        this(new ArrayList<>(), new ArrayList<>(), true);
    }

    public HomeSocialState(List<HomeBannerItem> banners, List<HomeLocketItem> locketItems, boolean locketCanPost) {
        this.banners = banners == null ? new ArrayList<>() : new ArrayList<>(banners);
        this.locketItems = locketItems == null ? new ArrayList<>() : new ArrayList<>(locketItems);
        this.locketCanPost = locketCanPost;
    }

    public HomeSocialState copy() {
        return new HomeSocialState(banners, locketItems, locketCanPost);
    }
}
