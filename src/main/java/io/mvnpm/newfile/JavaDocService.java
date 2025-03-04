package io.mvnpm.newfile;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.mvnpm.file.type.JarClient;
import io.quarkus.logging.Log;

/**
 * Create a dummy javadoc file
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class JavaDocService {

    @Inject
    JarClient jarClient;

    public Path createJavadoc(Path jarFile) {
        Path javadocFile = jarClient.createEmptyJar(jarFile, Constants.DASH_JAVADOC_DOT_JAR);
        Log.debug("javadoc created " + jarFile + "[ok]");
        return javadocFile;
    }
}
