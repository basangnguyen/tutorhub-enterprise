package com.mycompany.tutorhub_enterprise.client.ai.agent;

import com.mycompany.tutorhub_enterprise.client.ai.AiAgentRequest;
import com.mycompany.tutorhub_enterprise.client.ai.AiAgentService;
import com.mycompany.tutorhub_enterprise.client.ai.AiAgentStreamCallback;
import com.mycompany.tutorhub_enterprise.client.ai.AiAgentStreamHandle;
import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentLoopTest {

    @TempDir
    Path workspace;

    @Test
    void executesReadOnlyToolThenReturnsFinalAnswer() throws Exception {
        Path sourceDir = Files.createDirectories(workspace.resolve("src"));
        Files.writeString(sourceDir.resolve("App.java"), "class App {}\n");
        ToolRegistry registry = ToolRegistry.readOnlyDefaults(new WorkspaceBoundary(workspace));
        AgentContext context = AgentContext.builder(registry).build();
        ScriptedAiService service = new ScriptedAiService(List.of(
                "{\"type\":\"tool_call\",\"tool\":\"read_file\",\"arguments\":{\"path\":\"src/App.java\",\"maxLines\":10}}",
                "{\"type\":\"final\",\"answer\":\"Da doc App.java va khong sua file.\"}"
        ));
        AgentLoop loop = new AgentLoop(service, AgentConfig.builder()
                .maxTurns(3)
                .modelTimeout(Duration.ofSeconds(5))
                .build());

        AgentTurn turn = loop.run("Doc file App.java", context);

        assertTrue(turn.isCompleted());
        assertEquals("Da doc App.java va khong sua file.", turn.getFinalAnswer());
        assertEquals(1, turn.getToolInvocations().size());
        assertEquals("read_file", turn.getToolInvocations().get(0).getRequest().getToolName());
        assertTrue(turn.getToolInvocations().get(0).getResult().getOutput().contains("class App {}"));
        assertEquals(2, service.prompts.size());
        assertTrue(service.prompts.get(1).contains("Previous tool observations"));
        assertTrue(service.prompts.get(1).contains("class App {}"));
    }

    @Test
    void stopsAfterMaxToolTurns() throws Exception {
        Files.writeString(workspace.resolve("README.md"), "TutorHub\n");
        ToolRegistry registry = ToolRegistry.readOnlyDefaults(new WorkspaceBoundary(workspace));
        AgentContext context = AgentContext.builder(registry).build();
        ScriptedAiService service = new ScriptedAiService(List.of(
                "{\"type\":\"tool_call\",\"tool\":\"read_file\",\"arguments\":{\"path\":\"README.md\"}}",
                "{\"type\":\"tool_call\",\"tool\":\"read_file\",\"arguments\":{\"path\":\"README.md\"}}"
        ));
        AgentLoop loop = new AgentLoop(service, AgentConfig.builder()
                .maxTurns(2)
                .modelTimeout(Duration.ofSeconds(5))
                .build());

        AgentTurn turn = loop.run("Doc README", context);

        assertEquals(AgentTurn.Status.MAX_TURNS_REACHED, turn.getStatus());
        assertEquals(2, turn.getToolInvocations().size());
    }

    @Test
    void notifiesListenerWhenToolInvocationCompletes() throws Exception {
        Files.writeString(workspace.resolve("README.md"), "TutorHub\n");
        ToolRegistry registry = ToolRegistry.readOnlyDefaults(new WorkspaceBoundary(workspace));
        AgentContext context = AgentContext.builder(registry).build();
        ScriptedAiService service = new ScriptedAiService(List.of(
                "{\"type\":\"tool_call\",\"tool\":\"read_file\",\"arguments\":{\"path\":\"README.md\"}}",
                "{\"type\":\"final\",\"answer\":\"Da doc README.\"}"
        ));
        AgentLoop loop = new AgentLoop(service, AgentConfig.builder()
                .maxTurns(3)
                .modelTimeout(Duration.ofSeconds(5))
                .build());
        List<String> toolNames = new ArrayList<>();

        AgentTurn turn = loop.run("Doc README", context,
                invocation -> toolNames.add(invocation.getRequest().getToolName()));

        assertTrue(turn.isCompleted());
        assertEquals(List.of("read_file"), toolNames);
    }

    private static final class ScriptedAiService implements AiAgentService {
        private final Queue<String> responses;
        private final List<String> prompts = new ArrayList<>();

        private ScriptedAiService(List<String> responses) {
            this.responses = new ArrayDeque<>(responses);
        }

        @Override
        public AiAgentStreamHandle streamChat(AiAgentRequest request, AiAgentStreamCallback callback) {
            prompts.add(request.getMessage());
            String response = responses.isEmpty()
                    ? "{\"type\":\"final\",\"answer\":\"Het kich ban mock.\"}"
                    : responses.remove();
            callback.onDelta(response);
            callback.onComplete();
            return () -> { };
        }
    }
}
