package io.mvnpm.mcp;

import jakarta.inject.Inject;

import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.ProjectInfo;
import io.mvnpm.npm.model.SearchResult;
import io.mvnpm.npm.model.SearchResults;
import io.quarkiverse.mcp.server.TextContent;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkiverse.mcp.server.ToolResponse;

public class PackageTools {

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    @Inject
    McpNameResolver nameResolver;

    @Tool(description = "Search for NPM packages available as Maven dependencies via mvnpm. Returns package names, descriptions, versions, and Maven coordinates.")
    ToolResponse search_packages(
            @ToolArg(description = "Search query (e.g. 'lit', 'web components', 'react')") String query,
            @ToolArg(description = "Page number (1-based, 50 results per page)", defaultValue = "1") int page) {
        SearchResults results = npmRegistryFacade.search(query, page);
        StringBuilder sb = new StringBuilder();
        sb.append("Found ").append(results.total()).append(" results (page ").append(page).append("):\n\n");
        int i = (page - 1) * 50 + 1;
        for (SearchResult result : results.objects()) {
            var item = result.item();
            sb.append(i++).append(". ").append(item.name());
            if (item.version() != null) {
                sb.append(" (").append(item.version()).append(")");
            }
            if (item.description() != null) {
                sb.append(" - ").append(item.description());
            }
            Name name = nameResolver.resolve(item.name());
            sb.append("\n   Maven: ").append(name.mvnGroupId).append(":").append(name.mvnArtifactId);
            sb.append("\n");
        }
        return ToolResponse.success(new TextContent(sb.toString()));
    }

    @Tool(description = "Get detailed metadata for an NPM package. Accepts NPM name (e.g. 'lit', '@hotwired/stimulus') or Maven coordinates (e.g. 'org.mvnpm:lit').")
    ToolResponse get_package_info(
            @ToolArg(description = "Package name (NPM or Maven coordinates)") String name,
            @ToolArg(description = "Version (defaults to 'latest')", defaultValue = "latest") String version) {
        Name resolved = nameResolver.resolve(name);
        String ver = nameResolver.resolveVersion(resolved, version);
        io.mvnpm.npm.model.Package pkg = npmRegistryFacade.getPackage(resolved.npmFullName, ver);
        StringBuilder sb = new StringBuilder();
        sb.append("Package: ").append(pkg.name().npmFullName).append("\n");
        sb.append("Version: ").append(pkg.version()).append("\n");
        sb.append("Maven: ").append(resolved.mvnGroupId).append(":").append(resolved.mvnArtifactId).append(":")
                .append(pkg.version()).append("\n");
        if (pkg.description() != null)
            sb.append("Description: ").append(pkg.description()).append("\n");
        if (pkg.license() != null)
            sb.append("License: ").append(pkg.license().type()).append("\n");
        if (pkg.homepage() != null)
            sb.append("Homepage: ").append(pkg.homepage()).append("\n");
        if (pkg.repository() != null)
            sb.append("Repository: ").append(pkg.repository()).append("\n");
        if (pkg.author() != null)
            sb.append("Author: ").append(pkg.author().name()).append("\n");
        if (pkg.maintainers() != null && !pkg.maintainers().isEmpty()) {
            sb.append("Maintainers: ");
            pkg.maintainers().forEach(m -> sb.append(m.name()).append(", "));
            sb.setLength(sb.length() - 2);
            sb.append("\n");
        }
        if (pkg.main() != null)
            sb.append("Main: ").append(pkg.main()).append("\n");
        if (pkg.module() != null)
            sb.append("Module: ").append(pkg.module()).append("\n");
        if (pkg.type() != null)
            sb.append("Type: ").append(pkg.type()).append("\n");
        if (pkg.dependencies() != null && !pkg.dependencies().isEmpty()) {
            sb.append("Dependencies:\n");
            pkg.dependencies()
                    .forEach((dep, ver2) -> sb.append("  ").append(dep.npmFullName).append(": ").append(ver2).append("\n"));
        }
        if (pkg.peerDependencies() != null && !pkg.peerDependencies().isEmpty()) {
            sb.append("Peer Dependencies:\n");
            pkg.peerDependencies()
                    .forEach((dep, ver2) -> sb.append("  ").append(dep.npmFullName).append(": ").append(ver2).append("\n"));
        }
        return ToolResponse.success(new TextContent(sb.toString()));
    }

    @Tool(description = "List all available versions for an NPM package, including dist-tags (latest, next). Accepts NPM name or Maven coordinates.")
    ToolResponse list_versions(
            @ToolArg(description = "Package name (NPM or Maven coordinates)") String name) {
        Name resolved = nameResolver.resolve(name);
        ProjectInfo info = npmRegistryFacade.getProjectInfo(resolved.npmFullName);
        StringBuilder sb = new StringBuilder();
        sb.append("Package: ").append(resolved.npmFullName).append("\n");
        sb.append("Maven: ").append(resolved.mvnGroupId).append(":").append(resolved.mvnArtifactId).append("\n\n");
        sb.append("Dist Tags:\n");
        if (info.distTags() != null) {
            if (info.distTags().latest() != null)
                sb.append("  latest: ").append(info.distTags().latest()).append("\n");
            if (info.distTags().next() != null)
                sb.append("  next: ").append(info.distTags().next()).append("\n");
        }
        sb.append("\nAll Versions (").append(info.versions().size()).append("):\n");
        info.versions().forEach(v -> sb.append("  ").append(v).append("\n"));
        return ToolResponse.success(new TextContent(sb.toString()));
    }

    @Tool(description = "Get Maven coordinates and dependency snippet for an NPM package. Accepts NPM name (e.g. '@hotwired/stimulus') or Maven coordinates.")
    ToolResponse get_maven_coordinates(
            @ToolArg(description = "Package name (NPM or Maven coordinates)") String name) {
        Name resolved = nameResolver.resolve(name);
        ProjectInfo info = npmRegistryFacade.getProjectInfo(resolved.npmFullName);
        String latest = info.distTags() != null ? info.distTags().latest() : "LATEST";
        StringBuilder sb = new StringBuilder();
        sb.append("NPM: ").append(resolved.npmFullName).append("\n");
        sb.append("Maven GroupId: ").append(resolved.mvnGroupId).append("\n");
        sb.append("Maven ArtifactId: ").append(resolved.mvnArtifactId).append("\n");
        sb.append("Latest Version: ").append(latest).append("\n\n");
        sb.append("Maven Dependency:\n");
        sb.append("<dependency>\n");
        sb.append("    <groupId>").append(resolved.mvnGroupId).append("</groupId>\n");
        sb.append("    <artifactId>").append(resolved.mvnArtifactId).append("</artifactId>\n");
        sb.append("    <version>").append(latest).append("</version>\n");
        sb.append("</dependency>\n\n");
        sb.append("Gradle:\n");
        sb.append("implementation '").append(resolved.mvnGroupId).append(":").append(resolved.mvnArtifactId).append(":")
                .append(latest).append("'\n");
        return ToolResponse.success(new TextContent(sb.toString()));
    }
}
