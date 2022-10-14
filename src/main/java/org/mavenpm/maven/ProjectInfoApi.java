package org.mavenpm.maven;

import io.smallrye.mutiny.Uni;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mavenpm.npm.NpmRegistryClient;
import org.mavenpm.npm.model.Project;

/**
 * Some info on the NPM Project
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/npm")
public class ProjectInfoApi {

    @RestClient 
    NpmRegistryClient extensionsService;
    
    @GET
    @Path("/info/{project}")
    public Uni<Project> getProjectInfo(@PathParam("project") String project) {
        return extensionsService.getProject(project);
    }
}