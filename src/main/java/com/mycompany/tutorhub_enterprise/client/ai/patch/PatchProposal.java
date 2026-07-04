package com.mycompany.tutorhub_enterprise.client.ai.patch;

import java.nio.file.Path;
import java.time.Instant;

public final class PatchProposal {

    private final String id;
    private final Path file;
    private final String relativePath;
    private final String originalContent;
    private final String proposedContent;
    private final String diff;
    private final String reason;
    private final Instant createdAt;

    public PatchProposal(String id, Path file, String relativePath, String originalContent,
                         String proposedContent, String diff, String reason, Instant createdAt) {
        this.id = id == null ? "" : id;
        this.file = file;
        this.relativePath = relativePath == null ? "" : relativePath;
        this.originalContent = originalContent == null ? "" : originalContent;
        this.proposedContent = proposedContent == null ? "" : proposedContent;
        this.diff = diff == null ? "" : diff;
        this.reason = reason == null ? "" : reason;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String getId() {
        return id;
    }

    public Path getFile() {
        return file;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public String getProposedContent() {
        return proposedContent;
    }

    public String getDiff() {
        return diff;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
