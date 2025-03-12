package io.mvnpm.composite;

import java.util.Collection;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.NoCache;

import com.fasterxml.jackson.databind.JsonNode;

import io.mvnpm.file.metadata.MetadataClient;
import io.mvnpm.maven.exceptions.NotFoundInMavenCentralException;
import io.mvnpm.npm.model.Name;

/**
 * Some info on composites
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/api/composite")
public class CompositeApi {

    @Inject
    CompositeCreator compositeCreator;

    @Inject
    CompositeService compositeService;

    @Inject
    MetadataClient metadataClient;

    @GET
    public Collection<GitHubContent> listComposites() {
        return compositeCreator.listComposites();
    }

    @GET
    @Path("/refresh")
    @NoCache
    public void refresh() {
        compositeCreator.loadAllComposites();
    }

    @GET
    @NoCache
    @Path("/versions/{name}")
    public List<String> versions(@PathParam("name") String name) {
        Name n = new Name("@mvnpm/" + name);
        try {
            return metadataClient.getMetadata(n).getVersioning().getVersions();
        } catch (NotFoundInMavenCentralException e) {
            return List.of();
        }

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void handleWebhook(JsonNode payload) {
        JsonNode commits = payload.get("commits");
        if (commits != null) {
            compositeCreator.loadAllComposites();
        }
    }

}
