package io.mvnpm.creator.events;

import java.nio.file.Path;

import io.mvnpm.npm.model.Name;

public record DependencyVersionCheckRequest(Path pomFile, Path tgzFile,
        Name name,
        String version) {
    public static final String NAME = "DependencyVersionCheckRequest";
}
