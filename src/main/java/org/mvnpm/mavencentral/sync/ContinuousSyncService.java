package org.mvnpm.mavencentral.sync;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.UniAndGroupIterable;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import org.mvnpm.Constants;
import org.mvnpm.file.FileStore;
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
public class ContinuousSyncService {
    @Inject
    NpmRegistryFacade npmRegistryFacade;
    @Inject
    CentralSyncService centralSyncService;
    @Inject
    FileStore fileStore;
    
    /**
     * Check for version updates, and if a new version is out, do a sync
     */
    public Uni<Void> update(String groupId, String artifactId){
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        return update(name);
    }
    
    /**
     * Check for version updates, and if a new version is out, do a sync
     */
    public Uni<Void> update(Name name){
        Log.info("====== mvnpm: Continuous Updater ======");
        Log.info("\tChecking " + name.npmFullName());
        // Get latest in NPM TODO: Later make this per patch release...
        Uni<Project> project = npmRegistryFacade.getProject(name.npmFullName());
        
        return project.onItem().transformToUni((p) -> {
            if(p!=null){
                String latest = p.distTags().latest();
                return sync(name, latest);
            }
            return Uni.createFrom().nothing();
        });
    }
   
    /**
     * Sync a certain version of a artifact with central
     */
    public Uni<Void> sync(Name name, String version){
        
        Uni<SyncInfo> syncInfo = centralSyncService.getSyncInfo(name.mvnGroupId(), name.mvnArtifactId(), version);
        
        return syncInfo.onItem().transformToUni((t) -> {
             if(t.canSync()){
                // Kick off an update
                Log.info("Version [" + version + "] of " + name.npmFullName() + " is NOT in central. Kicking off sync...");
                return centralSyncService.sync(name, version);
            }else {
                Log.info("Version [" + version + "] of " + name.npmFullName() + " already synced");
            }
            return Uni.createFrom().voidItem();
        });
    }
    
    /**
     * Check all known artifacts for updates
     */
    @Scheduled(cron = "{mvnpm.cron.expr}")
    public Uni<Void> checkAll(){
        Log.info("Starting full update check...");
        Uni<List<String[]>> gavsToUpdate = findGaToUpdate();
        
        return gavsToUpdate.onItem().transformToUni((gas) -> {
            List<Uni<Void>> allUpdates = new ArrayList<>();
            for(String[]ga:gas){
                Uni<Void> current = update(ga[0], ga[1]);
                allUpdates.add(current);
            }
            
            return Uni.combine()
                .all().unis(allUpdates).discardItems();
            
        });
        
    }
    
    public Uni<List<String[]>> findGaToUpdate(){
        Uni<List<String>> artifactRoots = fileStore.getArtifactRoots();
        return artifactRoots.onItem().transform((dirs) -> {
            return dirs.stream().map(this::toGroupArtifact)
                    .filter(this::filterInternal)
                    .collect(Collectors.toUnmodifiableList());
        });
    }

    // Filter out our internal apps
    private boolean filterInternal(String[] ga){
        return !shouldIgnore(ga[0], ga[1]);
    }
    
    private String[] toGroupArtifact(String dir) {
        int i = dir.indexOf(Constants.SLASH_ORG_SLASH_MVNPM_SLASH);
        dir = dir.substring(i + 1);
        String[] parts = dir.split(Constants.SLASH);
        int l = parts.length;
        String artifactId = parts[l-1];
        String[] groupParts = Arrays.copyOf(parts, l-1);
        String groupId = String.join(".", groupParts);
        return new String[]{groupId, artifactId};
    }
    
    private boolean shouldIgnore(String groupId,String artifactId){
        return groupId.equals(Constants.ORG_DOT_MVNPM) && IGNORE_LIST.contains(artifactId);
    }
    
    private static final List<String> IGNORE_LIST = List.of("mvnpm","importmap"); // Our internal apps, TODO: Move to config ?
}
