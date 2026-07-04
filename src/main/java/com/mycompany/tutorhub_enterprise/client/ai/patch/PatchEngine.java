package com.mycompany.tutorhub_enterprise.client.ai.patch;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class PatchEngine {

    private static final int MAX_REWRITE_LINES = 400;

    private final WorkspaceBoundary boundary;

    public PatchEngine(WorkspaceBoundary boundary) {
        if (boundary == null) {
            throw new IllegalArgumentException("Workspace boundary is required");
        }
        this.boundary = boundary;
    }

    public PatchProposal proposeStrReplace(String path, String oldText, String newText, String reason) throws IOException {
        if (oldText == null || oldText.isEmpty()) {
            throw new IllegalArgumentException("oldText is required");
        }
        Path file = resolveWritableTextFile(path);
        String original = readUtf8(file);
        int first = original.indexOf(oldText);
        if (first < 0) {
            throw new IllegalArgumentException("oldText was not found in " + boundary.relativize(file));
        }
        int second = original.indexOf(oldText, first + oldText.length());
        if (second >= 0) {
            throw new IllegalArgumentException("oldText appears more than once. Provide a more specific block.");
        }
        String proposed = original.substring(0, first)
                + (newText == null ? "" : newText)
                + original.substring(first + oldText.length());
        return createProposal(file, original, proposed, reason);
    }

    public PatchProposal proposeRewrite(String path, String content, String reason) throws IOException {
        Path file = resolveWritableTextFile(path);
        String original = readUtf8(file);
        int lines = countLines(original);
        if (lines > MAX_REWRITE_LINES) {
            throw new IllegalArgumentException("Full rewrite is limited to files with "
                    + MAX_REWRITE_LINES + " lines or fewer. File has " + lines + " lines.");
        }
        return createProposal(file, original, content == null ? "" : content, reason);
    }

    public PatchResult apply(PatchProposal proposal) {
        if (proposal == null) {
            return PatchResult.failure("", "", "Patch proposal is missing");
        }
        try {
            Path file = boundary.validateExistingPath(proposal.getFile());
            String current = readUtf8(file);
            if (!current.equals(proposal.getOriginalContent())) {
                return PatchResult.failure(proposal.getId(), proposal.getRelativePath(),
                        "File has changed since the patch was proposed. Please ask the agent to re-read and propose again.");
            }
            String backupPath = createBackup(proposal, current);
            Files.writeString(file, proposal.getProposedContent(), StandardCharsets.UTF_8);
            return PatchResult.success(proposal, "Patch applied successfully.", backupPath);
        } catch (Exception ex) {
            return PatchResult.failure(proposal.getId(), proposal.getRelativePath(), ex.getMessage());
        }
    }

    private PatchProposal createProposal(Path file, String original, String proposed, String reason) {
        if (original.equals(proposed)) {
            throw new IllegalArgumentException("Proposed content is identical to the current file");
        }
        String relative = boundary.relativize(file);
        String id = "patch-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String diff = DiffGenerator.unifiedDiff(relative, original, proposed);
        return new PatchProposal(id, file, relative, original, proposed, diff, reason, Instant.now());
    }

    private Path resolveWritableTextFile(String path) throws IOException {
        Path file = boundary.resolveRequiredPath(path);
        if (!Files.isRegularFile(file)) {
            throw new IOException("Path is not a regular file: " + boundary.relativize(file));
        }
        readUtf8(file);
        return file;
    }

    private String readUtf8(Path file) throws IOException {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (MalformedInputException ex) {
            throw new IOException("File is not valid UTF-8 text: " + boundary.relativize(file), ex);
        }
    }

    private String createBackup(PatchProposal proposal, String current) throws IOException {
        Path backupRoot = Path.of(System.getProperty("user.home", "."), ".tutorhub", "ai-patch-backups");
        Files.createDirectories(backupRoot);
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-');
        String safeName = proposal.getRelativePath().replace('\\', '_').replace('/', '_');
        Path backup = backupRoot.resolve(timestamp + "_" + proposal.getId() + "_" + safeName + ".bak");
        Files.writeString(backup, current, StandardCharsets.UTF_8);
        return backup.toString();
    }

    private int countLines(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int count = 1;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
}
