package io.mvnpm.mavencentral.sync;

import jakarta.enterprise.context.ApplicationScoped;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import io.mvnpm.Constants;
import io.mvnpm.file.FileStore;
import io.mvnpm.mavencentral.SonatypeFacade;
import io.mvnpm.mavencentral.RepoStatus;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;
import io.smallrye.common.annotation.Blocking;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 * This runs Continuous (on some schedule) and check if any updates for libraries we have is available, 
 * and if so, kick of a sync. Can also be triggered manually
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
    @Inject
    SonatypeFacade sonatypeFacade;
    @Inject
    EventBus bus;
    
    private final ConcurrentLinkedQueue<CentralSyncItem> inProgressQueue = new ConcurrentLinkedQueue<>(); // Total process queue 
    private final ConcurrentLinkedQueue<CentralSyncItem> initQueue = new ConcurrentLinkedQueue<>(); 
    private final ConcurrentLinkedQueue<CentralSyncItem> closedQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<CentralSyncItem> releaseQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<CentralSyncItem> releasedQueue = new ConcurrentLinkedQueue<>();
    
    private Optional<CentralSyncItem> uploadInProgress = Optional.empty();
    
    public List<CentralSyncItem> getInitQueue(){
        return new ArrayList<>(initQueue);
    }
    
    public List<CentralSyncItem> getClosedQueue(){
        return new ArrayList<>(closedQueue);
    }
    
    public List<CentralSyncItem> getReleaseQueue(){
        return new ArrayList<>(releaseQueue);
    }
    
    public List<CentralSyncItem> getReleasedQueue(){
        return new ArrayList<>(releasedQueue);
    }
    
    public boolean uploadInProgress(){
        return uploadInProgress.isPresent();
    }
    
    public List<CentralSyncItem> getUploadInProgress(){
        return uploadInProgress.stream().collect(Collectors.toList());
    }
    
    /**
     * Check for version updates, and if a new version is out, do a sync
     */
    public void update(String groupId, String artifactId){
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        update(name);
    }
    
    /**
     * Check for version updates, and if a new version is out, do a sync
     */
    public void update(Name name){
        Log.debug("====== mvnpm: Continuous Updater ======");
        Log.debug("\tChecking " + name.npmFullName());
        if(!isInternal(name)){
            // Get latest in NPM TODO: Later make this per patch release...
            Project project = npmRegistryFacade.getProject(name.npmFullName());
            if(project!=null){
                String latest = project.distTags().latest();
                initializeSync(name, latest);
            }
        } else {
            Log.warn("TODO: !!!!!!! Handle internal " + name.mvnGroupId() + ":" + name.mvnArtifactId());
        }
    }
   
    /**
     * Sync a certain version of a artifact with central
     */
    public boolean initializeSync(Name name, String version){
        
        CentralSyncItem itemInQ = new CentralSyncItem (name, version);
        SyncInfo syncInfo = centralSyncService.getSyncInfo(name.mvnGroupId(), name.mvnArtifactId(), version);
        if(syncInfo.canSync()){ // Check if this is already synced
            if(!inProgressQueue.contains(itemInQ)){ // Already somewhere in the process
                inProgressQueue.add(itemInQ);
                initQueue.add(itemInQ);
                bus.publish("central-sync-item-stage-change", itemInQ);
                return true;
            }
        }
        return false;
    }
    
    @Scheduled(every="10s")
    void processInitQueue() {
        if(uploadInProgress.isEmpty()){ // We upload one at a time
            if(!initQueue.isEmpty()){
                CentralSyncItem centralSyncItem = initQueue.remove();
                uploadInProgress = Optional.of(centralSyncItem);
                try {
                    String repoId = processUpload(centralSyncItem);
                    if(repoId!=null){
                        centralSyncItem.setStagingRepoId(repoId);
                        centralSyncItem.setStage(Stage.UPLOADED);
                        closedQueue.add(centralSyncItem);
                        bus.publish("central-sync-item-stage-change", centralSyncItem);
                    }
                }finally {
                    uploadInProgress = Optional.empty();
                }
            }else{
                Log.debug("Nothing in the queue to sync");
            }
        }else{
            Log.debug("Sync upload in progress " + uploadInProgress.get().getNameVersionType().name().displayName() + " " + uploadInProgress.get().getNameVersionType().version());
        }
    }
    
    private String processUpload(CentralSyncItem centralSyncItem){
        SyncInfo syncInfo = centralSyncService.getSyncInfo(centralSyncItem.getNameVersionType().name().mvnGroupId(), centralSyncItem.getNameVersionType().name().mvnArtifactId(), centralSyncItem.getNameVersionType().version());
        if(syncInfo.canSync()){
            // Kick off an update
            Log.debug("Version [" + centralSyncItem.getNameVersionType().version() + "] of " + centralSyncItem.getNameVersionType().name().npmFullName() + " is NOT in central. Kicking off sync...");
            centralSyncItem.setStage(Stage.UPLOADING);
            bus.publish("central-sync-item-stage-change", centralSyncItem);
            return centralSyncService.sync(centralSyncItem.getNameVersionType().name(), centralSyncItem.getNameVersionType().version());
        }else {
            Log.debug("Version [" + centralSyncItem.getNameVersionType().version() + "] of " + centralSyncItem.getNameVersionType().name().npmFullName() + " already synced");
            return null;
        }
    }
    
    @Scheduled(every="10s")
    void processClosedQueue() {
        if(!closedQueue.isEmpty()){
            CentralSyncItem centralSyncItem = closedQueue.remove();
            RepoStatus status = sonatypeFacade.status(centralSyncItem.getStagingRepoId());
            if(status.equals(RepoStatus.closed)){
                centralSyncItem.setStage(Stage.CLOSED);
                releaseQueue.add(centralSyncItem);
                bus.publish("central-sync-item-stage-change", centralSyncItem);
            }else{
                closedQueue.add(centralSyncItem);
            }
        }
    }
    
    @Scheduled(every="10s")
    void processReleaseQueue() {
        if(!releaseQueue.isEmpty()){
            CentralSyncItem centralSyncItem = releaseQueue.remove();
            boolean released = sonatypeFacade.release(centralSyncItem.getStagingRepoId());
            if(released){
                centralSyncItem.setStage(Stage.RELEASING);
                releasedQueue.add(centralSyncItem);
                bus.publish("central-sync-item-stage-change", centralSyncItem);
            }else{
                // Try again
                releaseQueue.add(centralSyncItem);
            }
        }
    }
    
    @Scheduled(every="10s")
    void processReleasedQueue() {
        if(!releasedQueue.isEmpty()){
            CentralSyncItem centralSyncItem = releasedQueue.remove();
            RepoStatus status = sonatypeFacade.status(centralSyncItem.getStagingRepoId());
            if(status !=null && status.equals(RepoStatus.released)){
                centralSyncItem.setStage(Stage.RELEASED);
                inProgressQueue.remove(centralSyncItem);
                bus.publish("artifact-released-to-central", centralSyncItem);
                bus.publish("central-sync-item-stage-change", centralSyncItem);
            }else if(status !=null){
                releasedQueue.add(centralSyncItem);
            }
        }
    }
    
    private boolean isInternal(Name name){
        return name.mvnGroupId().equals("org.mvnpm.at.mvnpm");
    }
    
    /**
     * Check all known artifacts for updates
     */
    @Scheduled(cron = "{mvnpm.cron.expr}", concurrentExecution = SKIP)
    @Blocking
    public void checkAll(){
        try {
            Log.debug("Starting full update check...");
            List<String[]> gavsToUpdate = findGaToUpdate();
        
            for(String[]ga:gavsToUpdate){
                update(ga[0], ga[1]);
            }
        }catch(Throwable t){
            Log.error(t.getMessage());
        }
    }
    
    public List<String[]> findGaToUpdate(){
        List<Path> artifactRoots = fileStore.getArtifactRoots();
        return artifactRoots.stream().map(this::toGroupArtifact)
                    .collect(Collectors.toUnmodifiableList());
        
    }

    private String[] toGroupArtifact(Path p) {
        String dir = p.toString();
        int i = dir.indexOf(Constants.SLASH_ORG_SLASH_MVNPM_SLASH);
        dir = dir.substring(i + 1);
        String[] parts = dir.split(Constants.SLASH);
        int l = parts.length;
        String artifactId = parts[l-1];
        String[] groupParts = Arrays.copyOf(parts, l-1);
        String groupId = String.join(".", groupParts);
        return new String[]{groupId, artifactId};
    }
    
}
