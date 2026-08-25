package io.mvnpm.nexus.mvn;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.mvnpm.error.ErrorHandlingService;
import io.mvnpm.maven.api.BundleCreator.BundleRecord;
import io.mvnpm.maven.api.Gav;
import io.mvnpm.maven.api.MavenFacade;
import io.mvnpm.maven.sync.SyncItem;
import io.mvnpm.maven.sync.SyncItemService;
import io.mvnpm.maven.api.ReleaseStatus;
import io.mvnpm.maven.api.Stage;
import io.mvnpm.maven.exceptions.StatusCheckException;
import io.mvnpm.maven.exceptions.UploadFailedException;
import io.mvnpm.nexus.mvn.model.MavenResponse;
import io.mvnpm.nexus.mvn.upload.MavenArtifactUploader;
import io.mvnpm.version.Version;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;

/**
 * The {@link MavenFacade}-implementation for nexus sonatype.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@ApplicationScoped
@IfBuildProperty(name = "mvnpm.nexus-repository.enabled", stringValue = "true")
public class NexusMavenFacade implements MavenFacade {

    @Inject
    ErrorHandlingService errorHandlingService;

    @Inject
    @RestClient
    NexusMavenClient nexusClient;

    @Inject
    SyncItemService syncItemService;

    @ConfigProperty(name = "mvnpm.nexus.mvn-repository.releases")
    private String releaseRepository;

    @ConfigProperty(name = "mvnpm.nexus.mvn-repository.snapshots")
    private String snapshotsRepository;

    @Inject
    private MavenArtifactUploader uploader;

    @Override
    public boolean contains(String groupId, String artifactId, String version) {
        final Version parsedVersion = Version.fromString(version);
        String repository = releaseRepository;
        if (parsedVersion.hasQualifier() && parsedVersion.qualifier().contains("SNAPSHOT")) {
            // in semantic versioning this is the only path where SNAPSHOT version could be found
            repository = snapshotsRepository;
        }
        try {
            Response response = nexusClient.search(null, null, repository, "maven2", null, null, version, null, groupId,
                    artifactId, null, null, null);

            if (response.getStatus() < 300) {
                MavenResponse result = response.readEntity(MavenResponse.class);
                return !result.items().isEmpty();
            }
        } catch (Throwable t) {
            errorHandlingService.handle(groupId, artifactId, version,
                    "Error while checking nexus publish state for [" + groupId + ":" + artifactId + ":" + version + "]",
                    t);
        }
        return false;
    }

    @Override
    public String upload(Gav gav, List<BundleRecord> records) throws UploadFailedException {
        Log.infof("\tUploading '%s:%s:%s' to nexus...", gav.getGroupId(), gav.getArtifactId(), gav.getVersion());

        try {
            DeployResult uploadResult = uploader.upload(gav, records);
            Log.infof("Complete upload of '%s:%s:%s' succeeded!", gav.getGroupId(), gav.getArtifactId(),
                    gav.getVersion());
            return String.valueOf(uploadResult.hashCode());
        } catch (final Exception e) {
            e.printStackTrace();
            throw new UploadFailedException(
                    "Deployment for " + gav.getGroupId() + ":" + gav.getArtifactId() + " failed!");
        }
    }

    @Override
    public ReleaseStatus status(SyncItem syncItem, String releaseId) throws StatusCheckException {
        if (contains(syncItem.groupId, syncItem.artifactId, syncItem.version)) {
            return ReleaseStatus.PUBLISHED;
        } else {
            if (syncItem.isInError()) {
                return ReleaseStatus.FAILED;
            } else if (syncItem.isInProgress()) {
                return ReleaseStatus.PUBLISHING;
            } else {
                return ReleaseStatus.PENDING;
            }
        }
    }

    @Override
    public Stage transition(ReleaseStatus status) throws AssertionError {
        return switch (status) {
            case PENDING, VALIDATING, VALIDATED, PUBLISHING -> Stage.UPLOADING;
            case PUBLISHED -> Stage.UPLOADED;
            case FAILED -> Stage.ERROR;
            default -> throw new AssertionError();
        };
    }
}
