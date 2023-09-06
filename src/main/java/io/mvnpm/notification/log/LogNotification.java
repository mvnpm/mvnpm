package io.mvnpm.notification.log;

import io.mvnpm.maven.RepoNameVersionType;
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
    public void artifactReleased(RepoNameVersionType repoNameVersionType) {
        String message = repoNameVersionType.nameVersionType().name().mvnGroupId() + ":" + 
                repoNameVersionType.nameVersionType().name().mvnArtifactId() + ":" +
                repoNameVersionType.nameVersionType().version() +" released.";
        
        Log.info(message);
    }
    
}
