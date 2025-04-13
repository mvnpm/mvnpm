package io.mvnpm.mavencentral;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.mvnpm.error.ErrorHandlingService;
import io.mvnpm.mavencentral.exceptions.StatusCheckException;
import io.mvnpm.mavencentral.exceptions.UploadFailedException;
import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.CentralSyncItemService;
import io.quarkus.logging.Log;
import io.quarkus.security.UnauthorizedException;
import io.vertx.core.json.JsonObject;

/**
 * Facade on the Central server
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MavenCentralFacade {

    @Inject
    ErrorHandlingService errorHandlingService;

    @RestClient
    MavenCentralClient mavenCentralClient;

    @Inject
    CentralSyncItemService centralSyncItemService;

    @ConfigProperty(name = "mvnpm.mavencentral.authorization")
    Optional<String> authorization;

    @ConfigProperty(name = "mvnpm.mavencentral.autorelease")
    boolean autorelease;

    public boolean isInCentral(String groupId, String artifactId, String version) {
        try {
            if (authorization.isPresent()) {
                String a = "Bearer " + authorization.get();
                Response response = mavenCentralClient.isPublished(a, groupId, artifactId, version);

                if (response.getStatus() < 300) {
                    JsonObject result = response.readEntity(JsonObject.class);
                    return result.getBoolean("published", false);
                }
            } else {
                throw new UnauthorizedException("Authorization not present for " + groupId + ":" + artifactId + ":" + version);
            }
        } catch (Throwable t) {
            errorHandlingService.handle(groupId, artifactId, version,
                    "Error while checking maven central publish state for [" + groupId + ":" + artifactId + ":" + version + "]",
                    t);
        }
        return false;
    }

    public String upload(Path path) throws UploadFailedException {
        try {
            Log.debug("\tUploading " + path + "...");

            if (authorization.isPresent()) {
                String a = "Bearer " + authorization.get();

                MavenCentralClient.BundleUploadForm form = new MavenCentralClient.BundleUploadForm();
                form.bundle = Files.readAllBytes(path);

                MavenCentralClient.PublishingType publishingType = MavenCentralClient.PublishingType.USER_MANAGED;
                if (autorelease)
                    publishingType = MavenCentralClient.PublishingType.AUTOMATIC;

                Response uploadResponse = mavenCentralClient.uploadBundle(a, path.getFileName().toString(),
                        publishingType, form);

                if (uploadResponse.getStatus() == 201) {
                    String releaseId = uploadResponse.readEntity(String.class);
                    Log.info("Uploaded bundle " + path + " to releaseId [" + releaseId + "]");
                    return releaseId;
                } else {
                    throw new UploadFailedException("HTTP Response status [" + uploadResponse.getStatus() + "] for " + path);
                }
            } else {
                throw new UnauthorizedException("Authorization not present for " + path);
            }
        } catch (Throwable ex) {
            throw new UploadFailedException("Upload for " + path + " failed", ex);
        }
    }

    public ReleaseStatus status(CentralSyncItem csi, String releaseId) throws StatusCheckException {
        try {
            if (authorization.isPresent()) {
                String a = "Bearer " + authorization.get();

                Response statusResponse = mavenCentralClient.getReleaseStatus(a, releaseId);

                if (statusResponse.getStatus() < 300) {
                    JsonObject resp = statusResponse.readEntity(JsonObject.class);
                    String status = resp.getString("deploymentState");
                    return ReleaseStatus.valueOf(status.toUpperCase());
                } else {
                    throw new StatusCheckException(
                            "HTTP Response status " + statusResponse.getStatus() + "] for releaseId " + releaseId);
                }
            } else {
                throw new UnauthorizedException(
                        "Authorization not present for " + csi.toGavString() + " [" + releaseId + "]");
            }
        } catch (Throwable ex) {
            // Since we moved over to the new api, the old repoId in the DB does not work, so here we can try another way
            if (isInCentral(csi.groupId, csi.artifactId, csi.version)) {
                return ReleaseStatus.PUBLISHED;
            }
            throw new StatusCheckException("Status check for " + csi.toGavString() + " failed (releaseId " + releaseId + ")",
                    ex);
        }
    }
}
