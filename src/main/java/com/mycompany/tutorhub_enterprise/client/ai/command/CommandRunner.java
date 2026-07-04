package com.mycompany.tutorhub_enterprise.client.ai.command;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class CommandRunner {

    private static final int MAX_OUTPUT_CHARS = 30000;

    private final WorkspaceBoundary boundary;
    private final CommandPolicy policy;

    public CommandRunner(WorkspaceBoundary boundary, CommandPolicy policy) {
        if (boundary == null) {
            throw new IllegalArgumentException("Workspace boundary is required");
        }
        this.boundary = boundary;
        this.policy = policy == null ? new CommandPolicy() : policy;
    }

    public CommandResult run(CommandSpec command) {
        long started = System.nanoTime();
        Process process = null;
        ExecutorService readerExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ai-command-output-reader");
            thread.setDaemon(true);
            return thread;
        });
        try {
            policy.validate(command);
            Path workingDirectory = boundary.resolveDirectoryOrRoot(command.getWorkingDirectory());
            ProcessBuilder builder = new ProcessBuilder(command.getTokens());
            builder.directory(workingDirectory.toFile());
            builder.redirectErrorStream(true);
            process = builder.start();
            Process runningProcess = process;
            StringBuilder output = new StringBuilder();
            Future<?> reader = readerExecutor.submit(() -> readOutput(runningProcess, output));
            boolean finished = process.waitFor(command.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                reader.get(3, TimeUnit.SECONDS);
                return new CommandResult(false, command.getId(), command.getCommandLine(),
                        -1, true, elapsedMillis(started), output.toString(),
                        "Command timed out after " + command.getTimeoutSeconds() + " seconds");
            }
            reader.get(3, TimeUnit.SECONDS);
            int exitCode = process.exitValue();
            return new CommandResult(exitCode == 0, command.getId(), command.getCommandLine(),
                    exitCode, false, elapsedMillis(started), output.toString(),
                    exitCode == 0 ? "" : "Command exited with code " + exitCode);
        } catch (Exception ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            return new CommandResult(false,
                    command == null ? "" : command.getId(),
                    command == null ? "" : command.getCommandLine(),
                    -1, false, elapsedMillis(started), "", ex.getMessage());
        } finally {
            readerExecutor.shutdownNow();
        }
    }

    private void readOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() < MAX_OUTPUT_CHARS) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            if (output.length() >= MAX_OUTPUT_CHARS) {
                output.append("... command output truncated ...").append(System.lineSeparator());
            }
        } catch (Exception ignored) {
            // The process may be killed on timeout; output best-effort is enough.
        }
    }

    private long elapsedMillis(long startedNanos) {
        return Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
    }
}
