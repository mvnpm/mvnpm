package io.mvnpm.mcp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;

@ApplicationScoped
public class McpNameResolver {

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    /**
     * Accepts either NPM name ("lit", "@hotwired/stimulus")
     * or Maven coordinates ("org.mvnpm:lit", "org.mvnpm.at.hotwired:stimulus")
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
            return npmRegistryFacade.getProjectInfo(name.npmFullName).distTags().latest();
        }
        return version;
    }
}
