package com.mycompany.tutorhub_enterprise.client.ai.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PendingCommandStore {

    private static final int MAX_COMMANDS = 50;

    private final Map<String, CommandSpec> pending = new LinkedHashMap<>();

    public synchronized CommandSpec add(CommandSpec command) {
        if (command == null || command.getId().isBlank()) {
            throw new IllegalArgumentException("Command spec is required");
        }
        pending.put(command.getId(), command);
        trim();
        return command;
    }

    public synchronized Optional<CommandSpec> find(String commandId) {
        if (commandId == null || commandId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(pending.get(commandId.trim()));
    }

    public synchronized Optional<CommandSpec> remove(String commandId) {
        if (commandId == null || commandId.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(pending.remove(commandId.trim()));
    }

    public synchronized List<CommandSpec> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(pending.values()));
    }

    private void trim() {
        while (pending.size() > MAX_COMMANDS) {
            String firstKey = pending.keySet().iterator().next();
            pending.remove(firstKey);
        }
    }
}
