package io.mvnpm.mavencentral.sync;

import static io.mvnpm.mavencentral.ReleaseStatus.VALIDATED;
import static io.mvnpm.mavencentral.ReleaseStatus.VALIDATING;
import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

import org.apache.commons.io.FileUtils;

import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageCreator;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.creator.PackageListener;
import io.mvnpm.creator.composite.CompositeCreator;
import io.mvnpm.creator.events.DependencyVersionCheckRequest;
import io.mvnpm.creator.utils.FileUtil;
import io.mvnpm.error.ErrorHandlingService;
import io.mvnpm.maven.MavenCentralService;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.mvnpm.mavencentral.MavenCentralFacade;
import io.mvnpm.mavencentral.ReleaseStatus;
import io.mvnpm.mavencentral.exceptions.MissingFilesForBundleException;
import io.mvnpm.mavencentral.exceptions.StatusCheckException;
import io.mvnpm.mavencentral.exceptions.UploadFailedException;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.exceptions.GetPackageException;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;
import io.mvnpm.version.InvalidVersionException;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;

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
    MavenCentralFacade mavenCentralFacade;

    @Inject
    ErrorHandlingService errorHandlingService;

    @Inject
    CentralSyncItemService centralSyncItemService;

    @Inject
    PackageFileLocator packageFileLocator;

    @Inject
    CompositeCreator compositeCreator;

    @Inject
    PackageCreator packageCreator;

    @Inject
    MavenRepositoryService mavenRepositoryService;

    @Inject
    PackageListener packageListener;

    @Inject
    MavenCentralService mavenCentralService;

    @Inject
    io.vertx.mutiny.core.eventbus.EventBus bus;

    @Scheduled(cron = "{mvnpm.checkerror.cron.expr}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    public void checkError() {
        try {
            Log.debug("Starting error retry...");
            CentralSyncItem item;
            int count = 0;
            while ((item = centralSyncItemService.claimNextForErrorRetry()) != null && count < 10) {
                bus.publish("central-sync-item-stage-change", item);
                count++;
            }
        } catch (Throwable t) {
            Log.error(t.getMessage());
        }
    }

    /**
     * Check a batch of synced packages for updates using adaptive scheduling.
     * Replaces the old checkAll which loaded all rows into memory.
     */
    @Scheduled(every = "${mvnpm.check-all.every:10m}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    public void checkAll() {
        try {
            List<SyncedPackage> batch = claimBatchToCheck(10);
            if (batch.isEmpty()) {
                Log.debug("No packages due for update check");
                return;
            }
            Log.infof("Checking %d packages for updates", batch.size());
            for (SyncedPackage pkg : batch) {
                LocalDateTime nextCheck = checkAndComputeNextCheck(pkg);
                updateNextCheck(pkg, nextCheck);
            }
        } catch (Throwable t) {
            Log.error("Error during batch update check: " + t.getMessage());
        }
    }

    @Transactional
    List<SyncedPackage> claimBatchToCheck(int batchSize) {
        LocalDateTime claimUntil = LocalDateTime.now().plusHours(1);
        int claimed = SyncedPackage.claimBatch(batchSize, claimUntil);
        if (claimed == 0) {
            return List.of();
        }
        return SyncedPackage.findClaimed(claimUntil);
    }

    private LocalDateTime checkAndComputeNextCheck(SyncedPackage pkg) {
        try {
            update(pkg.groupId, pkg.artifactId);
            return computeNextCheck(pkg.groupId, pkg.artifactId);
        } catch (Throwable t) {
            Log.warnf("Error checking %s: %s", pkg.toGaString(), t.getMessage());
            // On error, retry in 1 hour
            return LocalDateTime.now().plusHours(1);
        }
    }

    private LocalDateTime computeNextCheck(String groupId, String artifactId) {
        try {
            if (isInternal(groupId, artifactId)) {
                return LocalDateTime.now().plusDays(1);
            }
            Name name = NameParser.fromMavenGA(groupId, artifactId);
            Project project = npmRegistryFacade.getProject(name.npmFullName);
            if (project != null && project.time() != null) {
                String modified = project.time().get("modified");
                if (modified != null) {
                    Instant lastModified = Instant.parse(modified);
                    long ageDays = Duration.between(lastModified, Instant.now()).toDays();
                    return LocalDateTime.now().plus(nextCheckInterval(ageDays));
                }
            }
        } catch (Exception e) {
            Log.debugf("Could not determine publish date for %s:%s, using default interval", groupId, artifactId);
        }
        return LocalDateTime.now().plusDays(1);
    }

    static Duration nextCheckInterval(long ageDays) {
        if (ageDays < 7) {
            return Duration.ofHours(4);
        } else if (ageDays < 30) {
            return Duration.ofHours(12);
        } else if (ageDays < 180) {
            return Duration.ofDays(1);
        } else if (ageDays < 1825) {
            return Duration.ofDays(3);
        } else {
            return Duration.ofDays(30);
        }
    }

    @Transactional
    void updateNextCheck(SyncedPackage pkg, LocalDateTime nextCheck) {
        SyncedPackage managed = SyncedPackage.findById(new SyncedPackageId(pkg.groupId, pkg.artifactId));
        if (managed != null) {
            managed.nextCheck = nextCheck;
            managed.persist();
        }
    }

    /**
     * This check is to auto-sync matching dependencies on existing packages
     */
    @Scheduled(every = "${mvnpm.check-versions.every:5m}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    void checkVersions() {
        final List<CentralSyncItem> byStage = CentralSyncItem.findPackageWithUncheckedDependencies(1);
        if (!byStage.isEmpty()) {
            for (CentralSyncItem item : byStage) {
                final Name name = NameParser.fromMavenGA(item.groupId, item.artifactId);
                final String gavString = name.toGavString(item.version);
                try {
                    Log.infof("Checking versions for %s", gavString);
                    final Path pom = mavenCentralService.downloadFromMavenCentral(name, item.version, FileType.pom);
                    mavenRepositoryService.checkDependencies(new DependencyVersionCheckRequest(pom, name, item.version))
                            .await().atMost(Duration.ofHours(1));
                } catch (Exception e) {
                    Log.warnf("Error while checking versions for %s because: %s", gavString, e.getMessage());
                }
            }
        }
    }

    /**
     * This just check if there is an artifact is stuck at packaging
     */
    @Scheduled(every = "${mvnpm.check-packaging.every:60s}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    void checkPackaging() {
        CentralSyncItem itemToBeCreated = centralSyncItemService.claimNextForPackagingCheck();
        if (itemToBeCreated != null) {
            if (centralSyncService.canProcessSync(itemToBeCreated)) {
                final Name name = NameParser.fromMavenGA(itemToBeCreated.groupId, itemToBeCreated.artifactId);
                try {
                    final Path jar = packageCreator.getFromCacheOrCreate(FileType.jar, name, itemToBeCreated.version);
                    if (FileUtil.isOlderThanTimeout(jar, 60)) {
                        centralSyncItemService.increaseCreationAttempt(itemToBeCreated);
                        if (itemToBeCreated.creationAttempts > 10) {
                            Log.errorf("Package creation failed after 10 attempts, removing: %s", itemToBeCreated);
                            deletePackagingItem(itemToBeCreated);
                            return;
                        }
                        // A jar which stays more than 60 minutes in NONE stage needs to be recreated
                        Log.warnf("Re-creating package (attempt: %d): %s",
                                itemToBeCreated.creationAttempts, itemToBeCreated);
                        Path dir = packageFileLocator.getLocalDirectory(itemToBeCreated.groupId, itemToBeCreated.artifactId,
                                itemToBeCreated.version);
                        FileUtils.deleteQuietly(dir.toFile());
                        packageCreator.getFromCacheOrCreate(FileType.jar, name, itemToBeCreated.version);
                    }
                } catch (PackageAlreadySyncedException e) {
                    // Already synced, nothing to do
                } catch (GetPackageException e) {
                    if (e.isPermanentlyUnavailable()) {
                        Log.warnf("Package permanently unavailable on NPM, removing: %s — %s", itemToBeCreated,
                                e.getMessage());
                        deletePackagingItem(itemToBeCreated);
                    } else {
                        Log.warnf("NPM error for %s: %s", itemToBeCreated, e.getMessage());
                    }
                } catch (InvalidVersionException e) {
                    Log.warnf("Invalid version, removing: %s — %s", itemToBeCreated, e.getVersion());
                    deletePackagingItem(itemToBeCreated);
                } catch (Exception e) {
                    Log.warnf("Error checking packaging for %s: %s", itemToBeCreated, e.getMessage());
                }

            }
        } else {
            Log.debug("Nothing in the queue to sync");
        }
    }

    private void deletePackagingItem(CentralSyncItem item) {
        centralSyncItemService.delete(item);
        Path dir = packageFileLocator.getLocalDirectory(item.groupId, item.artifactId, item.version);
        FileUtils.deleteQuietly(dir.toFile());
    }

    /**
     * This just check if there is an artifact being uploaded, and if not change the status and fire an event
     */
    @Scheduled(every = "${mvnpm.next-upload.every:3m}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    void nextToUploadStatusChange() {
        if (isCurrentlyUploading()) {
            Log.debug("Sync upload in progress");
            return;
        }
        CentralSyncItem item = centralSyncItemService.claimNextForUpload();
        if (item == null) {
            Log.debug("Nothing in the queue to sync");
            return;
        }
        // Check if already in Central (avoid duplicate upload)
        if (centralSyncService.checkCentralStatusAndUpdateStageIfNeeded(item)) {
            return; // Item moved to RELEASED inside the check
        }
        Log.debugf("Version [%s] of %s is NOT in central. Kicking off sync...",
                item.version, item.toGavString());
        bus.publish("central-sync-item-stage-change", item);
    }

    @Scheduled(every = "${mvnpm.clean-release.every:3m}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    void cleanCentralStatuses() {
        // Check if this is in central, and update the status
        List<CentralSyncItem> uploadedToCentral = CentralSyncItem.findUpdloadedButNotReleased();
        for (CentralSyncItem centralSyncItem : uploadedToCentral) {
            centralSyncService.checkCentralStatusAndUpdateStageIfNeeded(centralSyncItem);
        }
    }

    @Scheduled(every = "${mvnpm.release.every:60s}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    void processCentralStatuses() {
        List<CentralSyncItem> uploadedToCentral = CentralSyncItem.findUpdloadedButNotReleased();
        if (!uploadedToCentral.isEmpty()) {
            Map<String, CentralSyncItem> uploadedToCentralMap = mapByReleaseId(uploadedToCentral);
            if (!uploadedToCentralMap.isEmpty()) {

                for (Map.Entry<String, CentralSyncItem> itemToCheck : uploadedToCentralMap.entrySet()) {
                    CentralSyncItem uploadedItem = itemToCheck.getValue();
                    String releaseId = itemToCheck.getKey();
                    try {
                        ReleaseStatus releaseStatus = mavenCentralFacade.status(uploadedItem, releaseId);
                        switch (releaseStatus) {
                            case PENDING:
                            case VALIDATING:
                                uploadedItem = centralSyncItemService.changeStage(uploadedItem, Stage.UPLOADED);
                                break;
                            case VALIDATED:
                            case PUBLISHING:
                                uploadedItem = centralSyncItemService.changeStage(uploadedItem, Stage.CLOSED);
                                break;
                            case PUBLISHED:
                                uploadedItem = centralSyncItemService.changeStage(uploadedItem, Stage.RELEASED);
                                break;
                            case FAILED:
                                uploadedItem = centralSyncItemService.changeStage(uploadedItem, Stage.ERROR);
                                // TODO: Here we should get more details, and do a drop maybe ?
                                break;
                            default:
                                throw new AssertionError();
                        }
                    } catch (StatusCheckException ex) {
                        // Nothing really. We will catch this with the next one
                        Log.warn("Could not get status for " + uploadedItem.toGavString() + " (release Id: " + releaseId + ")");
                    }
                }
            }
        }
    }

    @ConsumeEvent("central-sync-item-stage-change")
    @Blocking
    public void processNextAction(CentralSyncItem centralSyncItem) {
        if (centralSyncItem.stage.equals(Stage.UPLOADING)) {
            processNextUpload(centralSyncItem);
        }
    }

    private Map<String, CentralSyncItem> mapByReleaseId(List<CentralSyncItem> uploadedToCentral) {
        Map<String, CentralSyncItem> mapByReleaseId = new HashMap<>();
        for (CentralSyncItem csi : uploadedToCentral) {
            if (csi.stagingRepoId != null && !csi.stagingRepoId.isEmpty()) {
                mapByReleaseId.put(csi.stagingRepoId, csi);
            }
        }
        return mapByReleaseId;
    }

    private boolean isCurrentlyUploading() {
        // We only process one at a time, so first check that there is not another process in progress
        long uploadingCount = CentralSyncItem.count("stage", Stage.UPLOADING);
        return uploadingCount != 0;
    }

    /**
     * Check for version updates, and if a new version is out, do a sync
     */
    private void update(String groupId, String artifactId) {
        Log.debug("====== mvnpm: Continuous Updater ======");
        Log.debug("\tChecking " + groupId + ":" + artifactId);
        if (!isInternal(groupId, artifactId)) {
            // Get latest in NPM TODO: Later make this per patch release...
            try {
                Name name = NameParser.fromMavenGA(groupId, artifactId);
                Project project = npmRegistryFacade.getProject(name.npmFullName);
                if (project != null) {
                    String latest = project.distTags().latest();
                    // Queue for sync without creating files — files are created at upload time
                    // by ensureFilesExist() on the pod that will upload
                    boolean queued = centralSyncService.initializeSync(name, latest);
                    if (queued) {
                        Log.infof("Continuous Updater: New package %s %s queued for sync", name.npmFullName, latest);
                    } else {
                        Log.debugf("Continuous Updater: Package %s already synced or in progress", name.npmFullName);
                    }
                }
            } catch (WebApplicationException wae) {
                Log.error("Could not do update for [" + groupId + ":" + artifactId + "] - " + wae.getMessage());
            }
        } else {
            // Handle internal compositions
            compositeCreator.getOrBuildComposite(artifactId, null);
        }
    }

    private void processNextUpload(CentralSyncItem centralSyncItem) {
        if (!centralSyncService.checkCentralStatusAndUpdateStageIfNeeded(centralSyncItem)) {
            // Ensure package files exist locally (may have been created on another pod)
            try {
                ensureFilesExist(centralSyncItem);
            } catch (PackageAlreadySyncedException e) {
                Log.infof("Package already synced, marking as released: %s", centralSyncItem.toGavString());
                centralSyncItemService.changeStage(centralSyncItem, Stage.RELEASED);
                return;
            }
            try {
                String releaseId = centralSyncService.sync(centralSyncItem);
                centralSyncItem.stagingRepoId = releaseId;
                centralSyncItem = centralSyncItemService.changeStage(centralSyncItem, Stage.UPLOADED);
            } catch (UploadFailedException exception) {
                Log.warnf("Upload failed for '%s' because of: %s", centralSyncItem.toGavString(), exception.getMessage());
                retryUpload(centralSyncItem, exception);
            } catch (UnauthorizedException unauthorizedException) {
                unauthorizedException.printStackTrace();
                errorHandlingService.handle(centralSyncItem, unauthorizedException);
            } catch (MissingFilesForBundleException e) {
                Log.info(e.getMessage());
                retryUpload(centralSyncItem, e);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                retryUpload(centralSyncItem, throwable);
            }
        }
    }

    /**
     * Ensure all bundle files exist locally before upload.
     * Files may have been created on another pod — this recreates them if missing.
     * All creation services are idempotent (skip if file already exists).
     */
    private void ensureFilesExist(CentralSyncItem centralSyncItem) {
        Name name = NameParser.fromMavenGA(centralSyncItem.groupId, centralSyncItem.artifactId);
        String version = centralSyncItem.version;
        // getPath creates jar + pom + tgz if not cached (jar creation triggers pom/tgz internally)
        Path jarPath = mavenRepositoryService.getPath(name, version, FileType.jar);
        Path pomPath = packageFileLocator.getLocalFullPath(FileType.pom, name, version);
        Path tgzPath = packageFileLocator.getLocalFullPath(FileType.tgz, name, version);
        // Synchronously create remaining bundle files (source, javadoc, asc, hashes)
        packageListener.createBundleFiles(pomPath, jarPath, tgzPath, List.of());
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

    void onStart(@Observes StartupEvent ev) throws StatusCheckException {
        // Reset upload if the server restarts
        resetUpload();
        // Reset promotion if the server restarts
        resetPromotion();
    }

    @Scheduled(every = "${mvnpm.reset-upload.every:30m}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    void periodicResetUpload() {
        resetUpload();
    }

    private void resetUpload() {
        List<CentralSyncItem> uploading = CentralSyncItem.findByStage(Stage.UPLOADING, 50);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        for (CentralSyncItem centralSyncItem : uploading) {
            if (centralSyncItem.stageChangeTime != null && centralSyncItem.stageChangeTime.isAfter(cutoff)) {
                Log.debugf("[MULTI-POD] Skipping recent UPLOADING item %s (may be in progress on another pod)",
                        centralSyncItem);
                continue;
            }
            centralSyncItem.increaseUploadAttempt();
            Log.infof("[MULTI-POD] Resetting stale upload for %s", centralSyncItem);
            centralSyncItem = centralSyncItemService.changeStage(centralSyncItem, Stage.INIT);
        }
    }

    private void resetPromotion() {
        List<CentralSyncItem> closed = CentralSyncItem.findByStage(Stage.CLOSED, 50);
        for (CentralSyncItem centralSyncItem : closed) {
            centralSyncItem.increasePromotionAttempt();
            Log.info("Resetting promotion for " + centralSyncItem + " after restart");
            centralSyncItem = centralSyncItemService.changeStage(centralSyncItem, Stage.UPLOADED);
        }
    }

    private boolean isInternal(String groupId, String artifactId) {
        return groupId.equals("org.mvnpm.at.mvnpm") ||
                (groupId.equals("org.mvnpm.locked") && artifactId.equals("lit")) || // Failed attempt at hardcoding versions
                (groupId.equals("org.mvnpm.locked.at.vaadin") && artifactId.equals("router")) ||
                // Failed attempt at hardcoding versions
                (groupId.equals("org.mvnpm") && artifactId.equals(
                        "vaadin-web-components")); // Before we used the @mvnpm namespave
    }

}
