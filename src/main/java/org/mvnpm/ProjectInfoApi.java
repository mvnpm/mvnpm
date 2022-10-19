package org.mvnpm;

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
 * Some info on the Project
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * TODO: Add per version info endpoint ?
 */
@Path("/info")
public class ProjectInfoApi {

    @RestClient 
    NpmRegistryClient extensionsService;
    
    @GET
    @Path("/project/{project : (.+)?}")
    public Uni<Project> projectInfo(@PathParam("project") String project ) {
        return extensionsService.getProject(project);
    }
    
    @GET
    @Path("/package/{project : (.+)?}")
    public Uni<org.mvnpm.npm.model.Package> packageInfo(@PathParam("project") String project, @DefaultValue("latest") @QueryParam("version") String version ) {
        return extensionsService.getPackage(project, version);
    }
    
    @GET
    @Path("/search/{term : (.+)?}")
    public Uni<SearchResults> search(@PathParam("term") String term, @QueryParam("page") @DefaultValue("1") int page) {
        if(page<0) page = 1;
        return extensionsService.search(term, ITEMS_PER_PAGE, page - 1, 1.0, 0.0, 0.0);
    }
    
    private static final int ITEMS_PER_PAGE = 50;
}