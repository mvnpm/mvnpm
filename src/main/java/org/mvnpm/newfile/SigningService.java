package org.mvnpm.newfile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;

import org.mvnpm.file.FileStoreEvent;
import org.mvnpm.file.FileUtil;

/**
 * Sign newly created file
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class SigningService {

    public void newFileCreated(@ObservesAsync FileStoreEvent fse) {
        FileUtil.createAsc(fse.fileName());
        FileUtil.createMd5(fse.fileName());
    }
}
