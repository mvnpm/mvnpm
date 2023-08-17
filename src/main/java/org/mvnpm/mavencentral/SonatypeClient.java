package org.mvnpm.mavencentral;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
    public Uni<Response> uploadBundle(@HeaderParam("Authorization") String authorization, byte[] b);
}

