package io.mvnpm.nexus.mvn.model;

public record MavenDetails(
        String extension,
        String artifactId,
        String version,
        String groupId) {
}
