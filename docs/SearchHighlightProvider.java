package com.tutorhub.ui.header.search;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.logging.Logger;

/**
 * Cung cấp ảnh danh lam thắng cảnh Việt Nam cho Search Highlights.
 *
 * <h3>Cách đặt ảnh:</h3>
 * <pre>
 * src/main/resources/
 *   com/tutorhub/resources/highlights/
 *     ha_long_bay.jpg    ← 400×200 px, JPEG 85%
 *     hoi_an.jpg
 *     son_doong.jpg
 *     sapa.jpg
 *     my_son.jpg
 *     mui_ne.jpg
 *     da_lat.jpg
 * </pre>
 *
 * Nếu thiếu file ảnh → tự sinh ảnh placeholder gradient có tên địa danh.
 * Mỗi ngày xoay vòng 1 ảnh theo công thức: dayOfYear % totalHighlights.
 */
public final class SearchHighlightProvider {

    private static final Logger LOG = Logger.getLogger(SearchHighlightProvider.class.getName());

    // Đường dẫn resource trong classpath
    private static final String RES = "/com/tutorhub/resources/highlights/";

    // ─── Catalog danh lam thắng cảnh ────────────────────────────────────────
    private record HighlightEntry(
        String file,        // Tên file ảnh
        String title,       // Tên địa danh (Tiếng Việt)
        String subtitle,    // Mô tả ngắn
        String category,    // Tỉnh/thành phố
        String url,         // URL thông tin khi click
        Color  grad1,       // Màu gradient đầu (fallback)
        Color  grad2        // Màu gradient cuối (fallback)
    ) {}

    private static final HighlightEntry[] CATALOG = {
        new HighlightEntry(
            "ha_long_bay.jpg",
            "Vịnh Hạ Long",
            "Kỳ quan thiên nhiên thế giới mới",
            "Quảng Ninh",
            "https://vi.wikipedia.org/wiki/V%E1%BB%8Bnh_H%E1%BA%A1_Long",
            new Color(0x156EA8), new Color(0x5BB3DA)
        ),
        new HighlightEntry(
            "hoi_an.jpg",
            "Phố cổ Hội An",
            "Di sản văn hóa thế giới UNESCO",
            "Quảng Nam",
            "https://vi.wikipedia.org/wiki/Ph%E1%BB%91_c%E1%BB%95_H%E1%BB%99i_An",
            new Color(0xB56A10), new Color(0xE8A840)
        ),
        new HighlightEntry(
            "son_doong.jpg",
            "Hang Sơn Đoòng",
            "Hang động lớn nhất thế giới",
            "Quảng Bình",
            "https://vi.wikipedia.org/wiki/Hang_S%C6%A1n_%C4%90o%C3%B2ng",
            new Color(0x1A5C3A), new Color(0x3E8A5F)
        ),
        new HighlightEntry(
            "sapa.jpg",
            "Ruộng bậc thang Sapa",
            "Vẻ đẹp hùng vĩ vùng Tây Bắc",
            "Lào Cai",
            "https://vi.wikipedia.org/wiki/Sa_Pa",
            new Color(0x2E6B2A), new Color(0x7AAB50)
        ),
        new HighlightEntry(
            "my_son.jpg",
            "Thánh địa Mỹ Sơn",
            "Di sản Chăm Pa cổ đại",
            "Quảng Nam",
            "https://vi.wikipedia.org/wiki/Th%C3%A1nh_%C4%91%E1%BB%8Ba_M%E1%BB%B9_S%C6%A1n",
            new Color(0x7A4A1A), new Color(0xB88442)
        ),
        new HighlightEntry(
            "mui_ne.jpg",
            "Đồi cát Mũi Né",
            "Hoang mạc cát vàng bên bờ biển",
            "Bình Thuận",
            "https://vi.wikipedia.org/wiki/M%C5%A9i_N%C3%A9",
            new Color(0xC89A1E), new Color(0xE8C06A)
        ),
        new HighlightEntry(
            "da_lat.jpg",
            "Đà Lạt",
            "Thành phố ngàn hoa trên cao nguyên",
            "Lâm Đồng",
            "https://vi.wikipedia.org/wiki/%C4%90%C3%A0_L%E1%BA%A1t",
            new Color(0x6B2A6B), new Color(0xA86AC8)
        ),
    };

    private SearchHighlightProvider() {}

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Trả về highlight cho ngày hôm nay.
     * Mỗi ngày trả về 1 địa danh khác nhau theo vòng xoay.
     * Gọi phương thức này từ SwingWorker.doInBackground().
     */
    public static SearchHighlight getTodayHighlight() {
        int idx = LocalDate.now().getDayOfYear() % CATALOG.length;
        return build(CATALOG[idx]);
    }

    /**
     * Trả về highlight theo index cụ thể.
     * Dùng để preview/debug từng địa danh.
     */
    public static SearchHighlight getAt(int index) {
        return build(CATALOG[Math.abs(index) % CATALOG.length]);
    }

    /** Tổng số địa danh trong catalog. */
    public static int count() {
        return CATALOG.length;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private static SearchHighlight build(HighlightEntry e) {
        BufferedImage img = tryLoadImage(RES + e.file());
        if (img == null) {
            LOG.warning("Không tìm thấy ảnh: " + e.file() + " — dùng fallback gradient");
            img = makeFallback(e);
        }
        return new SearchHighlight(e.title(), e.subtitle(), e.category(), e.url(), img);
    }

    private static BufferedImage tryLoadImage(String resourcePath) {
        try (InputStream is = SearchHighlightProvider.class.getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (Exception ex) {
            LOG.warning("Lỗi khi load ảnh [" + resourcePath + "]: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Tạo ảnh placeholder gradient cao cấp khi thiếu file ảnh.
     * Hiển thị tên địa danh trên nền gradient màu đặc trưng.
     */
    private static BufferedImage makeFallback(HighlightEntry e) {
        int w = 400, h = 200;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // Gradient background
        g.setPaint(new GradientPaint(0, 0, e.grad1(), w, h, e.grad2()));
        g.fillRect(0, 0, w, h);

        // Decorative circles (góc phải, mờ)
        g.setColor(new Color(255, 255, 255, 22));
        for (int i = 0; i < 5; i++) {
            int sz = 60 + i * 35;
            g.fillOval(w - sz / 2, -sz / 3, sz, sz);
        }

        // Subtle noise texture (dải ngang mờ)
        g.setColor(new Color(0, 0, 0, 15));
        for (int y = 0; y < h; y += 3) {
            g.drawLine(0, y, w, y);
        }

        // Category tag (góc trên trái)
        g.setColor(new Color(255, 255, 255, 130));
        g.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        FontMetrics fmS = g.getFontMetrics();
        String tag = e.category().toUpperCase();
        int tagX = 14, tagY = 20;
        g.drawString(tag, tagX, tagY);

        // Gạch dưới tag
        g.setColor(new Color(255, 255, 255, 80));
        g.fillRect(tagX, tagY + 3, fmS.stringWidth(tag), 1);

        // Subtitle (phía dưới)
        g.setColor(new Color(255, 255, 255, 155));
        g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g.drawString(e.subtitle(), 14, h - 24);

        // Title (dòng cuối, nổi bật)
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, 17));
        g.drawString(e.title(), 14, h - 8);

        g.dispose();
        return img;
    }
}
