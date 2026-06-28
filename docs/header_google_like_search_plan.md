# Header Google-like Search Plan

## 1. File/method currently creating search bar

Thanh tìm kiếm header hiện được tạo trong:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/HeaderPanel.java`
  - Constructor `HeaderPanel(MainDashboard dashboard, String userName)`.
  - Tạo `globalSearchContainer` kích thước `460 x 52`.
  - Gắn `com.mycompany.tutorhub_enterprise.client.search.GlobalSearchBar`.
  - Expose `getGlobalSearchInput()` và `getGlobalSearchContainer()`.

Component search thật nằm trong:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/GlobalSearchBar.java`
  - Custom `JPanel`, vẽ bằng Java2D.
  - Bên trong có `JTextField searchField`.
  - Có icon kính lúp, animation hover/focus, thumbnail cảnh Việt Nam bên phải.
  - Tự tạo `SearchSuggestionsPopup`.

Popup gợi ý mẫu nằm trong:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/SearchSuggestionsPopup.java`
  - Dùng `JPopupMenu`.
  - Gợi ý hiện tại lấy từ `POOL` hardcode.
  - Filter bằng `contains()` đơn giản.

Nguồn ảnh highlight:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/SearchHighlightProvider.java`
  - Catalog ảnh trong resources.
  - Xoay ảnh theo ngày và timer 60 giây.

## 2. Current search flow

Hiện tại search header không phải search toàn cục đúng nghĩa. Nó là sự kết hợp của 2 hệ:

1. `GlobalSearchBar` tự quản lý UI/popup gợi ý mẫu.
2. `ChatTab` bind trực tiếp vào `JTextField` của header để làm search tin nhắn/người dùng.

Luồng chính:

```text
MainDashboard.createMainArea()
  -> new HeaderPanel(...)
  -> new ChatTab(currentUserId)
  -> chatTab.bindGlobalSearchBar(headerPanel.getGlobalSearchInput(), headerPanel.getGlobalSearchContainer())

User typing
  -> GlobalSearchBar DocumentListener updates SearchSuggestionsPopup hardcoded suggestions
  -> ChatTab DocumentListener updates searchKeyword
  -> 300ms debounce
  -> ChatTab.executeSearch()
       -> refreshMessages()
       -> local search current conversation
       -> local search conversation list
       -> NetworkManager.sendPacket("SEARCH_USER", searchKeyword)

Server
  -> ClientHandler case "SEARCH_USER"
  -> UserDAO.searchUsers(keyword, currentUserId)
  -> Packet("SEARCH_USER_RESULT", List<UserInfo>)

Client
  -> MainDashboard handles "SEARCH_USER_RESULT"
  -> chatTab.updateSearchResults(...)
  -> ChatTab.renderSearchPopupResults()
```

Server search hiện tại:

- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
  - `case "SEARCH_USER"`.
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/UserDAO.java`
  - `searchUsers(String keyword, int currentUserId)`.
  - SQL `ILIKE` trên `users.email`, `users.full_name`, `LIMIT 10`.

## 3. Tabs using search

| Khu vực | File | Cách search hiện tại |
|---|---|---|
| Header | `HeaderPanel.java`, `GlobalSearchBar.java` | UI custom, popup hardcode, bind vào ChatTab |
| Tin nhắn | `ChatTab.java` | Local message/conversation search + server user search |
| Lớp học của tôi | `ClassManagerTab.java` | Local filter lesson/classroom theo tên, tổ chức, mã lớp |
| Bảng vẽ | `BlackboardManagerTab.java` | Local filter board title |
| Lớp đã nhận | `AcceptedClassTab.java` | Local filter lớp theo title/subject/student |
| Drive | `DriveTab.java` | Background JavaFX task, gọi `fileDAO.searchFiles(...)` |
| Quản lý gia sư | `TutorManagementTab.java` | `RowFilter.regexFilter` trên bảng |
| Reels | `ReelsTabPanel.java` | Popup JavaFX search UI riêng |

Kết luận: Search đang phân mảnh theo từng tab. Header search chưa có `SearchController`, chưa có `SearchService`, chưa có hợp đồng `SearchProvider`.

## 4. Current problems

### Kiến trúc

- Header search coupling trực tiếp với `ChatTab`.
- `GlobalSearchBar` và `ChatTab` cùng lắng nghe `DocumentListener` trên một `JTextField`, dễ sinh popup chồng popup.
- `SearchSuggestionsPopup` có pool hardcode, không đại diện dữ liệu thật.
- Chưa có model chuẩn cho result/suggestion.
- Chưa có ranking chung giữa lớp học, tin nhắn, tài liệu, bảng vẽ, tác vụ, người dùng.
- Chưa có cancel request cũ khi người dùng gõ nhanh.
- Chưa có cache/history gần đây.
- Chưa có command search kiểu Spotlight/Slack/Teams.

### UI/UX

- Placeholder đang bị `ChatTab` set thành `"Tìm kiếm Biểu tượng"`, không đúng vai trò search toàn cục.
- Có 2 popup cạnh tranh: popup của `GlobalSearchBar` và popup của `ChatTab`.
- `ChatTab.updatePopupSelection()` có bug: method khai báo biến cục bộ `int selectedPopupIndex = 0`, khiến keyboard selection không dùng field hiện tại.
- Chưa có grouping rõ ràng: Người dùng, Tin nhắn, Lớp học, Tài liệu, Bảng vẽ, Lệnh nhanh, Web.
- Chưa có trạng thái loading/error/offline chuẩn cho từng provider.

### Hiệu năng và bảo mật

- Một số search local chạy trực tiếp trên EDT.
- Server user search chưa có min query length, rate limit riêng, hoặc request cancellation.
- Không nên scrape Google/Bing.
- Không đưa API key web search vào desktop client.
- Query search có thể chứa dữ liệu riêng tư; web fallback phải do người dùng chủ động bấm.

## 5. Google/Omnibox/search UX takeaways

Nguồn tham khảo chính thức:

- Google Search Help: autocomplete dự đoán khi người dùng gõ, có personalization/trending và report prediction.  
  https://support.google.com/websearch/answer/106230
- Chrome Help: thanh địa chỉ tìm trên Internet, bookmark, history; gợi ý xuất hiện khi nhập.  
  https://support.google.com/chrome/answer/95440
- Apple Spotlight: kết quả xuất hiện khi gõ, top match trước, Enter để mở.  
  https://support.apple.com/guide/mac-help/search-with-spotlight-mchlp1008/mac
- Slack Search: search field ở top, recent search, modifiers, filters theo Messages/Files/People/Channels.  
  https://slack.com/help/articles/202528808-Search-in-Slack
- Microsoft Search: tìm app, people, files, sites, messages trong hệ sinh thái Microsoft.  
  https://support.microsoft.com/en-us/office/find-what-you-need-with-microsoft-search-d5ed5d11-9e5d-4f1d-b8b4-3d371fe0cb87

Pattern nên học:

- Search là entry point trung tâm, không chỉ là filter text box.
- Gợi ý hiện khi gõ, nội dung nội bộ phải ưu tiên trước web.
- Có nhóm result theo loại dữ liệu.
- Có top match và recent searches.
- Keyboard-first: `Ctrl+K`, `Up/Down`, `Enter`, `Esc`.
- Command search hỗ trợ câu tự nhiên: "tạo lớp", "mở bảng vẽ", "lịch hôm nay".
- Web search là fallback có chủ đích, không tự gửi query riêng tư ra ngoài.

## 6. Proposed search architecture

Đề xuất module:

```text
client/search/
  SearchController.java
  SearchService.java
  SearchProvider.java
  SearchQuery.java
  SearchResult.java
  SearchSuggestion.java
  SearchResultType.java
  SearchAction.java
  SearchRanking.java
  SearchTextUtils.java
  VietnameseNormalizer.java
  SearchHistoryStore.java
  SearchDropdownPanel.java
  SearchResultRenderer.java
  providers/
    LocalSearchProvider.java
    ChatSearchProvider.java
    ClassSearchProvider.java
    BlackboardSearchProvider.java
    DriveSearchProvider.java
    CommandSearchProvider.java
    UserSearchProvider.java
    WebSearchProvider.java
```

Trách nhiệm:

| Module | Vai trò |
|---|---|
| `SearchController` | Bind `GlobalSearchBar`, debounce, cancel request cũ, điều phối dropdown |
| `SearchService` | Gọi provider, merge result, ranking, timeout |
| `SearchProvider` | Interface chung cho từng nguồn dữ liệu |
| `SearchResult` | Model kết quả chuẩn: title, subtitle, type, icon, score, action |
| `SearchDropdownPanel` | UI popup duy nhất thay cho 2 popup hiện tại |
| `SearchHistoryStore` | Lưu query/result gần đây local |
| `VietnameseNormalizer` | Bỏ dấu, lowercase, chuẩn hóa `đ`, trim, collapse whitespace |
| `CommandSearchProvider` | Lệnh nhanh: tạo lớp, vào bảng vẽ, mở drive, tạo nhiệm vụ |
| `WebSearchProvider` | Fallback web search hoặc gọi backend search khi được bật |

Interface gợi ý:

```java
public interface SearchProvider {
    String id();
    boolean supports(SearchQuery query);
    CompletableFuture<List<SearchResult>> search(SearchQuery query);
}
```

## 7. Suggested dropdown UI

Dropdown nên là một popup duy nhất:

```text
┌──────────────────────────────────────────────┐
│ Tìm kiếm TutorHub                            │
│ ──────────────────────────────────────────── │
│ Top match                                    │
│  [icon] Lớp của basangthaonhi                │
│        Lớp học • My Account                  │
│                                              │
│ Tin nhắn                                     │
│  [avatar] Nguyễn Bá Sáng                     │
│          "...nội dung match..."              │
│                                              │
│ Lớp học                                      │
│  [class] Public lesson 03/06                 │
│                                              │
│ Lệnh nhanh                                   │
│  [+] Tạo lớp học mới                         │
│  [board] Mở bảng vẽ gần đây                  │
│                                              │
│ Web                                          │
│  [globe] Tìm trên web: <query>               │
└──────────────────────────────────────────────┘
```

Yêu cầu UI:

- Dùng cùng visual language với TutorHub: trắng, border nhẹ, shadow nhẹ, icon xanh tím.
- Gợi ý theo nhóm, mỗi nhóm tối đa 3-5 item.
- Result có icon/avatar, title, subtitle, matched snippet.
- Highlight phần match.
- `Enter` mở item đang chọn.
- `Esc` đóng popup.
- `Ctrl+K` focus search.
- Empty state rõ: "Không tìm thấy trong TutorHub" + dòng web fallback.

## 8. Ranking/search normalization

Chuẩn hóa query:

```text
trim
lowercase(Locale.ROOT)
normalize Unicode NFD
remove combining diacritics
replace đ/Đ -> d/D
collapse whitespace
```

Ranking MVP:

| Tín hiệu | Điểm gợi ý |
|---|---:|
| Exact title match | +100 |
| Prefix title match | +80 |
| Contains title | +60 |
| Match subtitle/snippet | +35 |
| Recent item | +15 |
| Same current tab | +12 |
| Frequently opened | +10 |
| Command exact alias | +90 |
| Web fallback | -20 |

Tiếng Việt:

- Search `lop hoc` phải match `lớp học`.
- Search `bang ve` phải match `bảng vẽ`.
- Search `nguyen ba sang` phải match `Nguyễn Bá Sáng`.
- Không chỉ dùng `String.toLowerCase().contains()` như hiện tại.

## 9. Safe internet search options

Không scrape Google/Bing từ client.

Phương án an toàn:

1. MVP không cần API key: chỉ hiện row `Tìm trên web: <query>` và mở trình duyệt bằng URL search khi người dùng bấm.
2. Production tốt hơn: desktop gọi backend TutorHub, backend giữ API key và gọi provider web search.
3. Provider có thể cân nhắc:
   - Google Programmable Search JSON API: https://developers.google.com/custom-search/v1/overview
   - Brave Search API: https://brave.com/search/api/
   - Bing Web Search API qua Microsoft Azure nếu có quota phù hợp.

Quy tắc:

- Không hardcode API key trong Java desktop.
- Không tự động gửi query riêng tư ra web.
- Web result phải đứng sau kết quả nội bộ.
- Cho phép tắt web fallback bằng config.

## 10. Detailed phases

### Phase 1 - Audit/fix conflict, không đổi UX lớn

- Tách `ChatTab.bindGlobalSearchBar(...)` khỏi vai trò owner duy nhất của header search.
- Sửa bug keyboard selection ở `ChatTab.updatePopupSelection()` khi bước implementation bắt đầu.
- Tắt hoặc thay thế `SearchSuggestionsPopup` hardcode bằng adapter tạm.
- Đặt placeholder header về `"Tìm kiếm trong TutorHub..."`.

### Phase 2 - Search model + dropdown thống nhất

- Tạo `SearchQuery`, `SearchResult`, `SearchResultType`, `SearchAction`.
- Tạo `SearchDropdownPanel`.
- `GlobalSearchBar` chỉ phát event query/focus/submit, không tự biết dữ liệu.

### Phase 3 - Local providers

- `CommandSearchProvider`: tạo lớp, tham gia lớp, mở bảng vẽ, mở drive, lịch hôm nay.
- `ChatSearchProvider`: conversation/message local.
- `ClassSearchProvider`: classroom/public lesson.
- `BlackboardSearchProvider`: board title.
- `DriveSearchProvider`: chỉ gọi async/background.

### Phase 4 - Ranking/history/Vietnamese normalization

- Thêm `VietnameseNormalizer`, `SearchRanking`, `SearchHistoryStore`.
- Recent searches và recently opened.
- Min query length cho server user search.

### Phase 5 - Server user search cleanup

- Chuẩn hóa `SEARCH_USER` hoặc tạo action mới `GLOBAL_SEARCH_USER`.
- Thêm debounce/cancel client-side, rate limit server-side nếu cần.
- Không log query nhạy cảm quá chi tiết.

### Phase 6 - Web fallback

- MVP: row mở trình duyệt web search khi user bấm.
- Production: backend Search API giữ API key.
- Không triển khai scraping.

## 11. Risks and rollback

| Rủi ro | Cách giảm |
|---|---|
| Popup mới làm hỏng chat search | Giữ `ChatTab` popup cũ sau feature flag trong phase đầu |
| Search chạy nặng trên EDT | Provider dùng `SwingWorker`/`CompletableFuture`, update UI trên EDT |
| Query riêng tư bị gửi ra web | Chỉ gửi khi user click web fallback |
| API key bị lộ | Chỉ dùng backend, không để key trong desktop |
| Kết quả quá nhiễu | Ranking + grouping + limit mỗi nhóm |
| Dữ liệu tabs chưa expose service | Bắt đầu từ providers đọc data sẵn có, sau đó mới chuẩn hóa store |

Rollback đơn giản:

- Giữ `GlobalSearchBar` hiện tại.
- Feature flag `tutorhub.search.v2=false`.
- Nếu lỗi, quay lại `ChatTab.bindGlobalSearchBar(...)` cũ.

## 12. Files to edit per phase

Phase 1:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/HeaderPanel.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/GlobalSearchBar.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/ChatTab.java`

Phase 2:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/SearchController.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/SearchService.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/SearchProvider.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/SearchResult.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/SearchDropdownPanel.java`

Phase 3:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/providers/CommandSearchProvider.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/providers/ChatSearchProvider.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/providers/ClassSearchProvider.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/providers/BlackboardSearchProvider.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/providers/DriveSearchProvider.java`

Phase 4:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/VietnameseNormalizer.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/SearchRanking.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/SearchHistoryStore.java`

Phase 5:

- `src/main/java/com/mycompany/tutorhub_enterprise/server/ClientHandler.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/server/dao/UserDAO.java`
- `src/main/java/com/mycompany/tutorhub_enterprise/client/NetworkManager.java` nếu cần correlation id/cancel token.

Phase 6:

- `src/main/java/com/mycompany/tutorhub_enterprise/client/search/providers/WebSearchProvider.java`
- Backend API riêng nếu chọn production web search.

