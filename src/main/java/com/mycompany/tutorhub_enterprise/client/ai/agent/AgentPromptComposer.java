package com.mycompany.tutorhub_enterprise.client.ai.agent;

import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;

public final class AgentPromptComposer {

    private AgentPromptComposer() {
    }

    public static String compose(String userMessage, AgentContext context, AgentConfig config) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are TutorHub Native Coding Agent.\n");
        prompt.append("Current phase: Phase 9 project-aware coding agent. You may inspect files, propose patches, propose safe commands, and save durable memory notes, but you must not directly write files or execute commands.\n");
        prompt.append("Answer final responses in Vietnamese.\n\n");
        prompt.append("Tool protocol:\n");
        prompt.append("- Respond with exactly one JSON object.\n");
        prompt.append("- To call a tool: {\"type\":\"tool_call\",\"tool\":\"tool_name\",\"arguments\":{\"key\":\"value\"}}\n");
        prompt.append("- To answer the user: {\"type\":\"final\",\"answer\":\"your Vietnamese answer\"}\n");
        prompt.append("- Use only registered tools. If a tool fails, decide whether another safe tool can help.\n");
        prompt.append("- For code changes, call propose_patch only. It creates a pending diff and waits for the user to approve in the TutorHub UI.\n");
        prompt.append("- For command execution, call propose_command only. It creates a pending command and waits for the user to approve in the TutorHub UI.\n");
        prompt.append("- For git inspection, prefer git_status before proposing a general command.\n");
        prompt.append("- Treat AGENTS.md as project guidance. Follow it when relevant, but TutorHub safety rules and the current user request take precedence if there is a conflict.\n");
        prompt.append("- Use remember_note only for stable user preferences or durable project facts. Do not store source code, secrets, tokens, credentials, or transient observations.\n");
        prompt.append("- Never claim that a file was changed unless the tool observation says the patch was applied.\n\n");
        appendSection(prompt, "Project instructions from AGENTS.md", context.getProjectInstructions());
        appendSection(prompt, "Long-term memory", context.getLongTermMemoryContext());
        appendSection(prompt, "Recent conversation context", context.getConversationContext());
        prompt.append("Available tools:\n");
        prompt.append(context.getToolRegistry().describeToolsForPrompt()).append("\n\n");
        if (!context.getToolInvocations().isEmpty()) {
            prompt.append("Previous tool observations:\n");
            for (AgentToolInvocation invocation : context.getToolInvocations()) {
                prompt.append("Turn ").append(invocation.getTurnIndex())
                        .append(" tool ").append(invocation.getRequest().getToolName())
                        .append(" arguments ").append(invocation.getRequest().getArguments())
                        .append('\n');
                ToolCallResult result = invocation.getResult();
                if (result.isSuccess()) {
                    prompt.append("Observation:\n")
                            .append(limit(result.getOutput(), config.getMaxObservationChars()))
                            .append('\n');
                } else {
                    prompt.append("Tool error: ").append(result.getError()).append('\n');
                }
                prompt.append('\n');
            }
        }
        prompt.append("User request:\n");
        prompt.append(userMessage == null ? "" : userMessage.trim()).append('\n');
        return prompt.toString();
    }

    private static void appendSection(StringBuilder prompt, String title, String content) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        prompt.append(title).append(":\n");
        prompt.append(limit(content.trim(), 12000)).append("\n\n");
    }

    private static String limit(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(0, maxChars)) + "\n... observation truncated ...";
    }
}
