package io.mvnpm.mavencentral;

import java.time.temporal.ChronoUnit;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.mvnpm.maven.api.MavenClient;

/**
 * The main client on https://central.sonatype.com
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@RegisterRestClient(configKey = "mavencentral")
public interface MavenCentralClient extends MavenClient {

    @POST
    @Path("/api/v1/publisher/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    @Timeout(unit = ChronoUnit.SECONDS, value = 300)
    Response uploadBundle(@HeaderParam("Authorization") String authorization, @QueryParam("name") String name,
            @QueryParam("publishingType") PublishingType publishingType, @BeanParam BundleUploadForm form);

    @POST
    @Path("/api/v1/publisher/status")
    @Produces(MediaType.APPLICATION_JSON)
    Response getReleaseStatus(@HeaderParam("Authorization") String authorization, @QueryParam("id") String releaseId);

    @GET
    @Path("/api/v1/publisher/published")
    @Produces(MediaType.APPLICATION_JSON)
    Response isPublished(@HeaderParam("Authorization") String authorization, @QueryParam("namespace") String groupId,
            @QueryParam("name") String artifactId, @QueryParam("version") String version);
}
