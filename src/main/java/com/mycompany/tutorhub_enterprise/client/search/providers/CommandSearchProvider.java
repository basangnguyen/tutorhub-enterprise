package com.mycompany.tutorhub_enterprise.client.search.providers;

import com.mycompany.tutorhub_enterprise.client.search.SearchAction;
import com.mycompany.tutorhub_enterprise.client.search.SearchProvider;
import com.mycompany.tutorhub_enterprise.client.search.SearchQuery;
import com.mycompany.tutorhub_enterprise.client.search.SearchResult;
import com.mycompany.tutorhub_enterprise.client.search.SearchResultType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CommandSearchProvider implements SearchProvider {

    private final Function<String, SearchAction> actionFactory;

    /**
     * @param actionFactory Hàm tạo SearchAction dựa trên mã màn hình (cardKey)
     */
    public CommandSearchProvider(Function<String, SearchAction> actionFactory) {
        this.actionFactory = actionFactory;
    }

    @Override
    public String id() {
        return "command";
    }

    @Override
    public boolean supports(SearchQuery query) {
        return true;
    }

    @Override
    public List<SearchResult> search(SearchQuery query) {
        List<SearchResult> results = new ArrayList<>();
        
        addCommandIfMatches(results, query, "Mở Bảng tin", "Đi tới màn hình tổng quan", "HOME", "Home", "bang tin home dashboard tong quan");
        addCommandIfMatches(results, query, "Mở Tin nhắn", "Mở hội thoại và tìm bạn bè", "MSG", "Chat", "tin nhan chat message hoi thoai");
        addCommandIfMatches(results, query, "Mở Lớp học", "Quản lý lớp học của tôi", "CLS", "Saved", "lop hoc class classroom quan ly");
        addCommandIfMatches(results, query, "Mở Lịch", "Xem lịch học và lịch dạy", "CAL", "Schedule", "lich calendar schedule");
        addCommandIfMatches(results, query, "Mở QuizHub", "Ôn tập và luyện quiz", "QUIZ", "QuizHub", "quiz quizhub on tap luyen tap");
        addCommandIfMatches(results, query, "Mở Tài liệu", "Mở drive tài liệu học tập", "DOC", "Docs", "tai lieu document docs drive");
        addCommandIfMatches(results, query, "Mở Hồ sơ", "Xem thông tin tài khoản", "USR", "Profile", "ho so profile tai khoan user");
        addCommandIfMatches(results, query, "Mở Nâng cấp", "Xem các gói TutorHub Premium", "PRO", "Upgrade", "nang cap upgrade premium vip");

        return results;
    }

    private void addCommandIfMatches(List<SearchResult> results, SearchQuery query, String title,
                                     String subtitle, String iconText, String cardKey, String aliases) {
        if (!matchesSearch(query, title, subtitle, aliases)) {
            return;
        }
        results.add(SearchResult.builder()
                .title(title)
                .subtitle(subtitle)
                .type(SearchResultType.COMMAND)
                .score(80.0)
                .iconText(iconText)
                .action(actionFactory.apply(cardKey))
                .build());
    }

    private boolean matchesSearch(SearchQuery query, String title, String subtitle, String aliases) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String haystack = SearchQuery.of(title + " " + subtitle + " " + aliases).getNormalizedText();
        return haystack.contains(query.getNormalizedText());
    }
}
