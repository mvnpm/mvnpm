package io.mvnpm.mavencentral;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.mvnpm.error.ErrorHandlingService;
import io.mvnpm.mavencentral.exceptions.PromotionException;
import io.mvnpm.mavencentral.exceptions.StatusCheckException;
import io.mvnpm.mavencentral.exceptions.UploadFailedException;
import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.CentralSyncItemService;
import io.mvnpm.mavencentral.sync.Stage;
import io.mvnpm.npm.model.Name;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.quarkus.security.UnauthorizedException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Facade on the OSS Sonatype server
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class SonatypeFacade {

    @Inject
    ErrorHandlingService errorHandlingService;

    @RestClient
    SonatypeClient sonatypeClient;

    @Inject
    CentralSyncItemService centralSyncItemService;

    @ConfigProperty(name = "mvnpm.sonatype.authorization")
    Optional<String> authorization;

    @ConfigProperty(name = "mvnpm.sonatype.autorelease")
    boolean autoRelease;

    @ConfigProperty(name = "mvnpm.sonatype.profileId", defaultValue = "473ee06cf882e")
    String profileId;

    @CacheResult(cacheName = "maven-search-cache")
    public CentralSyncItem search(String groupId, String artifactId, String version) {
        try {
            if (authorization.isPresent()) {
                String a = "Basic " + authorization.get();
                Response searchResponse = sonatypeClient.search(a, groupId, artifactId, version);
                if (searchResponse.getStatus() < 300) {
                    JsonObject pageJson = searchResponse.readEntity(JsonObject.class);
                    List<CentralSyncItem> list = toCentralSyncItemList(pageJson);

                    if (!list.isEmpty()) {
                        return list.get(0);
                    }
                }
            } else {
                throw new UnauthorizedException(
                        "Authorization not present for search " + groupId + ":" + artifactId + ":" + version);
            }
        } catch (Throwable t) {
            errorHandlingService.handle(groupId, artifactId, version,
                    "Error while searching maven central [" + groupId + ":" + artifactId + ":" + version + "]", t);
        }
        return null;
    }

    private List<CentralSyncItem> toCentralSyncItemList(JsonObject pageJson) {
        List<CentralSyncItem> items = new ArrayList<>();
        if (pageJson != null) {
            JsonArray data = pageJson.getJsonArray("data");
            if (data != null && !data.isEmpty()) {

                for (int i = 0; i < data.size(); i++) {
                    JsonObject item = data.getJsonObject(i);
                    String latestReleaseRepositoryId = item.getString("latestReleaseRepositoryId");
                    if (latestReleaseRepositoryId.equals("releases")) {
                        String groupId = item.getString("groupId");
                        String artifactId = item.getString("artifactId");
                        String version = item.getString("version");
                        items.add(centralSyncItemService.findOrCreate(groupId, artifactId, version, Stage.NONE));
                    }
                }
            }
        }
        return items;
    }

    public String upload(Path path) throws UploadFailedException {
        try {
            Log.debug("\tUploading " + path + "...");

            if (authorization.isPresent()) {
                String a = "Basic " + authorization.get();

                Response uploadResponse = sonatypeClient.uploadBundle(a, path);

                if (uploadResponse.getStatus() == 201) {
                    String resp = uploadResponse.readEntity(String.class);
                    String repositoryId = resp.substring(resp.lastIndexOf('/') + 1, resp.length() - 3);
                    Log.info("Uploaded bundle " + path + " to staging repo [" + repositoryId + "]");
                    return repositoryId;
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

    public Map<String, RepoStatus> statuses() throws StatusCheckException {
        try {
            if (authorization.isPresent()) {
                String a = "Basic " + authorization.get();

                Response statusesResponse = sonatypeClient.uploadBundleStatuses(a);

                if (statusesResponse.getStatus() < 300) {
                    Map<String, RepoStatus> repostatuses = new HashMap<>();
                    JsonObject resp = statusesResponse.readEntity(JsonObject.class);
                    JsonArray data = resp.getJsonArray("data");
                    data.forEach((t) -> {
                        JsonObject repo = (JsonObject) t;
                        String profileName = repo.getString("profileName");
                        if (profileName.equals("org.mvnpm")) {
                            String repositoryId = repo.getString("repositoryId");
                            String type = repo.getString("type");
                            RepoStatus repoStatus = RepoStatus.valueOf(type);
                            int notifications = resp.getInteger("notifications", 0);
                            if (repoStatus.equals(RepoStatus.open) && notifications > 0) {
                                repoStatus = RepoStatus.error;
                            }
                            repostatuses.put(repositoryId, repoStatus);
                        }

                    });
                    return repostatuses;
                } else {
                    throw new StatusCheckException(
                            "HTTP Response status " + statusesResponse.getStatus() + "] for all repos");
                }
            } else {
                throw new UnauthorizedException(
                        "Authorization not present for checking status in all repos");
            }
        } catch (Throwable ex) {
            throw new StatusCheckException("Status check for all repos failed", ex);
        }
    }

    public RepoStatus status(Name name, String version, String repositoryId) throws StatusCheckException {
        try {
            if (authorization.isPresent()) {
                String a = "Basic " + authorization.get();

                Response statusResponse = sonatypeClient.uploadBundleStatus(a, repositoryId);

                if (statusResponse.getStatus() < 300) {
                    JsonObject resp = statusResponse.readEntity(JsonObject.class);
                    String type = resp.getString("type");
                    RepoStatus repoStatus = RepoStatus.valueOf(type);
                    int notifications = resp.getInteger("notifications", 0);
                    if (repoStatus.equals(RepoStatus.open) && notifications > 0) {
                        repoStatus = RepoStatus.error;
                    }
                    return repoStatus;
                } else {
                    throw new StatusCheckException(
                            "HTTP Response status " + statusResponse.getStatus() + "] for repo " + repositoryId);
                }
            } else {
                throw new UnauthorizedException(
                        "Authorization not present for " + name.toGavString(version) + " [" + repositoryId + "]");
            }
        } catch (Throwable ex) {
            throw new StatusCheckException("Status check for " + name.toGavString(version) + " failed", ex);
        }
    }

    public void release(CentralSyncItem centralSyncItem) throws PromotionException {
        try {
            if (authorization.isPresent()) {
                if (autoRelease) {
                    String a = "Basic " + authorization.get();

                    Response promoteResponse = sonatypeClient.releaseToCentral(a, profileId,
                            toPromoteRequest(centralSyncItem.stagingRepoId));

                    if (promoteResponse.getStatus() >= 300) {
                        JsonObject error = promoteResponse.readEntity(JsonObject.class);
                        Log.error(error.toString());
                        throw new PromotionException(
                                "HTTP Response status " + promoteResponse.getStatus() + "] for item " + centralSyncItem);
                    }
                }
            } else {
                throw new UnauthorizedException("Authorization not present for " + centralSyncItem);
            }
        } catch (Throwable ex) {
            throw new PromotionException("Release request for  " + centralSyncItem + " failed", ex);
        }
    }

    public void drop(CentralSyncItem centralSyncItem) {
        if (centralSyncItem.stagingRepoId == null) {
            return;
        }
        if (authorization.isPresent()) {
            String a = "Basic " + authorization.get();

            sonatypeClient.drop(a, profileId,
                    toPromoteRequest(centralSyncItem.stagingRepoId));
        } else {
            throw new UnauthorizedException("Authorization not present for " + centralSyncItem);
        }
    }

    public JsonObject getStagingProfileRepos() {
        if (authorization.isPresent()) {
            String a = "Basic " + authorization.get();
            Response stagingProfileRepos = sonatypeClient.getStagingProfileRepos(a, profileId);
            if (stagingProfileRepos.getStatus() < 300) {
                return stagingProfileRepos.readEntity(JsonObject.class);
            } else {
                throw new RuntimeException("Could not get staging profile repos [" + stagingProfileRepos.getStatus() + " - "
                        + stagingProfileRepos.getStatusInfo().getReasonPhrase() + "]");
            }
        }
        return null;
    }

    private JsonObject toPromoteRequest(String repositoryId) {
        JsonObject data = JsonObject.of("description", "Released by mvnpm.org", "stagedRepositoryId", repositoryId);
        return JsonObject.of("data", data);
    }
}
