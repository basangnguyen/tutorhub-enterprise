package com.mycompany.tutorhub_enterprise.client.ai.command;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandRunnerTest {

    @TempDir
    Path workspace;

    @Test
    void runsAllowedCommandInsideWorkspaceWithoutShell() throws Exception {
        String javaExe = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();
        CommandRunner runner = new CommandRunner(new WorkspaceBoundary(workspace), new CommandPolicy());

        CommandResult result = runner.run(new CommandSpec(List.of(javaExe, "-version"), ".", 30, "java version"));

        assertTrue(result.isSuccess(), result.getError());
        assertEquals(0, result.getExitCode());
        assertTrue(result.getOutput().toLowerCase().contains("version"));
    }
}
