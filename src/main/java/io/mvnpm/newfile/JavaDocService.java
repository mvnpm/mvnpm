package io.mvnpm.newfile;

import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import java.nio.file.Path;
import io.mvnpm.Constants;

import io.mvnpm.file.FileStoreEvent;
import io.mvnpm.file.FileUtil;
import io.mvnpm.file.type.JarClient;
import jakarta.inject.Inject;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;

/**
 * Create a dummy javadoc file
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class JavaDocService {

    private final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.jar");
    
    @Inject
    JarClient jarClient; 
    
    @ConsumeEvent("new-file-created")
    public void newFileCreated(FileStoreEvent fse) {
        if (matcher.matches(fse.filePath().getFileName())) {
            Path jarFile = fse.filePath();
            Path javadocFile = jarClient.createEmptyJar(jarFile, Constants.DASH_JAVADOC_DOT_JAR);
            if(Files.exists(javadocFile)){
                FileUtil.createSha1(javadocFile);
                FileUtil.createMd5(javadocFile);
                FileUtil.createAsc(javadocFile);
            }
            Log.debug("javadoc created " + fse.filePath() + "[ok]");
        }
    }
}
