package io.mvnpm.mcp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;

@ApplicationScoped
public class McpNameResolver {

    @Inject
    NpmFacade npmFacade;

    /**
     * Accepts either NPM name (e.g. "lit", "@hotwired/stimulus")
     * or Maven coordinates (e.g. "org.mvnpm:lit", "org.mvnpm.at.hotwired:stimulus")
     */
    public Name resolve(String nameOrCoordinates) {
        if (nameOrCoordinates.contains(":") && nameOrCoordinates.startsWith("org.mvnpm")) {
            String[] parts = nameOrCoordinates.split(":", 2);
            return NameParser.fromMavenGA(parts[0], parts[1]);
        }
        return NameParser.fromNpmProject(nameOrCoordinates);
    }

    /**
     * Resolves "latest" to actual version number via NPM registry
     */
    public String resolveVersion(Name name, String version) {
        if (version == null || version.isBlank() || "latest".equalsIgnoreCase(version)) {
            return npmFacade.getProjectInfo(name.npmFullName).distTags().latest();
        }
        return version;
    }
}
