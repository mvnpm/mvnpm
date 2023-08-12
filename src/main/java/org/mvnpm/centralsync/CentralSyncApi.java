package org.mvnpm.centralsync;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.time.Duration;
import org.mvnpm.npm.NpmRegistryFacade;
import org.mvnpm.npm.model.Name;
import org.mvnpm.npm.model.NameParser;
import org.mvnpm.npm.model.Project;

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
    @Inject
    NpmRegistryFacade npmRegistryFacade;
    
    @GET
    @Path("/info/{groupId}/{artifactId}")
    public SyncInfo syncInfo(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @DefaultValue("latest") @QueryParam("version") String version ) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        if(version.equalsIgnoreCase("latest")){
            version = getLatestVersion(name);
        }
        
        boolean inCentral = mavenCentralChecker.isAvailable(name.mvnGroupId(), name.mvnArtifactId(), version);
        boolean inStaging = mavenCentralChecker.isStaged(name.mvnGroupId(), name.mvnArtifactId(), version);
        return new SyncInfo(inCentral, inStaging);
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
    
    private String getLatestVersion(Name fullName){
        Uni<Project> project = npmRegistryFacade.getProject(fullName.npmFullName());
        return project.onItem()
                .transform((p) -> {
                    return p.distTags().latest();
                }).await().atMost(Duration.ofSeconds(30));
    }
    
    // TODO: Add things to see and manage the q
    
}