package io.mvnpm.mavencentral.sync;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.mavencentral.SonatypeFacade;
import io.mvnpm.mavencentral.UploadFailedException;
import io.mvnpm.npm.model.Name;

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
    CentralSyncStageService stageService;

    @ConfigProperty(name = "mvnpm.local-user-directory")
    String localUserDir;
    @ConfigProperty(name = "mvnpm.local-m2-directory", defaultValue = ".m2")
    String localM2Dir;

    /**
     * Check if this is not already in Central, or in the process of being synced
     */
    public boolean canProcessSync(CentralSyncItem csi) {
        if (csi.alreadyRealeased()) {
            csi = stageService.changeStage(csi, Stage.RELEASED);
            return false;
        }
        if (csi.isInProgress()) {
            return false;
        }
        // Next try remote (might have been synced before we stored
        return !isInMavenCentralRemoteCheck(csi);
    }

    public boolean isInMavenCentralRemoteCheck(CentralSyncItem csi) {
        CentralSyncItem searchResult = sonatypeFacade.search(csi.name.mvnGroupId,
                csi.name.mvnArtifactId, csi.version);
        if (searchResult != null) {
            csi = stageService.changeStage(csi, Stage.RELEASED);
            return true;
        }
        return false;
    }

    public String sync(Name name, String version) throws UploadFailedException {
        Path bundlePath = bundleCreator.bundle(name, version);
        return sonatypeFacade.upload(name, version, bundlePath);
    }
}
