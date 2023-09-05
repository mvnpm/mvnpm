package io.mvnpm.ui;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/ui")
public class WebApi {
    
    @ConfigProperty(name = "quarkus.application.version")
    private String version;

    @GET
    @Path("/version")
    public String getVersion(){
        return version;
    }
}
