package io.mvnpm.mavencentral;

import java.time.temporal.ChronoUnit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.vertx.core.json.JsonObject;

/**
 * The main client on https://oss.sonatype.org
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@RegisterRestClient(configKey = "sonatype")
public interface SonatypeClient {

    @POST
    @Path("/service/local/staging/bundle_upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Timeout(unit = ChronoUnit.SECONDS, value = 300)
    public Response uploadBundle(@HeaderParam("Authorization") String authorization, java.nio.file.Path path);

    @GET
    @Path("/service/local/staging/repository/{stagingRepoId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadBundleStatus(@HeaderParam("Authorization") String authorization,
            @PathParam("stagingRepoId") String stagingRepoId);

    @GET
    @Path("/service/local/staging/profile_repositories")
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadBundleStatuses(@HeaderParam("Authorization") String authorization);

    @POST
    @Path("/service/local/staging/profiles/{profileId}/promote")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response releaseToCentral(@HeaderParam("Authorization") String authorization,
            @PathParam("profileId") String profileId, JsonObject promoteRequest);

    @POST
    @Path("/service/local/staging/profiles/{profileId}/drop")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response drop(@HeaderParam("Authorization") String authorization,
            @PathParam("profileId") String profileId, JsonObject promoteRequest);

    @GET
    @Path("/service/local/staging/profile_repositories/{profileId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStagingProfileRepos(@HeaderParam("Authorization") String authorization,
            @PathParam("profileId") String profileId);

    @GET
    @Path("/service/local/lucene/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@HeaderParam("Authorization") String authorization,
            @QueryParam("g") String g,
            @QueryParam("a") String a,
            @QueryParam("v") String v);

}
