package io.mvnpm.newfile;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileStoreEvent;
import io.mvnpm.file.type.JarClient;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

/**
 * Create a dummy javadoc file
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class JavaDocService {

    private final PathMatcher jarmatcher = FileSystems.getDefault().getPathMatcher("glob:*.jar");
    private final PathMatcher javadocMatcher = FileSystems.getDefault().getPathMatcher("glob:*-javadoc.jar");
    private final PathMatcher sourceMatcher = FileSystems.getDefault().getPathMatcher("glob:*-sources.jar");

    @Inject
    JarClient jarClient;

    @Inject
    FileStore fileStore;

    @ConsumeEvent("new-file-created")
    @Blocking
    public void newFileCreated(FileStoreEvent fse) {
        if (jarmatcher.matches(fse.filePath().getFileName())
                && !javadocMatcher.matches(fse.filePath().getFileName())
                && !sourceMatcher.matches(fse.filePath().getFileName())) {
            Path jarFile = fse.filePath();
            Path javadocFile = jarClient.createEmptyJar(jarFile, Constants.DASH_JAVADOC_DOT_JAR);
            fileStore.touch(fse.name(), fse.version(), javadocFile);
            Log.debug("javadoc created " + fse.filePath() + "[ok]");
        }
    }
}
