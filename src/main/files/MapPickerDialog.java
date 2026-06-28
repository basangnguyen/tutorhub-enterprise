package com.tutorhub.mappicker;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Dialog Swing chọn địa chỉ trên bản đồ, dùng JCEF để hiển thị
 * map_picker.html (MapLibre GL JS + OpenFreeMap tiles + Goong Geocoding).
 *
 * <h2>Cách dùng</h2>
 * <pre>{@code
 *   // cefApp là CefApp đã khởi tạo sẵn trong app của bạn (đã setup JCEF core)
 *   MapAddressResult result = MapPickerDialog.pickAddress(
 *           parentFrame, cefApp, "YOUR_GOONG_API_KEY");
 *
 *   if (result != null) {
 *       System.out.println(result.getAddress() + " @ " + result.getLat() + "," + result.getLng());
 *   } else {
 *       System.out.println("Người dùng đã hủy chọn địa chỉ.");
 *   }
 * }</pre>
 *
 * <h2>Resource HTML</h2>
 * File {@code map_picker.html} phải nằm trên classpath tại
 * {@code /mappicker/map_picker.html} (ví dụ trong Maven:
 * {@code src/main/resources/mappicker/map_picker.html}). Nếu bạn đặt ở vị
 * trí khác, sửa {@link #HTML_RESOURCE_PATH}.
 *
 * <h2>Lưu ý threading</h2>
 * Theo Javadoc của JCEF, {@code CefMessageRouterHandler#onQuery} được gọi
 * trên luồng UI riêng của trình duyệt (CEF UI thread), KHÔNG phải Swing
 * Event Dispatch Thread. Vì vậy mọi thay đổi lên UI Swing (đóng dialog...)
 * đều được bọc trong {@code SwingUtilities.invokeLater(...)}.
 *
 * <h2>Tương thích API JCEF</h2>
 * Các lời gọi {@code dispose()/close()/removeMessageRouter(...)} có thể
 * hơi khác nhau tùy bản phân phối JCEF bạn dùng (chromiumembedded/java-cef
 * gốc, JetBrains/jcef, hay wrapper jcefmaven). Phần dọn dẹp resource được
 * bọc try/catch — nếu IDE báo thiếu method nào, chỉnh lại tên cho khớp với
 * API trong jar JCEF bạn đang dùng.
 */
public class MapPickerDialog extends JDialog {

    private static final String HTML_RESOURCE_PATH = "/mappicker/map_picker.html";
    private static final String PLACEHOLDER = "__GOONG_API_KEY__";

    private final CefClient cefClient;
    private final boolean ownsCefClient;
    private final CefBrowser browser;
    private final CefMessageRouter messageRouter;
    private final MapAddressResult initialValue;

    private File tempHtmlFile;
    private MapAddressResult result;

    // ─────────────────────────────────────────────────────────────
    // Constructors
    // ─────────────────────────────────────────────────────────────

    /**
     * Tạo dialog với một {@link CefClient} mới, riêng cho dialog này
     * (được dispose tự động khi dialog đóng). Dùng overload này khi bạn
     * chỉ có {@link CefApp} và muốn MapPickerDialog tự quản lý client.
     */
    public MapPickerDialog(Window owner, CefApp cefApp, String goongApiKey, MapAddressResult initialValue) {
        this(owner, requireCefApp(cefApp).createClient(), true, goongApiKey, initialValue);
    }

    /**
     * Tạo dialog dùng lại một {@link CefClient} đã có sẵn (do app của bạn
     * quản lý). MapPickerDialog sẽ KHÔNG dispose client này khi đóng —
     * chỉ gỡ message router/load handler mà nó tự thêm vào.
     */
    public MapPickerDialog(Window owner, CefClient existingClient, String goongApiKey, MapAddressResult initialValue) {
        this(owner, existingClient, false, goongApiKey, initialValue);
    }

    private MapPickerDialog(Window owner, CefClient cefClient, boolean ownsCefClient,
                             String goongApiKey, MapAddressResult initialValue) {
        super(owner, "Chọn địa chỉ dạy học", ModalityType.APPLICATION_MODAL);
        this.cefClient = cefClient;
        this.ownsCefClient = ownsCefClient;
        this.initialValue = initialValue;

        String url = prepareHtmlFileUrl(goongApiKey);

        // false, false => windowed (native) rendering, không phải offscreen.
        // Đây là cách phổ biến nhất khi nhúng JCEF vào Swing JDialog/JPanel.
        this.browser = cefClient.createBrowser(url, false, false);

        this.messageRouter = CefMessageRouter.create(
                new CefMessageRouter.CefMessageRouterConfig(), queryHandler);
        this.cefClient.addMessageRouter(messageRouter);
        this.cefClient.addLoadHandler(loadHandler);

        setLayout(new BorderLayout());
        add(browser.getUIComponent(), BorderLayout.CENTER);
        setPreferredSize(new Dimension(980, 640));
        pack();
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Người dùng bấm nút "X" trên thanh title => coi như hủy chọn
                result = null;
                dispose();
            }
        });
    }

    private static CefApp requireCefApp(CefApp cefApp) {
        if (cefApp == null) {
            throw new IllegalArgumentException("cefApp không được null. "
                    + "Truyền vào CefApp đã khởi tạo sẵn trong app của bạn.");
        }
        return cefApp;
    }

    // ─────────────────────────────────────────────────────────────
    // Static convenience API
    // ─────────────────────────────────────────────────────────────

    /** Mở dialog chọn địa chỉ (không có giá trị khởi tạo), chặn (block) tới khi đóng. */
    public static MapAddressResult pickAddress(Window owner, CefApp cefApp, String goongApiKey) {
        return pickAddress(owner, cefApp, goongApiKey, null);
    }

    /**
     * Mở dialog chọn địa chỉ, có thể truyền sẵn vị trí ban đầu (chế độ sửa).
     *
     * @return {@link MapAddressResult} nếu người dùng bấm "Xác nhận",
     *         hoặc {@code null} nếu người dùng bấm "Đóng"/hủy.
     */
    public static MapAddressResult pickAddress(Window owner, CefApp cefApp, String goongApiKey,
                                                MapAddressResult initialValue) {
        MapPickerDialog dialog = new MapPickerDialog(owner, cefApp, goongApiKey, initialValue);
        dialog.setVisible(true); // chặn ở đây (modal) cho tới khi dispose() được gọi
        return dialog.getResult();
    }

    /** Biến thể dùng CefClient có sẵn (do app bạn tự quản lý vòng đời). */
    public static MapAddressResult pickAddress(Window owner, CefClient existingClient, String goongApiKey,
                                                 MapAddressResult initialValue) {
        MapPickerDialog dialog = new MapPickerDialog(owner, existingClient, goongApiKey, initialValue);
        dialog.setVisible(true);
        return dialog.getResult();
    }

    public MapAddressResult getResult() {
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // JS -> Java bridge (window.cefQuery)
    // ─────────────────────────────────────────────────────────────

    private final CefMessageRouterHandlerAdapter queryHandler = new CefMessageRouterHandlerAdapter() {
        @Override
        public boolean onQuery(CefBrowser br, CefFrame frame, long queryId, String request,
                                boolean persistent, CefQueryCallback callback) {
            if (request == null) {
                return false;
            }

            if (request.startsWith("MAP_SELECT:")) {
                String json = request.substring("MAP_SELECT:".length());
                try {
                    Map<String, String> fields = parseFlatJsonObject(json);
                    String address = fields.get("address");
                    double lat = Double.parseDouble(fields.get("lat"));
                    double lng = Double.parseDouble(fields.get("lng"));
                    result = new MapAddressResult(address, lat, lng);
                    callback.success("ok");
                } catch (RuntimeException ex) {
                    callback.failure(-1, "Không đọc được dữ liệu địa chỉ: " + ex.getMessage());
                    return true;
                }
                SwingUtilities.invokeLater(MapPickerDialog.this::dispose);
                return true;
            }

            if ("MAP_CANCEL".equals(request)) {
                result = null;
                callback.success("ok");
                SwingUtilities.invokeLater(MapPickerDialog.this::dispose);
                return true;
            }

            return false; // không phải request của router này
        }
    };

    // ─────────────────────────────────────────────────────────────
    // Java -> JS: preload vị trí ban đầu (chế độ sửa) sau khi trang load xong
    // ─────────────────────────────────────────────────────────────

    private final CefLoadHandlerAdapter loadHandler = new CefLoadHandlerAdapter() {
        @Override
        public void onLoadEnd(CefBrowser br, CefFrame frame, int httpStatusCode) {
            if (initialValue == null) {
                return;
            }
            String js = String.format(Locale.US,
                    "if (typeof goTo === 'function') { goTo(%s, %s, %s); }",
                    Double.toString(initialValue.getLat()),
                    Double.toString(initialValue.getLng()),
                    toJsStringLiteral(initialValue.getAddress()));
            br.executeJavaScript(js, br.getURL(), 0);
        }
    };

    // ─────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────

    @Override
    public void dispose() {
        try {
            cefClient.removeLoadHandler(loadHandler);
        } catch (RuntimeException ignored) {
            // tùy bản JCEF, có thể không có removeLoadHandler — bỏ qua an toàn
        }
        try {
            cefClient.removeMessageRouter(messageRouter);
        } catch (RuntimeException ignored) {
        }
        try {
            messageRouter.dispose();
        } catch (RuntimeException ignored) {
        }
        try {
            if (browser != null) {
                browser.close(true);
            }
        } catch (RuntimeException ignored) {
        }
        if (ownsCefClient) {
            try {
                cefClient.dispose();
            } catch (RuntimeException ignored) {
            }
        }
        deleteTempFileQuietly();
        super.dispose();
    }

    private void deleteTempFileQuietly() {
        if (tempHtmlFile != null) {
            try {
                Files.deleteIfExists(tempHtmlFile.toPath());
            } catch (IOException ignored) {
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // HTML resource -> temp file with injected API key
    // ─────────────────────────────────────────────────────────────

    /**
     * Đọc map_picker.html từ classpath, thay placeholder __GOONG_API_KEY__
     * bằng API key thật, ghi ra file tạm, rồi trả về URL file:// để nạp
     * vào CefBrowser.
     */
    private String prepareHtmlFileUrl(String goongApiKey) {
        String html = readHtmlResource();
        String safeKey = goongApiKey == null ? "" : goongApiKey;
        String injected = html.replace(PLACEHOLDER, Matcher.quoteReplacement(safeKey));

        try {
            tempHtmlFile = File.createTempFile("tutorhub_map_picker_", ".html");
            tempHtmlFile.deleteOnExit();
            Files.write(tempHtmlFile.toPath(), injected.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Không thể tạo file HTML tạm cho map picker", e);
        }
        return tempHtmlFile.toURI().toString();
    }

    private String readHtmlResource() {
        try (InputStream in = MapPickerDialog.class.getResourceAsStream(HTML_RESOURCE_PATH)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Không tìm thấy resource " + HTML_RESOURCE_PATH + " trên classpath. "
                        + "Đảm bảo map_picker.html nằm tại src/main/resources" + HTML_RESOURCE_PATH);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Lỗi đọc resource " + HTML_RESOURCE_PATH, e);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Tiny helpers — không phụ thuộc thư viện JSON ngoài
    // ─────────────────────────────────────────────────────────────

    /**
     * Parser JSON tối giản, chỉ đủ dùng cho object phẳng dạng
     * {@code {"address":"...","lat":10.1,"lng":106.2}} mà phía JS gửi qua
     * {@code JSON.stringify}. Hỗ trợ escape chuẩn (\", \\, \n, \t, \r, \/, \\uXXXX).
     * Không dùng cho JSON lồng nhau / mảng.
     */
    static Map<String, String> parseFlatJsonObject(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        if (json == null) return map;
        int i = 0;
        int n = json.length();

        while (i < n) {
            while (i < n && json.charAt(i) != '"') i++;
            if (i >= n) break;
            i++; // bỏ qua dấu " mở của key
            StringBuilder key = new StringBuilder();
            while (i < n && json.charAt(i) != '"') {
                key.append(json.charAt(i));
                i++;
            }
            i++; // bỏ qua dấu " đóng của key
            while (i < n && json.charAt(i) != ':') i++;
            i++; // bỏ qua dấu ':'
            while (i < n && Character.isWhitespace(json.charAt(i))) i++;

            StringBuilder val = new StringBuilder();
            if (i < n && json.charAt(i) == '"') {
                i++; // bỏ qua dấu " mở của value
                while (i < n && json.charAt(i) != '"') {
                    char c = json.charAt(i);
                    if (c == '\\' && i + 1 < n) {
                        char next = json.charAt(i + 1);
                        switch (next) {
                            case 'n': val.append('\n'); i += 2; break;
                            case 't': val.append('\t'); i += 2; break;
                            case 'r': val.append('\r'); i += 2; break;
                            case '"': val.append('"'); i += 2; break;
                            case '\\': val.append('\\'); i += 2; break;
                            case '/': val.append('/'); i += 2; break;
                            case 'u':
                                if (i + 5 < n) {
                                    String hex = json.substring(i + 2, i + 6);
                                    val.append((char) Integer.parseInt(hex, 16));
                                    i += 6;
                                } else {
                                    i += 2;
                                }
                                break;
                            default:
                                val.append(next);
                                i += 2;
                        }
                    } else {
                        val.append(c);
                        i++;
                    }
                }
                i++; // bỏ qua dấu " đóng của value
            } else {
                while (i < n && json.charAt(i) != ',' && json.charAt(i) != '}') {
                    val.append(json.charAt(i));
                    i++;
                }
            }

            map.put(key.toString(), val.toString().trim());

            while (i < n && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
            if (i < n && json.charAt(i) == ',') i++;
        }
        return map;
    }

    /** Escape một chuỗi Java thành literal chuỗi JS an toàn (đặt trong dấu nháy đơn). */
    static String toJsStringLiteral(String s) {
        if (s == null) return "''";
        StringBuilder sb = new StringBuilder("'");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\'': sb.append("\\'"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\u2028': sb.append("\\u2028"); break; // line separator — phá vỡ JS string nếu để nguyên
                case '\u2029': sb.append("\\u2029"); break;
                default: sb.append(c);
            }
        }
        sb.append("'");
        return sb.toString();
    }
}
