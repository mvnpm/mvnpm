package io.mvnpm.notification.log;

import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.Stage;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

/**
 * Log a notification in the log file
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com
 */
@ApplicationScoped
public class LogNotification {

    @ConsumeEvent("central-sync-item-stage-change")
    @Blocking
    public void artifactReleased(CentralSyncItem centralSyncItem) {
        if (centralSyncItem.stage.equals(Stage.RELEASED)) {
            String message = centralSyncItem.name.mvnGroupId + ":" +
                    centralSyncItem.name.mvnArtifactId + ":" +
                    centralSyncItem.version + " released.";

            Log.info(message);
        }
    }

}
