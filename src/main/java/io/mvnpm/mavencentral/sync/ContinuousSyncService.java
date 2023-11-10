package io.mvnpm.mavencentral.sync;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import io.mvnpm.error.ErrorHandlingService;
import io.mvnpm.log.EventLogApi;
import io.mvnpm.mavencentral.PromotionException;
import io.mvnpm.mavencentral.RepoStatus;
import io.mvnpm.mavencentral.SonatypeFacade;
import io.mvnpm.mavencentral.StatusCheckException;
import io.mvnpm.mavencentral.UploadFailedException;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.mutiny.core.eventbus.EventBus;

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
    SonatypeFacade sonatypeFacade;
    @Inject
    EventBus bus;
    @Inject
    ErrorHandlingService errorHandlingService;
    @Inject
    CentralSyncItemService centralSyncItemService;
    @Inject
    EventLogApi eventLogApi;

    /**
     * Check for version updates, and if a new version is out, do a sync
     */
    public void update(String groupId, String artifactId) {
        Log.debug("====== mvnpm: Continuous Updater ======");
        Log.debug("\tChecking " + groupId + ":" + artifactId);
        if (!isInternal(groupId, artifactId)) {
            // Get latest in NPM TODO: Later make this per patch release...
            try {
                Name name = NameParser.fromMavenGA(groupId, artifactId);
                Project project = npmRegistryFacade.getProject(name.npmFullName);
                if (project != null) {
                    String latest = project.distTags().latest();
                    initializeSync(name, latest);
                }
            } catch (WebApplicationException wae) {
                Log.error("Could not do update for [" + groupId + ":" + artifactId + "] - " + wae.getMessage());
            }
        } else {
            // TODO: Handle internal compositions !
        }
    }

    /**
     * Sync a certain version of a artifact with central
     */
    public boolean initializeSync(Name name, String version) {
        return initializeSync(name.mvnGroupId, name.mvnArtifactId, version);
    }

    /**
     * Sync a certain version of a artifact with central
     */
    public boolean initializeSync(String groupId, String artifactId, String version) {
        CentralSyncItem itemToSync = centralSyncItemService.findOrCreate(groupId, artifactId, version);

        if (centralSyncService.canProcessSync(itemToSync)) { // Check if this is already synced or in progess
            itemToSync = centralSyncItemService.changeStage(itemToSync, Stage.INIT);
            return true;
        }
        return false;
    }

    /**
     * This just check if there is an artifact being uploaded, and if not change the status and fire an event
     */
    @Scheduled(every = "60s", concurrentExecution = SKIP)
    @Blocking
    void nextToUploadStatusChange() {
        // We only process one at a time, so first check that there is not another process in progress
        if (!isCurrentlyUploading()) {
            List<CentralSyncItem> initQueue = CentralSyncItem.findByStage(Stage.INIT);
            if (!initQueue.isEmpty()) {
                CentralSyncItem itemToBeUploaded = initQueue.get(0);
                if (centralSyncService.canProcessSync(itemToBeUploaded)) {
                    // Kick off an update
                    Log.debug("Version [" + itemToBeUploaded.version + "] of "
                            + itemToBeUploaded.toGavString() + " is NOT in central. Kicking off sync...");
                    itemToBeUploaded.increaseUploadAttempt();
                    itemToBeUploaded = centralSyncItemService.changeStage(itemToBeUploaded, Stage.UPLOADING);
                }
            } else {
                Log.debug("Nothing in the queue to sync");
            }
        } else {
            Log.debug("Sync upload in progress ");
        }
    }

    public boolean isCurrentlyUploading() {
        // We only process one at a time, so first check that there is not another process in progress
        long uploadingCount = CentralSyncItem.count("stage", Stage.UPLOADING);
        return uploadingCount != 0;
    }

    @Scheduled(every = "60s", concurrentExecution = SKIP)
    @Blocking
    void processSonatypeStatuses() {
        List<CentralSyncItem> uploadedToSonatype = CentralSyncItem.findNotReleased();
        if (!uploadedToSonatype.isEmpty()) {
            Map<String, CentralSyncItem> uploadedToSonatypeMap = mapByRepoId(uploadedToSonatype);
            if (!uploadedToSonatypeMap.isEmpty()) {
                try {
                    Set<Map.Entry<String, RepoStatus>> statuses = sonatypeFacade.statuses().entrySet();
                    for (Map.Entry<String, RepoStatus> statusEntry : statuses) {
                        String repoId = statusEntry.getKey();
                        if (uploadedToSonatypeMap.containsKey(repoId)) {
                            CentralSyncItem uploadedItem = uploadedToSonatypeMap.get(repoId);
                            RepoStatus status = statusEntry.getValue();
                            if (status.equals(RepoStatus.open)) {
                                uploadedItem = centralSyncItemService.changeStage(uploadedItem, Stage.UPLOADED);
                            } else if (status.equals(RepoStatus.closed)) {
                                if (!uploadedItem.stage.equals(Stage.RELEASING)) {
                                    uploadedItem = centralSyncItemService.changeStage(uploadedItem, Stage.CLOSED);
                                }
                            } else if (status.equals(RepoStatus.released)) {
                                uploadedItem = centralSyncItemService.changeStage(uploadedItem, Stage.RELEASED);
                            } else if (status.equals(RepoStatus.error)) {
                                uploadedItem = centralSyncItemService.changeStage(uploadedItem, Stage.ERROR);
                            }
                        }
                    }
                } catch (Throwable exception) {
                    // TODO ?
                    exception.printStackTrace();
                }
            }
        }
    }

    private Map<String, CentralSyncItem> mapByRepoId(List<CentralSyncItem> uploadedToSonatype) {
        Map<String, CentralSyncItem> mapByRepoId = new HashMap<>();
        for (CentralSyncItem csi : uploadedToSonatype) {
            if (csi.stagingRepoId != null && !csi.stagingRepoId.isEmpty()) {
                mapByRepoId.put(csi.stagingRepoId, csi);
            }
        }
        return mapByRepoId;
    }

    @ConsumeEvent("central-sync-item-stage-change")
    @Blocking
    public void processNextAction(CentralSyncItem centralSyncItem) {
        if (centralSyncItem.stage.equals(Stage.UPLOADING)) {
            processNextUpload(centralSyncItem);
        } else if (centralSyncItem.stage.equals(Stage.CLOSED)) {
            processRelease(centralSyncItem);
        }
    }

    private void processNextUpload(CentralSyncItem centralSyncItem) {
        if (!centralSyncService.isInMavenCentralRemoteCheck(centralSyncItem)) {
            try {
                String repoId = centralSyncService.sync(centralSyncItem);
                centralSyncItem.stagingRepoId = repoId;
                centralSyncItem = centralSyncItemService.changeStage(centralSyncItem, Stage.UPLOADED);
            } catch (UploadFailedException exception) {
                exception.printStackTrace();
                retryUpload(centralSyncItem, exception);
            } catch (UnauthorizedException unauthorizedException) {
                unauthorizedException.printStackTrace();
                errorHandlingService.handle(centralSyncItem, unauthorizedException);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                retryUpload(centralSyncItem, throwable);
            }
        }
    }

    private void processRelease(CentralSyncItem centralSyncItem) {
        try {
            centralSyncItem.increasePromotionAttempt();
            sonatypeFacade.release(centralSyncItem);
            centralSyncItemService.changeStage(centralSyncItem, Stage.RELEASING);
        } catch (PromotionException exception) {
            retryPromotion(centralSyncItem, exception);
        } catch (UnauthorizedException unauthorizedException) {
            errorHandlingService.handle(centralSyncItem, unauthorizedException);
        } catch (Throwable throwable) {
            retryPromotion(centralSyncItem, throwable);
        }
    }

    private void retryUpload(CentralSyncItem centralSyncItem, Throwable t) {
        if (centralSyncItem.uploadAttempts < 10) {
            centralSyncItem = centralSyncItemService.changeStage(centralSyncItem, Stage.INIT);
        } else {
            t.printStackTrace();
            errorHandlingService.handle(centralSyncItem, t);
            centralSyncItem = centralSyncItemService.changeStage(centralSyncItem, Stage.ERROR);
        }
    }

    private void retryPromotion(CentralSyncItem centralSyncItem, Throwable t) {
        if (centralSyncItem.promotionAttempts < 10) {
            centralSyncItem = centralSyncItemService.changeStage(centralSyncItem, Stage.UPLOADED);
        } else {
            t.printStackTrace();
            errorHandlingService.handle(centralSyncItem, t);
            centralSyncItem = centralSyncItemService.changeStage(centralSyncItem, Stage.ERROR);
        }
    }

    void onStart(@Observes StartupEvent ev) throws StatusCheckException {
        // Reset upload if the server restarts
        resetUpload();
        // Reset promotion if the server restarts
        resetPromotion();

        eventLogApi.clearLog();
    }

    private void resetUpload() {
        List<CentralSyncItem> uploading = CentralSyncItem.findByStage(Stage.UPLOADING);
        for (CentralSyncItem centralSyncItem : uploading) {
            centralSyncItem.increaseUploadAttempt();
            Log.info("Resetting upload for " + centralSyncItem + " after restart");
            centralSyncItem = centralSyncItemService.changeStage(centralSyncItem, Stage.INIT);
        }
    }

    private void resetPromotion() {
        List<CentralSyncItem> closed = CentralSyncItem.findByStage(Stage.CLOSED);
        for (CentralSyncItem centralSyncItem : closed) {
            centralSyncItem.increasePromotionAttempt();
            Log.info("Resetting promotion for " + centralSyncItem + " after restart");
            centralSyncItem = centralSyncItemService.changeStage(centralSyncItem, Stage.UPLOADED);
        }
    }

    private boolean isInternal(String groupId, String artifactId) {
        return groupId.equals("org.mvnpm.at.mvnpm") ||
                (groupId.equals("org.mvnpm.locked") && artifactId.equals("lit")) || // Failed attempt at hardcoding versions
                (groupId.equals("org.mvnpm.locked.at.vaadin") && artifactId.equals("router")) || // Failed attempt at hardcoding versions
                (groupId.equals("org.mvnpm") && artifactId.equals("vaadin-web-components")); // Before we used the @mvnpm namespave
    }

    /**
     * Check all known artifacts for updates
     */
    @Scheduled(cron = "{mvnpm.cron.expr}", concurrentExecution = SKIP)
    @Blocking
    public void checkAll() {
        try {
            Log.debug("Starting full update check...");
            List<CentralSyncItem> distinct = CentralSyncItem.findDistinctGA();

            if (distinct != null && !distinct.isEmpty()) {
                for (CentralSyncItem centralSyncItem : distinct) {
                    update(centralSyncItem.groupId, centralSyncItem.artifactId);
                }
            }
        } catch (Throwable t) {
            Log.error(t.getMessage());
        }
    }
}
