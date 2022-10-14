package org.mavenpm.npm;

import io.smallrye.mutiny.Uni;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.mavenpm.npm.model.Project;

/**
 * The main client on https://registry.npmjs.org
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@RegisterRestClient(configKey="npm-registry")
public interface NpmRegistryClient {
    
  
    @GET
    @Path("/{project}")
    Uni<Project> getProject(@PathParam("project") String project);

    @GET
    @Path("/{project}/{version}")
    public Uni<org.mavenpm.npm.model.Package> getPackage(
            @PathParam("project") String project, 
            @PathParam("version") String version);
}
