package com.mycompany.tutorhub_enterprise.client.ai.tool.impl;

import com.mycompany.tutorhub_enterprise.client.ai.permission.WorkspaceBoundary;
import com.mycompany.tutorhub_enterprise.client.ai.tool.AgentTool;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallRequest;
import com.mycompany.tutorhub_enterprise.client.ai.tool.ToolCallResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class GetProjectInfoTool implements AgentTool {

    private final WorkspaceBoundary boundary;

    public GetProjectInfoTool(WorkspaceBoundary boundary) {
        this.boundary = boundary;
    }

    @Override
    public String name() {
        return "get_project_info";
    }

    @Override
    public String description() {
        return "Summarize project structure and Maven metadata inside the selected workspace. Read-only.";
    }

    @Override
    public Map<String, String> parameters() {
        return new LinkedHashMap<>();
    }

    @Override
    public ToolCallResult execute(ToolCallRequest request) {
        try {
            Path root = boundary.getWorkspaceRoot();
            StringBuilder output = new StringBuilder();
            output.append("Workspace: ").append(root).append('\n');
            output.append("AGENTS.md: ").append(Files.exists(root.resolve("AGENTS.md")) ? "present" : "missing").append('\n');
            output.append("README.md: ").append(Files.exists(root.resolve("README.md")) ? "present" : "missing").append('\n');

            Path pom = root.resolve("pom.xml");
            if (Files.exists(pom) && boundary.canRead(pom)) {
                MavenInfo maven = readPom(pom);
                output.append("Build: Maven").append('\n');
                output.append("GroupId: ").append(maven.groupId).append('\n');
                output.append("ArtifactId: ").append(maven.artifactId).append('\n');
                output.append("Version: ").append(maven.version).append('\n');
                output.append("Packaging: ").append(maven.packaging).append('\n');
            } else {
                output.append("Build: unknown (pom.xml not found or blocked)").append('\n');
            }

            appendPathCount(output, root.resolve("src/main/java"), "Main Java files");
            appendPathCount(output, root.resolve("src/main/resources"), "Main resource files");
            appendPathCount(output, root.resolve("src/test/java"), "Test Java files");
            return ToolCallResult.success(output.toString());
        } catch (Exception ex) {
            return ToolCallResult.failure(ex.getMessage());
        }
    }

    private MavenInfo readPom(Path pom) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder().parse(pom.toFile());
        Element root = document.getDocumentElement();
        return new MavenInfo(
                childText(root, "groupId", "(missing)"),
                childText(root, "artifactId", "(missing)"),
                childText(root, "version", "(missing)"),
                childText(root, "packaging", "jar")
        );
    }

    private void appendPathCount(StringBuilder output, Path path, String label) throws Exception {
        if (!Files.exists(path) || !boundary.canRead(path)) {
            output.append(label).append(": missing").append('\n');
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            long count = stream
                    .filter(Files::isRegularFile)
                    .filter(boundary::canRead)
                    .count();
            output.append(label).append(": ").append(count).append('\n');
        }
    }

    private String childText(Element element, String tagName, String defaultValue) {
        var nodes = element.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getParentNode() == element) {
                String text = nodes.item(i).getTextContent();
                return text == null || text.trim().isEmpty() ? defaultValue : text.trim();
            }
        }
        return defaultValue;
    }

    private record MavenInfo(String groupId, String artifactId, String version, String packaging) {
    }
}
