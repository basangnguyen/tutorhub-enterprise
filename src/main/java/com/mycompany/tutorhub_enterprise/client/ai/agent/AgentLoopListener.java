package com.mycompany.tutorhub_enterprise.client.ai.agent;

public interface AgentLoopListener {

    AgentLoopListener NOOP = invocation -> { };

    void onToolInvocation(AgentToolInvocation invocation);
}
