package com.mycompany.tutorhub_enterprise.client.ai.agent;

import com.mycompany.tutorhub_enterprise.client.ai.AiAgentRequest;
import com.mycompany.tutorhub_enterprise.client.ai.AiAgentService;
import com.mycompany.tutorhub_enterprise.client.ai.AiAgentStreamCallback;
import com.mycompany.tutorhub_enterprise.client.ai.AiAgentStreamHandle;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public final class AgentLoop {

    private final AiAgentService service;
    private final AgentConfig config;

    public AgentLoop(AiAgentService service) {
        this(service, AgentConfig.defaults());
    }

    public AgentLoop(AiAgentService service, AgentConfig config) {
        if (service == null) {
            throw new IllegalArgumentException("AI service is required");
        }
        this.service = service;
        this.config = config == null ? AgentConfig.defaults() : config;
    }

    public AgentTurn run(String userMessage, AgentContext context) {
        return run(userMessage, context, AgentLoopListener.NOOP);
    }

    public AgentTurn run(String userMessage, AgentContext context, AgentLoopListener listener) {
        if (context == null) {
            return AgentTurn.failed("Agent context is required");
        }
        String safeMessage = userMessage == null ? "" : userMessage.trim();
        if (safeMessage.isEmpty()) {
            return AgentTurn.failed("User message is required");
        }

        try {
            AgentLoopListener safeListener = listener == null ? AgentLoopListener.NOOP : listener;
            for (int turnIndex = 1; turnIndex <= config.getMaxTurns(); turnIndex++) {
                if (Thread.currentThread().isInterrupted()) {
                    return AgentTurn.failed("Agent run was interrupted.", context.getToolInvocations());
                }
                String prompt = AgentPromptComposer.compose(safeMessage, context, config);
                String rawModelResponse = requestModel(prompt, context);
                AgentModelResponse response = AgentModelResponseParser.parse(rawModelResponse);

                if (response.isToolCall()) {
                    ToolCallRequest toolCall = response.getToolCall();
                    ToolCallResult result = context.getToolRegistry().execute(toolCall);
                    AgentToolInvocation invocation = new AgentToolInvocation(turnIndex, toolCall, result);
                    context.addToolInvocation(invocation);
                    safeListener.onToolInvocation(invocation);
                    if (result.isSuccess()) {
                        continue;
                    }
                    if (isRecoverableToolFailure(result)) {
                        continue;
                    }
                    return AgentTurn.failed("Tool failed: " + result.getError(), context.getToolInvocations());
                }

                return AgentTurn.completed(response.getFinalAnswer(), context.getToolInvocations());
            }
            return AgentTurn.maxTurnsReached(context.getToolInvocations());
        } catch (Exception ex) {
            return AgentTurn.failed(ex.getMessage(), context.getToolInvocations());
        }
    }

    private boolean isRecoverableToolFailure(ToolCallResult result) {
        String error = result.getError();
        if (error == null) {
            return false;
        }
        return error.startsWith("Unknown tool:")
                || error.startsWith("Access denied:")
                || error.contains("is required")
                || error.contains("does not exist")
                || error.contains("not a regular file");
    }

    private String requestModel(String prompt, AgentContext context) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        StringBuilder output = new StringBuilder();
        AtomicReference<Exception> error = new AtomicReference<>();

        AiAgentRequest request = AiAgentRequest.builder()
                .message(prompt)
                .userId(context.getUserId())
                .conversationId(context.getConversationId())
                .metadata("agentMode", "readOnly")
                .build();

        AiAgentStreamHandle handle = service.streamChat(request, new AiAgentStreamCallback() {
            @Override
            public void onDelta(String delta) {
                if (delta != null) {
                    output.append(delta);
                }
            }

            @Override
            public void onComplete() {
                done.countDown();
            }

            @Override
            public void onError(Exception ex) {
                error.set(ex);
                done.countDown();
            }
        });

        boolean completed;
        try {
            completed = done.await(config.getModelTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            if (handle != null) {
                handle.cancel();
            }
            Thread.currentThread().interrupt();
            throw ex;
        }
        if (!completed) {
            if (handle != null) {
                handle.cancel();
            }
            throw new TimeoutException("Model response timed out after " + config.getModelTimeout().toSeconds() + " seconds");
        }
        if (error.get() != null) {
            throw error.get();
        }
        return output.toString();
    }
}
