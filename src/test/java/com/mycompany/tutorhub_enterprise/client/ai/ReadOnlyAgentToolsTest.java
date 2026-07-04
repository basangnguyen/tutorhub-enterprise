package com.mycompany.tutorhub_enterprise.client.ai;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadOnlyAgentToolsTest {

    @TempDir
    Path workspace;

    @Test
    void readOnlyToolsListReadSearchAndDescribeProject() throws Exception {
        Files.writeString(workspace.resolve("pom.xml"), """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>1.0</version>
                </project>
                """);
        Path sourceDir = Files.createDirectories(workspace.resolve("src/main/java/demo"));
        Files.writeString(sourceDir.resolve("App.java"), """
                package demo;
                class App {
                    String value = "TutorHubSearchNeedle";
                }
                """);
        Files.writeString(workspace.resolve(".env"), "TOKEN=secret\n");

        ToolRegistry registry = ToolRegistry.readOnlyDefaults(new WorkspaceBoundary(workspace));

        ToolCallResult list = registry.execute(ToolCallRequest.of("list_files",
                Map.of("path", ".", "depth", "5")));
        assertTrue(list.isSuccess());
        assertTrue(list.getOutput().contains("src/main/java/demo/App.java"));
        assertFalse(list.getOutput().contains(".env"));

        ToolCallResult read = registry.execute(ToolCallRequest.of("read_file",
                Map.of("path", "src/main/java/demo/App.java", "maxLines", "20")));
        assertTrue(read.isSuccess());
        assertTrue(read.getOutput().contains("1 | package demo;"));

        ToolCallResult search = registry.execute(ToolCallRequest.of("search_text",
                Map.of("query", "TutorHubSearchNeedle")));
        assertTrue(search.isSuccess());
        assertTrue(search.getOutput().contains("src/main/java/demo/App.java"));

        ToolCallResult info = registry.execute(ToolCallRequest.of("get_project_info", Map.of()));
        assertTrue(info.isSuccess());
        assertTrue(info.getOutput().contains("ArtifactId: demo"));
    }
}
