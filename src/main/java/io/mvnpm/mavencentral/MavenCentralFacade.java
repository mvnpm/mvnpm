package io.mvnpm.mavencentral;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import io.mvnpm.mavencentral.sync.Stage;
import io.mvnpm.npm.model.Name;
import io.quarkus.logging.Log;
import io.quarkus.security.UnauthorizedException;
import io.vertx.core.json.JsonArray;
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
                form.bundle = Files.newInputStream(path);
                Response uploadResponse = mavenCentralClient.uploadBundle(a, path.getFileName().toString(),
                        MavenCentralClient.PublishingType.USER_MANAGED, form);

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

    public ReleaseStatus status(Name name, String version, String releaseId) throws StatusCheckException {
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
                        "Authorization not present for " + name.toGavString(version) + " [" + releaseId + "]");
            }
        } catch (Throwable ex) {
            throw new StatusCheckException("Status check for " + name.toGavString(version) + " failed", ex);
        }
    }

    private List<CentralSyncItem> toCentralSyncItemList(JsonObject pageJson) {
        List<CentralSyncItem> items = new ArrayList<>();
        if (pageJson != null) {
            JsonObject response = pageJson.getJsonObject("response");
            if (response != null) {
                JsonArray docs = response.getJsonArray("docs");
                if (docs != null && !docs.isEmpty()) {
                    for (int i = 0; i < docs.size(); i++) {
                        JsonObject doc = docs.getJsonObject(i);
                        String groupId = doc.getString("g");
                        String artifactId = doc.getString("a");
                        String version = doc.getString("v");
                        items.add(centralSyncItemService.findOrCreate(groupId, artifactId, version, Stage.NONE));
                    }
                }
            }
        }
        return items;
    }
}
