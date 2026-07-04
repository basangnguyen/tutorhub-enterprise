package com.mycompany.tutorhub_enterprise.client.ai;

import java.util.Map;

public final class AiPromptComposer {

    public static final String METADATA_CONTEXT = "conversationContext";
    public static final String METADATA_MEMORY_SIZE = "conversationMemorySize";
    public static final String METADATA_LONG_TERM_MEMORY = "longTermMemory";
    public static final String METADATA_LONG_TERM_MEMORY_SIZE = "longTermMemorySize";

    private AiPromptComposer() {
    }

    public static String compose(AiAgentRequest request) {
        if (request == null) {
            return "";
        }
        String message = safe(request.getMessage());
        String context = safe(getMetadata(request, METADATA_CONTEXT));
        String longTermMemory = safe(getMetadata(request, METADATA_LONG_TERM_MEMORY));
        if (context.isEmpty() && longTermMemory.isEmpty()) {
            return message;
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là Lavie AI Agent trong TutorHub. Hãy trả lời tự nhiên bằng tiếng Việt, ")
                .append("dựa trên bộ nhớ và ngữ cảnh nếu chúng liên quan. ")
                .append("Không nhắc lại toàn bộ ngữ cảnh nếu người dùng không yêu cầu.\n\n");
        if (!longTermMemory.isEmpty()) {
            prompt.append("Bộ nhớ lâu dài do người dùng chủ động lưu:\n")
                    .append(longTermMemory)
                    .append("\n\n");
        }
        if (!context.isEmpty()) {
            prompt.append("Ngữ cảnh hội thoại gần đây:\n")
                    .append(context)
                    .append("\n\n");
        }
        prompt.append("Tin nhắn mới của người dùng:\n")
                .append(message);
        return prompt.toString();
    }

    private static String getMetadata(AiAgentRequest request, String key) {
        Map<String, String> metadata = request.getMetadata();
        if (metadata == null || key == null) {
            return "";
        }
        return metadata.getOrDefault(key, "");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
