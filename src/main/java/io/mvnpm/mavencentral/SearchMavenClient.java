package io.mvnpm.mavencentral;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * The main client on https://search.maven.org
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@RegisterRestClient(configKey="search-maven")
public interface SearchMavenClient {
  
    @GET
    @Path("/solrsearch/select")
    public Response search(@QueryParam("q") String q, 
                @QueryParam("core") String core,
                @QueryParam("rows") String rows,
                @QueryParam("wt") String wt);
}