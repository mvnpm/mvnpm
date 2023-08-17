package org.mvnpm.mavencentral;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * The main client on https://search.maven.org
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@RegisterRestClient(configKey="search-maven")
public interface SearchMavenClient {
  
    // TODO: Change to Response
    @GET
    @Path("/solrsearch/select")
    public Uni<JsonObject> search(@QueryParam("q") String q, 
                @QueryParam("core") String core,
                @QueryParam("rows") String rows,
                @QueryParam("wt") String wt);
}

