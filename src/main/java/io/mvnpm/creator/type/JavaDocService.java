package io.mvnpm.creator.type;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.quarkus.logging.Log;

/**
 * Create a dummy javadoc file
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class JavaDocService {

    @Inject
    JarService jarService;

    public Path createJavadoc(Path jarFile) {
        Path javadocFile = jarService.createEmptyJar(jarFile, Constants.DASH_JAVADOC_DOT_JAR);
        Log.debug("javadoc created " + jarFile + "[ok]");
        return javadocFile;
    }
}
