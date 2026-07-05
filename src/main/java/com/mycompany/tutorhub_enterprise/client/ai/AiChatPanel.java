package com.mycompany.tutorhub_enterprise.client.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import javax.swing.*;
import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AiChatPanel extends JPanel {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String BRIDGE_CHANNEL = "tutorhub.ai";

    private CefBrowser browser;
    private Timer activeMockTimer;
    private List<String> activeStreamChunks = new ArrayList<>();
    private int activeChunkIndex = 0;
    private AiAgentStreamHandle activeStreamHandle;
    private volatile AiAgentService aiService;
    private volatile AiAgentProviderConfig providerConfig;
    private final ExecutorService providerProbeExecutor;
    private final ExecutorService agentExecutor;
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
        this.agentWorkspacePath = defaultAgentWorkspacePath();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        initBrowser();
    }

    public AiChatPanel(AiAgentProviderConfig providerConfig, String userId, String conversationId) {
        this.providerConfig = providerConfig == null ? AiAgentServiceFactory.loadDefaultConfig() : providerConfig;
        this.aiService = AiAgentServiceFactory.create(this.providerConfig);
        this.userId = userId == null || userId.trim().isEmpty() ? "tutorhub_desktop" : userId.trim();
        this.conversationId = conversationId == null || conversationId.trim().isEmpty() ? "lavie" : conversationId.trim();
        this.longTermMemoryStore = new AiLongTermMemoryStore(this.userId, this.conversationId);
        this.providerProbeExecutor = createProviderProbeExecutor();
        this.agentExecutor = createAgentExecutor();
        this.agentWorkspacePath = defaultAgentWorkspacePath();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        initBrowser();
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

    @Override
    public void removeNotify() {
        stopStream();
        providerProbeExecutor.shutdownNow();
        agentExecutor.shutdownNow();
        super.removeNotify();
    }

    public void focusComposer() {
        executeAgentJs("focusComposer");
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
                executeAgentJs("applyProviderConfig", buildProviderConfigJson());
                executeAgentJs("setMemoryState", buildMemoryStateJson());
                executeAgentJs("setLongTermMemoryState", buildLongTermMemoryStateJson());
                executeAgentJs("setAgentModeState", buildAgentModeStateJson());
                executeAgentJs("setAgentContextState", buildAgentContextJson());
                setLavieExpandedState(lavieExpanded);
                executeAgentJs("setStatus", "Sẵn sàng - " + providerConfig.getDisplayName());
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
                if (text.isEmpty()) {
                    callback.failure(-3, "Tin nhắn rỗng");
                    return;
                }
                callback.success("{\"ok\":true}");
                if (getBoolean(payload, "agentMode", agentModeEnabled)) {
                    startReadOnlyAgent(text);
                } else {
                    startAiStream(text);
                }
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
        executeAgentJs("setStatus", "Sẵn sàng - " + nextConfig.getDisplayName());
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
        executeAgentJs("setStatus", "San sang - " + defaultConfig.getDisplayName());
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

    private void startReadOnlyAgent(String userMessage) {
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

            activeAgentFuture = agentExecutor.submit(() -> runReadOnlyAgent(userMessage, service, config, workspace));
        });
    }

    private void runReadOnlyAgent(String userMessage, AiAgentService service,
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
            AgentTurn turn = loop.run(userMessage, context, invocation -> SwingUtilities.invokeLater(() ->
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
                executeAgentJs("setStatus", "Sẵn sàng - " + (config == null ? "AI Agent" : config.getDisplayName()));
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

    private void startAiStream(String userMessage) {
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
            AiAgentRequest request = AiAgentRequest.builder()
                    .message(userMessage)
                    .userId(userId)
                    .conversationId(conversationId)
                    .metadata("provider", service == null ? "" : service.getProviderName())
                    .metadata("providerKey", config == null ? "" : config.getProvider())
                    .metadata(AiPromptComposer.METADATA_CONTEXT, context)
                    .metadata(AiPromptComposer.METADATA_MEMORY_SIZE, String.valueOf(conversationMemory.size()))
                    .metadata(AiPromptComposer.METADATA_LONG_TERM_MEMORY, longTermSnapshot.getContext())
                    .metadata(AiPromptComposer.METADATA_LONG_TERM_MEMORY_SIZE, String.valueOf(longTermSnapshot.getCount()))
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
                        executeAgentJs("setStatus", "Sẵn sàng - " + (config == null ? "AI Agent" : config.getDisplayName()));
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
        return "Mình chưa kết nối được Lavie server cho tin nhắn: \"" + clean + "\".\n\n"
                + "UI Agent vẫn hoạt động: WebView đã nhận tin nhắn, bridge JSON đã chạy, và fallback cục bộ đã được kích hoạt. "
                + "Khi Hugging Face Space phản hồi ổn định, nội dung sẽ stream trực tiếp từ Lavie.\n\n"
                + "Lỗi kỹ thuật: " + (detail.isEmpty() ? "không có mô tả chi tiết." : detail);
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
