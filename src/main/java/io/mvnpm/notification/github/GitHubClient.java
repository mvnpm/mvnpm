package io.mvnpm.notification.github;

import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * The main client on api.github.com
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@RegisterRestClient(configKey="github")
public interface GitHubClient {
  
    @POST
    @Path("/graphql")
    public JsonObject graphql(@HeaderParam("Authorization") String authorization, JsonObject query);
    
}

