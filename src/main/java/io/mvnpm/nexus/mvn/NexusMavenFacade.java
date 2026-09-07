package io.mvnpm.nexus.mvn;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.mvnpm.Constants;
import io.mvnpm.error.ErrorHandlingService;
import io.mvnpm.maven.api.BundleCreator.BundleRecord;
import io.mvnpm.maven.api.Gav;
import io.mvnpm.maven.api.MavenFacade;
import io.mvnpm.maven.api.ReleaseStatus;
import io.mvnpm.maven.api.Stage;
import io.mvnpm.maven.exceptions.StatusCheckException;
import io.mvnpm.maven.exceptions.UploadFailedException;
import io.mvnpm.maven.sync.SyncItem;
import io.mvnpm.maven.sync.SyncItemService;
import io.mvnpm.nexus.mvn.model.MavenResponse;
import io.mvnpm.nexus.mvn.upload.MavenArtifactUploader;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;

/**
 * The {@link MavenFacade}-implementation for nexus sonatype.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@ApplicationScoped
@IfBuildProperty(name = "mvnpm.custom.repository.enabled", stringValue = "true")
public class NexusMavenFacade implements MavenFacade, Constants {

    @Inject
    ErrorHandlingService errorHandlingService;

    @Inject
    @RestClient
    NexusMavenClient nexusClient;

    @Inject
    SyncItemService syncItemService;

    @ConfigProperty(name = "mvnpm.custom.repository.releases")
    String releaseRepository;

    @ConfigProperty(name = "mvnpm.custom.repository.snapshots")
    String snapshotsRepository;

    @Inject
    MavenArtifactUploader uploader;

    @Override
    public boolean contains(String groupId, String artifactId, String version) {
        final boolean snapshot = version.endsWith(DASH_SNAPSHOT);
        final String repository = snapshot ? snapshotsRepository : releaseRepository;
        final String searchVersion = snapshot ? null : version;
        final String baseVersion = snapshot ? version : null;

        try (final Response response = nexusClient.search(null, null, repository, "maven2", null, null, searchVersion, null,
                groupId, artifactId, baseVersion, null, null)) {

            if (response.getStatus() < 300) {
                MavenResponse result = response.readEntity(MavenResponse.class);
                return !result.items().isEmpty();
            }
        } catch (Exception e) {
            errorHandlingService.handle(groupId, artifactId, version,
                    "Error while checking nexus publish state for [" + groupId + ":" + artifactId + ":" + version + "]",
                    e);
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
            // TODO: the MavenFacade contract is Maven-Central-shaped and expects a releaseId. Nexus has no
            // equivalent, so we return a synthetic token for now. Revisit once the Central status/stage model
            // is moved behind the facade (see PR #41659 discussion).
            return String.valueOf(uploadResult.hashCode());
        } catch (final Exception e) {
            throw new UploadFailedException(
                    "Deployment for " + gav.getGroupId() + ":" + gav.getArtifactId() + " failed!", e);
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
            case PENDING, VALIDATING, VALIDATED, PUBLISHING -> Stage.UPLOADED;
            case PUBLISHED -> Stage.RELEASED;
            case FAILED -> Stage.ERROR;
            // TODO: bare AssertionError kept intentionally for now; revisit together with the releaseId
            // handling once the Central status/stage model lives behind the facade (see PR #41659 discussion).
            default -> throw new AssertionError("Unexpected release status: " + status);
        };
    }
}
