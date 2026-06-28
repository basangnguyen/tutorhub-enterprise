# Patch Java cho `HomeTab.java` — chuyển khu vực "Lớp học nổi bật" sang JavaFX WebView

> Đã đọc đúng code bạn gửi (1134 dòng). Tên field/method dưới đây **trùng 100%** với code thật:
> `classGridPanel`, `classModels`, `applyFilterAndSort()`, `updateUIState()`, `showClassDetailModal(...)`,
> `ClassModel.id/subj/sal/addr/time/req/tagText/tagColor/isSaved/isTaken`, `isGridView`, `getSubjectImagePath(...)`.
>
> `ClassModel` **đã có `id` kiểu `String`** (dùng trong `markClassAsTaken`, `resetClassButton` qua `m.id.equals(id)`),
> nên dùng trực tiếp `id` làm key — **không cần fallback index**.
>
> Project **đã có Gson** (`com.google.gson.JsonObject`, `JsonParser` dùng ở phần Locket) → dùng Gson để build JSON,
> **không thêm thư viện mới**, theo đúng cách bạn đang dùng (gọi full-qualified `com.google.gson.JsonObject`,
> không cần thêm `import` mới vì code hiện tại cũng không import Gson, chỉ gọi full path).

---

## ⚠️ Quan trọng về luồng thread (bắt buộc phải đúng, không thì sẽ crash/treo UI)

- `JFXPanel`/`WebView`/`WebEngine` phải được tạo & gọi trên **JavaFX Application Thread** → dùng `Platform.runLater(...)`.
- `classModels`, `applyFilterAndSort()`, `showClassDetailModal(...)` là Swing → phải chạy trên **Swing EDT**.
- Khi JS gọi `acceptClass`/`toggleHeart`/`reportHeight` xuống Java, callback đó chạy trên **JavaFX thread**, KHÔNG phải EDT
  → bên trong bridge phải bọc `SwingUtilities.invokeLater(...)` trước khi đụng vào `classModels`/Swing component.

---

## 1. Import cần thêm

Thêm ngay dưới khối import hiện tại (sau `import java.util.Random;`):

```java
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
```

---

## 2. Field cần thêm

Thêm ngay dưới dòng `private JPanel classGridPanel;` (giữ nguyên field cũ, không xoá):

```java
// --- WEBVIEW: Khu vực "Lớp học nổi bật" (thay cho card Swing) ---
private JFXPanel classesWebViewPanel;
private WebView classesWebView;
private WebEngine classesWebEngine;
private volatile boolean classesWebViewReady = false;
private String pendingClassesJson = null;
private String pendingClassesView = "grid";
```

---

## 3. Method mới — đặt ở cuối class, ví dụ ngay trước `// --- Helpers ---` (dòng 1098)

```java
// =====================================================================
// KHU VỰC "LỚP HỌC NỔI BẬT" — JavaFX WebView (thay cho Swing card cũ)
// =====================================================================

/**
 * Tạo JFXPanel chứa WebView, load classes.html từ resources, và setup bridge.
 * Gọi 1 LẦN duy nhất trong constructor.
 */
private JFXPanel initClassesWebView() {
    JFXPanel panel = new JFXPanel();
    // Chiều cao khởi tạo tạm, sẽ được JS báo lại đúng giá trị qua reportHeight()
    panel.setPreferredSize(new Dimension(0, 420));

    Platform.runLater(() -> {
        try {
            classesWebView = new WebView();
            classesWebEngine = classesWebView.getEngine();

            java.net.URL htmlUrl = getClass().getResource("/home-classes/classes.html");
            if (htmlUrl == null) {
                System.err.println("[HOME_TAB] Không tìm thấy /home-classes/classes.html trong resources!");
            } else {
                classesWebEngine.load(htmlUrl.toExternalForm());
            }

            classesWebEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    try {
                        JSObject window = (JSObject) classesWebEngine.executeScript("window");
                        window.setMember("TutorHubClassesBridge", new ClassesBridge());
                    } catch (Exception ex) {
                        System.err.println("[HOME_TAB] Lỗi inject TutorHubClassesBridge: " + ex.getMessage());
                    }
                    classesWebViewReady = true;
                    if (pendingClassesJson != null) {
                        pushDataToWebView(pendingClassesJson, pendingClassesView);
                    }
                } else if (newState == Worker.State.FAILED) {
                    System.err.println("[HOME_TAB] WebView load classes.html FAILED.");
                }
            });

            Scene scene = new Scene(classesWebView);
            panel.setScene(scene);
        } catch (Exception ex) {
            System.err.println("[HOME_TAB] Lỗi khởi tạo classesWebView: " + ex.getMessage());
        }
    });

    return panel;
}

/**
 * Gọi từ applyFilterAndSort() (trên EDT) mỗi khi danh sách hiển thị thay đổi.
 * Convert displayList -> JSON rồi đẩy xuống WebView qua window.TutorHubClasses.updateData(...)
 */
private void updateClassesWebView(List<ClassModel> displayList) {
    String json = buildClassesJson(displayList);
    String view = isGridView ? "grid" : "list";

    Platform.runLater(() -> {
        if (!classesWebViewReady) {
            // WebView chưa load xong -> lưu lại, sẽ render ngay khi SUCCEEDED
            pendingClassesJson = json;
            pendingClassesView = view;
            return;
        }
        pushDataToWebView(json, view);
    });
}

/** Phải gọi trên JavaFX Application Thread (đã ở trong Platform.runLater khi gọi method này). */
private void pushDataToWebView(String json, String view) {
    try {
        if (classesWebEngine == null) return;
        // json (từ Gson) là JSON array hợp lệ -> cũng là JS literal hợp lệ, không cần escape thêm
        String script = "window.TutorHubClasses && window.TutorHubClasses.updateData(" + json + ", '" + view + "');";
        classesWebEngine.executeScript(script);
    } catch (Exception ex) {
        System.err.println("[HOME_TAB] Lỗi push data vào WebView: " + ex.getMessage());
    }
}

/** Convert list ClassModel hiện đang hiển thị (đã filter/sort) thành JSON cho JS. */
private String buildClassesJson(List<ClassModel> list) {
    com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
    for (ClassModel m : list) {
        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
        o.addProperty("id", m.id);
        o.addProperty("subj", m.subj);
        o.addProperty("sal", m.sal);
        o.addProperty("addr", m.addr);
        o.addProperty("time", m.time);
        o.addProperty("req", m.req);
        o.addProperty("tagText", m.tagText);
        o.addProperty("tagColor", m.tagColor);
        o.addProperty("isSaved", m.isSaved);
        o.addProperty("isTaken", m.isTaken);
        o.addProperty("img", toWebViewImagePath(getSubjectImagePath(m.subj)));
        arr.add(o);
    }
    return arr.toString();
}

/**
 * classes.html nằm tại /home-classes/classes.html (resources root/home-classes/).
 * getSubjectImagePath(...) trả về path dạng "/images/xxx/yyy.jpg" (resources root/images/...).
 * Từ vị trí classes.html, ảnh đó nằm ở "../images/xxx/yyy.jpg" -> WebView resolve được
 * cả khi chạy từ thư mục classes (mvn) lẫn từ trong jar (jar:file:...!/home-classes/classes.html).
 * KHÔNG dùng CDN/ảnh online, 100% local resource, đúng yêu cầu.
 */
private String toWebViewImagePath(String classpathAbsolutePath) {
    if (classpathAbsolutePath == null || classpathAbsolutePath.isEmpty()) return "";
    String rel = classpathAbsolutePath.startsWith("/") ? classpathAbsolutePath.substring(1) : classpathAbsolutePath;
    return "../" + rel;
}

/** Tìm ClassModel theo id (id là String, đã có sẵn trong code — không cần fallback index). */
private ClassModel findClassModelById(String id) {
    if (id == null) return null;
    for (ClassModel m : classModels) {
        if (id.equals(m.id)) return m;
    }
    return null;
}

/**
 * Bridge object được set vào window.TutorHubClassesBridge bên JS.
 * Các method PHẢI public để JSObject gọi được.
 * Các method này chạy trên JavaFX thread -> luôn bọc SwingUtilities.invokeLater
 * trước khi đụng tới classModels / component Swing.
 */
public class ClassesBridge {

    /** JS gọi khi user bấm "Nhận lớp". Giữ đúng ý nghĩa: mở showClassDetailModal(...). */
    public void acceptClass(String id) {
        SwingUtilities.invokeLater(() -> {
            ClassModel m = findClassModelById(id);
            if (m == null) {
                System.out.println("[HOME_TAB] acceptClass: không tìm thấy ClassModel id=" + id);
                return;
            }
            if (m.isTaken) return; // đã chốt rồi thì không mở modal nữa, giữ đúng logic nút cũ
            showClassDetailModal(m.id, m.subj, m.sal, m.addr, m.time, m.req, m.tagText, m.tagColor, m);
        });
    }

    /** JS gọi khi user bấm "Thả tim". Giữ đúng ý nghĩa: đổi isSaved rồi applyFilterAndSort(). */
    public void toggleHeart(String id) {
        SwingUtilities.invokeLater(() -> {
            ClassModel m = findClassModelById(id);
            if (m == null) {
                System.out.println("[HOME_TAB] toggleHeart: không tìm thấy ClassModel id=" + id);
                return;
            }
            m.isSaved = !m.isSaved;
            applyFilterAndSort();
        });
    }

    /**
     * JS gọi sau khi render xong để báo chiều cao thật của nội dung (px).
     * Cần thiết vì WebView KHÔNG tự auto-height như iframe — nếu không resize
     * theo nội dung thật, JScrollPane bên ngoài (mainScrollPane) sẽ không scroll đúng
     * hoặc card bị cắt mất phía dưới.
     */
    public void reportHeight(double height) {
        SwingUtilities.invokeLater(() -> {
            int h = (int) Math.max(120, height);
            if (classesWebViewPanel != null) {
                classesWebViewPanel.setPreferredSize(new Dimension(classesWebViewPanel.getWidth(), h));
                classGridPanel.revalidate();
                if (mainScrollPane != null) mainScrollPane.revalidate();
            }
        });
    }
}
```

---

## 4. Sửa constructor — thay khu vực tạo `classGridPanel`

**Tìm đoạn này** (dòng ~209–212):

```java
        // --- 4. THẺ LỚP HỌC ---
        classGridPanel = new JPanel();
        classGridPanel.setOpaque(false);
        classGridPanel.setBorder(new EmptyBorder(0, 24, 40, 24)); 
```

**Thay bằng:**

```java
        // --- 4. THẺ LỚP HỌC (đã chuyển sang JavaFX WebView) ---
        classGridPanel = new JPanel(new BorderLayout());
        classGridPanel.setOpaque(false);
        classGridPanel.setBorder(new EmptyBorder(0, 24, 40, 24));
        classesWebViewPanel = initClassesWebView();
        classGridPanel.add(classesWebViewPanel, BorderLayout.CENTER);
```

> Lưu ý: `classGridPanel` giờ chỉ là khung chứa **cố định** cho `JFXPanel` — nó được add **một lần** ở constructor,
> không bị tạo lại mỗi lần filter. Đây là lý do bước 5 dưới đây phải bỏ `classGridPanel.removeAll()`.

---

## 5. Sửa `applyFilterAndSort()` — chỉ thay phần RENDER, giữ 100% logic filter/sort

**Toàn bộ phần tính `displayList` (lọc theo `quickFilter`, `filterSubject`, `filterLocation`, `filterSalary`,
`filterSchedule`, `filterStatus`, rồi `sort` theo `filterSort`) GIỮ NGUYÊN KHÔNG ĐỔI MỘT DÒNG.**

**Tìm đoạn đầu method** (dòng 903–905):

```java
    private void applyFilterAndSort() {
        classGridPanel.removeAll(); 
        List<ClassModel> displayList = new ArrayList<>();
```

**Thay bằng** (chỉ bỏ dòng `removeAll()` — vì `classGridPanel` giờ luôn chứa `classesWebViewPanel`, xoá hết sẽ làm mất WebView):

```java
    private void applyFilterAndSort() {
        List<ClassModel> displayList = new ArrayList<>();
```

**Tìm đoạn cuối method** (dòng 930–942 — phần build card Swing):

```java
        if (displayList.isEmpty()) { 
            classGridPanel.setLayout(new BorderLayout()); classGridPanel.add(emptyStatePanel, BorderLayout.CENTER); 
        } else { 
            if (isGridView) {
                // ĐÃ CHỈNH SỬA: Đổi sang 5 cột và giảm khoảng cách (gap) xuống 16px
                classGridPanel.setLayout(new GridLayout(0, 5, 16, 16)); 
                for (ClassModel m : displayList) classGridPanel.add(createCompactGridCard(m)); 
            } else {
                classGridPanel.setLayout(new BoxLayout(classGridPanel, BoxLayout.Y_AXIS));
                for (ClassModel m : displayList) { classGridPanel.add(createListCard(m)); classGridPanel.add(Box.createVerticalStrut(12)); }
            }
        }
        classGridPanel.revalidate(); classGridPanel.repaint();
    }
```

**Thay bằng:**

```java
        // Render khu vực "Lớp học nổi bật" bằng WebView, thay cho Swing card cũ.
        // displayList ở trên đã được filter + sort đúng y nguyên logic cũ.
        updateClassesWebView(displayList);
    }
```

---

## 6. Có xoá `createCompactGridCard(...)` / `createListCard(...)` không?

**Không xoá.** Chỉ ngưng gọi (đã ngưng gọi ở bước 5). Lý do:

- `HoverEffect(...)`, `getSubjectImagePath(...)`, `truncate(...)` vẫn được các method khác dùng chung (kể cả
  `getSubjectImagePath` giờ còn được `buildClassesJson` dùng lại).
- Xoá `createCompactGridCard`/`createListCard` không sai về mặt logic, nhưng nếu sau này bạn muốn rollback
  nhanh về Swing (ví dụ WebView lỗi máy nào đó không có JavaFX runtime), 2 method này vẫn còn nguyên để dùng lại.
- `emptyStatePanel` / `initEmptyState()` cũng **giữ nguyên, không xoá** — chỉ không còn được gọi trong
  `applyFilterAndSort()` vì empty state giờ render bằng HTML/CSS (`#emptyState` trong `classes.html`).

Nếu về sau bạn chắc chắn không cần rollback, có thể xoá an toàn: `createCompactGridCard`, `createListCard`,
`initEmptyState()`, field `emptyStatePanel`. Nhưng đây là lựa chọn của bạn, không bắt buộc.

---

## 7. Vì sao nút Lưới/Danh sách (`btnViewToggle`, `isGridView`) vẫn hoạt động?

Code cũ: bấm nút đổi `isGridView` rồi gọi `applyFilterAndSort()` (dòng ~652–656) — **không đổi gì ở đây**.
Vì `applyFilterAndSort()` giờ gọi `updateClassesWebView(displayList)`, trong đó:

```java
String view = isGridView ? "grid" : "list";
```

→ JS nhận `view` và tự đổi class `view-grid`/`view-list` (đã style sẵn trong `classes.css`). Vậy nút Lưới/Danh sách
**vẫn hoạt động đúng như cũ**, chỉ đổi nơi render.

---

## 8. Checklist đối chiếu với yêu cầu "logic bắt buộc giữ nguyên"

| Yêu cầu | Đáp ứng bằng |
|---|---|
| Không sửa logic lấy dữ liệu lớp học / API / DB | Không đụng tới `displayNewClass`, `markClassAsTaken`, `resetClassButton`, `NetworkManager`, `Packet`. |
| Không đổi `classModels` | Không đổi field, không đổi cấu trúc `ClassModel`. |
| Không đổi logic filter/sort | Toàn bộ block tính `displayList` trong `applyFilterAndSort()` giữ nguyên 100%. |
| "Nhận lớp" vẫn gọi `showClassDetailModal(...)` | `ClassesBridge.acceptClass(id)` gọi đúng method này với đúng tham số. |
| "Thả tim" vẫn đổi `m.isSaved` + gọi `applyFilterAndSort()` | `ClassesBridge.toggleHeart(id)` làm đúng 2 việc đó. |
| Filter/sort đổi → WebView render lại đúng danh sách | `updateUIState()` → `applyFilterAndSort()` → `updateClassesWebView(displayList)` mỗi lần filter đổi. |
| Không CDN / không icon online / không path tuyệt đối | `classes.html/css/js` 100% local, icon là inline SVG, ảnh dùng resource path tương đối `../images/...`. |
| Không thêm thư viện JSON mới | Dùng lại Gson (`com.google.gson.*`) đã có sẵn trong project. |

---

## 9. Resource cần đặt đúng chỗ

```
src/main/resources/home-classes/classes.html
src/main/resources/home-classes/classes.css
src/main/resources/home-classes/classes.js
```

3 file này tôi đã tạo sẵn ở phần trên — copy nguyên vào đúng path là chạy được, không cần sửa gì thêm trong đó.

---

## 10. Một quyết định tôi đã tự đưa ra — cần bạn xác nhận

- **Không thêm menu ba chấm (⋯)** trên card, vì hiện code Java không có method/logic nào tương ứng (ví dụ
  "Sửa lớp", "Xoá lớp", "Báo cáo"...). Nếu tôi thêm menu mà không có hành động thật phía Java, đó sẽ là UI giả —
  vi phạm đúng nguyên tắc bạn đặt ra ("không tự bịa method/logic không tồn tại"). Nếu bạn có sẵn 1-2 hành động cụ
  thể muốn gắn vào menu này (và method Java tương ứng), gửi tên method/field thật, tôi sẽ bổ sung `classes.js` +
  patch Java cho phần đó.
- **Thêm `reportHeight(double)` vào bridge** — không có trong checklist gốc của bạn (mục 8 chỉ liệt kê 8 việc),
  nhưng đây là phần kỹ thuật bắt buộc phải có để WebView không bị cắt nội dung/scroll sai, vì WebView không tự
  auto-resize theo nội dung như iframe HTML thường. Đây là bổ sung kỹ thuật cần thiết, không đổi logic nghiệp vụ
  nào trong 8 mục bạn yêu cầu.
