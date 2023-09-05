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
import io.mvnpm.maven.NameVersionType;
import io.mvnpm.maven.RepoNameVersionType;
import io.mvnpm.mavencentral.MavenFacade;
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
    MavenFacade mavenFacade;
    @Inject
    EventBus bus;
    
    private final ConcurrentLinkedQueue<NameVersionType> inProgressQueue = new ConcurrentLinkedQueue<>(); // Total process queue 
    private final ConcurrentLinkedQueue<NameVersionType> uploadQueue = new ConcurrentLinkedQueue<>(); 
    private final ConcurrentLinkedQueue<RepoNameVersionType> closedQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<RepoNameVersionType> releaseQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<RepoNameVersionType> releasedQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<RepoNameVersionType> dropQueue = new ConcurrentLinkedQueue<>();
    
    private Optional<NameVersionType> uploadInProgress = Optional.empty();
    
    public List<RepoNameVersionType> getReleaseQueue(){
        return new ArrayList<>(releaseQueue);
    }
    
    public List<NameVersionType> getStagingQueue(){
        return new ArrayList<>(uploadQueue);
    }
    
    public boolean uploadInProgress(){
        return uploadInProgress.isPresent();
    }
    
    public NameVersionType getUploadInProgress(){
        return uploadInProgress.orElse(null);
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
                addToSyncQueue(name, latest);
            }
        } else {
            Log.warn("TODO: !!!!!!! Handle internal " + name.mvnGroupId() + ":" + name.mvnArtifactId());
        }
    }
   
    /**
     * Sync a certain version of a artifact with central
     */
    public boolean addToSyncQueue(Name name, String version){
        NameVersionType itemInQ = new NameVersionType(name, version);
        SyncInfo syncInfo = centralSyncService.getSyncInfo(name.mvnGroupId(), name.mvnArtifactId(), version);
        if(syncInfo.canSync()){ // Check if this is already synced
            if(!inProgressQueue.contains(itemInQ)){ // Already queued
                inProgressQueue.add(itemInQ);
                uploadQueue.add(itemInQ);
                return true;
            }
        }
        return false;
    }
    
    @Scheduled(every="10s")
    void processUploadQueue() {
        if(uploadInProgress.isEmpty()){
            if(!uploadQueue.isEmpty()){
                NameVersionType nav = uploadQueue.remove();
                uploadInProgress = Optional.of(nav);
                try {
                    String repoId = dequeueStagingUploads(nav.name(), nav.version());
                    if(repoId!=null){
                        closedQueue.add(new RepoNameVersionType(repoId, nav));
                    }
                }finally {
                    uploadInProgress = Optional.empty();
                }
            }else{
                Log.debug("Nothing in the queue to sync");
            }
        }else{
            Log.debug("Sync upload in progress " + uploadInProgress.get().name().displayName() + " " + uploadInProgress.get().version());
        }
    }
    
    @Scheduled(every="10s")
    void processClosedQueue() {
        if(!closedQueue.isEmpty()){
            RepoNameVersionType repoNameVersionType = closedQueue.remove();
            RepoStatus status = mavenFacade.status(repoNameVersionType.stagingRepoId());
            if(status.equals(RepoStatus.closed)){
                releaseQueue.add(repoNameVersionType);
            }else{
                closedQueue.add(repoNameVersionType);
            }
        }
    }
    
    @Scheduled(every="10s")
    void processReleaseQueue() {
        if(!releaseQueue.isEmpty()){
            RepoNameVersionType repoNameVersionType = releaseQueue.remove();
            boolean released = mavenFacade.release(repoNameVersionType.stagingRepoId());
            if(released){
                releasedQueue.add(repoNameVersionType);
            }else{
                // Try again
                releaseQueue.add(repoNameVersionType);
            }
        }
    }
    
    @Scheduled(every="10s")
    void processReleasedQueue() {
        if(!releasedQueue.isEmpty()){
            RepoNameVersionType repoNameVersionType = releasedQueue.remove();
            RepoStatus status = mavenFacade.status(repoNameVersionType.stagingRepoId());
            if(status.equals(RepoStatus.released)){
                dropQueue.add(repoNameVersionType);
                bus.publish("artifact-released-to-central", repoNameVersionType);
            }else{
                releasedQueue.add(repoNameVersionType);
            }
        }
    }
    
    @Scheduled(every="10s")
    void processDropQueue() {
        if(!dropQueue.isEmpty()){
            RepoNameVersionType repoNameVersionType = dropQueue.remove();
            boolean droped = mavenFacade.drop(repoNameVersionType.stagingRepoId());
            if(droped){
                inProgressQueue.remove(repoNameVersionType.nameVersionType());
            }else{
                // Try again
                dropQueue.add(repoNameVersionType);                
            }
        }
    }
    
    private boolean isInternal(Name name){
        return name.mvnGroupId().equals("org.mvnpm.at.mvnpm");
    }
    
    private String dequeueStagingUploads(Name name, String version){
        SyncInfo syncInfo = centralSyncService.getSyncInfo(name.mvnGroupId(), name.mvnArtifactId(), version);
        if(syncInfo.canSync()){
            // Kick off an update
            Log.debug("Version [" + version + "] of " + name.npmFullName() + " is NOT in central. Kicking off sync...");
            return centralSyncService.sync(name, version);
        }else {
            Log.debug("Version [" + version + "] of " + name.npmFullName() + " already synced");
            return null;
        }
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
