package io.mvnpm.file;
import io.mvnpm.npm.model.Name;
import java.nio.file.Path;

/**
 * Event that fires when a file has been created
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public record FileStoreEvent(Path filePath, Name name, String version) {}