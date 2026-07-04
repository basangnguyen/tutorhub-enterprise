package com.mycompany.tutorhub_enterprise.client.ai.command;

public final class CommandResult {

    private final boolean success;
    private final String commandId;
    private final String commandLine;
    private final int exitCode;
    private final boolean timedOut;
    private final long durationMillis;
    private final String output;
    private final String error;

    public CommandResult(boolean success, String commandId, String commandLine,
                         int exitCode, boolean timedOut, long durationMillis,
                         String output, String error) {
        this.success = success;
        this.commandId = commandId == null ? "" : commandId;
        this.commandLine = commandLine == null ? "" : commandLine;
        this.exitCode = exitCode;
        this.timedOut = timedOut;
        this.durationMillis = durationMillis;
        this.output = output == null ? "" : output;
        this.error = error == null ? "" : error;
    }

    public static CommandResult failure(CommandSpec command, String error) {
        return new CommandResult(false,
                command == null ? "" : command.getId(),
                command == null ? "" : command.getCommandLine(),
                -1, false, 0, "", error);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }
}
