package com.mycompany.tutorhub_enterprise.client.ai.command;

import java.util.ArrayList;
import java.util.List;

public final class CommandParser {

    private CommandParser() {
    }

    public static List<String> parse(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            throw new IllegalArgumentException("Command is required");
        }
        if (containsShellOperator(commandLine)) {
            throw new IllegalArgumentException("Shell operators are not allowed. Propose one command at a time.");
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean escaped = false;
        for (int i = 0; i < commandLine.length(); i++) {
            char ch = commandLine.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\' && inDouble) {
                if (i + 1 < commandLine.length()) {
                    char next = commandLine.charAt(i + 1);
                    if (next == '"' || next == '\\') {
                        escaped = true;
                        continue;
                    }
                }
            }
            if (ch == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (ch == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }
            if (Character.isWhitespace(ch) && !inSingle && !inDouble) {
                flush(tokens, current);
                continue;
            }
            current.append(ch);
        }
        if (inSingle || inDouble) {
            throw new IllegalArgumentException("Unclosed quote in command");
        }
        flush(tokens, current);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Command is required");
        }
        return tokens;
    }

    private static void flush(List<String> tokens, StringBuilder current) {
        if (current.length() > 0) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }

    private static boolean containsShellOperator(String commandLine) {
        String raw = commandLine.trim();
        return raw.contains("&&")
                || raw.contains("||")
                || raw.contains("|")
                || raw.contains(";")
                || raw.contains(">")
                || raw.contains("<")
                || raw.contains("`")
                || raw.contains("$(")
                || raw.matches(".*\\s&\\s.*");
    }
}
