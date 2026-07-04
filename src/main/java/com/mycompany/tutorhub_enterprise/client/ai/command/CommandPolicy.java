package com.mycompany.tutorhub_enterprise.client.ai.command;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CommandPolicy {

    private static final Set<String> ALLOWED_EXECUTABLES = Set.of(
            "git", "git.exe",
            "mvn", "mvn.cmd", "mvn.bat",
            "gradle", "gradle.bat", "gradlew", "gradlew.bat",
            "npm", "npm.cmd",
            "node", "node.exe",
            "java", "java.exe"
    );

    private static final Set<String> DENIED_EXECUTABLES = Set.of(
            "cmd", "cmd.exe",
            "powershell", "powershell.exe", "pwsh", "pwsh.exe",
            "bash", "bash.exe", "sh", "sh.exe",
            "python", "python.exe", "py", "py.exe",
            "rm", "del", "erase", "rmdir", "rd",
            "format", "shutdown", "taskkill", "reg", "reg.exe"
    );

    private static final Set<String> DANGEROUS_ARGUMENTS = Set.of(
            "clean", "install"
    );

    public void validate(CommandSpec command) {
        if (command == null) {
            throw new IllegalArgumentException("Command is required");
        }
        List<String> tokens = command.getTokens();
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Command tokens are required");
        }
        String executableName = executableName(tokens.get(0));
        if (DENIED_EXECUTABLES.contains(executableName)) {
            throw new SecurityException("Executable is denied: " + executableName);
        }
        if (!ALLOWED_EXECUTABLES.contains(executableName)) {
            throw new SecurityException("Executable is not in TutorHub Agent allowlist: " + executableName);
        }
        for (String token : tokens) {
            validateArgument(executableName, token);
        }
    }

    private void validateArgument(String executableName, String token) {
        String value = token == null ? "" : token.trim();
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return;
        }
        if (lower.equals("--force") || lower.equals("-f") || lower.equals("/f")
                || lower.equals("--delete") || lower.equals("--hard")) {
            throw new SecurityException("Dangerous command argument is denied: " + value);
        }
        if (lower.contains("://")) {
            throw new SecurityException("Network URLs are not allowed in command arguments");
        }
        if (("git".equals(executableName) || "git.exe".equals(executableName))
                && (lower.equals("push") || lower.equals("reset") || lower.equals("checkout")
                || lower.equals("clean") || lower.equals("rebase") || lower.equals("commit"))) {
            throw new SecurityException("Git mutating operation is denied by command policy: " + value);
        }
        if ((executableName.startsWith("mvn") || executableName.startsWith("gradle") || executableName.startsWith("gradlew"))
                && DANGEROUS_ARGUMENTS.contains(lower)) {
            throw new SecurityException("Build lifecycle goal is denied by command policy: " + value);
        }
    }

    private String executableName(String executable) {
        String raw = executable == null ? "" : executable.trim();
        if (raw.isEmpty()) {
            return "";
        }
        try {
            Path fileName = Path.of(raw).getFileName();
            if (fileName != null) {
                return fileName.toString().toLowerCase(Locale.ROOT);
            }
        } catch (RuntimeException ignored) {
            // Fall through to raw value.
        }
        return raw.toLowerCase(Locale.ROOT);
    }
}
