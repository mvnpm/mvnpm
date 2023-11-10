package io.mvnpm;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;

import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
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

    @GET
    @Path("/npm/{groupId}/{artifactId}")
    public Response toNpmRegistry(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId,
            @DefaultValue("latest") @QueryParam("version") String version) {

        try {
            Name name = NameParser.fromMavenGA(groupId, artifactId);
            String url = "https://www.npmjs.com/package/" + name.npmFullName;

            if (version != null && !version.isBlank() && !version.equals("latest")) {
                url = url + "/v/" + version;
            }
            URI externalUri = new URI(url);
            return Response.seeOther(externalUri).build();
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

}
