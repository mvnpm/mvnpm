package org.mvnpm.npm;

import io.smallrye.mutiny.Uni;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.mvnpm.npm.model.Project;
import org.mvnpm.npm.model.SearchResults;

/**
 * The main client on https://registry.npmjs.org
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@RegisterRestClient(configKey="npm-registry")
public interface NpmRegistryClient {
  
    @GET
    @Path("/{project}")
    Uni<Project> getProject(@PathParam("project") String project);

    @GET
    @Path("/{project}/{version}")
    Uni<org.mvnpm.npm.model.Package> getPackage(
            @PathParam("project") String project, 
            @PathParam("version") String version);
    
    @GET
    @Path("/-/v1/search")
    Uni<SearchResults> search(
            @QueryParam("text") String text, 
            @QueryParam("size") int size,
            @QueryParam("from") int from,
            @QueryParam("quality") double quality,
            @QueryParam("maintenance") double maintenance,
            @QueryParam("popularity") double popularity);
    
}
