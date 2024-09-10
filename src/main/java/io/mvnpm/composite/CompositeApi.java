package io.mvnpm.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.NoCache;

import com.fasterxml.jackson.databind.JsonNode;

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
        Map<String, Date> versions = compositeService.getVersions(n);
        if (versions == null || versions.isEmpty()) {
            return List.of();
        } else {
            return new ArrayList<>(versions.keySet());
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
