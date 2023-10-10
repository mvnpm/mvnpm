package io.mvnpm.mavencentral.sync;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.Constants;
import io.mvnpm.maven.NameVersionType;
import io.mvnpm.mavencentral.SonatypeFacade;
import io.mvnpm.npm.model.Name;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

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

    @ConfigProperty(name = "mvnpm.local-user-directory")
    String localUserDir;
    @ConfigProperty(name = "mvnpm.local-m2-directory", defaultValue = ".m2")
    String localM2Dir;

    public SyncInfo getSyncInfo(String groupId, String artifactId, String version) {
        boolean checkCentral = isAvailable(groupId, artifactId, version);
        boolean checkStaging = isStaged(groupId, artifactId, version);
        return new SyncInfo(checkCentral, checkStaging);
    }

    public boolean isAvailable(String groupId, String artifactId, String version) {
        // First check local cache
        MetadataService metadataService = new MetadataService(localUserDir, localM2Dir, groupId, artifactId, version);
        boolean local = metadataService.isTrue(Constants.AVAILABLE_IN_CENTRAL);
        if (local) {
            return true;
        }
        // Next try remote
        boolean remote = sonatypeFacade.isAvailable(groupId, artifactId, version);

        if (remote) {
            // store cache
            metadataService.set(Constants.AVAILABLE_IN_CENTRAL, Constants.TRUE);
        }
        return remote;

    }

    public boolean isStaged(String groupId, String artifactId, String version) {
        MetadataService metadataService = new MetadataService(localUserDir, localM2Dir, groupId, artifactId, version);
        return metadataService.isTrue(Constants.STAGED_TO_OSS);
    }

    public String sync(Name name, String version) {
        Path bundlePath = bundleCreator.bundle(name, version);
        return sonatypeFacade.upload(bundlePath);
    }

    @ConsumeEvent("artifact-released-to-central")
    @Blocking
    public void artifactReleased(CentralSyncItem centralSyncItem) {
        NameVersionType nameVersionType = centralSyncItem.getNameVersionType();
        Name name = nameVersionType.name();
        String version = nameVersionType.version();
        String repoId = centralSyncItem.getStagingRepoId();
        MetadataService metadataService = new MetadataService(localUserDir, localM2Dir, name.mvnGroupId(), name.mvnArtifactId(),
                version);
        metadataService.set(Constants.STAGED_TO_OSS, Constants.TRUE);
        metadataService.set(Constants.STAGED_REPO_ID, repoId);
    }
}
