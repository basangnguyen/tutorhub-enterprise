package com.tutorhub.ui.header.search;

import java.awt.image.BufferedImage;

/**
 * Immutable data class đại diện cho "Search Highlight" hàng ngày.
 * Tương tự Windows 11 Search Highlights / Bing Daily.
 *
 * Đặt ảnh vào: src/main/resources/com/tutorhub/resources/highlights/
 * Kích thước ảnh khuyến nghị: 400×200 px (tỷ lệ 2:1), JPEG 85% chất lượng.
 */
public final class SearchHighlight {

    private final String      title;       // "Vịnh Hạ Long"
    private final String      subtitle;    // "Kỳ quan thiên nhiên thế giới mới"
    private final String      category;    // "Quảng Ninh"
    private final String      infoUrl;     // URL để mở khi click thumbnail
    private final BufferedImage image;     // Ảnh đã load vào bộ nhớ

    public SearchHighlight(String title, String subtitle, String category,
                           String infoUrl, BufferedImage image) {
        this.title    = title;
        this.subtitle = subtitle;
        this.category = category;
        this.infoUrl  = infoUrl;
        this.image    = image;
    }

    public String       getTitle()    { return title; }
    public String       getSubtitle() { return subtitle; }
    public String       getCategory() { return category; }
    public String       getInfoUrl()  { return infoUrl; }
    public BufferedImage getImage()   { return image; }

    @Override
    public String toString() {
        return "SearchHighlight{title='" + title + "', category='" + category + "'}";
    }
}
