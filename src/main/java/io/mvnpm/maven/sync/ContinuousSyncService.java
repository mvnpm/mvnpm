package io.mvnpm.maven.sync;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageCreator;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.creator.PackageListener;
import io.mvnpm.creator.composite.CompositeCreator;
import io.mvnpm.creator.events.DependencyVersionCheckRequest;
import io.mvnpm.creator.utils.FileUtil;
import io.mvnpm.error.ErrorHandlingService;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.api.ReleaseStatus;
import io.mvnpm.maven.api.Stage;
import io.mvnpm.maven.exceptions.MissingFilesForBundleException;
import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.mvnpm.maven.exceptions.StatusCheckException;
import io.mvnpm.maven.exceptions.UploadFailedException;
import io.mvnpm.maven.MavenService;
import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.exceptions.GetPackageException;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.ProjectInfo;
import io.mvnpm.version.InvalidVersionException;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;

/**
 * This runs Continuous (on some schedule) and check if any updates for libraries we have is available,
 * and if so, kick of a sync. Can also be triggered manually
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class ContinuousSyncService {

    @Inject
    NpmFacade npmFacade;

    @Inject
    SyncService syncService;

    @Inject
    MavenFacade mavenFacade;

    @Inject
    ErrorHandlingService errorHandlingService;

    @Inject
    SyncItemService syncItemService;

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
    MavenService mavenService;

    @Inject
    io.vertx.mutiny.core.eventbus.EventBus bus;

    @Inject
    private Namespace namespace;

    @Scheduled(cron = "{mvnpm.checkerror.cron.expr}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    public void checkError() {
        try {
            Log.debug("Starting error retry...");
            SyncItem item;
            int count = 0;
            while ((item = syncItemService.claimNextForErrorRetry()) != null && count < 10) {
                bus.publish("sync-item-stage-change", item);
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
            if (namespace.isInternal(groupId, artifactId)) {
                return LocalDateTime.now().plusDays(1);
            }
            Name name = NameParser.fromMavenGA(groupId, artifactId);
            ProjectInfo info = npmFacade.getProjectInfo(name.npmFullName);
            if (info != null && info.lastModified() != null) {
                Instant lastModified = Instant.parse(info.lastModified());
                long ageDays = Duration.between(lastModified, Instant.now()).toDays();
                return LocalDateTime.now().plus(nextCheckInterval(ageDays));
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
        final List<SyncItem> byStage = SyncItem.findPackageWithUncheckedDependencies(1);
        if (!byStage.isEmpty()) {
            for (SyncItem item : byStage) {
                final Name name = NameParser.fromMavenGA(item.groupId, item.artifactId);
                final String gavString = name.toGavString(item.version);
                try {
                    Log.infof("Checking versions for %s", gavString);
                    final Path pom = mavenService.download(name, item.version, FileType.pom);
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
        SyncItem itemToBeCreated = syncItemService.claimNextForPackagingCheck();
        if (itemToBeCreated != null) {
            if (syncService.canProcessSync(itemToBeCreated)) {
                final Name name = NameParser.fromMavenGA(itemToBeCreated.groupId, itemToBeCreated.artifactId);
                try {
                    final Path jar = packageCreator.getFromCacheOrCreate(FileType.jar, name, itemToBeCreated.version);
                    if (FileUtil.isOlderThanTimeout(jar, 60)) {
                        syncItemService.increaseCreationAttempt(itemToBeCreated);
                        if (itemToBeCreated.creationAttempts > 10) {
                            Log.errorf("Package creation failed after 10 attempts, removing: %s", itemToBeCreated);
                            deletePackagingItem(itemToBeCreated);
                            return;
                        }
                        // A jar which stays more than 60 minutes in NONE stage needs to be recreated
                        Log.warnf("Re-creating package (attempt: %d): %s", itemToBeCreated.creationAttempts,
                                itemToBeCreated);
                        Path dir = packageFileLocator.getLocalDirectory(itemToBeCreated.groupId,
                                itemToBeCreated.artifactId, itemToBeCreated.version);
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

    private void deletePackagingItem(SyncItem item) {
        syncItemService.delete(item);
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
        SyncItem item = syncItemService.claimNextForUpload();
        if (item == null) {
            Log.debug("Nothing in the queue to sync");
            return;
        }
        // Check if already in repository (avoid duplicate upload)
        if (syncService.checkStatusAndUpdateStageIfNeeded(item)) {
            return; // Item moved to RELEASED inside the check
        }
        Log.debugf("Version [%s] of %s is NOT in repository. Kicking off sync...", item.version, item.toGavString());
        bus.publish("sync-item-stage-change", item);
    }

    @Scheduled(every = "${mvnpm.clean-release.every:3m}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    void cleanCentralStatuses() {
        // Check if this is in central, and update the status
        List<SyncItem> uploaded = SyncItem.findUpdloadedButNotReleased();
        for (SyncItem syncItem : uploaded) {
            syncService.checkStatusAndUpdateStageIfNeeded(syncItem);
        }
    }

    @Scheduled(every = "${mvnpm.release.every:60s}", concurrentExecution = SKIP)
    @RunOnVirtualThread
    void processCentralStatuses() {
        List<SyncItem> uploaded = SyncItem.findUpdloadedButNotReleased();
        if (!uploaded.isEmpty()) {
            Map<String, SyncItem> uploadedMap = mapByReleaseId(uploaded);
            if (!uploadedMap.isEmpty()) {

                for (Map.Entry<String, SyncItem> itemToCheck : uploadedMap.entrySet()) {
                    SyncItem uploadedItem = itemToCheck.getValue();
                    String releaseId = itemToCheck.getKey();
                    try {
                        ReleaseStatus releaseStatus = mavenFacade.status(uploadedItem, releaseId);
                        switch (releaseStatus) {
                        case PENDING:
                        case VALIDATING:
                            uploadedItem = syncItemService.changeStage(uploadedItem, Stage.UPLOADED);
                            break;
                        case VALIDATED:
                        case PUBLISHING:
                            uploadedItem = syncItemService.changeStage(uploadedItem, Stage.CLOSED);
                            break;
                        case PUBLISHED:
                            uploadedItem = syncItemService.changeStage(uploadedItem, Stage.RELEASED);
                            break;
                        case FAILED:
                            uploadedItem = syncItemService.changeStage(uploadedItem, Stage.ERROR);
                            // TODO: Here we should get more details, and do a drop maybe ?
                            break;
                        default:
                            throw new AssertionError();
                        }
                    } catch (StatusCheckException ex) {
                        // Nothing really. We will catch this with the next one
                        Log.warn("Could not get status for " + uploadedItem.toGavString() + " (release Id: " + releaseId
                                + ")");
                    }
                }
            }
        }
    }

    @ConsumeEvent("sync-item-stage-change")
    @Blocking
    public void processNextAction(SyncItem syncItem) {
        if (syncItem.stage.equals(Stage.UPLOADING)) {
            processNextUpload(syncItem);
        }
    }

    private Map<String, SyncItem> mapByReleaseId(List<SyncItem> uploaded) {
        Map<String, SyncItem> mapByReleaseId = new HashMap<>();
        for (SyncItem syncItem : uploaded) {
            if (syncItem.releaseId != null && !syncItem.releaseId.isEmpty()) {
                mapByReleaseId.put(syncItem.releaseId, syncItem);
            }
        }
        return mapByReleaseId;
    }

    private boolean isCurrentlyUploading() {
        // We only process one at a time, so first check that there is not another process in progress
        long uploadingCount = SyncItem.count("stage", Stage.UPLOADING);
        return uploadingCount != 0;
    }

    /**
     * Check for version updates, and if a new version is out, do a sync
     */
    private void update(String groupId, String artifactId) {
        Log.debug("====== mvnpm: Continuous Updater ======");
        Log.debug("\tChecking " + groupId + ":" + artifactId);
        if (!namespace.isInternal(groupId, artifactId)) {
            // Get latest in NPM TODO: Later make this per patch release...
            try {
                Name name = NameParser.fromMavenGA(groupId, artifactId);
                ProjectInfo info = npmFacade.getProjectInfo(name.npmFullName);
                if (info != null) {
                    String latest = info.distTags().latest();
                    // Queue for sync without creating files — files are created at upload time
                    // by ensureFilesExist() on the pod that will upload
                    boolean queued = syncService.initializeSync(name, latest);
                    if (queued) {
                        Log.infof("Continuous Updater: New package %s %s queued for sync", name.npmFullName, latest);
                    } else {
                        Log.infof("Continuous Updater: Package %s already synced or in progress", name.npmFullName);
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

    private void processNextUpload(SyncItem syncItem) {
        if (!syncService.checkStatusAndUpdateStageIfNeeded(syncItem)) {
            // Ensure package files exist locally (may have been created on another pod)
            try {
                ensureFilesExist(syncItem);
            } catch (PackageAlreadySyncedException e) {
                Log.infof("Package already synced, marking as released: %s", syncItem.toGavString());
                syncItemService.changeStage(syncItem, Stage.RELEASED);
                return;
            }
            try {
                String releaseId = syncService.sync(syncItem);
                syncItem.releaseId = releaseId;
                syncItem = syncItemService.changeStage(syncItem, Stage.UPLOADED);
            } catch (UploadFailedException exception) {
                Log.warnf("Upload failed for '%s' because of: %s", syncItem.toGavString(), exception.getMessage());
                retryUpload(syncItem, exception);
            } catch (UnauthorizedException unauthorizedException) {
                unauthorizedException.printStackTrace();
                errorHandlingService.handle(syncItem, unauthorizedException);
            } catch (MissingFilesForBundleException e) {
                Log.info(e.getMessage());
                retryUpload(syncItem, e);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                retryUpload(syncItem, throwable);
            }
        }
    }

    /**
     * Ensure all bundle files exist locally before upload.
     * Files may have been created on another pod — this recreates them if missing.
     * All creation services are idempotent (skip if file already exists).
     */
    private void ensureFilesExist(SyncItem syncItem) {
        Name name = NameParser.fromMavenGA(syncItem.groupId, syncItem.artifactId);
        String version = syncItem.version;
        // getPath creates jar + pom + tgz if not cached (jar creation triggers pom/tgz internally)
        Path jarPath = mavenRepositoryService.getPath(name, version, FileType.jar);
        Path pomPath = packageFileLocator.getLocalFullPath(FileType.pom, name, version);
        // Composites (internal packages) don't have a tgz file
        Path tgzPath = name.isInternal() ? null : packageFileLocator.getLocalFullPath(FileType.tgz, name, version);
        // Synchronously create remaining bundle files (source, javadoc, asc, hashes)
        packageListener.createBundleFiles(pomPath, jarPath, tgzPath, List.of());
    }

    private void retryUpload(SyncItem syncItem, Throwable t) {
        if (syncItem.uploadAttempts < 10) {
            syncItem = syncItemService.changeStage(syncItem, Stage.INIT);
        } else {
            t.printStackTrace();
            errorHandlingService.handle(syncItem, t);
            syncItem = syncItemService.changeStage(syncItem, Stage.ERROR);
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
        List<SyncItem> uploading = SyncItem.findByStage(Stage.UPLOADING, 50);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        for (SyncItem syncItem : uploading) {
            if (syncItem.stageChangeTime != null && syncItem.stageChangeTime.isAfter(cutoff)) {
                Log.debugf("[MULTI-POD] Skipping recent UPLOADING item %s (may be in progress on another pod)",
                        syncItem);
                continue;
            }
            syncItem.increaseUploadAttempt();
            if (syncItem.uploadAttempts >= 10) {
                Log.errorf("Upload stuck after %d attempts, moving to ERROR: %s", syncItem.uploadAttempts, syncItem);
                syncItem = syncItemService.changeStage(syncItem, Stage.ERROR);
            } else {
                Log.infof("[MULTI-POD] Resetting stale upload for %s", syncItem);
                syncItem = syncItemService.changeStage(syncItem, Stage.INIT);
            }
        }
    }

    private void resetPromotion() {
        List<SyncItem> closed = SyncItem.findByStage(Stage.CLOSED, 50);
        for (SyncItem syncItem : closed) {
            syncItem.increasePromotionAttempt();
            Log.info("Resetting promotion for " + syncItem + " after restart");
            syncItem = syncItemService.changeStage(syncItem, Stage.UPLOADED);
        }
    }
}
