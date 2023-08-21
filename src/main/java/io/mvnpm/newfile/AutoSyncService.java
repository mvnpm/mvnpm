package io.mvnpm.newfile;

import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.mvnpm.Constants;
import io.mvnpm.mavencentral.sync.ContinuousSyncService;

import io.mvnpm.file.FileStoreEvent;
import io.mvnpm.npm.model.Name;

/**
 * Once a new jar file is created, we need to kick of the sync process
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class AutoSyncService {

    @Inject
    ContinuousSyncService projectUpdater;
    
    @ConsumeEvent("new-file-created")
    public void newFileCreated(FileStoreEvent fse) {
        String jarFile = fse.fileName();
        if(jarFile.endsWith(Constants.DOT_JAR)){
            Name name = fse.p().name();
            String version = fse.p().version();
            projectUpdater.sync(name, version).subscribe().with((t) -> {
                Log.info(name.displayName() + " sync done");
            });
        }
    }
    
}
