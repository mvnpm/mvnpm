package io.mvnpm.npm;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * The main client on https://registry.npmjs.org
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@RegisterRestClient(configKey = "npm-registry")
public interface NpmRegistryClient {

    @GET
    @Path("/{project}")
    Response getProject(@PathParam("project") String project);

    @GET
    @Path("/{project}/{version}")
    Response getPackage(
            @PathParam("project") String project,
            @PathParam("version") String version);

    @GET
    @Path("/-/v1/search")
    Response search(
            @QueryParam("text") String text,
            @QueryParam("size") int size,
            @QueryParam("from") int from,
            @QueryParam("quality") double quality,
            @QueryParam("maintenance") double maintenance,
            @QueryParam("popularity") double popularity);

}
