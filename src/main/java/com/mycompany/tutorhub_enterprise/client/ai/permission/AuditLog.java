package com.mycompany.tutorhub_enterprise.client.ai.permission;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

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

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim();
    }
}
