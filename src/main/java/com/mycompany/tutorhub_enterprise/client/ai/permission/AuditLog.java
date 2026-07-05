package com.mycompany.tutorhub_enterprise.client.ai.permission;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AuditLog {

    private final Path logFile;

    public AuditLog() {
        this(Path.of(System.getProperty("user.home", "."), ".tutorhub", "ai-agent-audit.log"));
    }

    public AuditLog(Path logFile) {
        this.logFile = logFile;
    }

    public synchronized void record(String action, String patchId, String path, String status, String message) {
        try {
            Files.createDirectories(logFile.getParent());
            String line = Instant.now()
                    + "\t" + safe(action)
                    + "\t" + safe(patchId)
                    + "\t" + safe(path)
                    + "\t" + safe(status)
                    + "\t" + safe(message)
                    + System.lineSeparator();
            Files.writeString(logFile, line, StandardCharsets.UTF_8,
                    Files.exists(logFile)
                            ? java.nio.file.StandardOpenOption.APPEND
                            : java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException ignored) {
            // Audit logging must not break the user-facing patch flow.
        }
    }

    public synchronized List<Entry> readRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(50, limit));
        if (!Files.isRegularFile(logFile)) {
            return Collections.emptyList();
        }
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            List<Entry> entries = new ArrayList<>();
            for (int i = lines.size() - 1; i >= 0 && entries.size() < safeLimit; i--) {
                Entry entry = parseLine(lines.get(i));
                if (entry != null) {
                    entries.add(entry);
                }
            }
            return Collections.unmodifiableList(entries);
        } catch (IOException ignored) {
            return Collections.emptyList();
        }
    }

    private Entry parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.split("\t", -1);
        if (parts.length < 6) {
            return null;
        }
        return new Entry(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim();
    }

    public static final class Entry {
        private final String timestamp;
        private final String action;
        private final String targetId;
        private final String path;
        private final String status;
        private final String message;

        private Entry(String timestamp, String action, String targetId, String path, String status, String message) {
            this.timestamp = timestamp == null ? "" : timestamp;
            this.action = action == null ? "" : action;
            this.targetId = targetId == null ? "" : targetId;
            this.path = path == null ? "" : path;
            this.status = status == null ? "" : status;
            this.message = message == null ? "" : message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getAction() {
            return action;
        }

        public String getTargetId() {
            return targetId;
        }

        public String getPath() {
            return path;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
