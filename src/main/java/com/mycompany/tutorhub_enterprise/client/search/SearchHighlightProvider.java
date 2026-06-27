package com.mycompany.tutorhub_enterprise.client.search;

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

    private record HighlightEntry(
        String file,        // Tên file ảnh trong /images/Search/
        String previewFile, // Tên file ảnh trong /images/Preview/
        String title,       // Tên địa danh (Tiếng Việt)
        String subtitle,    // Mô tả ngắn
        String category,    // Tỉnh/thành phố
        String url,         // URL thông tin khi click
        Color  grad1,       // Màu gradient đầu (fallback)
        Color  grad2        // Màu gradient cuối (fallback)
    ) {}

    private static final HighlightEntry[] CATALOG = {
        new HighlightEntry("01-vinh-ha-long.png", "01-ha-long-bay-preview.png", "Vịnh Hạ Long", "Kỳ quan thiên nhiên", "Quảng Ninh", "https://vi.wikipedia.org/wiki/V%E1%BB%8Bnh_H%E1%BA%A1_Long", new Color(0x156EA8), new Color(0x5BB3DA)),
        new HighlightEntry("02-ho-guom-ha-noi.png", "02-ha-noi-ho-guom-preview.png", "Hồ Gươm", "Biểu tượng thủ đô", "Hà Nội", "https://vi.wikipedia.org/wiki/H%E1%BB%93_Ho%C3%A0n_Ki%E1%BA%BFm", new Color(0x2A6B2A), new Color(0x5E9E5E)),
        new HighlightEntry("03-pho-co-hoi-an-chua-cau.png", "03-hoi-an-japanese-bridge-preview.png", "Phố cổ Hội An", "Chùa Cầu lịch sử", "Quảng Nam", "https://vi.wikipedia.org/wiki/Ph%E1%BB%91_c%E1%BB%95_H%E1%BB%99i_An", new Color(0xB56A10), new Color(0xE8A840)),
        new HighlightEntry("04-dai-noi-hue.png", "04-hue-imperial-city-preview.png", "Đại Nội Huế", "Cố đô lịch sử", "Thừa Thiên Huế", "https://vi.wikipedia.org/wiki/Qu%E1%BA%A7n_th%E1%BB%83_di_t%C3%ADch_C%E1%BB%91_%C4%91%C3%B4_Hu%E1%BA%BF", new Color(0x7A4A1A), new Color(0xB88442)),
        new HighlightEntry("05-cau-vang-da-nang.png", "05-da-nang-golden-bridge-preview.png", "Cầu Vàng", "Bàn tay khổng lồ", "Đà Nẵng", "https://vi.wikipedia.org/wiki/C%E1%BA%A7u_V%C3%A0ng_(%C4%90%C3%A0_N%E1%BA%B5ng)", new Color(0xC89A1E), new Color(0xE8C06A)),
        new HighlightEntry("06-trang-an-tam-coc-ninh-binh.png", "06-ninh-binh-trang-an-preview.png", "Tràng An", "Vịnh Hạ Long trên cạn", "Ninh Bình", "https://vi.wikipedia.org/wiki/Qu%E1%BA%A7n_th%E1%BB%83_danh_th%E1%BA%AFng_Tr%C3%A0ng_An", new Color(0x1A5C3A), new Color(0x3E8A5F)),
        new HighlightEntry("07-thac-ban-gioc.png", "07-ban-gioc-waterfall-preview.png", "Thác Bản Giốc", "Thác nước hùng vĩ", "Cao Bằng", "https://vi.wikipedia.org/wiki/Th%C3%A1c_B%E1%BA%A3n_Gi%E1%BB%91c", new Color(0x156EA8), new Color(0x5BB3DA)),
        new HighlightEntry("08-ruong-bac-thang-sa-pa.png", "08-sapa-terraced-fields-preview.png", "Ruộng bậc thang", "Vẻ đẹp Tây Bắc", "Lào Cai", "https://vi.wikipedia.org/wiki/Sa_Pa", new Color(0x2E6B2A), new Color(0x7AAB50)),
        new HighlightEntry("09-cho-ben-thanh-tphcm.png", "09-ben-thanh-market-preview.png", "Chợ Bến Thành", "Biểu tượng Sài Gòn", "TP. Hồ Chí Minh", "https://vi.wikipedia.org/wiki/Ch%E1%BB%A3_B%E1%BA%BFn_Th%C3%A0nh", new Color(0x8A2A2A), new Color(0xC85A5A)),
        new HighlightEntry("10.png", "10-phu-quoc-beach-preview.png", "Biển Phú Quốc", "Đảo ngọc", "Kiên Giang", "https://vi.wikipedia.org/wiki/Ph%C3%BA_Qu%E1%BB%91c", new Color(0x156EA8), new Color(0x5BB3DA)),
        new HighlightEntry("11-bien-phu-quoc.png", "11-cai-rang-floating-market-preview.png", "Chợ Nổi Cái Răng", "Đặc trưng miền Tây", "Cần Thơ", "https://vi.wikipedia.org/wiki/Ch%E1%BB%A3_n%E1%BB%95i_C%C3%A1i_R%C4%83ng", new Color(0x156EA8), new Color(0x5BB3DA))
    };

    private SearchHighlightProvider() {}

    // ─── Public API ──────────────────────────────────────────────────────────

    private static int currentRotationIndex = -1;
    private static SearchHighlight[] cache = new SearchHighlight[CATALOG.length];

    /**
     * Trả về highlight cho ngày hôm nay.
     * Mỗi ngày trả về 1 địa danh khác nhau theo vòng xoay.
     */
    public static SearchHighlight getTodayHighlight() {
        int idx = LocalDate.now().getDayOfYear() % CATALOG.length;
        if (currentRotationIndex == -1) currentRotationIndex = idx;
        return getAt(idx);
    }

    /**
     * Tự động xoay vòng ảnh tiếp theo. Dùng cho Timer.
     */
    public static SearchHighlight getNextHighlight() {
        if (currentRotationIndex == -1) {
            currentRotationIndex = LocalDate.now().getDayOfYear() % CATALOG.length;
        }
        currentRotationIndex = (currentRotationIndex + 1) % CATALOG.length;
        return getAt(currentRotationIndex);
    }

    /**
     * Trả về highlight theo index cụ thể.
     */
    public static SearchHighlight getAt(int index) {
        int idx = Math.abs(index) % CATALOG.length;
        if (cache[idx] == null) {
            cache[idx] = build(CATALOG[idx]);
        }
        return cache[idx];
    }
    
    public static int getCurrentIndex() {
        if (currentRotationIndex == -1) {
            currentRotationIndex = LocalDate.now().getDayOfYear() % CATALOG.length;
        }
        return currentRotationIndex;
    }

    /** Tổng số địa danh trong catalog. */
    public static int count() {
        return CATALOG.length;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private static SearchHighlight build(HighlightEntry e) {
        String resPath = "/images/Search/" + e.file();
        String previewPath = "/images/Preview/" + e.previewFile();
        
        BufferedImage img = tryLoadImageFromResource(resPath);
        BufferedImage previewImg = tryLoadImageFromResource(previewPath);
        
        if (img == null) {
            LOG.warning("Không tìm thấy ảnh: " + resPath + " — dùng fallback gradient");
            img = makeFallback(e);
        }
        return new SearchHighlight(e.title(), e.subtitle(), e.category(), e.url(), img, previewImg);
    }

    private static BufferedImage tryLoadImageFromResource(String path) {
        try (InputStream is = SearchHighlightProvider.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (Exception ex) {
            LOG.warning("Lỗi khi load ảnh từ resource [" + path + "]: " + ex.getMessage());
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
