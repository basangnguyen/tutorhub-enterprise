package com.mycompany.tutorhub_enterprise.client.ai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AiAgentRequest {

    private final String message;
    private final String userId;
    private final String conversationId;
    private final Map<String, String> metadata;

    private AiAgentRequest(Builder builder) {
        this.message = builder.message;
        this.userId = builder.userId;
        this.conversationId = builder.conversationId;
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
    }

    public String getMessage() {
        return message;
    }

    public String getUserId() {
        return userId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message = "";
        private String userId = "tutorhub_desktop";
        private String conversationId = "lavie";
        private final Map<String, String> metadata = new LinkedHashMap<>();

        public Builder message(String message) {
            this.message = message == null ? "" : message;
            return this;
        }

        public Builder userId(String userId) {
            if (userId != null && !userId.trim().isEmpty()) {
                this.userId = userId.trim();
            }
            return this;
        }

        public Builder conversationId(String conversationId) {
            if (conversationId != null && !conversationId.trim().isEmpty()) {
                this.conversationId = conversationId.trim();
            }
            return this;
        }

        public Builder metadata(String key, String value) {
            if (key != null && !key.trim().isEmpty() && value != null) {
                metadata.put(key.trim(), value);
            }
            return this;
        }

        public AiAgentRequest build() {
            return new AiAgentRequest(this);
        }
    }
}
