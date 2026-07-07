package com.mycompany.tutorhub_enterprise.client.ai;

import java.util.Map;

public final class AiPromptComposer {

    private static final String BASE_LAVIE_CONTEXT = """
            Bạn là Lavie AI Agent, trợ lý AI độc quyền của TutorHub Enterprise.
            TutorHub Enterprise là nền tảng học online, quản lý lớp học và kết nối gia sư được phát triển cho hệ sinh thái TutorHub.
            Người sáng lập và phát triển TutorHub Enterprise là Nguyễn Bá Sáng, sinh viên ngành Công nghệ Thông tin tại Học viện Kỹ thuật Mật mã (KMA).
            Lavie được thiết kế để hỗ trợ người dùng trong TutorHub bằng tiếng Việt: giải thích kiến thức, hỗ trợ học tập, hỗ trợ sử dụng app, phân tích tài liệu, hình ảnh và trò chuyện tự nhiên.
            Nếu người dùng hỏi về TutorHub, Lavie, người tạo ra Lavie/TutorHub, tính năng nội bộ hoặc cách dùng app, hãy ưu tiên thông tin nội bộ thay vì trả lời chung chung.
            """;

    public static final String METADATA_CONTEXT = "conversationContext";
    public static final String METADATA_MEMORY_SIZE = "conversationMemorySize";
    public static final String METADATA_LONG_TERM_MEMORY = "longTermMemory";
    public static final String METADATA_LONG_TERM_MEMORY_SIZE = "longTermMemorySize";
    public static final String METADATA_ATTACHMENTS_CONTEXT = "attachmentsContext";
    public static final String METADATA_ATTACHMENTS_JSON = "attachmentsJson";
    public static final String METADATA_REMOTE_SERVER_CONTEXT = "remoteServerContext";

    private AiPromptComposer() {
    }

    public static String compose(AiAgentRequest request) {
        if (request == null) {
            return "";
        }
        String message = safe(request.getMessage());
        String remoteServerContext = safe(getMetadata(request, METADATA_REMOTE_SERVER_CONTEXT));
        String context = safe(getMetadata(request, METADATA_CONTEXT));
        String longTermMemory = safe(getMetadata(request, METADATA_LONG_TERM_MEMORY));
        String attachmentsContext = safe(getMetadata(request, METADATA_ATTACHMENTS_CONTEXT));

        StringBuilder prompt = new StringBuilder();
        prompt.append(BASE_LAVIE_CONTEXT).append('\n');
        if (!remoteServerContext.isEmpty()) {
            prompt.append("Ngữ cảnh đồng bộ từ máy chủ Lavie trên Hugging Face (ưu tiên cao):\n")
                    .append(remoteServerContext)
                    .append("\n\n");
        }
        prompt.append("Quy tắc trả lời bắt buộc:\n")
                .append("- Trả lời tự nhiên bằng tiếng Việt, đúng trọng tâm, không quá dài nếu người dùng không yêu cầu.\n")
                .append("- Nếu ngữ cảnh Hugging Face có communication_style, call_user, assistant_self_reference hoặc lavie_behavior_rules thì phải tuân theo các trường đó.\n")
                .append("- Không chỉ dùng dòng đầu của bộ nhớ. Hãy đọc và dùng toàn bộ user_memory JSON và knowledge base liên quan.\n")
                .append("- Khi người dùng hỏi về TutorHub, Lavie, người sáng lập, tính cách Lavie, kiến thức nội bộ hoặc tính năng app, ưu tiên ngữ cảnh máy chủ Lavie.\n")
                .append("- Dựa trên bộ nhớ, ngữ cảnh hội thoại và tệp/ngữ cảnh đính kèm nếu chúng liên quan.\n")
                .append("- Không nhắc lại toàn bộ ngữ cảnh hoặc JSON thô nếu người dùng không yêu cầu.\n")
                .append("- Nếu không chắc, nói rõ phần chưa chắc thay vì bịa.\n\n");
        if (!longTermMemory.isEmpty()) {
            prompt.append("Bộ nhớ lâu dài do người dùng chủ động lưu trong app desktop:\n")
                    .append(longTermMemory)
                    .append("\n\n");
        }
        if (!context.isEmpty()) {
            prompt.append("Ngữ cảnh hội thoại gần đây:\n")
                    .append(context)
                    .append("\n\n");
        }
        if (!attachmentsContext.isEmpty()) {
            prompt.append("Tệp, ảnh hoặc ngữ cảnh nội bộ được đính kèm cho lượt này:\n")
                    .append(attachmentsContext)
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
