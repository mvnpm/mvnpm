package io.mvnpm.mcp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.GZIPInputStream;

import jakarta.inject.Inject;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import io.mvnpm.creator.FileType;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.NameVersion;
import io.mvnpm.npm.model.Name;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;

public class ArtifactTools {

    @Inject
    MavenRepositoryService mavenRepositoryService;

    @Inject
    McpNameResolver nameResolver;

    @Tool(description = "Get the generated Maven POM XML for an NPM package version. Accepts NPM name or Maven coordinates.")
    ToolResponse get_pom(
            @ToolArg(description = "Package name (NPM or Maven coordinates)") String name,
            @ToolArg(description = "Version (defaults to 'latest')", defaultValue = "latest") String version) {
        try {
            Name resolved = nameResolver.resolve(name);
            String ver = nameResolver.resolveVersion(resolved, version);
            Path pomPath = mavenRepositoryService.getPath(resolved, ver, FileType.pom);
            String content = Files.readString(pomPath);
            return ToolResponse.success(new TextContent(content));
        } catch (IOException e) {
            return ToolResponse.error("Failed to read POM: " + e.getMessage());
        }
    }

    @Tool(description = "Get the ES module import map JSON for an NPM package version. Accepts NPM name or Maven coordinates.")
    ToolResponse get_import_map(
            @ToolArg(description = "Package name (NPM or Maven coordinates)") String name,
            @ToolArg(description = "Version (defaults to 'latest')", defaultValue = "latest") String version) {
        Name resolved = nameResolver.resolve(name);
        String ver = nameResolver.resolveVersion(resolved, version);
        byte[] importMap = mavenRepositoryService.getImportMap(new NameVersion(resolved, ver));
        return ToolResponse.success(new TextContent(new String(importMap)));
    }

    @Tool(description = "Browse the file listing inside a JAR, TGZ, sources, or javadoc archive for an NPM package. Accepts NPM name or Maven coordinates.")
    ToolResponse browse_artifact_contents(
            @ToolArg(description = "Package name (NPM or Maven coordinates)") String name,
            @ToolArg(description = "Version (defaults to 'latest')", defaultValue = "latest") String version,
            @ToolArg(description = "Archive type: jar, tgz, source, javadoc", defaultValue = "jar") String type) {
        try {
            Name resolved = nameResolver.resolve(name);
            String ver = nameResolver.resolveVersion(resolved, version);
            FileType fileType = FileType.valueOf(type.toLowerCase());
            Path archivePath = mavenRepositoryService.getPath(resolved, ver, fileType);

            StringBuilder sb = new StringBuilder();
            sb.append("Contents of ").append(resolved.npmFullName).append(" ").append(ver).append(" (").append(type)
                    .append("):\n\n");
            listArchiveEntries(archivePath, fileType, sb);
            return ToolResponse.success(new TextContent(sb.toString()));
        } catch (Exception e) {
            return ToolResponse.error("Failed to browse artifact: " + e.getMessage());
        }
    }

    @Tool(description = "Download/create the JAR for an NPM package version. This triggers a sync to Maven Central if not already synced. Accepts NPM name or Maven coordinates.")
    ToolResponse download_jar(
            @ToolArg(description = "Package name (NPM or Maven coordinates)") String name,
            @ToolArg(description = "Version (defaults to 'latest')", defaultValue = "latest") String version) {
        try {
            Name resolved = nameResolver.resolve(name);
            String ver = nameResolver.resolveVersion(resolved, version);
            Path jarPath = mavenRepositoryService.getPath(resolved, ver, FileType.jar);
            long size = Files.size(jarPath);

            StringBuilder sb = new StringBuilder();
            sb.append("JAR: ").append(jarPath).append("\n");
            sb.append("Size: ").append(formatSize(size)).append("\n");
            sb.append("Maven: ").append(resolved.mvnGroupId).append(":").append(resolved.mvnArtifactId).append(":")
                    .append(ver).append("\n");
            sb.append("Note: Sync to Maven Central triggered if not already synced.\n");
            return ToolResponse.success(new TextContent(sb.toString()));
        } catch (Exception e) {
            return ToolResponse.error("Failed to get JAR: " + e.getMessage());
        }
    }

    private void listArchiveEntries(Path archivePath, FileType fileType, StringBuilder sb) throws IOException {
        if (fileType == FileType.tgz) {
            try (GZIPInputStream gzis = new GZIPInputStream(Files.newInputStream(archivePath));
                    TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
                TarArchiveEntry entry;
                while ((entry = tais.getNextEntry()) != null) {
                    sb.append(entry.isDirectory() ? "[DIR]  " : "[FILE] ");
                    sb.append(entry.getName());
                    if (!entry.isDirectory()) {
                        sb.append(" (").append(formatSize(entry.getSize())).append(")");
                    }
                    sb.append("\n");
                }
            }
        } else {
            try (JarInputStream jis = new JarInputStream(Files.newInputStream(archivePath))) {
                JarEntry entry;
                while ((entry = jis.getNextJarEntry()) != null) {
                    sb.append(entry.isDirectory() ? "[DIR]  " : "[FILE] ");
                    sb.append(entry.getName());
                    if (!entry.isDirectory() && entry.getSize() >= 0) {
                        sb.append(" (").append(formatSize(entry.getSize())).append(")");
                    }
                    sb.append("\n");
                }
            }
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + "B";
        if (bytes < 1024 * 1024)
            return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024));
    }
}
