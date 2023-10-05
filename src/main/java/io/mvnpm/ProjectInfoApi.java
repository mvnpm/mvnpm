package io.mvnpm;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.SearchResults;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Some info on the Project
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * TODO: Add per version info endpoint ?
 */
@Path("/api/info")
public class ProjectInfoApi {

    @Inject
    NpmRegistryFacade npmRegistryFacade;
    
    @GET
    @Path("/project/{project : (.+)?}")
    public Response projectInfo(@PathParam("project") String project ) {
        try {
            return Response.ok(npmRegistryFacade.getProject(project)).build();
        } catch (WebApplicationException wae){
            return wae.getResponse();
        } catch(Throwable t){
            return Response.serverError().header("reason", t.getMessage()).build();
        }
    }
    
    @GET
    @Path("/package/{project : (.+)?}")
    public io.mvnpm.npm.model.Package packageInfo(@PathParam("project") String project, @DefaultValue("latest") @QueryParam("version") String version ) {
        return npmRegistryFacade.getPackage(project, version);
    }
    
    @GET
    @Path("/search/{term : (.+)?}")
    public SearchResults search(@PathParam("term") String term, @QueryParam("page") @DefaultValue("1") int page) {
        return npmRegistryFacade.search(term, page);
    }
}