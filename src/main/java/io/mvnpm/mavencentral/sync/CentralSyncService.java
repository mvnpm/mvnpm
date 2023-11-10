package io.mvnpm.mavencentral.sync;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.mavencentral.SonatypeFacade;
import io.mvnpm.mavencentral.UploadFailedException;

/**
 * This sync a package with maven central
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class CentralSyncService {
    @Inject
    BundleCreator bundleCreator;

    @Inject
    SonatypeFacade sonatypeFacade;

    @Inject
    CentralSyncItemService centralSyncItemService;

    /**
     * Check if this is not already in Central, or in the process of being synced
     */
    public boolean canProcessSync(CentralSyncItem csi) {
        if (csi.alreadyRealeased()) {
            csi = centralSyncItemService.changeStage(csi, Stage.RELEASED);
            return false;
        }
        if (csi.isInProgress() || csi.isInError()) {
            return false;
        }
        // Next try remote (might have been synced before we stored
        return !isInMavenCentralRemoteCheck(csi);
    }

    public boolean isInMavenCentralRemoteCheck(CentralSyncItem csi) {
        CentralSyncItem searchResult = sonatypeFacade.search(csi.groupId,
                csi.artifactId, csi.version);
        if (searchResult != null) {
            csi = centralSyncItemService.changeStage(csi, Stage.RELEASED);
            return true;
        }
        return false;
    }

    public String sync(CentralSyncItem centralSyncItem) throws UploadFailedException {
        return sync(centralSyncItem.groupId,
                centralSyncItem.artifactId,
                centralSyncItem.version);
    }

    public String sync(String groupId, String artifactId, String version) throws UploadFailedException {
        Path bundlePath = bundleCreator.bundle(groupId, artifactId, version);
        return sonatypeFacade.upload(bundlePath);
    }
}
