package io.mvnpm.notification.log;

import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Log a notification in the log file
 * @author Phillip Kruger (phillip.kruger@gmail.com
 */
@ApplicationScoped
public class LogNotification {
    
    @ConsumeEvent("artifact-released-to-central")
    @Blocking
    public void artifactReleased(CentralSyncItem centralSyncItem) {
        String message = centralSyncItem.getNameVersionType().name().mvnGroupId() + ":" + 
                centralSyncItem.getNameVersionType().name().mvnArtifactId() + ":" +
                centralSyncItem.getNameVersionType().version() +" released.";
        
        Log.info(message);
    }
    
}
