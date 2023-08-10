package org.mvnpm.centralsync;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Stack;
import org.mvnpm.npm.NpmRegistryFacade;
import org.mvnpm.npm.model.Name;
import org.mvnpm.npm.model.NameParser;
import org.mvnpm.npm.model.Project;

/**
 * This runs Continuous (on some schedule) and check if any updates for libraries we have is available, 
 * and if so, kick of a sync. Can also be triggered manually
 * 
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class ProjectUpdater {
    @Inject
    NpmRegistryFacade npmRegistryFacade;
    @Inject
    MavenCentralChecker mavenCentralChecker;
    @Inject
    CentralSyncer centralSyncer;
    @Inject
    M2Scanner m2Scanner;
    
    @Blocking
    public void update(String groupId, String artifactId){
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        update(name);
    }
    
    @Blocking
    public void update(Name name){
        Log.debug("====== mvnpm: Continuous Updater ======");
        Log.debug("\tChecking " + name.npmFullName());
        // Get latest in NPM TODO: Later make this per patch release...
        Uni<Project> project = npmRegistryFacade.getProject(name.npmFullName());
        
        Project p = project.await().atMost(Duration.ofSeconds(5));
        
        if(p!=null){
            String latest = p.distTags().latest();
            update(name, latest);
        }
    }
    
    @Blocking
    public void update(Name name, String version){
        // Check if it's in central   
        boolean isInCentral = mavenCentralChecker.isAvailable(name.mvnGroupId(), name.mvnArtifactId(), version);
        if(isInCentral){
            Log.debug("Version [" + version + "] of " + name.npmFullName() + " is in central");
        }else{
            // Kick off an update
            Log.info("Version [" + version + "] of " + name.npmFullName() + " is NOT in central. Kicking off sync...");
            centralSyncer.sync(name, version);
        }
        
    }
    
    @Scheduled(cron = "{cron.expr}")
    @Blocking
    public void updateAll(){
        Log.debug("Starting full update check...");
        // Check all know libraries
        Stack<M2Scanner.ArtifactInfo> artifacts = new Stack<>();
        artifacts.addAll(m2Scanner.scan());
        
        Log.debug("...found " + artifacts.size() + " artifacts to check");
            
        while(!artifacts.empty()) {
            M2Scanner.ArtifactInfo artifact = artifacts.pop();
            update(artifact.getGroupId(), artifact.getArtifactId());    
        }  
    }
    
}
