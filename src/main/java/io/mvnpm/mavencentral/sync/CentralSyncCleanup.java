package io.mvnpm.mavencentral.sync;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;

import io.mvnpm.creator.*;
import io.mvnpm.mavencentral.SonatypeFacade;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

/**
 * Once released in central we can clean up locally and drop the repo in oss
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class CentralSyncCleanup {

    @Inject
    PackageFileLocator packageFileLocator;

    @Inject
    SonatypeFacade sonatypeFacade;

    @ConsumeEvent("central-sync-item-stage-change")
    @Blocking
    public void artifactReleased(CentralSyncItem centralSyncItem) {
        if (centralSyncItem.stage.equals(Stage.RELEASED)) {
            Log.infof("Deleting cache directory for: %s", centralSyncItem);
            Path dir = packageFileLocator.getLocalDirectory(centralSyncItem.groupId, centralSyncItem.artifactId,
                    centralSyncItem.version);
            FileUtils.deleteQuietly(dir.toFile());

            // Also make sure oss drop the repo
            sonatypeFacade.drop(centralSyncItem);
        }
    }
}
