package io.mvnpm;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;

import org.jboss.resteasy.reactive.RestResponse;

import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Project;
import io.mvnpm.npm.model.SearchResults;

/**
 * Some info on the Project
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 *         TODO: Add per version info endpoint ?
 */
@Path("/api/info")
public class ProjectInfoApi {

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    @GET
    @Path("/project/{project : (.+)?}")
    public RestResponse<Project> projectInfo(@PathParam("project") String project) {
        try {
            return RestResponse.ok(npmRegistryFacade.getProject(project));
        } catch (WebApplicationException wae) {
            return RestResponse.status(wae.getResponse().getStatus(), wae.getResponse().getStatusInfo().getReasonPhrase());
        } catch (Throwable t) {
            return RestResponse.status(500, t.getMessage());
        }
    }

    @GET
    @Path("/package/{project : (.+)?}")
    public io.mvnpm.npm.model.Package packageInfo(@PathParam("project") String project,
            @DefaultValue("latest") @QueryParam("version") String version) {
        return npmRegistryFacade.getPackage(project, version);
    }

    @GET
    @Path("/search/{term : (.+)?}")
    public SearchResults search(@PathParam("term") String term, @QueryParam("page") @DefaultValue("1") int page) {
        return npmRegistryFacade.search(term, page);
    }
}
