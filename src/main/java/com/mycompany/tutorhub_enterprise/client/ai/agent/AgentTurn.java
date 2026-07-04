package com.mycompany.tutorhub_enterprise.client.ai.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AgentTurn {

    public enum Status {
        COMPLETED,
        MAX_TURNS_REACHED,
        FAILED
    }

    private final Status status;
    private final String finalAnswer;
    private final String error;
    private final List<AgentToolInvocation> toolInvocations;

    private AgentTurn(Status status, String finalAnswer, String error, List<AgentToolInvocation> toolInvocations) {
        this.status = status;
        this.finalAnswer = finalAnswer == null ? "" : finalAnswer;
        this.error = error == null ? "" : error;
        this.toolInvocations = Collections.unmodifiableList(new ArrayList<>(toolInvocations));
    }

    public static AgentTurn completed(String finalAnswer, List<AgentToolInvocation> toolInvocations) {
        return new AgentTurn(Status.COMPLETED, finalAnswer, "", safeList(toolInvocations));
    }

    public static AgentTurn maxTurnsReached(List<AgentToolInvocation> toolInvocations) {
        return new AgentTurn(Status.MAX_TURNS_REACHED, "",
                "Agent reached the maximum number of read-only tool turns.", safeList(toolInvocations));
    }

    public static AgentTurn failed(String error) {
        return failed(error, Collections.emptyList());
    }

    public static AgentTurn failed(String error, List<AgentToolInvocation> toolInvocations) {
        return new AgentTurn(Status.FAILED, "", error, safeList(toolInvocations));
    }

    public Status getStatus() {
        return status;
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public String getFinalAnswer() {
        return finalAnswer;
    }

    public String getError() {
        return error;
    }

    public List<AgentToolInvocation> getToolInvocations() {
        return toolInvocations;
    }

    private static List<AgentToolInvocation> safeList(List<AgentToolInvocation> toolInvocations) {
        return toolInvocations == null ? Collections.emptyList() : toolInvocations;
    }
}
