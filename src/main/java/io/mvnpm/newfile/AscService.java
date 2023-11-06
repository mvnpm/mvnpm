package io.mvnpm.newfile;

import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.file.FileStoreEvent;
import io.mvnpm.file.FileUtil;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

/**
 * Sign newly created file
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class AscService {

    @ConsumeEvent("new-file-created")
    @Blocking
    public void newFileCreated(FileStoreEvent fse) {
        boolean success = FileUtil.createAsc(fse.filePath());
        if (!success) {
            Log.warn("file signed " + fse.filePath() + ".asc [failed] trying again");
        }
        Log.debug("file signed " + fse.filePath() + " [ok]");
    }
}
