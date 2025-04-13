package io.mvnpm.mavencentral.sync;

import static io.mvnpm.mavencentral.ReleaseStatus.VALIDATED;
import static io.mvnpm.mavencentral.ReleaseStatus.VALIDATING;
import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

import java.nio.file.Path;
import java.time.Duration;
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
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

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
    MavenCentralService mavenCentralService;

    @Scheduled(cron = "{mvnpm.checkerror.cron.expr}", concurrentExecution = SKIP)
    @Blocking
    public void checkError() {
        try {
            Log.debug("Starting error retry...");
            List<CentralSyncItem> error = CentralSyncItem.findByStage(Stage.ERROR, 50);

            if (error != null && !error.isEmpty()) {
                for (CentralSyncItem centralSyncItem : error) {
                    centralSyncItemService.tryErroredItemAgain(centralSyncItem);
                }
            }
        } catch (Throwable t) {
            Log.error(t.getMessage());
        }
    }

    /**
     * Check all known artifacts for updates
     */
    @Scheduled(cron = "{mvnpm.checkall.cron.expr}", concurrentExecution = SKIP)
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

    /**
     * This check is to auto-sync matching dependencies on existing packages
     */
    @Scheduled(every = "${mvnpm.check-versions.every:5m}", concurrentExecution = SKIP)
    @Blocking
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
    @Blocking
    @Transactional
    void checkPackaging() {
        List<CentralSyncItem> initQueue = CentralSyncItem.findByStage(Stage.PACKAGING, 1);
        if (!initQueue.isEmpty()) {
            CentralSyncItem itemToBeCreated = initQueue.get(0);
            if (centralSyncService.canProcessSync(itemToBeCreated)) {
                final Name name = NameParser.fromMavenGA(itemToBeCreated.groupId, itemToBeCreated.artifactId);
                try {
                    final Path jar = packageCreator.getFromCacheOrCreate(FileType.jar, name, itemToBeCreated.version);
                    if (FileUtil.isOlderThanTimeout(jar, 60)) {
                        centralSyncItemService.increaseCreationAttempt(itemToBeCreated);
                        if (itemToBeCreated.creationAttempts > 10) {
                            Log.errorf("Package creation attempts exceeded maximum 10 attempts for:" + itemToBeCreated);
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
                    // Do nothing
                } catch (WebApplicationException e) {
                    Log.error(e);
                }

            }
        } else {
            Log.debug("Nothing in the queue to sync");
        }
    }

    /**
     * This just check if there is an artifact being uploaded, and if not change the status and fire an event
     */
    @Scheduled(every = "${mvnpm.next-upload.every:3m}", concurrentExecution = SKIP)
    @Blocking
    void nextToUploadStatusChange() {
        // We only process one at a time, so first check that there is not another process in progress
        if (!isCurrentlyUploading()) {
            List<CentralSyncItem> initQueue = CentralSyncItem.findByStage(Stage.INIT, 1);
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

    @Scheduled(every = "${mvnpm.clean-release.every:3m}", concurrentExecution = SKIP)
    @Blocking
    void cleanCentralStatuses() {
        // Check if this is in central, and update the status
        List<CentralSyncItem> uploadedToCentral = CentralSyncItem.findUpdloadedButNotReleased();
        for (CentralSyncItem centralSyncItem : uploadedToCentral) {
            centralSyncService.checkCentralStatusAndUpdateStageIfNeeded(centralSyncItem);
        }
    }

    @Scheduled(every = "${mvnpm.release.every:60s}", concurrentExecution = SKIP)
    @Blocking
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
                    try {
                        mavenRepositoryService.getPath(name, latest, FileType.jar);
                        Log.infof("Continuous Updater: New package %s found", name.npmFullName);
                    } catch (PackageAlreadySyncedException e) {
                        Log.debugf("Continuous Updater: Package %s already synced", name.npmFullName);
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

    private void resetUpload() {
        List<CentralSyncItem> uploading = CentralSyncItem.findByStage(Stage.UPLOADING, 50);
        for (CentralSyncItem centralSyncItem : uploading) {
            centralSyncItem.increaseUploadAttempt();
            Log.info("Resetting upload for " + centralSyncItem + " after restart");
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
