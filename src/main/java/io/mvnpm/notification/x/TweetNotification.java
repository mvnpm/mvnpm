package io.mvnpm.notification.x;
import io.mvnpm.maven.RepoNameVersionType;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Sends a notification to X (Twitter)
 * @author Phillip Kruger (phillip.kruger@gmail.com
 */
@ApplicationScoped
public class TweetNotification {
    
    @ConfigProperty(name = "mvnpm.notification.x.apikey")
    Optional<String> apiKey;
    @ConfigProperty(name = "mvnpm.notification.x.apikeysecret")
    Optional<String> apiKeySecret;
    @ConfigProperty(name = "mvnpm.notification.x.bearerToken")
    Optional<String> bearerToken;
    
    @ConsumeEvent("artifact-released-to-central")
    @Blocking
    public void artifactReleased(RepoNameVersionType repoNameVersionType) {
        String message = repoNameVersionType.nameVersionType().name().mvnGroupId() + ":" + 
                repoNameVersionType.nameVersionType().name().mvnArtifactId() + ":" +
                repoNameVersionType.nameVersionType().version() +" released.";
        
        tweet(message);    
           
    }
    
    public void tweet(String message){
        if(isConfigured()){
            try {
                // Here tweet
            }catch(Throwable t){
                t.printStackTrace();
            }
        }  
    }
    
    private boolean isConfigured(){
        return apiKey.isPresent() && apiKeySecret.isPresent() && bearerToken.isPresent();
    }
    
}
