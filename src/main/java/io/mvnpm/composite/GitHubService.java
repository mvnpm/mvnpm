package io.mvnpm.composite;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@Path("/repos/{owner}/{repo}/contents/{path}")
@RegisterRestClient(configKey = "github")
public interface GitHubService {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    //TODO add cache
    Uni<List<GitHubContent>> getContents(@PathParam("owner") String owner,
            @PathParam("repo") String repo,
            @PathParam("path") String path);
}
