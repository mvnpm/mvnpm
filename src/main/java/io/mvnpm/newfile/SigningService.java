package io.mvnpm.newfile;

import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.file.FileStoreEvent;
import io.mvnpm.file.FileUtil;
import io.smallrye.common.annotation.Blocking;

/**
 * Sign newly created file
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class SigningService {

    @ConsumeEvent("new-file-created")
    @Blocking
    public void newFileCreated(FileStoreEvent fse) {
        FileUtil.createAsc(fse.filePath());
        FileUtil.createMd5(fse.filePath());
        Log.debug("file signed " + fse.filePath() + " [ok]");
    }
}
