package io.mvnpm.notification.github;

import io.mvnpm.maven.RepoNameVersionType;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Sends a message to the GitHub discussion
 * @author Phillip Kruger (phillip.kruger@gmail.com
 */
@ApplicationScoped
public class GitHubAnnouncement {
    
    @RestClient
    GitHubClient gitHubClient; 
    
    @ConfigProperty(name = "mvnpm.notification.github.token")
    Optional<String> token;
    
    @ConfigProperty(name = "mvnpm.notification.github.repositoryId", defaultValue = "R_kgDOIL8NhQ")
    String repositoryId;
    
    @ConfigProperty(name = "mvnpm.notification.github.categoryId", defaultValue = "DIC_kwDOIL8Nhc4CYqTN")
    String categoryId;
    
    @ConsumeEvent("artifact-released-to-central")
    @Blocking
    public void artifactReleased(RepoNameVersionType repoNameVersionType) {
        if(token.isPresent()){
            
            String groupId = repoNameVersionType.nameVersionType().name().mvnGroupId();
            String artifactId = repoNameVersionType.nameVersionType().name().mvnArtifactId();
            String version = repoNameVersionType.nameVersionType().version();
            String npmName = repoNameVersionType.nameVersionType().name().npmFullName();
            String repo = repoNameVersionType.stagingRepoId();
            
            String title = groupId + ":" + artifactId + ":" + version;
            
            String body = BODY.formatted(groupId, artifactId, version, npmName, repo);
            
            String a = "Bearer " + token.get();
            String query = ANNOUNCE_MUTATION.formatted(repositoryId, categoryId, body, title);
            gitHubClient.graphql(a, new JsonObject().put("query", query));
        } 
    }
    
    
    private static final String BODY = """
                                       mvnpm.org has automatically released the following artifact: 
                                       Group Id: %s
                                       Artifact Id: %s
                                       Version: %s
                                       
                                       This represent the NPM Package %s
                                       
                                       Release has been done using the %s staging repo
                                       """;
    
    private static final String ANNOUNCE_MUTATION = """
                            mutation CreateAnnoucement {
                                createDiscussion(input: {repositoryId: "%s", categoryId: "%s", body: "%s", title: "%s"}) {
                                    discussion {
                                        body
                                    }
                                }
                            }""";
    
}
