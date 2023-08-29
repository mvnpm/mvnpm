package io.mvnpm.mavencentral.sync;

import io.mvnpm.maven.NameVersionType;
import io.mvnpm.mavencentral.MavenFacade;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;
import jakarta.ws.rs.DELETE;
import java.util.List;
import java.util.Map;

/**
 * The central sync
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * TODO: Add security
 */
@Path("/api/sync")
public class CentralSyncApi {

    @Inject
    ContinuousSyncService continuousSyncService;
    @Inject
    CentralSyncService centralSyncService;
    @Inject
    NpmRegistryFacade npmRegistryFacade;
    @Inject
    MavenFacade mavenFacade;
    
    @GET
    @Path("/info/{groupId}/{artifactId}")
    public SyncInfo syncInfo(@PathParam("groupId") String groupId, @PathParam("artifactId") String artifactId, @DefaultValue("latest") @QueryParam("version") String version ) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        if(version.equalsIgnoreCase("latest")){
            version = getLatestVersion(name);
        }
        return centralSyncService.getSyncInfo(groupId, artifactId, version);
    }
    
    @GET
    @Path("/project/{project : (.+)?}")
    public void sync(@PathParam("project") String project ) {
        Name name = NameParser.fromNpmProject(project);
        continuousSyncService.update(name);
    }
    
    @GET
    @Path("/all")
    public void syncAll() {
        continuousSyncService.checkAll();
    }
    
    @DELETE
    @Path("/all")
    public void dropAll(){
        mavenFacade.dropStagingProfileRepos();
    }
    
    @GET
    @Path("/queue/staging")
    public List<NameVersionType> getStagingQueue() {
        return continuousSyncService.getStagingQueue();
    }
    
    @GET
    @Path("/queue/release")
    public List<Map.Entry<String,NameVersionType>> getReleaseQueue() {
        return continuousSyncService.getReleaseQueue();
    }
    
    private String getLatestVersion(Name fullName){
        Project project = npmRegistryFacade.getProject(fullName.npmFullName());
        return project.distTags().latest();
    }
    
}