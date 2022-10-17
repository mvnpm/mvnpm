package org.mvnpm.maven;

import io.smallrye.mutiny.Uni;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mvnpm.npm.NpmRegistryClient;
import org.mvnpm.npm.model.Project;
import org.mvnpm.npm.model.SearchResults;

/**
 * Some info on the NPM Project
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/info")
public class ProjectInfoApi {
    private static final int ITEMS_PER_PAGE = 50;

    @RestClient 
    NpmRegistryClient extensionsService;
    
    @GET
    @Path("/project/{project : (.+)?}")
    public Uni<Project> project(@PathParam("project") String project) {
        return extensionsService.getProject(project);
    }
    
    @GET
    @Path("/search/{term : (.+)?}")
    public Uni<SearchResults> search(@PathParam("term") String term, @QueryParam("page") @DefaultValue("1") int page) {
        if(page<0) page = 1;
        return extensionsService.search(term, ITEMS_PER_PAGE, page - 1, 1.0, 0.0, 0.0);
    }
}