package org.mvnpm;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.mvnpm.npm.NpmRegistryFacade;
import org.mvnpm.npm.model.Project;
import org.mvnpm.npm.model.SearchResults;

/**
 * Some info on the Project
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * TODO: Add per version info endpoint ?
 */
@Path("/info")
public class ProjectInfoApi {

    @Inject
    NpmRegistryFacade npmRegistryFacade;
    
    @GET
    @Path("/project/{project : (.+)?}")
    public Uni<Project> projectInfo(@PathParam("project") String project ) {
        return npmRegistryFacade.getProject(project);
    }
    
    @GET
    @Path("/package/{project : (.+)?}")
    public Uni<org.mvnpm.npm.model.Package> packageInfo(@PathParam("project") String project, @DefaultValue("latest") @QueryParam("version") String version ) {
        return npmRegistryFacade.getPackage(project, version);
    }
    
    @GET
    @Path("/search/{term : (.+)?}")
    public Uni<SearchResults> search(@PathParam("term") String term, @QueryParam("page") @DefaultValue("1") int page) {
        return npmRegistryFacade.search(term, page);
    }
}