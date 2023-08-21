package io.mvnpm.newfile;

import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.file.FileStoreEvent;
import io.mvnpm.file.FileUtil;

/**
 * Sign newly created file
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class SigningService {

    @ConsumeEvent("new-file-created")
    public void newFileCreated(FileStoreEvent fse) {
        FileUtil.createAsc(fse.fileName());
        FileUtil.createMd5(fse.fileName());
        Log.info("file signed " + fse.fileName() + "[true]");
    }
}
