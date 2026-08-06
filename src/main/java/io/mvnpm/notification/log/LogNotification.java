package io.mvnpm.notification.log;

import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.maven.api.Stage;
import io.mvnpm.maven.sync.SyncItem;
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

    @ConsumeEvent("sync-item-stage-change")
    @Blocking
    public void artifactReleased(SyncItem syncItem) {
        if (syncItem.stage.equals(Stage.RELEASED)) {
            String message = syncItem.toGavString() + " released.";
            Log.info(message);
        }
    }

}
