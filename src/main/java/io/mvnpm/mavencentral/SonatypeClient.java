package io.mvnpm.mavencentral;

import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * The main client on https://oss.sonatype.org
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@RegisterRestClient(configKey="sonatype")
public interface SonatypeClient {
  
    @POST
    @Path("/service/local/staging/bundle_upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response uploadBundle(@HeaderParam("Authorization") String authorization, byte[] b);
    
    @GET
    @Path("/service/local/staging/repository/{stagingRepoId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadBundleStatus(@HeaderParam("Authorization") String authorization, @PathParam("stagingRepoId") String stagingRepoId);
    
    @POST
    @Path("/service/local/staging/profiles/{profileId}/finish")
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response closeUploadBundle(@HeaderParam("Authorization") String authorization, @PathParam("profileId") String profileId, JsonObject closeRequest);
    
    @POST   
    @Path("/service/local/staging/profiles/{profileId}/promote")
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Response releaseToCentral(@HeaderParam("Authorization") String authorization, @PathParam("profileId") String profileId, JsonObject promoteRequest);
    
    @GET
    @Path("/service/local/staging/profiles")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStagingProfiles(@HeaderParam("Authorization") String authorization);
    
    @GET
    @Path("/service/local/staging/profiles/{profileId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStagingProfile(@HeaderParam("Authorization") String authorization, @PathParam("profileId") String profileId);
    
    @GET
    @Path("/service/local/staging/profile_repositories/{profileId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStagingProfileRepos(@HeaderParam("Authorization") String authorization, @PathParam("profileId") String profileId);
    
    @DELETE
    @Path("/service/local/staging/deployByRepositoryId/{stagingRepoId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response dropStagingProfileRepo(@HeaderParam("Authorization") String authorization, @PathParam("stagingRepoId") String stagingRepoId);
    
}

