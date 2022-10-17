package org.mvnpm.maven;

import io.smallrye.mutiny.Uni;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mvnpm.npm.NpmRegistryClient;
import org.mvnpm.npm.model.Project;

/**
 * Search for projects on the NPM Registry
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * 
 * TODO: Implement this
 */
@Path("/search")
public class ProjectSearchApi {

    @RestClient 
    NpmRegistryClient extensionsService;
    
    @GET
    @Path("/{term : (.+)?}}")
    public Uni<List<Project>> search(@PathParam("term") String term) {
        return Uni.createFrom().nullItem();
    }
}