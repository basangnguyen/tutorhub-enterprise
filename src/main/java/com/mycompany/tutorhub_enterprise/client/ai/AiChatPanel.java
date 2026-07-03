package com.mycompany.tutorhub_enterprise.client.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.tutorhub_enterprise.client.JcefManager;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AiChatPanel extends JPanel {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String BRIDGE_CHANNEL = "tutorhub.ai";

    private CefBrowser browser;
    private Timer activeMockTimer;
    private List<String> activeStreamChunks = new ArrayList<>();
    private int activeChunkIndex = 0;
    private AiAgentStreamHandle activeStreamHandle;
    private final AiAgentService aiService;
    private final String userId;
    private final String conversationId;

    public AiChatPanel() {
        this(AiAgentServiceFactory.createDefault(), "tutorhub_desktop", "lavie");
    }

    public AiChatPanel(AiAgentService aiService, String userId, String conversationId) {
        this.aiService = aiService == null ? new LavieAiService() : aiService;
        this.userId = userId == null || userId.trim().isEmpty() ? "tutorhub_desktop" : userId.trim();
        this.conversationId = conversationId == null || conversationId.trim().isEmpty() ? "lavie" : conversationId.trim();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        initBrowser();
    }

    public void focusComposer() {
        executeAgentJs("focusComposer");
    }

    private void initBrowser() {
        try {
            URL url = getClass().getResource("/ai/ai_chat.html");
            if (url == null) {
                add(createFallback("Khong tim thay /ai/ai_chat.html"), BorderLayout.CENTER);
                return;
            }

            browser = JcefManager.getClient().createBrowser(url.toExternalForm(), false, false);

            CefMessageRouter.CefMessageRouterConfig config =
                    new CefMessageRouter.CefMessageRouterConfig("cefQuery", "cefQueryCancel");
            CefMessageRouter router = CefMessageRouter.create(config);
            router.addHandler(new AiBridgeHandler(), true);
            JcefManager.getClient().addMessageRouter(router);

            add(browser.getUIComponent(), BorderLayout.CENTER);
        } catch (Exception ex) {
            ex.printStackTrace();
            add(createFallback("Khong the khoi tao AI Agent WebView: " + ex.getMessage()), BorderLayout.CENTER);
        }
    }

    private JComponent createFallback(String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setForeground(new Color(0x64748B));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(label);
        return panel;
    }

    private void handleBridgeRequest(String request, CefQueryCallback callback) {
        JsonObject envelope = JsonParser.parseString(request).getAsJsonObject();
        if (!BRIDGE_CHANNEL.equals(getString(envelope, "channel"))) {
            callback.failure(-2, "Unsupported channel");
            return;
        }

        String type = getString(envelope, "type");
        JsonObject payload = envelope.has("payload") && envelope.get("payload").isJsonObject()
                ? envelope.getAsJsonObject("payload")
                : new JsonObject();

        switch (type) {
            case "READY":
                callback.success("{\"ok\":true}");
                break;
            case "SEND_MESSAGE":
                String text = getString(payload, "text").trim();
                if (text.isEmpty()) {
                    callback.failure(-3, "Tin nhan rong");
                    return;
                }
                callback.success("{\"ok\":true}");
                startLavieStream(text);
                break;
            case "STOP_STREAM":
                stopStream();
                callback.success("{\"ok\":true}");
                break;
            case "ATTACH_FILE":
            case "VOICE_START":
                callback.success("{\"ok\":true}");
                executeAgentJs("showError", "Chức năng này sẽ được nối vào Lavie service trong phase tiếp theo.");
                break;
            default:
                callback.failure(-4, "Unknown AI request type: " + type);
        }
    }

    private String getString(JsonObject obj, String key) {
        if (obj == null || key == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        return obj.get(key).getAsString();
    }

    private void startMockStream(String userMessage) {
        SwingUtilities.invokeLater(() -> {
            stopStream();
            JsonObject meta = new JsonObject();
            meta.addProperty("startedAt", new Date().getTime());
            meta.addProperty("mode", "phase0");
            executeAgentJs("startAssistantMessage", meta);

            activeStreamChunks = chunk(buildMockResponse(userMessage), 3);
            activeChunkIndex = 0;
            activeMockTimer = new Timer(24, event -> {
                if (activeChunkIndex >= activeStreamChunks.size()) {
                    stopStream();
                    executeAgentJs("finishAssistantMessage");
                    return;
                }
                executeAgentJs("appendAssistantDelta", activeStreamChunks.get(activeChunkIndex));
                activeChunkIndex++;
            });
            activeMockTimer.start();
        });
    }

    private void startLavieStream(String userMessage) {
        SwingUtilities.invokeLater(() -> {
            stopStream();
            JsonObject meta = new JsonObject();
            meta.addProperty("startedAt", new Date().getTime());
            meta.addProperty("mode", "lavie-hf");
            executeAgentJs("startAssistantMessage", meta);
            executeAgentJs("setStatus", "Đang kết nối Lavie");

            AtomicBoolean hasDelta = new AtomicBoolean(false);
            AiAgentRequest request = AiAgentRequest.builder()
                    .message(userMessage)
                    .userId(userId)
                    .conversationId(conversationId)
                    .metadata("provider", aiService.getProviderName())
                    .build();
            activeStreamHandle = aiService.streamChat(request, new AiAgentStreamCallback() {
                @Override
                public void onDelta(String delta) {
                    if (delta == null || delta.isEmpty()) {
                        return;
                    }
                    hasDelta.set(true);
                    SwingUtilities.invokeLater(() -> {
                        executeAgentJs("setStatus", "Đang trả lời");
                        executeAgentJs("appendAssistantDelta", delta);
                    });
                }

                @Override
                public void onComplete() {
                    SwingUtilities.invokeLater(() -> {
                        activeStreamHandle = null;
                        executeAgentJs("finishAssistantMessage");
                        executeAgentJs("setStatus", "Sẵn sàng");
                    });
                }

                @Override
                public void onError(Exception error) {
                    SwingUtilities.invokeLater(() -> {
                        activeStreamHandle = null;
                        String message = hasDelta.get()
                                ? "\n\n[Kết nối Lavie bị ngắt. Bạn có thể gửi lại tin nhắn.]"
                                : buildFallbackResponse(userMessage);
                        executeAgentJs("appendAssistantDelta", message);
                        executeAgentJs("finishAssistantMessage");
                        executeAgentJs("setStatus", "Sẵn sàng");
                        executeAgentJs("showError", "Không kết nối được Lavie server. Đã dùng phản hồi fallback cục bộ.");
                    });
                }
            });
        });
    }

    private void stopStream() {
        if (activeStreamHandle != null) {
            activeStreamHandle.cancel();
            activeStreamHandle = null;
        }
        if (activeMockTimer != null) {
            activeMockTimer.stop();
            activeMockTimer = null;
        }
    }

    private String buildMockResponse(String userMessage) {
        String clean = userMessage.replaceAll("\\s+", " ").trim();
        return "Mình đã nhận: \"" + clean + "\".\n\n"
                + "Phase 0 đang kiểm tra shell AI Agent trong ChatTab: WebView, bridge JSON, "
                + "streaming token và bố cục hội thoại. Kết nối Lavie/Hugging Face và LangChain4j "
                + "sẽ được tách sang phase tiếp theo để không làm vỡ chat thường.\n\n"
                + "```java\n"
                + "AiChatPanel panel = new AiChatPanel();\n"
                + "chatCenter.add(panel, \"AI_CARD\");\n"
                + "```\n\n"
                + "Trạng thái: UI agent đã sẵn sàng cho bước nối service.";
    }

    private String buildFallbackResponse(String userMessage) {
        String clean = userMessage.replaceAll("\\s+", " ").trim();
        return "Mình chưa kết nối được Lavie server cho tin nhắn: \"" + clean + "\".\n\n"
                + "UI Agent vẫn hoạt động bình thường: WebView đã nhận tin nhắn, bridge JSON đã chạy, "
                + "và fallback cục bộ đã được kích hoạt. Khi Hugging Face Space phản hồi ổn định, "
                + "nội dung sẽ stream trực tiếp từ endpoint Lavie.\n\n"
                + "```text\n"
                + "POST /api/chat/stream\n"
                + "SSE data: { \"content\": \"...\" }\n"
                + "```";
    }

    private List<String> chunk(String text, int size) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        int step = Math.max(1, size);
        for (int i = 0; i < text.length(); i += step) {
            chunks.add(text.substring(i, Math.min(i + step, text.length())));
        }
        return chunks;
    }

    private void executeAgentJs(String functionName, Object... args) {
        if (browser == null || functionName == null || functionName.trim().isEmpty()) {
            return;
        }
        StringBuilder js = new StringBuilder();
        js.append("if (window.TutorHubAgent && typeof window.TutorHubAgent.")
                .append(functionName)
                .append(" === 'function') { window.TutorHubAgent.")
                .append(functionName)
                .append("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                js.append(',');
            }
            js.append(GSON.toJson(args[i]));
        }
        js.append("); }");
        browser.executeJavaScript(js.toString(), browser.getURL(), 0);
    }

    private class AiBridgeHandler extends CefMessageRouterHandlerAdapter {
        @Override
        public boolean onQuery(CefBrowser br, CefFrame frame, long queryId,
                               String request, boolean persistent, CefQueryCallback callback) {
            if (request == null || !request.contains("\"channel\":\"" + BRIDGE_CHANNEL + "\"")) {
                return false;
            }
            try {
                handleBridgeRequest(request, callback);
            } catch (Exception ex) {
                callback.failure(-1, "AI bridge error: " + ex.getMessage());
                executeAgentJs("showError", "AI bridge error: " + ex.getMessage());
            }
            return true;
        }
    }
}
