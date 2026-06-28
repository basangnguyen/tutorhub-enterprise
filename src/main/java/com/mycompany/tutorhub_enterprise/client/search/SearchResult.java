package com.mycompany.tutorhub_enterprise.client.search;

import java.util.Objects;

public final class SearchResult {
    private final String title;
    private final String subtitle;
    private final SearchResultType type;
    private final double score;
    private final String iconText;
    private final SearchAction action;

    private SearchResult(Builder builder) {
        this.title = sanitize(builder.title);
        this.subtitle = sanitize(builder.subtitle);
        this.type = builder.type == null ? SearchResultType.EMPTY : builder.type;
        this.score = builder.score;
        this.iconText = sanitize(builder.iconText);
        this.action = builder.action == null ? SearchAction.noop() : builder.action;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public SearchResultType getType() {
        return type;
    }

    public double getScore() {
        return score;
    }

    public String getIconText() {
        return iconText;
    }

    public SearchAction getAction() {
        return action;
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Builder {
        private String title;
        private String subtitle;
        private SearchResultType type;
        private double score;
        private String iconText;
        private SearchAction action;

        private Builder() {
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder subtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder type(SearchResultType type) {
            this.type = type;
            return this;
        }

        public Builder score(double score) {
            this.score = score;
            return this;
        }

        public Builder iconText(String iconText) {
            this.iconText = iconText;
            return this;
        }

        public Builder action(SearchAction action) {
            this.action = action;
            return this;
        }

        public SearchResult build() {
            Objects.requireNonNull(title, "title");
            return new SearchResult(this);
        }
    }
}
