package io.mvnpm.notification;

import io.mvnpm.maven.RepoNameVersionType;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationService {
    
    @ConsumeEvent("artifact-released-to-central")
    @Blocking
    public void artifactReleased(RepoNameVersionType repoNameVersionType) {
        String message = repoNameVersionType.nameVersionType().name().mvnGroupId() + ":" + 
                repoNameVersionType.nameVersionType().name().mvnArtifactId() + ":" +
                repoNameVersionType.nameVersionType().version() +" released.";
        
        this.sendMessage(message);
    }
    
    public void sendMessage(String message){
        Log.info(message);
        // TODO: Here send a mail/tweet/zulip message ?
    }

    
}
