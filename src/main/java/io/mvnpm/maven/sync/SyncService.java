package io.mvnpm.maven.sync;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.mvnpm.maven.api.BundleCreator;
import io.mvnpm.maven.api.BundleCreator.BundleRecord;
import io.mvnpm.maven.api.Gav;
import io.mvnpm.maven.api.MavenFacade;
import io.mvnpm.maven.api.Stage;
import io.mvnpm.maven.exceptions.MissingFilesForBundleException;
import io.mvnpm.maven.exceptions.UploadFailedException;
import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.ProjectInfo;

/**
 * This sync a package with maven central
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class SyncService {

    @Inject
    BundleCreator bundleCreator;

    @Inject
    MavenFacade mavenFacade;

    @Inject
    NpmFacade npmFacade;

    @Inject
    SyncItemService syncItemService;

    @Transactional
    public SyncItem checkReleaseInDbAndRepo(String groupId, String artifactId, String version, boolean startSync) {
        if ("latest".equalsIgnoreCase(version)) {
            version = getLatestVersion(groupId, artifactId);
        }

        SyncItem syncItem = syncItemService.findOrCreate(groupId, artifactId, version,
                startSync ? Stage.PACKAGING : Stage.NONE);

        // Check the status
        if (!syncItem.alreadyReleased()) {
            checkStatusAndUpdateStageIfNeeded(syncItem);
            // Reload to get the updated state (changeStage may have set RELEASED)
            syncItem = SyncItem.findById(new Gav(syncItem.groupId, syncItem.artifactId, syncItem.version));
        }
        if (startSync && syncItem.stage == Stage.NONE) {
            syncItem = syncItemService.changeStage(syncItem, Stage.PACKAGING);
        }
        return syncItem;
    }

    @Transactional
    public boolean initializeSync(Name name, String version) {
        return initializeSync(name.mvnGroupId, name.mvnArtifactId, version);
    }

    /**
     * Sync a certain version of a artifact with central
     */
    private boolean initializeSync(String groupId, String artifactId, String version) {
        SyncItem itemToSync = syncItemService.findOrCreate(groupId, artifactId, version, Stage.INIT);
        if (itemToSync.stage == Stage.INIT) {
            // Already started
            return false;
        }
        if (canProcessSync(itemToSync)) { // Check if this is already synced or in progress
            itemToSync = syncItemService.changeStage(itemToSync, Stage.INIT);
            return true;
        }
        return false;
    }

    /**
     * Check if this is not already in Central, or in the process of being synced
     */
    public boolean canProcessSync(SyncItem syncItem) {
        if (syncItem.alreadyReleased()) {
            syncItem = syncItemService.changeStage(syncItem, Stage.RELEASED);
            return false;
        }
        if (syncItem.isInProgress() || syncItem.isInError()) {
            checkStatusAndUpdateStageIfNeeded(syncItem); // Clear the queue
            return false;
        }
        // Next try remote (might have been synced before we stored)
        return !checkStatusAndUpdateStageIfNeeded(syncItem);
    }

    public boolean checkStatusAndUpdateStageIfNeeded(SyncItem syncItem) {
        boolean isPublished = mavenFacade.contains(syncItem.groupId, syncItem.artifactId, syncItem.version);
        if (isPublished) {
            syncItem = syncItemService.changeStage(syncItem, Stage.RELEASED);
        }
        return isPublished;
    }

    public String sync(SyncItem syncItem) throws UploadFailedException, MissingFilesForBundleException {
        return sync(syncItem.groupId, syncItem.artifactId, syncItem.version);
    }

    public String sync(String groupId, String artifactId, String version)
            throws UploadFailedException, MissingFilesForBundleException {
        List<BundleRecord> records = bundleCreator.bundle(groupId, artifactId, version);
        return mavenFacade.upload(new Gav(groupId, artifactId, version), records);
    }

    public String getLatestVersion(String groupId, String artifactId) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        return getLatestVersion(name);
    }

    public String getLatestVersion(Name fullName) {
        ProjectInfo info = npmFacade.getProjectInfo(fullName.npmFullName);
        return info.distTags().latest();
    }
}
