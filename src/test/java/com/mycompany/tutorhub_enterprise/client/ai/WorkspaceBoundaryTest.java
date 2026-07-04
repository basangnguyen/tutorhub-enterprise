package com.mycompany.tutorhub_enterprise.client.ai;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceBoundaryTest {

    @TempDir
    Path workspace;

    @Test
    void resolvesRelativePathInsideWorkspace() throws Exception {
        Path source = Files.createDirectories(workspace.resolve("src")).resolve("App.java");
        Files.writeString(source, "class App {}\n");

        WorkspaceBoundary boundary = new WorkspaceBoundary(workspace);

        assertEquals(source.toRealPath(), boundary.resolveRequiredPath("src/App.java"));
        assertEquals("src/App.java", boundary.relativize(source));
    }

    @Test
    void deniesAbsolutePathOutsideWorkspace() throws Exception {
        Path outside = Files.createTempFile("tutorhub-agent-outside", ".txt");
        WorkspaceBoundary boundary = new WorkspaceBoundary(workspace);

        try {
            assertThrows(SecurityException.class, () -> boundary.resolveRequiredPath(outside.toString()));
        } finally {
            Files.deleteIfExists(outside);
        }
    }

    @Test
    void blocksLikelySecretFiles() throws Exception {
        Path env = workspace.resolve(".env");
        Files.writeString(env, "TOKEN=secret\n");
        WorkspaceBoundary boundary = new WorkspaceBoundary(workspace);

        assertThrows(SecurityException.class, () -> boundary.resolveRequiredPath(".env"));
        assertTrue(boundary.isDeniedPath(env));
    }
}
