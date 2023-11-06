package io.mvnpm.mavencentral;

import java.io.IOException;
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

import io.mvnpm.Constants;
import io.mvnpm.error.ErrorHandlingService;
import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.quarkus.security.UnauthorizedException;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Facade on the OSS Sonatype server
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class SonatypeFacade {

    private static final String Q_FORMAT = "g:%s AND a:%s AND v:%s";
    private static final String CORE = "gav";
    private static final String ROWS = "1";
    private static final String WT = "json";
    private static final String ZERO = "0";

    @Inject
    ErrorHandlingService errorHandlingService;

    @RestClient
    SearchMavenClient searchMavenClient;

    @RestClient
    SonatypeClient sonatypeClient;

    @ConfigProperty(name = "mvnpm.sonatype.authorization")
    Optional<String> authorization;

    @ConfigProperty(name = "mvnpm.sonatype.autorelease")
    boolean autoRelease;

    @ConfigProperty(name = "mvnpm.sonatype.profileId", defaultValue = "473ee06cf882e")
    String profileId;

    @ConfigProperty(name = "quarkus.rest-client.search-maven.url")
    String searchMavenUrl;

    @Blocking
    @CacheResult(cacheName = "maven-search-cache")
    public CentralSyncItem search(String groupId, String artifactId, String version) {
        String q = String.format(Q_FORMAT, groupId, artifactId, version);
        try {
            Response searchResponse = searchMavenClient.search(q, CORE, ROWS, WT, ZERO);
            if (searchResponse.getStatus() < 300) {
                JsonObject pageJson = searchResponse.readEntity(JsonObject.class);
                List<CentralSyncItem> list = toCentralSyncItemList(pageJson);

                if (!list.isEmpty()) {
                    return list.get(0);
                }
            }
        } catch (Throwable t) {
            String u = searchMavenUrl + "/solrsearch/select?q=" + q.replaceAll(Constants.SPACE, Constants.SPACE_URL_ENCODED)
                    + "&core=gav&rows=1&wt=json";
            errorHandlingService.handle(groupId, artifactId, version, "Error while searching maven central [" + u + "]", t);
        }
        return null;
    }

    @Blocking
    public List<CentralSyncItem> findAllInCentral() {
        List<CentralSyncItem> items = new ArrayList<>();
        return page(items);
    }

    private List<CentralSyncItem> page(List<CentralSyncItem> items) {
        Response page = searchMavenClient.search("org.mvnpm", "", "200", WT, String.valueOf(items.size())); // 200 is the max.

        if (page.getStatus() < 300) {
            JsonObject pageJson = page.readEntity(JsonObject.class);
            int size = numFound(pageJson);
            items.addAll(toCentralSyncItemList(pageJson));

            if (items.size() >= size) {
                return items;
            } else {
                return page(items);
            }
        }
        // retry
        return page(items);
    }

    private int numFound(JsonObject pageJson) {
        if (pageJson != null) {
            JsonObject response = pageJson.getJsonObject("response");
            if (response != null) {
                Integer numFound = response.getInteger("numFound");
                if (numFound != null && numFound > 0) {
                    return numFound;
                }
            }
        }
        return 0;
    }

    private List<CentralSyncItem> toCentralSyncItemList(JsonObject pageJson) {
        List<CentralSyncItem> items = new ArrayList<>();
        if (pageJson != null) {
            JsonObject response = pageJson.getJsonObject("response");
            if (response != null) {

                JsonArray docs = response.getJsonArray("docs");
                if (docs != null && !docs.isEmpty()) {

                    docs.forEach(i -> {
                        JsonObject item = (JsonObject) i;
                        String groupId = item.getString("g");
                        String artifactId = item.getString("a");
                        String version = item.getString("latestVersion");
                        Name name = NameParser.fromMavenGA(groupId, artifactId);
                        items.add(new CentralSyncItem(name, version));
                    });
                }
            }
        }
        return items;
    }

    @Blocking
    public String upload(Name name, String version, Path path) throws UploadFailedException {
        Log.debug("====== mvnpm: Nexus Uploader ======");
        Log.debug("\tUploading " + path + "...");
        byte[] b;
        try {
            b = Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new UploadFailedException(ex);
        }

        if (authorization.isPresent()) {
            String a = "Basic " + authorization.get();

            Response uploadResponse = sonatypeClient.uploadBundle(a, b);

            if (uploadResponse.getStatus() == 201) {
                String resp = uploadResponse.readEntity(String.class);
                String repositoryId = resp.substring(resp.lastIndexOf('/') + 1, resp.length() - 3);
                Log.info("Uploaded bundle " + path + " to staging repo [" + repositoryId + "]");
                return repositoryId;
            } else {
                throw new UploadFailedException("HTTP Response status " + uploadResponse.getStatus() + "] for " + path);
            }
        } else {
            throw new UnauthorizedException("Authorization not present for " + path);
        }
    }

    @Blocking
    public RepoStatus status(Name name, String version, String repositoryId) throws StatusCheckException {
        if (authorization.isPresent()) {
            String a = "Basic " + authorization.get();

            Response statusResponse = sonatypeClient.uploadBundleStatus(a, repositoryId);

            if (statusResponse.getStatus() < 300) {
                JsonObject resp = statusResponse.readEntity(JsonObject.class);
                System.err.println(resp.toString());
                String type = resp.getString("type");
                return RepoStatus.valueOf(type);
            } else {
                throw new StatusCheckException(
                        "HTTP Response status " + statusResponse.getStatus() + "] for repo " + repositoryId);
            }
        } else {
            throw new UnauthorizedException("Authorization not present for " + repositoryId);
        }
    }

    @Blocking
    public boolean release(Name name, String version, String repositoryId) throws PromotionException {
        if (authorization.isPresent() && autoRelease) {
            String a = "Basic " + authorization.get();

            Response promoteResponse = sonatypeClient.releaseToCentral(a, profileId, toPromoteRequest(repositoryId));

            if (promoteResponse.getStatus() < 300) {
                return true;
            } else {
                throw new PromotionException(
                        "HTTP Response status " + promoteResponse.getStatus() + "] for repo " + repositoryId);

            }
        } else {
            throw new UnauthorizedException("Authorization not present for " + repositoryId);
        }
    }

    @Blocking
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
