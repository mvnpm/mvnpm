package io.mvnpm.file;

import java.nio.file.Path;
import java.util.List;

import io.mvnpm.npm.model.Name;

/**
 * Event that fires when a new jar has been created
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public record NewJarEvent(Path tempDirectory, Path pomFile, Path jarFile, Path tgzFile, List<Path> others, Path targetDirectory,
        Name name,
        String version) {
    public static final String EVENT_NAME = "NewJarEvent";
}
