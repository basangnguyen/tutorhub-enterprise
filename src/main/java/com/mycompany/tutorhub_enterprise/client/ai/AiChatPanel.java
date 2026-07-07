package com.mycompany.tutorhub_enterprise.client.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mycompany.tutorhub_enterprise.client.JcefManager;
import com.mycompany.tutorhub_enterprise.client.ai.agent.AgentConfig;
import com.mycompany.tutorhub_enterprise.client.ai.agent.AgentContext;
import com.mycompany.tutorhub_enterprise.client.ai.agent.AgentLoop;
import com.mycompany.tutorhub_enterprise.client.ai.agent.AgentTurn;
import com.mycompany.tutorhub_enterprise.client.ai.agent.AgentsMdLoader;
import com.mycompany.tutorhub_enterprise.client.ai.command.PendingCommandStore;
import com.mycompany.tutorhub_enterprise.client.ai.command.CommandSpec;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpServerRegistry;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.McpToolCallSpec;
import com.mycompany.tutorhub_enterprise.client.ai.mcp.PendingMcpToolCallStore;
import com.mycompany.tutorhub_enterprise.client.ai.patch.PendingPatchStore;
import com.mycompany.tutorhub_enterprise.client.ai.patch.PatchProposal;
import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.permission.AuditLog;
import com.mycompany.tutorhub_enterprise.client.ai.permission.PermissionPolicy;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolRegistry;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.ApplyPatchTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.RunMcpToolCallTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.impl.RunCommandTool;
import com.mycompany.tutorhub_enterprise.client.ai.ui.CommandPreviewView;
import com.mycompany.tutorhub_enterprise.client.ai.ui.McpToolCallPreviewView;
import com.mycompany.tutorhub_enterprise.client.ai.ui.PatchPreviewView;
import com.mycompany.tutorhub_enterprise.client.ai.ui.ToolCallLogView;
import com.mycompany.tutorhub_enterprise.config.AppConfig;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AiChatPanel extends JPanel {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String BRIDGE_CHANNEL = "tutorhub.ai";
    private static final long MAX_ATTACHMENT_BYTES = 12L * 1024L * 1024L;
    private static final long MAX_INLINE_IMAGE_BYTES = 8L * 1024L * 1024L;
    private static final int MAX_ATTACHMENT_TEXT_BYTES = 96 * 1024;
    private static final int MAX_ATTACHMENT_CONTEXT_CHARS = 14_000;
    private static final int MAX_ATTACHMENT_PREVIEW_CHARS = 9_000;
    private static final int MAX_PENDING_ATTACHMENTS = 8;
    private static final int MAX_VOICE_RECORD_SECONDS = 60;
    private static final int MIN_VOICE_PCM_BYTES = 3_200;
    private static final String LAVIE_SERVER_USER_ID = "java_user";

    private CefBrowser browser;
    private Timer activeMockTimer;
    private List<String> activeStreamChunks = new ArrayList<>();
    private int activeChunkIndex = 0;
    private AiAgentStreamHandle activeStreamHandle;
    private volatile AiAgentService aiService;
    private volatile AiAgentProviderConfig providerConfig;
    private final ExecutorService providerProbeExecutor;
    private final ExecutorService agentExecutor;
    private final ExecutorService attachmentExecutor;
    private final ExecutorService voiceExecutor;
    private final ExecutorService remoteContextExecutor;
    private final AiConversationMemory conversationMemory = new AiConversationMemory();
    private final AiLongTermMemoryStore longTermMemoryStore;
    private final PendingPatchStore pendingPatchStore = new PendingPatchStore();
    private final PendingCommandStore pendingCommandStore = new PendingCommandStore();
    private final PendingMcpToolCallStore pendingMcpToolCallStore = new PendingMcpToolCallStore();
    private final PermissionPolicy permissionPolicy = PermissionPolicy.phase101Defaults();
    private final AuditLog auditLog = new AuditLog();
    private final McpServerRegistry mcpServerRegistry = McpServerRegistry.fromEnvironment();
    private final String userId;
    private final String conversationId;
    private volatile boolean agentModeEnabled = false;
    private volatile String agentWorkspacePath;
    private volatile Future<?> activeAgentFuture;
    private volatile boolean lavieExpanded = false;
    private volatile Consumer<Boolean> lavieExpandedListener;
    private Runnable onOpenEmojiCallback;
    private final List<AiAttachment> pendingAttachments = new ArrayList<>();
    private volatile VoiceCaptureSession activeVoiceSession;

    public AiChatPanel() {
        this(AiAgentServiceFactory.loadDefaultConfig(), "tutorhub_desktop", "lavie");
    }

    public AiChatPanel(String userId, String conversationId) {
        this(AiAgentServiceFactory.loadDefaultConfig(), userId, conversationId);
    }

    public AiChatPanel(AiAgentService aiService, String userId, String conversationId) {
        this.providerConfig = AiAgentServiceFactory.loadDefaultConfig();
        this.aiService = aiService == null ? AiAgentServiceFactory.create(providerConfig) : aiService;
        this.userId = userId == null || userId.trim().isEmpty() ? "tutorhub_desktop" : userId.trim();
        this.conversationId = conversationId == null || conversationId.trim().isEmpty() ? "lavie" : conversationId.trim();
        this.longTermMemoryStore = new AiLongTermMemoryStore(this.userId, this.conversationId);
        this.providerProbeExecutor = createProviderProbeExecutor();
        this.agentExecutor = createAgentExecutor();
        this.attachmentExecutor = createAttachmentExecutor();
        this.voiceExecutor = createVoiceExecutor();
        this.remoteContextExecutor = createRemoteContextExecutor();
        this.agentWorkspacePath = defaultAgentWorkspacePath();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        initBrowser();
        warmUpLavieRemoteContext();
    }

    public AiChatPanel(AiAgentProviderConfig providerConfig, String userId, String conversationId) {
        this.providerConfig = providerConfig == null ? AiAgentServiceFactory.loadDefaultConfig() : providerConfig;
        this.aiService = AiAgentServiceFactory.create(this.providerConfig);
        this.userId = userId == null || userId.trim().isEmpty() ? "tutorhub_desktop" : userId.trim();
        this.conversationId = conversationId == null || conversationId.trim().isEmpty() ? "lavie" : conversationId.trim();
        this.longTermMemoryStore = new AiLongTermMemoryStore(this.userId, this.conversationId);
        this.providerProbeExecutor = createProviderProbeExecutor();
        this.agentExecutor = createAgentExecutor();
        this.attachmentExecutor = createAttachmentExecutor();
        this.voiceExecutor = createVoiceExecutor();
        this.remoteContextExecutor = createRemoteContextExecutor();
        this.agentWorkspacePath = defaultAgentWorkspacePath();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        initBrowser();
        warmUpLavieRemoteContext();
    }

    private ExecutorService createProviderProbeExecutor() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "ai-provider-probe");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    private ExecutorService createAgentExecutor() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "ai-readonly-agent-loop");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    private ExecutorService createAttachmentExecutor() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "ai-attachment-loader");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    private ExecutorService createVoiceExecutor() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "ai-native-voice");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    private ExecutorService createRemoteContextExecutor() {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "ai-lavie-remote-context");
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    private void warmUpLavieRemoteContext() {
        remoteContextExecutor.submit(() -> LavieRemoteContextProvider.shared().contextFor(LAVIE_SERVER_USER_ID));
    }

    @Override
    public void removeNotify() {
        stopStream();
        VoiceCaptureSession voiceSession = activeVoiceSession;
        if (voiceSession != null) {
            voiceSession.stop();
            activeVoiceSession = null;
        }
        providerProbeExecutor.shutdownNow();
        agentExecutor.shutdownNow();
        attachmentExecutor.shutdownNow();
        voiceExecutor.shutdownNow();
        remoteContextExecutor.shutdownNow();
        super.removeNotify();
    }

    public void focusComposer() {
        executeAgentJs("focusComposer");
    }

    public void setOnOpenEmojiCallback(Runnable callback) {
        this.onOpenEmojiCallback = callback;
    }

    public void insertEmoji(String tag) {
        String js = String.format(
            "const el = document.getElementById('input');\n" +
            "if (el) {\n" +
            "  const start = el.selectionStart;\n" +
            "  const end = el.selectionEnd;\n" +
            "  const val = el.value;\n" +
            "  el.value = val.substring(0, start) + '%s' + val.substring(end);\n" +
            "  el.selectionStart = el.selectionEnd = start + %d;\n" +
            "  el.focus();\n" +
            "  el.dispatchEvent(new Event('input', { bubbles: true }));\n" +
            "}", tag, tag.length()
        );
        if (browser != null) {
            browser.executeJavaScript(js, browser.getURL(), 0);
        }
    }

    public void setLavieExpandedListener(Consumer<Boolean> listener) {
        this.lavieExpandedListener = listener;
    }

    public void setLavieExpandedState(boolean expanded) {
        this.lavieExpanded = expanded;
        JsonObject json = new JsonObject();
        json.addProperty("expanded", expanded);
        executeAgentJs("setLavieLayoutState", json);
    }

    private void initBrowser() {
        try {
            URL url = getClass().getResource("/ai/ai_chat.html");
            if (url == null) {
                add(createFallback("Khong tim thay /ai/ai_chat.html"), BorderLayout.CENTER);
                return;
            }

            browser = JcefManager.getClient().createBrowser(url.toExternalForm(), false, false);

            JcefManager.getSharedMessageRouter().addHandler(new AiBridgeHandler(), true);

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
                executeAgentJs("applyProviderConfig", buildProviderConfigJson());
                executeAgentJs("setMemoryState", buildMemoryStateJson());
                executeAgentJs("setLongTermMemoryState", buildLongTermMemoryStateJson());
                executeAgentJs("setAgentModeState", buildAgentModeStateJson());
                executeAgentJs("setAgentContextState", buildAgentContextJson());
                setLavieExpandedState(lavieExpanded);
                executeAgentJs("setStatus", providerInitialStatusLabel(providerConfig));
                break;
            case "TOGGLE_LAVIE_EXPANDED":
                lavieExpanded = !lavieExpanded;
                Consumer<Boolean> listener = lavieExpandedListener;
                if (listener != null) {
                    SwingUtilities.invokeLater(() -> listener.accept(lavieExpanded));
                }
                JsonObject layout = new JsonObject();
                layout.addProperty("expanded", lavieExpanded);
                callback.success(GSON.toJson(layout));
                setLavieExpandedState(lavieExpanded);
                break;
            case "GET_CONFIG":
                callback.success(GSON.toJson(buildProviderConfigJson()));
                break;
            case "UPDATE_CONFIG":
                updateProviderConfig(payload);
                callback.success(GSON.toJson(buildProviderConfigJson()));
                break;
            case "RESET_CONFIG":
                resetProviderConfig();
                callback.success(GSON.toJson(buildProviderConfigJson()));
                break;
            case "CHECK_PROVIDER":
                callback.success("{\"ok\":true,\"status\":\"checking\"}");
                checkProviderConnectionAsync();
                break;
            case "CLEAR_MEMORY":
                clearConversationMemory();
                callback.success(GSON.toJson(buildMemoryStateJson()));
                break;
            case "GET_LONG_TERM_MEMORY":
                callback.success(GSON.toJson(buildLongTermMemoryStateJson()));
                break;
            case "ADD_LONG_TERM_MEMORY":
                addLongTermMemory(payload);
                callback.success(GSON.toJson(buildLongTermMemoryStateJson()));
                break;
            case "UPDATE_LONG_TERM_MEMORY":
                updateLongTermMemory(payload);
                callback.success(GSON.toJson(buildLongTermMemoryStateJson()));
                break;
            case "DELETE_LONG_TERM_MEMORY":
                deleteLongTermMemory(payload);
                callback.success(GSON.toJson(buildLongTermMemoryStateJson()));
                break;
            case "CLEAR_LONG_TERM_MEMORY":
                clearLongTermMemory();
                callback.success(GSON.toJson(buildLongTermMemoryStateJson()));
                break;
            case "GET_AGENT_MODE":
                callback.success(GSON.toJson(buildAgentModeStateJson()));
                break;
            case "GET_AGENT_CONTEXT":
                callback.success(GSON.toJson(buildAgentContextJson()));
                break;
            case "GET_AUDIT_LOG":
                callback.success(GSON.toJson(buildAuditLogJson()));
                break;
            case "UPDATE_AGENT_MODE":
                try {
                    updateAgentMode(payload);
                    callback.success(GSON.toJson(buildAgentModeStateJson()));
                } catch (Exception ex) {
                    callback.failure(-5, "Agent workspace khong hop le: " + ex.getMessage());
                }
                break;
            case "APPLY_PATCH":
                callback.success(GSON.toJson(applyApprovedPatch(payload)));
                break;
            case "REJECT_PATCH":
                callback.success(GSON.toJson(rejectPatch(payload)));
                break;
            case "RUN_COMMAND":
                String commandId = getString(payload, "commandId").trim();
                callback.success(GSON.toJson(CommandPreviewView.running(commandId)));
                runApprovedCommandAsync(commandId);
                break;
            case "REJECT_COMMAND":
                callback.success(GSON.toJson(rejectCommand(payload)));
                break;
            case "RUN_MCP_TOOL":
                String mcpCallId = getString(payload, "mcpCallId").trim();
                callback.success(GSON.toJson(McpToolCallPreviewView.running(mcpCallId)));
                runApprovedMcpToolAsync(mcpCallId);
                break;
            case "REJECT_MCP_TOOL":
                callback.success(GSON.toJson(rejectMcpToolCall(payload)));
                break;
            case "SEND_MESSAGE":
                String text = getString(payload, "text").trim();
                if (text.isEmpty() && !hasPendingAttachments()) {
                    callback.failure(-3, "Tin nhắn rỗng");
                    return;
                }
                if (text.isEmpty()) {
                    text = "Hãy phân tích các tệp hoặc hình ảnh mình vừa đính kèm.";
                }
                AttachmentPayload attachmentPayload = drainPendingAttachmentPayload();
                callback.success("{\"ok\":true}");
                if (getBoolean(payload, "agentMode", agentModeEnabled)) {
                    startReadOnlyAgent(text, attachmentPayload.context);
                } else {
                    startAiStream(text, attachmentPayload.context, attachmentPayload.attachmentsJson);
                }
                break;
            case "STOP_STREAM":
                stopStream();
                callback.success("{\"ok\":true}");
                break;
            case "OPEN_EMOJI":
                if (onOpenEmojiCallback != null) {
                    javax.swing.SwingUtilities.invokeLater(onOpenEmojiCallback);
                }
                callback.success("{\"ok\":true}");
                break;
            case "ATTACH_FILE":
                callback.success("{\"ok\":true}");
                chooseAttachmentAsync();
                break;
            case "ATTACH_IMAGE":
                callback.success("{\"ok\":true}");
                chooseImageAsync();
                break;
            case "CLEAR_ATTACHMENTS":
                clearPendingAttachments();
                callback.success(GSON.toJson(buildAttachmentStateJson()));
                break;
            case "VOICE_START":
                System.out.println("[LAVIE_VOICE] VOICE_START received");
                callback.success("{\"ok\":true}");
                startVoiceCapture();
                break;
            case "VOICE_STOP":
                System.out.println("[LAVIE_VOICE] VOICE_STOP received");
                callback.success("{\"ok\":true}");
                stopVoiceCapture();
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

    private boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
        if (obj == null || key == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return obj.get(key).getAsBoolean();
        } catch (RuntimeException ex) {
            String raw = getString(obj, key).trim();
            if (raw.isEmpty()) {
                return defaultValue;
            }
            return "true".equalsIgnoreCase(raw) || "1".equals(raw) || "yes".equalsIgnoreCase(raw);
        }
    }

    private boolean hasPendingAttachments() {
        synchronized (pendingAttachments) {
            return !pendingAttachments.isEmpty();
        }
    }

    private int pendingAttachmentCount() {
        synchronized (pendingAttachments) {
            return pendingAttachments.size();
        }
    }

    private void chooseAttachmentAsync() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn tệp gửi cho Lavie");
            chooser.setMultiSelectionEnabled(true);
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                    "Ảnh, tài liệu, mã nguồn",
                    "png", "jpg", "jpeg", "webp", "gif", "bmp",
                    "txt", "md", "java", "js", "ts", "html", "css", "json", "xml", "csv",
                    "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx"));
            try {
                Path workspace = Paths.get(agentWorkspacePath == null ? "." : agentWorkspacePath);
                if (Files.isDirectory(workspace)) {
                    chooser.setCurrentDirectory(workspace.toFile());
                }
            } catch (Exception ignored) {
                // File chooser can still open with the platform default directory.
            }
            int result = chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this));
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            for (java.io.File file : chooser.getSelectedFiles()) {
                if (file != null) {
                    attachmentExecutor.submit(() -> loadAttachment(file.toPath()));
                }
            }
        });
    }

    private void chooseImageAsync() {
        SwingUtilities.invokeLater(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn hình ảnh gửi cho Lavie");
            chooser.setMultiSelectionEnabled(true);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                    "Hình ảnh",
                    "png", "jpg", "jpeg", "webp", "gif", "bmp"));
            try {
                Path workspace = Paths.get(agentWorkspacePath == null ? "." : agentWorkspacePath);
                if (Files.isDirectory(workspace)) {
                    chooser.setCurrentDirectory(workspace.toFile());
                }
            } catch (Exception ignored) {
            }
            int result = chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this));
            if (result != JFileChooser.APPROVE_OPTION) {
                return;
            }
            for (java.io.File file : chooser.getSelectedFiles()) {
                if (file != null) {
                    attachmentExecutor.submit(() -> loadAttachment(file.toPath()));
                }
            }
        });
    }

    private void loadAttachment(Path selectedPath) {
        try {
            Path path = selectedPath.toRealPath();
            if (!Files.isRegularFile(path)) {
                SwingUtilities.invokeLater(() -> executeAgentJs("showError", "Tệp đính kèm không hợp lệ."));
                return;
            }
            long size = Files.size(path);
            if (size > MAX_ATTACHMENT_BYTES) {
                SwingUtilities.invokeLater(() -> executeAgentJs("showError",
                        "Tệp quá lớn. Lavie hiện nhận tối đa 12MB cho mỗi tệp."));
                return;
            }
            String mime = Files.probeContentType(path);
            if (mime == null || mime.isBlank()) {
                mime = guessMimeFromExtension(path);
            }
            String kind = detectAttachmentKind(path, mime);
            String preview = isTextLikeAttachment(path, mime, kind) ? readTextPreview(path) : "";
            AiAttachment attachment = new AiAttachment(
                    UUID.randomUUID().toString(),
                    path.getFileName().toString(),
                    path.toString(),
                    mime,
                    kind,
                    size,
                    preview);
            synchronized (pendingAttachments) {
                while (pendingAttachments.size() >= MAX_PENDING_ATTACHMENTS) {
                    pendingAttachments.remove(0);
                }
                pendingAttachments.add(attachment);
            }
            JsonObject json = toAttachmentJson(attachment);
            SwingUtilities.invokeLater(() -> {
                executeAgentJs("addAttachmentPreview", json);
                executeAgentJs("setAgentContextState", buildAgentContextJson());
                executeAgentJs("showError", "");
            });
        } catch (IOException ex) {
            SwingUtilities.invokeLater(() -> executeAgentJs("showError",
                    "Không đọc được tệp đính kèm: " + safeErrorMessage(ex)));
        }
    }

    private AttachmentPayload drainPendingAttachmentPayload() {
        List<AiAttachment> drained;
        synchronized (pendingAttachments) {
            if (pendingAttachments.isEmpty()) {
                return new AttachmentPayload("", "[]");
            }
            drained = new ArrayList<>(pendingAttachments);
            pendingAttachments.clear();
        }
        executeAgentJs("clearAttachments");
        executeAgentJs("setAgentContextState", buildAgentContextJson());
        return new AttachmentPayload(buildAttachmentsContext(drained), buildAttachmentsJson(drained));
    }

    private void clearPendingAttachments() {
        synchronized (pendingAttachments) {
            pendingAttachments.clear();
        }
        executeAgentJs("clearAttachments");
        executeAgentJs("setAgentContextState", buildAgentContextJson());
    }

    private JsonObject buildAttachmentStateJson() {
        JsonObject json = new JsonObject();
        JsonArray items = new JsonArray();
        synchronized (pendingAttachments) {
            json.addProperty("count", pendingAttachments.size());
            for (AiAttachment attachment : pendingAttachments) {
                items.add(toAttachmentJson(attachment));
            }
        }
        json.add("items", items);
        return json;
    }

    private JsonObject toAttachmentJson(AiAttachment attachment) {
        JsonObject json = new JsonObject();
        json.addProperty("id", attachment.id);
        json.addProperty("name", attachment.name);
        json.addProperty("path", attachment.path);
        json.addProperty("mime", attachment.mime);
        json.addProperty("kind", attachment.kind);
        json.addProperty("size", attachment.size);
        json.addProperty("hasTextPreview", !attachment.textPreview.isBlank());
        json.addProperty("textPreviewChars", attachment.textPreview.length());
        return json;
    }

    private String buildAttachmentsJson(List<AiAttachment> attachments) {
        JsonArray items = new JsonArray();
        if (attachments == null || attachments.isEmpty()) {
            return GSON.toJson(items);
        }
        for (AiAttachment attachment : attachments) {
            JsonObject node = toAttachmentJson(attachment);
            if ("image".equals(attachment.kind) && attachment.size <= MAX_INLINE_IMAGE_BYTES) {
                try {
                    byte[] bytes = Files.readAllBytes(Paths.get(attachment.path));
                    String mime = attachment.mime == null || attachment.mime.isBlank()
                            ? "image/png"
                            : attachment.mime;
                    node.addProperty("dataUrl", "data:" + mime + ";base64,"
                            + Base64.getEncoder().encodeToString(bytes));
                } catch (IOException ex) {
                    node.addProperty("inlineError", safeErrorMessage(ex));
                }
            } else if ("image".equals(attachment.kind)) {
                node.addProperty("inlineError", "Image is larger than inline multimodal limit.");
            }
            items.add(node);
        }
        return GSON.toJson(items);
    }

    private String buildAttachmentsContext(List<AiAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Tệp và ngữ cảnh đính kèm trong lượt này:\n");
        int index = 1;
        for (AiAttachment attachment : attachments) {
            sb.append(index++).append(". ")
                    .append(attachment.name)
                    .append(" | loại: ").append(attachment.kind)
                    .append(" | MIME: ").append(attachment.mime)
                    .append(" | kích thước: ").append(attachment.size).append(" bytes")
                    .append(" | đường dẫn nội bộ: ").append(attachment.path)
                    .append("\n");
            if (!attachment.textPreview.isBlank()) {
                sb.append("Nội dung trích xuất:\n")
                        .append(attachment.textPreview)
                        .append("\n");
            } else if ("image".equals(attachment.kind)) {
                sb.append("Ghi chú: đây là ảnh. Nếu provider hỗ trợ multimodal, ảnh đã được gửi kèm riêng trong payload; nếu không, hãy dựa trên metadata này.\n");
            } else {
                sb.append("Ghi chú: chưa trích xuất nội dung cho định dạng này; Lavie dùng metadata tệp làm ngữ cảnh.\n");
            }
            sb.append("\n");
            if (sb.length() > MAX_ATTACHMENT_CONTEXT_CHARS) {
                return sb.substring(0, MAX_ATTACHMENT_CONTEXT_CHARS).trim()
                        + "\n... attachment context truncated ...";
            }
        }
        return sb.toString().trim();
    }

    private String mergeAttachmentContext(String userMessage, String attachmentContext) {
        if (attachmentContext == null || attachmentContext.isBlank()) {
            return userMessage;
        }
        return userMessage + "\n\n" + attachmentContext;
    }

    private String readTextPreview(Path path) throws IOException {
        int limit = MAX_ATTACHMENT_TEXT_BYTES;
        byte[] bytes;
        try (InputStream inputStream = Files.newInputStream(path)) {
            bytes = inputStream.readNBytes(limit);
        }
        String text = new String(bytes, StandardCharsets.UTF_8)
                .replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
        if (text.length() > MAX_ATTACHMENT_PREVIEW_CHARS) {
            return text.substring(0, MAX_ATTACHMENT_PREVIEW_CHARS).trim()
                    + "\n... nội dung tệp đã được rút gọn ...";
        }
        return text;
    }

    private boolean isTextLikeAttachment(Path path, String mime, String kind) {
        String ext = extension(path);
        if ("text".equals(kind) || "code".equals(kind) || ("spreadsheet".equals(kind) && "csv".equals(ext))) {
            return true;
        }
        String lowerMime = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        if (lowerMime.startsWith("text/")
                || lowerMime.contains("json")
                || lowerMime.contains("xml")
                || lowerMime.contains("csv")
                || lowerMime.contains("javascript")) {
            return true;
        }
        return ext.matches("txt|md|markdown|java|js|ts|tsx|jsx|html|htm|css|json|xml|csv|yml|yaml|properties|ini|log|sql|py|kt|rs|go|c|cpp|h|hpp|cs|php|rb|sh|bat|ps1");
    }

    private String detectAttachmentKind(Path path, String mime) {
        String ext = extension(path);
        String lowerMime = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        if (lowerMime.startsWith("image/") || ext.matches("png|jpg|jpeg|webp|gif|bmp|svg")) {
            return "image";
        }
        if ("pdf".equals(ext) || lowerMime.contains("pdf")) {
            return "pdf";
        }
        if (ext.matches("doc|docx|ppt|pptx") || lowerMime.contains("word") || lowerMime.contains("presentation")) {
            return "document";
        }
        if (ext.matches("xls|xlsx|csv") || lowerMime.contains("spreadsheet") || lowerMime.contains("csv")) {
            return "spreadsheet";
        }
        if (ext.matches("java|js|ts|tsx|jsx|html|htm|css|json|xml|yml|yaml|py|kt|rs|go|c|cpp|cs|php|rb|sh|ps1")) {
            return "code";
        }
        if (lowerMime.startsWith("text/") || ext.matches("txt|md|markdown|log|sql|properties|ini")) {
            return "text";
        }
        return "file";
    }

    private String guessMimeFromExtension(Path path) {
        String ext = extension(path);
        return switch (ext) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "json" -> "application/json";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js", "mjs" -> "text/javascript";
            case "csv" -> "text/csv";
            case "md", "txt", "log" -> "text/plain";
            default -> "application/octet-stream";
        };
    }

    private String extension(Path path) {
        String name = path == null || path.getFileName() == null ? "" : path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot >= name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void startVoiceCapture() {
        synchronized (this) {
            if (activeVoiceSession != null) {
                setVoiceNativeState("recording", true, "Đang nghe. Bấm mic lần nữa để dừng.");
                return;
            }
            activeVoiceSession = new VoiceCaptureSession(MAX_VOICE_RECORD_SECONDS);
        }
        VoiceCaptureSession session = activeVoiceSession;
        setVoiceNativeState("recording", true, "Đang nghe. Bấm mic lần nữa để dừng.");
        voiceExecutor.submit(() -> {
            System.out.println("[LAVIE_VOICE] Native recording started");
            session.record();
            finishVoiceCapture(session);
        });
    }

    private void stopVoiceCapture() {
        VoiceCaptureSession session = activeVoiceSession;
        if (session == null) {
            setVoiceNativeState("idle", false, "");
            return;
        }
        setVoiceNativeState("transcribing", true, "Đang chuyển giọng nói thành văn bản...");
        session.stop();
    }

    private void finishVoiceCapture(VoiceCaptureSession session) {
        if (session == null || !session.markFinalizing()) {
            return;
        }
        session.stop();
        try {
            byte[] wavBytes = session.awaitWav();
            synchronized (this) {
                if (activeVoiceSession == session) {
                    activeVoiceSession = null;
                }
            }
            if (session.getPcmByteCount() < MIN_VOICE_PCM_BYTES) {
                throw new IllegalStateException("Âm thanh quá ngắn hoặc chưa thu được tín hiệu micro.");
            }
            System.out.println("[LAVIE_VOICE] Captured WAV bytes=" + wavBytes.length
                    + ", PCM bytes=" + session.getPcmByteCount());
            setVoiceNativeState("transcribing", true, "Đang gửi giọng nói tới Lavie...");
            try {
                VoiceBackendResult voiceResult = callLavieVoiceChat(wavBytes);
                if (voiceResult.hasUserText()) {
                    submitVoiceTranscript(voiceResult.userText);
                    return;
                }
                if (voiceResult.hasAssistantText()) {
                    JsonObject json = new JsonObject();
                    String userText = "Voice message";
                    json.addProperty("userText", userText);
                    json.addProperty("assistantText", voiceResult.assistantText);
                    json.addProperty("audioUrl", voiceResult.audioUrl);
                    SwingUtilities.invokeLater(() -> {
                        executeAgentJs("appendVoiceExchange", json);
                        rememberCompletedExchange(userText, voiceResult.assistantText);
                        executeAgentJs("setMemoryState", buildMemoryStateJson());
                        executeAgentJs("setAgentContextState", buildAgentContextJson());
                        setVoiceNativeState("idle", false, "");
                        executeAgentJs("showError", "");
                    });
                    return;
                }
            } catch (Exception lavieVoiceError) {
                System.out.println("[LAVIE_VOICE] Lavie voice endpoint failed: "
                        + safeErrorMessage(lavieVoiceError));
                setVoiceNativeState("transcribing", true,
                        "Lavie voice chưa phản hồi. Đang dùng Gemini STT...");
            }

            String transcript = transcribeVoiceWithGemini(wavBytes);
            submitVoiceTranscript(transcript);
        } catch (Exception ex) {
            System.out.println("[LAVIE_VOICE] Voice capture failed: " + safeErrorMessage(ex));
            synchronized (this) {
                if (activeVoiceSession == session) {
                    activeVoiceSession = null;
                }
            }
            SwingUtilities.invokeLater(() -> setVoiceNativeState("error", false,
                    "Không nhận được giọng nói. " + safeErrorMessage(ex)));
        }
    }

    private void setVoiceNativeState(String status, boolean active, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("status", status == null ? "idle" : status);
        json.addProperty("active", active);
        json.addProperty("message", message == null ? "" : message);
        executeAgentJs("setVoiceNativeState", json);
    }

    private void submitVoiceTranscript(String transcript) {
        String text = transcript == null ? "" : transcript.trim();
        if (text.isEmpty()) {
            SwingUtilities.invokeLater(() -> setVoiceNativeState("error", false,
                    "Không nhận được nội dung giọng nói."));
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("text", text);
        SwingUtilities.invokeLater(() -> {
            executeAgentJs("submitVoiceTranscript", json);
            setVoiceNativeState("idle", false, "");
            executeAgentJs("showError", "");
        });
    }

    private VoiceBackendResult callLavieVoiceChat(byte[] wavBytes) throws IOException {
        String boundary = "----TutorHubLavieVoice" + System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(joinUrl(AppConfig.AI_SERVER_URL, "/api/chat/voice")).openConnection();
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("Accept", "application/json, text/event-stream, text/plain, */*");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream outputStream = conn.getOutputStream()) {
                writeMultipartFile(outputStream, boundary, "audio", "lavie_voice.wav", "audio/wav", wavBytes);
                writeMultipartField(outputStream, boundary, "user_id", LAVIE_SERVER_USER_ID);
                writeUtf8(outputStream, "--" + boundary + "--\r\n");
            }

            int status = conn.getResponseCode();
            String response = status >= 200 && status < 300
                    ? readHttpBody(conn.getInputStream())
                    : readHttpBody(conn.getErrorStream());
            if (status < 200 || status >= 300) {
                throw new IOException("Lavie voice HTTP " + status + ": " + response);
            }
            VoiceBackendResult result = parseVoiceBackendResponse(response);
            if (result.isEmpty()) {
                throw new IOException("Lavie voice endpoint returned no transcript or answer.");
            }
            return result;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void writeMultipartFile(OutputStream outputStream, String boundary, String fieldName,
                                    String fileName, String contentType, byte[] bytes) throws IOException {
        writeUtf8(outputStream, "--" + boundary + "\r\n");
        writeUtf8(outputStream, "Content-Disposition: form-data; name=\"" + fieldName
                + "\"; filename=\"" + fileName + "\"\r\n");
        writeUtf8(outputStream, "Content-Type: " + contentType + "\r\n\r\n");
        outputStream.write(bytes == null ? new byte[0] : bytes);
        writeUtf8(outputStream, "\r\n");
    }

    private void writeMultipartField(OutputStream outputStream, String boundary, String fieldName,
                                     String value) throws IOException {
        writeUtf8(outputStream, "--" + boundary + "\r\n");
        writeUtf8(outputStream, "Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n");
        writeUtf8(outputStream, value == null ? "" : value);
        writeUtf8(outputStream, "\r\n");
    }

    private void writeUtf8(OutputStream outputStream, String value) throws IOException {
        outputStream.write((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private VoiceBackendResult parseVoiceBackendResponse(String response) {
        if (response == null || response.isBlank()) {
            return VoiceBackendResult.empty();
        }
        VoiceBackendResult merged = VoiceBackendResult.empty();
        boolean sawSse = false;
        for (String line : response.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            sawSse = true;
            String data = trimmed.substring(5).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) {
                continue;
            }
            merged = merged.merge(parseVoiceJsonFragment(data));
        }
        if (sawSse && !merged.isEmpty()) {
            return merged;
        }
        return parseVoiceJsonFragment(response.trim());
    }

    private VoiceBackendResult parseVoiceJsonFragment(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            return VoiceBackendResult.empty();
        }
        try {
            return extractVoiceBackendResult(JsonParser.parseString(fragment));
        } catch (RuntimeException ex) {
            return new VoiceBackendResult("", fragment.trim(), "");
        }
    }

    private VoiceBackendResult extractVoiceBackendResult(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return VoiceBackendResult.empty();
        }
        if (element.isJsonArray()) {
            VoiceBackendResult merged = VoiceBackendResult.empty();
            for (JsonElement child : element.getAsJsonArray()) {
                merged = merged.merge(extractVoiceBackendResult(child));
            }
            return merged;
        }
        if (element.isJsonPrimitive()) {
            return new VoiceBackendResult("", element.getAsString(), "");
        }
        if (!element.isJsonObject()) {
            return VoiceBackendResult.empty();
        }

        JsonObject object = element.getAsJsonObject();
        String userText = firstJsonString(object, "user_text", "transcript", "text", "query", "prompt");
        String assistantText = firstJsonString(object, "answer", "response", "reply",
                "assistant", "assistant_text", "content", "message", "chunk");
        String audioUrl = firstJsonString(object, "audio_url", "audioUrl", "audio");

        VoiceBackendResult result = new VoiceBackendResult(userText, assistantText, audioUrl);
        for (String nestedKey : List.of("data", "result", "output")) {
            if (object.has(nestedKey)) {
                result = result.merge(extractVoiceBackendResult(object.get(nestedKey)));
            }
        }
        return result;
    }

    private String firstJsonString(JsonObject object, String... keys) {
        if (object == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            if (!object.has(key) || object.get(key).isJsonNull()) {
                continue;
            }
            JsonElement element = object.get(key);
            if (element.isJsonPrimitive()) {
                String value = element.getAsString();
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    private String transcribeVoiceWithGemini(byte[] wavBytes) throws IOException {
        AiAgentProviderConfig config = providerConfig == null
                ? AiAgentServiceFactory.loadDefaultConfig()
                : providerConfig;
        String baseUrl = config.getOpenAiBaseUrl();
        if (!config.isOpenAiCompatible()
                || baseUrl == null
                || !baseUrl.toLowerCase(Locale.ROOT).contains("generativelanguage.googleapis.com")) {
            throw new IllegalStateException("STT native hiện dùng Gemini. Hãy chọn provider Gemini/OpenAI-compatible.");
        }
        String apiKey = config.getOpenAiApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Cần API key Gemini để nhận giọng nói native.");
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(geminiGenerateContentUrl(config)).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(90000);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("x-goog-api-key", apiKey.trim());
            conn.setDoOutput(true);

            byte[] body = GSON.toJson(buildGeminiVoicePayload(wavBytes)).getBytes(StandardCharsets.UTF_8);
            try (OutputStream outputStream = conn.getOutputStream()) {
                outputStream.write(body);
            }

            int status = conn.getResponseCode();
            String response = status >= 200 && status < 300
                    ? readHttpBody(conn.getInputStream())
                    : readHttpBody(conn.getErrorStream());
            if (status < 200 || status >= 300) {
                throw new IOException("Gemini STT HTTP " + status + ": " + response);
            }
            String text = extractGeminiText(response);
            if (text.isBlank()) {
                throw new IOException("Gemini không trả về transcript.");
            }
            return text;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String geminiGenerateContentUrl(AiAgentProviderConfig config) {
        String base = config.getOpenAiBaseUrl() == null || config.getOpenAiBaseUrl().trim().isEmpty()
                ? AiAgentProviderConfig.DEFAULT_OPENAI_BASE_URL
                : config.getOpenAiBaseUrl().trim();
        String lower = base.toLowerCase(Locale.ROOT);
        int openAiIndex = lower.indexOf("/openai");
        if (openAiIndex >= 0) {
            base = base.substring(0, openAiIndex);
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String model = config.getOpenAiModel() == null || config.getOpenAiModel().isBlank()
                ? AiAgentProviderConfig.DEFAULT_OPENAI_MODEL
                : config.getOpenAiModel().trim();
        String encodedModel = URLEncoder.encode(model, StandardCharsets.UTF_8).replace("+", "%20");
        return base + "/models/" + encodedModel + ":generateContent";
    }

    private JsonObject buildGeminiVoicePayload(byte[] wavBytes) {
        JsonObject payload = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();

        JsonObject instruction = new JsonObject();
        instruction.addProperty("text",
                "Hãy chép lại nguyên văn nội dung tiếng Việt hoặc tiếng Anh trong audio. "
                        + "Chỉ trả về transcript, không giải thích thêm.");
        parts.add(instruction);

        JsonObject inlineData = new JsonObject();
        inlineData.addProperty("mimeType", "audio/wav");
        inlineData.addProperty("data", Base64.getEncoder().encodeToString(wavBytes));
        JsonObject audioPart = new JsonObject();
        audioPart.add("inlineData", inlineData);
        parts.add(audioPart);

        content.add("parts", parts);
        contents.add(content);
        payload.add("contents", contents);

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.0);
        generationConfig.addProperty("maxOutputTokens", 512);
        payload.add("generationConfig", generationConfig);
        return payload;
    }

    private String extractGeminiText(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }
        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            JsonArray candidates = root.getAsJsonArray("candidates");
            if (candidates == null || candidates.size() == 0) {
                return "";
            }
            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            if (content == null) {
                return "";
            }
            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null || parts.size() == 0) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (JsonElement partElement : parts) {
                if (partElement == null || !partElement.isJsonObject()) {
                    continue;
                }
                JsonObject part = partElement.getAsJsonObject();
                if (part.has("text") && !part.get("text").isJsonNull()) {
                    sb.append(part.get("text").getAsString());
                }
            }
            return sb.toString().trim();
        } catch (RuntimeException ex) {
            return "";
        }
    }

    private String readHttpBody(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream inputStream = stream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private JsonObject buildProviderConfigJson() {
        AiAgentProviderConfig config = providerConfig == null
                ? AiAgentServiceFactory.loadDefaultConfig()
                : providerConfig;
        JsonObject json = new JsonObject();
        json.addProperty("provider", config.getProvider());
        json.addProperty("displayName", config.getDisplayName());
        json.addProperty("providerName", aiService == null ? config.getDisplayName() : aiService.getProviderName());
        json.addProperty("ollamaBaseUrl", config.getOllamaBaseUrl());
        json.addProperty("ollamaModel", config.getOllamaModel());
        json.addProperty("openAiBaseUrl", config.getOpenAiBaseUrl());
        json.addProperty("openAiModel", config.getOpenAiModel());
        json.addProperty("openAiApiKeyConfigured", config.getOpenAiApiKey() != null && !config.getOpenAiApiKey().isBlank());
        json.addProperty("mode", config.isOllama()
                ? "langchain4j-ollama"
                : (config.isOpenAiCompatible() ? "openai-compatible" : "lavie-hf"));
        return json;
    }

    private String providerInitialStatusLabel(AiAgentProviderConfig config) {
        AiAgentProviderConfig effectiveConfig = config == null
                ? AiAgentProviderConfig.defaults()
                : config;
        if (effectiveConfig.isOllama()) {
            return "Cần kiểm tra - " + effectiveConfig.getDisplayName();
        }
        if (effectiveConfig.isOpenAiCompatible()) {
            boolean hasKey = effectiveConfig.getOpenAiApiKey() != null && !effectiveConfig.getOpenAiApiKey().isBlank();
            if (!hasKey && !isLocalHttpEndpoint(effectiveConfig.getOpenAiBaseUrl())) {
                return "Chưa cấu hình - " + effectiveConfig.getDisplayName();
            }
            return "Cần kiểm tra - " + effectiveConfig.getDisplayName();
        }
        return "Sẵn sàng - " + effectiveConfig.getDisplayName();
    }

    private JsonObject buildMemoryStateJson() {
        JsonObject json = new JsonObject();
        json.addProperty("messageCount", conversationMemory.size());
        json.addProperty("hasContext", conversationMemory.size() > 0);
        json.addProperty("hasCompactedSummary", conversationMemory.hasCompactedSummary());
        json.addProperty("contextChars", conversationMemory.buildContext().length());
        return json;
    }

    private JsonObject buildLongTermMemoryStateJson() {
        AiLongTermMemoryStore.MemorySnapshot snapshot = longTermMemoryStore.snapshot();
        JsonObject json = new JsonObject();
        json.addProperty("count", snapshot.getCount());
        json.addProperty("hasMemory", snapshot.getCount() > 0);
        JsonArray items = new JsonArray();
        for (AiLongTermMemoryStore.MemoryItem item : snapshot.getDisplayItems()) {
            JsonObject node = new JsonObject();
            node.addProperty("id", item.getId());
            node.addProperty("content", item.getContent());
            node.addProperty("createdAt", item.getCreatedAt());
            node.addProperty("autoGenerated", item.isAutoGenerated());
            node.addProperty("source", item.getSource());
            items.add(node);
        }
        json.add("items", items);
        return json;
    }

    private JsonObject buildAgentModeStateJson() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", agentModeEnabled);
        json.addProperty("workspacePath", agentWorkspacePath == null ? "" : agentWorkspacePath);
        json.addProperty("phase", "10.1");
        json.addProperty("mode", "approval-required");
        json.addProperty("pendingPatchCount", pendingPatchStore.snapshot().size());
        json.addProperty("pendingCommandCount", pendingCommandStore.snapshot().size());
        json.addProperty("pendingMcpToolCallCount", pendingMcpToolCallStore.snapshot().size());
        json.addProperty("maxTurns", AgentConfig.defaults().getMaxTurns());
        json.addProperty("mcpServerCount", mcpServerRegistry.getServers().size());

        JsonArray tools = new JsonArray();
        try {
            WorkspaceBoundary boundary = WorkspaceBoundary.from(agentWorkspacePath);
            ToolRegistry registry = ToolRegistry.phase101AgentDefaults(boundary, pendingPatchStore,
                    pendingCommandStore, longTermMemoryStore, mcpServerRegistry, pendingMcpToolCallStore);
            registry.getTools().forEach(tool -> tools.add(tool.name()));
            AgentsMdLoader.ProjectInstructionSnapshot projectInstructions = AgentsMdLoader.loadForWorkspace(boundary);
            json.addProperty("validWorkspace", true);
            json.addProperty("projectInstructionCount", projectInstructions.getCount());
        } catch (Exception ex) {
            json.addProperty("validWorkspace", false);
            json.addProperty("workspaceError", ex.getMessage());
            tools.add("list_files");
            tools.add("read_file");
            tools.add("search_text");
            tools.add("get_project_info");
            tools.add("propose_patch");
            tools.add("git_status");
            tools.add("propose_command");
            tools.add("remember_note");
            tools.add("mcp_list_tools");
            tools.add("propose_mcp_tool_call");
        }
        json.add("tools", tools);
        return json;
    }

    private JsonObject buildAgentContextJson() {
        JsonObject json = new JsonObject();
        json.addProperty("phase", "10.1");
        json.addProperty("provider", providerConfig == null ? "" : providerConfig.getDisplayName());
        json.addProperty("agentEnabled", agentModeEnabled);
        json.addProperty("workspacePath", agentWorkspacePath == null ? "" : agentWorkspacePath);
        json.addProperty("conversationMessageCount", conversationMemory.size());
        json.addProperty("conversationContextChars", conversationMemory.buildContext().length());
        json.addProperty("hasCompactedSummary", conversationMemory.hasCompactedSummary());
        json.addProperty("longTermMemoryCount", longTermMemoryStore.snapshot().getCount());
        json.addProperty("pendingAttachmentCount", pendingAttachmentCount());
        json.addProperty("pendingPatchCount", pendingPatchStore.snapshot().size());
        json.addProperty("pendingCommandCount", pendingCommandStore.snapshot().size());
        json.addProperty("pendingMcpToolCallCount", pendingMcpToolCallStore.snapshot().size());
        json.addProperty("mcpServerCount", mcpServerRegistry.getServers().size());

        JsonArray instructionFiles = new JsonArray();
        try {
            WorkspaceBoundary boundary = WorkspaceBoundary.from(agentWorkspacePath);
            AgentsMdLoader.ProjectInstructionSnapshot projectInstructions = AgentsMdLoader.loadForWorkspace(boundary);
            json.addProperty("validWorkspace", true);
            json.addProperty("projectInstructionCount", projectInstructions.getCount());
            json.addProperty("projectInstructionChars", projectInstructions.getContext().length());
            for (AgentsMdLoader.ProjectInstruction instruction : projectInstructions.getInstructions()) {
                JsonObject node = new JsonObject();
                node.addProperty("path", instruction.getRelativePath());
                node.addProperty("chars", instruction.getContent().length());
                node.addProperty("truncated", instruction.isTruncated());
                instructionFiles.add(node);
            }
        } catch (Exception ex) {
            json.addProperty("validWorkspace", false);
            json.addProperty("workspaceError", ex.getMessage());
            json.addProperty("projectInstructionCount", 0);
            json.addProperty("projectInstructionChars", 0);
        }
        json.add("instructionFiles", instructionFiles);
        json.add("audit", buildAuditLogEntriesArray(10));
        return json;
    }

    private JsonObject buildAuditLogJson() {
        JsonObject json = new JsonObject();
        JsonArray entries = buildAuditLogEntriesArray(20);
        json.addProperty("count", entries.size());
        json.add("entries", entries);
        return json;
    }

    private JsonArray buildAuditLogEntriesArray(int limit) {
        JsonArray entries = new JsonArray();
        for (AuditLog.Entry entry : auditLog.readRecent(limit)) {
            JsonObject node = new JsonObject();
            node.addProperty("timestamp", entry.getTimestamp());
            node.addProperty("action", entry.getAction());
            node.addProperty("targetId", entry.getTargetId());
            node.addProperty("path", entry.getPath());
            node.addProperty("status", entry.getStatus());
            node.addProperty("message", entry.getMessage());
            entries.add(node);
        }
        return entries;
    }

    private void addLongTermMemory(JsonObject payload) {
        String note = getString(payload, "note");
        longTermMemoryStore.add(note);
        executeAgentJs("setLongTermMemoryState", buildLongTermMemoryStateJson());
        executeAgentJs("setAgentContextState", buildAgentContextJson());
        executeAgentJs("setProviderCheckResult", "ok", "Đã lưu ghi chú vào bộ nhớ lâu dài.");
    }

    private void updateLongTermMemory(JsonObject payload) {
        String id = getString(payload, "id");
        String note = getString(payload, "note");
        AiLongTermMemoryStore.MemoryWriteResult result = longTermMemoryStore.update(id, note);
        executeAgentJs("setLongTermMemoryState", buildLongTermMemoryStateJson());
        executeAgentJs("setAgentContextState", buildAgentContextJson());
        executeAgentJs("setProviderCheckResult", result.isSaved() ? "ok" : "error", result.getMessage());
    }

    private void deleteLongTermMemory(JsonObject payload) {
        String id = getString(payload, "id");
        AiLongTermMemoryStore.MemoryWriteResult result = longTermMemoryStore.remove(id);
        executeAgentJs("setLongTermMemoryState", buildLongTermMemoryStateJson());
        executeAgentJs("setAgentContextState", buildAgentContextJson());
        executeAgentJs("setProviderCheckResult", result.isSaved() ? "ok" : "error", result.getMessage());
    }

    private void clearLongTermMemory() {
        longTermMemoryStore.clear();
        executeAgentJs("setLongTermMemoryState", buildLongTermMemoryStateJson());
        executeAgentJs("setAgentContextState", buildAgentContextJson());
        executeAgentJs("setProviderCheckResult", "ok", "Đã xóa bộ nhớ lâu dài.");
    }

    private void clearConversationMemory() {
        conversationMemory.clear();
        executeAgentJs("setMemoryState", buildMemoryStateJson());
        executeAgentJs("setAgentContextState", buildAgentContextJson());
        executeAgentJs("setProviderCheckResult", "ok", "Đã xóa bộ nhớ phiên hội thoại.");
    }

    private void updateAgentMode(JsonObject payload) throws Exception {
        boolean enabled = getBoolean(payload, "enabled", agentModeEnabled);
        String workspace = getString(payload, "workspacePath").trim();
        if (workspace.isEmpty()) {
            workspace = agentWorkspacePath;
        }

        if (enabled) {
            WorkspaceBoundary.from(workspace);
        }

        agentModeEnabled = enabled;
        agentWorkspacePath = normalizeWorkspacePath(workspace);
        executeAgentJs("setAgentModeState", buildAgentModeStateJson());
        executeAgentJs("setProviderCheckResult", "ok", enabled
                ? "Agent Mode đã bật. Agent có thể đề xuất patch/lệnh nhưng phải chờ bạn duyệt."
                : "Agent Mode đã tắt. Chat quay lại chế độ trò chuyện thường.");
    }

    private JsonObject applyApprovedPatch(JsonObject payload) {
        String patchId = getString(payload, "patchId").trim();
        try {
            WorkspaceBoundary boundary = WorkspaceBoundary.from(agentWorkspacePath);
            ApplyPatchTool tool = new ApplyPatchTool(boundary, pendingPatchStore, permissionPolicy, auditLog);
            ToolCallResult result = tool.execute(ToolCallRequest.of("apply_patch",
                    java.util.Map.of("patchId", patchId, "approved", "true")));
            JsonObject json = PatchPreviewView.applyResult(patchId, result);
            executeAgentJs("updatePatchProposal", json);
            executeAgentJs("setAgentModeState", buildAgentModeStateJson());
            executeAgentJs("setAgentContextState", buildAgentContextJson());
            return json;
        } catch (Exception ex) {
            ToolCallResult result = ToolCallResult.failure(ex.getMessage());
            JsonObject json = PatchPreviewView.applyResult(patchId, result);
            executeAgentJs("updatePatchProposal", json);
            executeAgentJs("setAgentContextState", buildAgentContextJson());
            return json;
        }
    }

    private JsonObject rejectPatch(JsonObject payload) {
        String patchId = getString(payload, "patchId").trim();
        PatchProposal proposal = pendingPatchStore.remove(patchId).orElse(null);
        auditLog.record("reject_patch", patchId, proposal == null ? "" : proposal.getRelativePath(),
                "rejected", "Rejected by user");
        JsonObject json = PatchPreviewView.rejected(proposal);
        executeAgentJs("updatePatchProposal", json);
        executeAgentJs("setAgentModeState", buildAgentModeStateJson());
        executeAgentJs("setAgentContextState", buildAgentContextJson());
        return json;
    }

    private void runApprovedCommandAsync(String commandId) {
        if (commandId == null || commandId.isBlank()) {
            executeAgentJs("updateCommandProposal", CommandPreviewView.result("", ToolCallResult.failure("commandId is required")));
            return;
        }
        executeAgentJs("updateCommandProposal", CommandPreviewView.running(commandId));
        agentExecutor.submit(() -> {
            JsonObject json;
            try {
                WorkspaceBoundary boundary = WorkspaceBoundary.from(agentWorkspacePath);
                RunCommandTool tool = new RunCommandTool(boundary, pendingCommandStore, permissionPolicy, auditLog);
                ToolCallResult result = tool.execute(ToolCallRequest.of("run_command",
                        java.util.Map.of("commandId", commandId, "approved", "true")));
                json = CommandPreviewView.result(commandId, result);
            } catch (Exception ex) {
                json = CommandPreviewView.result(commandId, ToolCallResult.failure(ex.getMessage()));
            }
            JsonObject finalJson = json;
            SwingUtilities.invokeLater(() -> {
                executeAgentJs("updateCommandProposal", finalJson);
                executeAgentJs("setAgentModeState", buildAgentModeStateJson());
                executeAgentJs("setAgentContextState", buildAgentContextJson());
            });
        });
    }

    private JsonObject rejectCommand(JsonObject payload) {
        String commandId = getString(payload, "commandId").trim();
        CommandSpec command = pendingCommandStore.remove(commandId).orElse(null);
        auditLog.record("reject_command", commandId, command == null ? "" : command.getWorkingDirectory(),
                "rejected", "Rejected by user");
        JsonObject json = CommandPreviewView.rejected(command);
        executeAgentJs("updateCommandProposal", json);
        executeAgentJs("setAgentModeState", buildAgentModeStateJson());
        executeAgentJs("setAgentContextState", buildAgentContextJson());
        return json;
    }

    private void runApprovedMcpToolAsync(String mcpCallId) {
        if (mcpCallId == null || mcpCallId.isBlank()) {
            executeAgentJs("updateMcpToolCallProposal",
                    McpToolCallPreviewView.result("", ToolCallResult.failure("mcpCallId is required")));
            return;
        }
        executeAgentJs("updateMcpToolCallProposal", McpToolCallPreviewView.running(mcpCallId));
        agentExecutor.submit(() -> {
            JsonObject json;
            try {
                RunMcpToolCallTool tool = new RunMcpToolCallTool(
                        mcpServerRegistry,
                        pendingMcpToolCallStore,
                        permissionPolicy,
                        auditLog);
                ToolCallResult result = tool.execute(ToolCallRequest.of("run_mcp_tool_call",
                        java.util.Map.of("mcpCallId", mcpCallId, "approved", "true")));
                json = McpToolCallPreviewView.result(mcpCallId, result);
            } catch (Exception ex) {
                json = McpToolCallPreviewView.result(mcpCallId, ToolCallResult.failure(ex.getMessage()));
            }
            JsonObject finalJson = json;
            SwingUtilities.invokeLater(() -> {
                executeAgentJs("updateMcpToolCallProposal", finalJson);
                executeAgentJs("setAgentModeState", buildAgentModeStateJson());
                executeAgentJs("setAgentContextState", buildAgentContextJson());
            });
        });
    }

    private JsonObject rejectMcpToolCall(JsonObject payload) {
        String mcpCallId = getString(payload, "mcpCallId").trim();
        McpToolCallSpec spec = pendingMcpToolCallStore.remove(mcpCallId).orElse(null);
        auditLog.record("reject_mcp_tool_call", mcpCallId,
                spec == null ? "" : spec.getServerName() + "/" + spec.getToolName(),
                "rejected", "Rejected by user");
        JsonObject json = McpToolCallPreviewView.rejected(spec);
        executeAgentJs("updateMcpToolCallProposal", json);
        executeAgentJs("setAgentModeState", buildAgentModeStateJson());
        executeAgentJs("setAgentContextState", buildAgentContextJson());
        return json;
    }

    private void updateProviderConfig(JsonObject payload) {
        String nextOpenAiKey = getString(payload, "openAiApiKey");
        if (nextOpenAiKey.isBlank() && providerConfig != null) {
            nextOpenAiKey = providerConfig.getOpenAiApiKey();
        }
        AiAgentProviderConfig nextConfig = AiAgentProviderConfig.of(
                getString(payload, "provider"),
                getString(payload, "ollamaBaseUrl"),
                getString(payload, "ollamaModel"),
                getString(payload, "openAiBaseUrl"),
                getString(payload, "openAiModel"),
                nextOpenAiKey);
        stopStream();
        providerConfig = nextConfig;
        AiAgentSettingsStore.save(nextConfig);
        aiService = AiAgentServiceFactory.create(nextConfig);
        executeAgentJs("applyProviderConfig", buildProviderConfigJson());
        executeAgentJs("setStatus", providerInitialStatusLabel(nextConfig));
        executeAgentJs("setProviderCheckResult", "ok", "Da luu cau hinh AI provider cho lan mo app sau.");
        executeAgentJs("showError", "");
    }

    private void resetProviderConfig() {
        stopStream();
        AiAgentSettingsStore.reset();
        AiAgentProviderConfig defaultConfig = AiAgentProviderConfig.defaults();
        providerConfig = defaultConfig;
        aiService = AiAgentServiceFactory.create(defaultConfig);
        executeAgentJs("applyProviderConfig", buildProviderConfigJson());
        executeAgentJs("setStatus", providerInitialStatusLabel(defaultConfig));
        executeAgentJs("setProviderCheckResult", "ok", "Da khoi phuc cau hinh AI provider mac dinh.");
        executeAgentJs("showError", "");
    }

    private void checkProviderConnectionAsync() {
        AiAgentProviderConfig config = providerConfig;
        executeAgentJs("setProviderCheckResult", "checking", "Đang kiểm tra " + config.getDisplayName() + "...");
        providerProbeExecutor.submit(() -> {
            ProviderProbeResult result = probeProvider(config);
            SwingUtilities.invokeLater(() -> {
                executeAgentJs("setProviderCheckResult", result.status, result.message);
                executeAgentJs("setStatus", result.ok
                        ? "Đã kết nối - " + config.getDisplayName()
                        : "Chưa kết nối - " + config.getDisplayName());
            });
        });
    }

    private ProviderProbeResult probeProvider(AiAgentProviderConfig config) {
        if (config != null && config.isOpenAiCompatible()) {
            return probeOpenAiCompatible(config);
        }
        if (config == null || !config.isOllama()) {
            return probeHttp(
                    LavieAiService.DEFAULT_STREAM_URL,
                    true,
                    "Lavie server phản hồi. Có thể dùng chế độ Hugging Face.",
                    "Không kết nối được Lavie server. App vẫn giữ fallback cục bộ.");
        }
        return probeHttp(
                joinUrl(config.getOllamaBaseUrl(), "/api/tags"),
                false,
                "Ollama đang chạy. Model hiện chọn: " + config.getOllamaModel() + ".",
                "Không kết nối được Ollama. Hãy kiểm tra ollama serve và model " + config.getOllamaModel() + ".");
    }

    private ProviderProbeResult probeOpenAiCompatible(AiAgentProviderConfig config) {
        String apiKey = config.getOpenAiApiKey();
        if ((apiKey == null || apiKey.isBlank()) && !isLocalHttpEndpoint(config.getOpenAiBaseUrl())) {
            return ProviderProbeResult.fail("OpenAI-compatible cần API key. Hãy đặt TUTORHUB_OPENAI_API_KEY hoặc nhập key cho phiên này.");
        }
        return probeHttp(
                joinUrl(config.getOpenAiBaseUrl(), "/models"),
                false,
                "OpenAI-compatible endpoint phản hồi. Model hiện chọn: " + config.getOpenAiModel() + ".",
                "Không kết nối được OpenAI-compatible endpoint.",
                apiKey);
    }

    private ProviderProbeResult probeHttp(String targetUrl, boolean allowClientError, String okMessage, String failMessage) {
        return probeHttp(targetUrl, allowClientError, okMessage, failMessage, "");
    }

    private ProviderProbeResult probeHttp(String targetUrl, boolean allowClientError,
                                         String okMessage, String failMessage, String bearerToken) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(targetUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4500);
            conn.setReadTimeout(4500);
            if (bearerToken != null && !bearerToken.isBlank()) {
                conn.setRequestProperty("Authorization", "Bearer " + bearerToken.trim());
            }
            int status = conn.getResponseCode();
            boolean ok = status >= 200 && (status < 300 || (allowClientError && status < 500));
            if (ok) {
                return ProviderProbeResult.ok(okMessage + " HTTP " + status + ".");
            }
            return ProviderProbeResult.fail(failMessage + " HTTP " + status + ".");
        } catch (Exception ex) {
            return ProviderProbeResult.fail(failMessage + " " + safeErrorMessage(ex));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private boolean isLocalHttpEndpoint(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("http://localhost")
                || lower.startsWith("http://127.0.0.1")
                || lower.startsWith("http://[::1]");
    }

    private String joinUrl(String baseUrl, String path) {
        String base = baseUrl == null || baseUrl.trim().isEmpty()
                ? AiAgentProviderConfig.DEFAULT_OLLAMA_BASE_URL
                : baseUrl.trim();
        String suffix = path == null ? "" : path.trim();
        if (base.endsWith("/") && suffix.startsWith("/")) {
            return base + suffix.substring(1);
        }
        if (!base.endsWith("/") && !suffix.startsWith("/")) {
            return base + "/" + suffix;
        }
        return base + suffix;
    }

    private String defaultAgentWorkspacePath() {
        return normalizeWorkspacePath(System.getProperty("user.dir", "."));
    }

    private String normalizeWorkspacePath(String path) {
        String safe = path == null || path.trim().isEmpty() ? "." : path.trim();
        try {
            return Paths.get(safe).toRealPath().toString();
        } catch (Exception ex) {
            return Paths.get(safe).toAbsolutePath().normalize().toString();
        }
    }

    private String safeErrorMessage(Exception error) {
        if (error == null || error.getMessage() == null || error.getMessage().trim().isEmpty()) {
            return "";
        }
        return "(" + error.getMessage().trim() + ")";
    }

    private void rememberCompletedExchange(String userMessage, String assistantMessage) {
        conversationMemory.rememberUser(userMessage);
        conversationMemory.rememberAssistant(assistantMessage);
    }

    private void startReadOnlyAgent(String userMessage, String attachmentContext) {
        SwingUtilities.invokeLater(() -> {
            stopStream();
            AiAgentService service = aiService;
            AiAgentProviderConfig config = providerConfig;
            String workspace = agentWorkspacePath;

            JsonObject meta = new JsonObject();
            meta.addProperty("startedAt", new Date().getTime());
            meta.addProperty("mode", "agent-readonly");
            meta.addProperty("provider", service == null ? "AI Agent" : service.getProviderName());
            meta.addProperty("workspacePath", workspace);
            executeAgentJs("startAssistantMessage", meta);
            executeAgentJs("setStatus", "Agent đang đọc workspace");
            executeAgentJs("showError", "");

            activeAgentFuture = agentExecutor.submit(() -> runReadOnlyAgent(userMessage, attachmentContext, service, config, workspace));
        });
    }

    private void runReadOnlyAgent(String userMessage, String attachmentContext, AiAgentService service,
                                  AiAgentProviderConfig config, String workspace) {
        try {
            WorkspaceBoundary boundary = WorkspaceBoundary.from(workspace);
            ToolRegistry registry = ToolRegistry.phase101AgentDefaults(boundary, pendingPatchStore,
                    pendingCommandStore, longTermMemoryStore, mcpServerRegistry, pendingMcpToolCallStore);
            AgentsMdLoader.ProjectInstructionSnapshot projectInstructions = AgentsMdLoader.loadForWorkspace(boundary);
            AiLongTermMemoryStore.MemorySnapshot longTermSnapshot = longTermMemoryStore.snapshot();
            AgentContext context = AgentContext.builder(registry)
                    .userId(userId)
                    .conversationId(conversationId + "-readonly-agent")
                    .projectInstructions(projectInstructions.getContext())
                    .conversationContext(conversationMemory.buildContext())
                    .longTermMemoryContext(longTermSnapshot.getContext())
                    .build();
            AgentLoop loop = new AgentLoop(service, AgentConfig.defaults());
            String effectiveUserMessage = mergeAttachmentContext(userMessage, attachmentContext);
            AgentTurn turn = loop.run(effectiveUserMessage, context, invocation -> SwingUtilities.invokeLater(() ->
                    handleAgentToolInvocation(invocation)));
            String assistantMessage = buildAgentTurnMessage(turn, workspace);

            SwingUtilities.invokeLater(() -> {
                activeAgentFuture = null;
                executeAgentJs("appendToolLogs", ToolCallLogView.toJsonArray(turn.getToolInvocations()));
                executeAgentJs("appendAssistantDelta", assistantMessage);
                executeAgentJs("finishAssistantMessage");
                if (turn.isCompleted()) {
                    rememberCompletedExchange(userMessage, assistantMessage);
                    executeAgentJs("setMemoryState", buildMemoryStateJson());
                    executeAgentJs("setLongTermMemoryState", buildLongTermMemoryStateJson());
                    executeAgentJs("setAgentContextState", buildAgentContextJson());
                }
                executeAgentJs("setStatus", "Sẵn sàng - Agent Mode");
                executeAgentJs("setAgentModeState", buildAgentModeStateJson());
            });
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                activeAgentFuture = null;
                String message = "Agent Mode chưa thể chạy trên workspace này.\n\n"
                        + "Lỗi kỹ thuật: " + safeErrorMessage(ex);
                executeAgentJs("appendAssistantDelta", message);
                executeAgentJs("finishAssistantMessage");
                executeAgentJs("setStatus", providerInitialStatusLabel(config));
                executeAgentJs("showError", "Agent Mode lỗi: " + ex.getMessage());
            });
        }
    }

    private void handleAgentToolInvocation(com.mycompany.tutorhub_enterprise.client.ai.agent.AgentToolInvocation invocation) {
        executeAgentJs("appendToolLog", ToolCallLogView.toJson(invocation));
        String patchId = invocation.getResult() == null
                ? ""
                : invocation.getResult().getMetadata().getOrDefault("patchId", "");
        if (!patchId.isBlank()) {
            pendingPatchStore.find(patchId)
                    .map(PatchPreviewView::proposal)
                    .ifPresent(json -> executeAgentJs("appendPatchProposal", json));
        }
        String commandId = invocation.getResult() == null
                ? ""
                : invocation.getResult().getMetadata().getOrDefault("commandId", "");
        if (!commandId.isBlank()) {
            pendingCommandStore.find(commandId)
                    .map(CommandPreviewView::proposal)
                    .ifPresent(json -> executeAgentJs("appendCommandProposal", json));
        }
        String mcpCallId = invocation.getResult() == null
                ? ""
                : invocation.getResult().getMetadata().getOrDefault("mcpCallId", "");
        if (!mcpCallId.isBlank()) {
            pendingMcpToolCallStore.find(mcpCallId)
                    .map(McpToolCallPreviewView::proposal)
                    .ifPresent(json -> executeAgentJs("appendMcpToolCallProposal", json));
        }
        if (invocation.getResult() != null
                && invocation.getResult().getMetadata().containsKey("memoryCount")) {
            executeAgentJs("setLongTermMemoryState", buildLongTermMemoryStateJson());
        }
    }

    private String buildAgentTurnMessage(AgentTurn turn, String workspace) {
        if (turn == null) {
            return "Agent Mode không trả về kết quả.";
        }
        if (turn.isCompleted()) {
            String answer = turn.getFinalAnswer() == null ? "" : turn.getFinalAnswer().trim();
            if (!answer.isEmpty()) {
                return answer;
            }
            return "Agent đã hoàn tất đọc workspace `" + workspace + "`, nhưng model không trả về nội dung cuối.";
        }
        if (turn.getStatus() == AgentTurn.Status.MAX_TURNS_REACHED) {
            return "Agent đã dừng vì đạt giới hạn lượt đọc/tìm kiếm an toàn. "
                    + "Bạn có thể hỏi cụ thể hơn hoặc thu hẹp phạm vi file cần kiểm tra.";
        }
        String error = turn.getError() == null || turn.getError().trim().isEmpty()
                ? "Không có mô tả chi tiết."
                : turn.getError().trim();
        return "Agent Mode chưa hoàn tất yêu cầu.\n\nLý do: " + error;
    }

    private void startAiStream(String userMessage, String attachmentContext, String attachmentsJson) {
        SwingUtilities.invokeLater(() -> {
            stopStream();
            AiAgentService service = aiService;
            AiAgentProviderConfig config = providerConfig;

            JsonObject meta = new JsonObject();
            meta.addProperty("startedAt", new Date().getTime());
            meta.addProperty("mode", config == null ? "lavie-hf" : config.getProvider());
            meta.addProperty("provider", service == null ? "AI Agent" : service.getProviderName());
            executeAgentJs("startAssistantMessage", meta);
            executeAgentJs("setStatus", "Đang kết nối " + (config == null ? "AI Agent" : config.getDisplayName()));

            AtomicBoolean hasDelta = new AtomicBoolean(false);
            String context = conversationMemory.buildContext();
            AiLongTermMemoryStore.MemorySnapshot longTermSnapshot = longTermMemoryStore.snapshot();
            StringBuilder assistantBuffer = new StringBuilder();
            String remoteServerContext = "";
            if (config != null) {
                remoteServerContext = LavieRemoteContextProvider.shared().contextFor(LAVIE_SERVER_USER_ID);
            }
            AiAgentRequest request = AiAgentRequest.builder()
                    .message(userMessage)
                    .userId(requestUserIdForProvider(config))
                    .conversationId(conversationId)
                    .metadata("provider", service == null ? "" : service.getProviderName())
                    .metadata("providerKey", config == null ? "" : config.getProvider())
                    .metadata(AiPromptComposer.METADATA_REMOTE_SERVER_CONTEXT, remoteServerContext)
                    .metadata(AiPromptComposer.METADATA_CONTEXT, context)
                    .metadata(AiPromptComposer.METADATA_MEMORY_SIZE, String.valueOf(conversationMemory.size()))
                    .metadata(AiPromptComposer.METADATA_LONG_TERM_MEMORY, longTermSnapshot.getContext())
                    .metadata(AiPromptComposer.METADATA_LONG_TERM_MEMORY_SIZE, String.valueOf(longTermSnapshot.getCount()))
                    .metadata(AiPromptComposer.METADATA_ATTACHMENTS_CONTEXT, attachmentContext)
                    .metadata(AiPromptComposer.METADATA_ATTACHMENTS_JSON, attachmentsJson)
                    .build();

            activeStreamHandle = service.streamChat(request, new AiAgentStreamCallback() {
                @Override
                public void onDelta(String delta) {
                    if (delta == null || delta.isEmpty()) {
                        return;
                    }
                    hasDelta.set(true);
                    assistantBuffer.append(delta);
                    SwingUtilities.invokeLater(() -> {
                        executeAgentJs("setStatus", "Đang trả lời");
                        executeAgentJs("appendAssistantDelta", delta);
                    });
                }

                @Override
                public void onComplete() {
                    SwingUtilities.invokeLater(() -> {
                        activeStreamHandle = null;
                        rememberCompletedExchange(userMessage, assistantBuffer.toString());
                        executeAgentJs("finishAssistantMessage");
                        executeAgentJs("setMemoryState", buildMemoryStateJson());
                        executeAgentJs("setAgentContextState", buildAgentContextJson());
                        executeAgentJs("setStatus", "Sẵn sàng - " + (config == null ? "AI Agent" : config.getDisplayName()));
                    });
                }

                @Override
                public void onAudio(String audioUrl) {
                    SwingUtilities.invokeLater(() -> {
                        JsonObject json = new JsonObject();
                        json.addProperty("audioUrl", audioUrl);
                        executeAgentJs("playVoiceAudio", json);
                    });
                }

                @Override
                public void onError(Exception error) {
                    SwingUtilities.invokeLater(() -> {
                        activeStreamHandle = null;
                        String message = hasDelta.get()
                                ? "\n\n[Kết nối AI bị ngắt. Bạn có thể gửi lại tin nhắn.]"
                                : buildProviderFallbackResponse(userMessage, config, error);
                        if (hasDelta.get()) {
                            rememberCompletedExchange(userMessage, assistantBuffer.toString());
                            executeAgentJs("setMemoryState", buildMemoryStateJson());
                            executeAgentJs("setAgentContextState", buildAgentContextJson());
                        }
                        executeAgentJs("appendAssistantDelta", message);
                        executeAgentJs("finishAssistantMessage");
                        executeAgentJs("setStatus", providerInitialStatusLabel(config));
                        executeAgentJs("showError", "Không kết nối được "
                                + (config == null ? "AI Agent" : config.getDisplayName())
                                + ". Đã dùng phản hồi fallback cục bộ.");
                    });
                }
            });
        });
    }

    private String buildProviderFallbackResponse(String userMessage, AiAgentProviderConfig config, Exception error) {
        String clean = userMessage == null ? "" : userMessage.replaceAll("\\s+", " ").trim();
        String detail = safeErrorMessage(error);
        if (config != null && config.isOllama()) {
            return "Mình chưa kết nối được LangChain4j Ollama cho tin nhắn: \"" + clean + "\".\n\n"
                    + "Kiểm tra nhanh:\n"
                    + "- Ollama đang chạy tại `" + config.getOllamaBaseUrl() + "`.\n"
                    + "- Model `" + config.getOllamaModel() + "` đã được pull về máy.\n"
                    + "- Có thể chạy `ollama serve` và `ollama pull " + config.getOllamaModel() + "` trước khi thử lại.\n\n"
                    + "Lỗi kỹ thuật: " + (detail.isEmpty() ? "không có mô tả chi tiết." : detail);
        }
        if (config != null && config.isOpenAiCompatible()) {
            String normalizedDetail = detail.toLowerCase();
            String overloadedHint = detail.contains("HTTP 503")
                    || normalizedDetail.contains("high demand")
                    || normalizedDetail.contains("unavailable")
                    ? "\n\nGợi ý: Gemini đang quá tải tạm thời. Hãy thử gửi lại sau vài phút hoặc đổi model sang `gemini-2.5-flash-lite` trong Cấu hình."
                    : "";
            String providerName = config.getOpenAiBaseUrl().toLowerCase().contains("generativelanguage.googleapis.com")
                    ? "Gemini"
                    : "OpenAI-compatible";
            return "Mình chưa kết nối được " + providerName + " cho tin nhắn: \"" + clean + "\".\n\n"
                    + "Cấu hình hiện tại:\n"
                    + "- Endpoint: `" + config.getOpenAiBaseUrl() + "`.\n"
                    + "- Model: `" + config.getOpenAiModel() + "`.\n"
                    + "- API key: " + ((config.getOpenAiApiKey() == null || config.getOpenAiApiKey().isBlank()) ? "chưa có" : "đã cấu hình") + "."
                    + overloadedHint + "\n\n"
                    + "Lỗi kỹ thuật: " + (detail.isEmpty() ? "không có mô tả chi tiết." : detail);
        }
        return "Mình chưa kết nối được Lavie server cho tin nhắn: \"" + clean + "\".\n\n"
                + "UI Agent vẫn hoạt động: WebView đã nhận tin nhắn, bridge JSON đã chạy, và fallback cục bộ đã được kích hoạt. "
                + "Khi Hugging Face Space phản hồi ổn định, nội dung sẽ stream trực tiếp từ Lavie.\n\n"
                + "Lỗi kỹ thuật: " + (detail.isEmpty() ? "không có mô tả chi tiết." : detail);
    }

    private String requestUserIdForProvider(AiAgentProviderConfig config) {
        if (config == null || (!config.isOpenAiCompatible() && !config.isOllama())) {
            return LAVIE_SERVER_USER_ID;
        }
        return userId == null || userId.trim().isEmpty() ? LAVIE_SERVER_USER_ID : userId;
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
            String remoteServerContext = ""; // Lavie already has its own context

            AiAgentRequest request = AiAgentRequest.builder()
                    .message(userMessage)
                    .userId(LAVIE_SERVER_USER_ID)
                    .conversationId(conversationId)
                    .metadata("provider", aiService.getProviderName())
                    .metadata(AiPromptComposer.METADATA_REMOTE_SERVER_CONTEXT, remoteServerContext)
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
                public void onAudio(String audioUrl) {
                    SwingUtilities.invokeLater(() -> {
                        JsonObject json = new JsonObject();
                        json.addProperty("audioUrl", audioUrl);
                        executeAgentJs("playVoiceAudio", json);
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
        if (activeAgentFuture != null && !activeAgentFuture.isDone()) {
            activeAgentFuture.cancel(true);
            activeAgentFuture = null;
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

    private static final class AttachmentPayload {
        private final String context;
        private final String attachmentsJson;

        private AttachmentPayload(String context, String attachmentsJson) {
            this.context = context == null ? "" : context;
            this.attachmentsJson = attachmentsJson == null || attachmentsJson.isBlank() ? "[]" : attachmentsJson;
        }
    }

    private static final class VoiceBackendResult {
        private final String userText;
        private final String assistantText;
        private final String audioUrl;

        private VoiceBackendResult(String userText, String assistantText, String audioUrl) {
            this.userText = clean(userText);
            this.assistantText = clean(assistantText);
            this.audioUrl = clean(audioUrl);
        }

        private static VoiceBackendResult empty() {
            return new VoiceBackendResult("", "", "");
        }

        private boolean isEmpty() {
            return !hasUserText() && !hasAssistantText() && audioUrl.isEmpty();
        }

        private boolean hasUserText() {
            return !userText.isEmpty();
        }

        private boolean hasAssistantText() {
            return !assistantText.isEmpty();
        }

        private VoiceBackendResult merge(VoiceBackendResult other) {
            if (other == null || other.isEmpty()) {
                return this;
            }
            String mergedUser = hasUserText() ? userText : other.userText;
            String mergedAssistant = mergeText(assistantText, other.assistantText);
            String mergedAudio = audioUrl.isEmpty() ? other.audioUrl : audioUrl;
            return new VoiceBackendResult(mergedUser, mergedAssistant, mergedAudio);
        }

        private static String mergeText(String left, String right) {
            String a = clean(left);
            String b = clean(right);
            if (a.isEmpty()) {
                return b;
            }
            if (b.isEmpty()) {
                return a;
            }
            return a + b;
        }

        private static String clean(String value) {
            return value == null ? "" : value.trim();
        }
    }

    private static final class VoiceCaptureSession {
        private final AudioFormat format = new AudioFormat(16_000f, 16, 1, true, false);
        private final int maxPcmBytes;
        private final ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        private final CountDownLatch finished = new CountDownLatch(1);
        private final AtomicBoolean finalizing = new AtomicBoolean(false);
        private volatile boolean running = true;
        private volatile Exception error;
        private volatile TargetDataLine line;

        private VoiceCaptureSession(int maxSeconds) {
            this.maxPcmBytes = Math.max(1, (int) (format.getFrameRate() * format.getFrameSize() * maxSeconds));
        }

        private void record() {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                byte[] buffer = new byte[4096];
                while (running && pcm.size() < maxPcmBytes) {
                    int read = line.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        pcm.write(buffer, 0, read);
                    }
                }
            } catch (Exception ex) {
                error = ex;
            } finally {
                TargetDataLine currentLine = line;
                if (currentLine != null) {
                    try {
                        currentLine.stop();
                    } catch (Exception ignored) {
                    }
                    try {
                        currentLine.close();
                    } catch (Exception ignored) {
                    }
                }
                finished.countDown();
            }
        }

        private void stop() {
            running = false;
            TargetDataLine currentLine = line;
            if (currentLine != null) {
                try {
                    currentLine.stop();
                } catch (Exception ignored) {
                }
                try {
                    currentLine.close();
                } catch (Exception ignored) {
                }
            }
        }

        private boolean markFinalizing() {
            return finalizing.compareAndSet(false, true);
        }

        private int getPcmByteCount() {
            return pcm.size();
        }

        private byte[] awaitWav() throws Exception {
            finished.await(8, TimeUnit.SECONDS);
            if (error != null) {
                throw error;
            }
            return toWav(pcm.toByteArray(), format);
        }

        private static byte[] toWav(byte[] pcmBytes, AudioFormat format) {
            byte[] audio = pcmBytes == null ? new byte[0] : pcmBytes;
            int byteRate = (int) (format.getSampleRate() * format.getChannels() * format.getSampleSizeInBits() / 8);
            int blockAlign = format.getChannels() * format.getSampleSizeInBits() / 8;
            ByteArrayOutputStream out = new ByteArrayOutputStream(44 + audio.length);
            writeAscii(out, "RIFF");
            writeLeInt(out, 36 + audio.length);
            writeAscii(out, "WAVE");
            writeAscii(out, "fmt ");
            writeLeInt(out, 16);
            writeLeShort(out, 1);
            writeLeShort(out, format.getChannels());
            writeLeInt(out, (int) format.getSampleRate());
            writeLeInt(out, byteRate);
            writeLeShort(out, blockAlign);
            writeLeShort(out, format.getSampleSizeInBits());
            writeAscii(out, "data");
            writeLeInt(out, audio.length);
            out.writeBytes(audio);
            return out.toByteArray();
        }

        private static void writeAscii(ByteArrayOutputStream out, String value) {
            out.writeBytes(value.getBytes(StandardCharsets.US_ASCII));
        }

        private static void writeLeInt(ByteArrayOutputStream out, int value) {
            out.write(value & 0xff);
            out.write((value >> 8) & 0xff);
            out.write((value >> 16) & 0xff);
            out.write((value >> 24) & 0xff);
        }

        private static void writeLeShort(ByteArrayOutputStream out, int value) {
            out.write(value & 0xff);
            out.write((value >> 8) & 0xff);
        }
    }

    private static final class AiAttachment {
        private final String id;
        private final String name;
        private final String path;
        private final String mime;
        private final String kind;
        private final long size;
        private final String textPreview;

        private AiAttachment(String id, String name, String path, String mime,
                             String kind, long size, String textPreview) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.path = path == null ? "" : path;
            this.mime = mime == null || mime.isBlank() ? "application/octet-stream" : mime;
            this.kind = kind == null || kind.isBlank() ? "file" : kind;
            this.size = Math.max(0L, size);
            this.textPreview = textPreview == null ? "" : textPreview;
        }
    }

    private static final class ProviderProbeResult {
        private final boolean ok;
        private final String status;
        private final String message;

        private ProviderProbeResult(boolean ok, String status, String message) {
            this.ok = ok;
            this.status = status;
            this.message = message;
        }

        private static ProviderProbeResult ok(String message) {
            return new ProviderProbeResult(true, "ok", message);
        }

        private static ProviderProbeResult fail(String message) {
            return new ProviderProbeResult(false, "error", message);
        }
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

