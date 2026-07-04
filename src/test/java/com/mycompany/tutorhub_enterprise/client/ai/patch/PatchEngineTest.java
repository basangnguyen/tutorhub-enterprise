package com.mycompany.tutorhub_enterprise.client.ai.patch;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatchEngineTest {

    @TempDir
    Path workspace;

    @Test
    void proposesAndAppliesExactStrReplacePatch() throws Exception {
        Path file = workspace.resolve("App.java");
        Files.writeString(file, "class App {\n    int value = 1;\n}\n");
        PatchEngine engine = new PatchEngine(new WorkspaceBoundary(workspace));

        PatchProposal proposal = engine.proposeStrReplace(
                "App.java",
                "int value = 1;",
                "int value = 2;",
                "Update test value");
        PatchResult result = engine.apply(proposal);

        assertTrue(proposal.getDiff().contains("-    int value = 1;"));
        assertTrue(proposal.getDiff().contains("+    int value = 2;"));
        assertTrue(result.isSuccess());
        assertEquals("class App {\n    int value = 2;\n}\n", Files.readString(file));
    }

    @Test
    void rejectsAmbiguousOldText() throws Exception {
        Path file = workspace.resolve("App.java");
        Files.writeString(file, "value\nvalue\n");
        PatchEngine engine = new PatchEngine(new WorkspaceBoundary(workspace));

        assertThrows(IllegalArgumentException.class,
                () -> engine.proposeStrReplace("App.java", "value", "next", "ambiguous"));
    }

    @Test
    void refusesToApplyWhenFileChangedAfterProposal() throws Exception {
        Path file = workspace.resolve("App.java");
        Files.writeString(file, "alpha\n");
        PatchEngine engine = new PatchEngine(new WorkspaceBoundary(workspace));
        PatchProposal proposal = engine.proposeStrReplace("App.java", "alpha", "beta", "change");

        Files.writeString(file, "gamma\n");
        PatchResult result = engine.apply(proposal);

        assertEquals(false, result.isSuccess());
        assertTrue(result.getMessage().contains("File has changed"));
        assertEquals("gamma\n", Files.readString(file));
    }
}
