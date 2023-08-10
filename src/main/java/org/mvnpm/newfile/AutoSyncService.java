package org.mvnpm.newfile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import org.mvnpm.Constants;
import org.mvnpm.centralsync.ProjectUpdater;

import org.mvnpm.file.FileStoreEvent;
import org.mvnpm.npm.model.Name;

/**
 * Once a new jar file is created, we need to kick of the sync process
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class AutoSyncService {

    @Inject
    ProjectUpdater projectUpdater;
    
    public void newFileCreated(@ObservesAsync FileStoreEvent fse) {
        String jarFile = fse.fileName();
        if(jarFile.endsWith(Constants.DOT_JAR)){
            Name name = fse.p().name();
            String version = fse.p().version();
            projectUpdater.update(name, version);
        }
    }
    
}
