package org.mvnpm.centralsync;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.mvnpm.npm.model.Name;
import org.mvnpm.npm.model.NameParser;

/**
 * The central sync
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * TODO: Add security
 */
@Path("/sync")
public class CentralSyncApi {

    @Inject
    ProjectUpdater projectUpdater;
    @Inject
    MavenCentralChecker mavenCentralChecker;
    
    @GET
    @Path("/info/{project : (.+)?}")
    public SyncInfo syncInfo(@PathParam("project") String project, @DefaultValue("latest") @QueryParam("version") String version ) {
        Name name = NameParser.fromNpmProject(project);
        boolean available = mavenCentralChecker.isAvailable(name.mvnGroupId(), name.mvnArtifactId(), version);
        return new SyncInfo(available);
    }
    
    @GET
    @Path("/project/{project : (.+)?}")
    public void sync(@PathParam("project") String project ) {
        Name name = NameParser.fromNpmProject(project);
        projectUpdater.update(name);
    }
    
    @GET
    @Path("/all")
    public void syncAll() {
        projectUpdater.updateAll();
    }
    
    // TODO: Add things to see and manage the q
    
}