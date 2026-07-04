package com.mycompany.tutorhub_enterprise.client.ai.permission;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;

public final class WorkspaceBoundary {

    private static final Set<String> DENIED_SEGMENTS = Set.of(
            ".git",
            ".hg",
            ".svn",
            ".ssh",
            ".aws",
            ".azure",
            ".gnupg"
    );

    private static final Set<String> DENIED_FILE_NAMES = Set.of(
            ".env",
            ".env.local",
            ".env.development",
            ".env.production",
            "application.properties",
            "application-local.properties",
            "application-secret.properties",
            "credentials",
            "credentials.json",
            "id_rsa",
            "id_ed25519",
            "known_hosts"
    );

    private final Path workspaceRoot;

    public WorkspaceBoundary(Path workspaceRoot) throws IOException {
        if (workspaceRoot == null) {
            throw new IllegalArgumentException("Workspace root is required");
        }
        Path realRoot = workspaceRoot.toRealPath();
        if (!Files.isDirectory(realRoot)) {
            throw new IllegalArgumentException("Workspace root must be a directory: " + workspaceRoot);
        }
        this.workspaceRoot = realRoot;
    }

    public static WorkspaceBoundary from(String workspaceRoot) throws IOException {
        if (workspaceRoot == null || workspaceRoot.trim().isEmpty()) {
            throw new IllegalArgumentException("Workspace root is required");
        }
        return new WorkspaceBoundary(Paths.get(workspaceRoot.trim()));
    }

    public Path getWorkspaceRoot() {
        return workspaceRoot;
    }

    public Path resolveRequiredPath(String requestedPath) throws IOException {
        Path candidate = toCandidatePath(requestedPath);
        if (!Files.exists(candidate)) {
            throw new IOException("Path does not exist: " + safePath(requestedPath));
        }
        return validateExistingPath(candidate);
    }

    public Path resolveDirectoryOrRoot(String requestedPath) throws IOException {
        Path path = resolveRequiredPath(requestedPath == null || requestedPath.trim().isEmpty()
                ? "."
                : requestedPath);
        if (!Files.isDirectory(path)) {
            throw new IOException("Path is not a directory: " + relativize(path));
        }
        return path;
    }

    public Path validateExistingPath(Path candidate) throws IOException {
        if (candidate == null) {
            throw new IllegalArgumentException("Path is required");
        }
        Path realPath = candidate.toRealPath();
        if (!realPath.startsWith(workspaceRoot)) {
            throw new SecurityException("Path is outside the selected workspace");
        }
        if (isDeniedPath(realPath)) {
            throw new SecurityException("Path is blocked by workspace safety policy: " + relativize(realPath));
        }
        return realPath;
    }

    public boolean canRead(Path candidate) {
        try {
            validateExistingPath(candidate);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isDeniedPath(Path existingPath) {
        if (existingPath == null) {
            return true;
        }
        Path normalized = toComparablePath(existingPath);
        if (!normalized.startsWith(workspaceRoot)) {
            return true;
        }

        Path relative = workspaceRoot.relativize(normalized);
        for (Path segment : relative) {
            String name = segment.toString().toLowerCase(Locale.ROOT);
            if (DENIED_SEGMENTS.contains(name) || DENIED_FILE_NAMES.contains(name)) {
                return true;
            }
            if (looksLikeSecretFileName(name)) {
                return true;
            }
        }
        return false;
    }

    public String relativize(Path path) {
        if (path == null) {
            return "";
        }
        Path normalized = toComparablePath(path);
        if (normalized.startsWith(workspaceRoot)) {
            String value = workspaceRoot.relativize(normalized).toString();
            return value.isEmpty() ? "." : value.replace('\\', '/');
        }
        return normalized.toString();
    }

    private Path toCandidatePath(String requestedPath) {
        String safe = requestedPath == null || requestedPath.trim().isEmpty()
                ? "."
                : requestedPath.trim();
        Path raw = Paths.get(safe);
        if (raw.isAbsolute()) {
            return raw.normalize();
        }
        return workspaceRoot.resolve(raw).normalize();
    }

    private Path toComparablePath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException ex) {
            return path.toAbsolutePath().normalize();
        }
    }

    private boolean looksLikeSecretFileName(String name) {
        if (name.endsWith(".pem") || name.endsWith(".key") || name.endsWith(".p12") || name.endsWith(".pfx")) {
            return true;
        }
        return name.equals("secrets")
                || name.equals("secrets.json")
                || name.equals("credentials.yml")
                || name.equals("credentials.yaml")
                || name.equals("tokens.json")
                || name.endsWith(".secret")
                || name.endsWith(".secrets")
                || name.endsWith(".credentials");
    }

    private String safePath(String requestedPath) {
        return requestedPath == null || requestedPath.trim().isEmpty() ? "." : requestedPath.trim();
    }
}
