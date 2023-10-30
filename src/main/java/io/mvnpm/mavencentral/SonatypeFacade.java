package io.mvnpm.mavencentral;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.mvnpm.Constants;
import io.mvnpm.error.ErrorHandlingService;
import io.mvnpm.npm.model.Name;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;
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
    public JsonObject search(String groupId, String artifactId, String version) {
        String q = String.format(Q_FORMAT, groupId, artifactId, version);
        try {
            Response searchResponse = searchMavenClient.search(q, CORE, ROWS, WT);
            if (searchResponse.getStatus() < 300) {
                return searchResponse.readEntity(JsonObject.class);
            }
        } catch (Throwable t) {
            String u = searchMavenUrl + "/solrsearch/select?q=" + q.replaceAll(Constants.SPACE, Constants.SPACE_URL_ENCODED)
                    + "&core=gav&rows=1&wt=json";
            errorHandlingService.handle(groupId, artifactId, version, "Error while searching maven central [" + u + "]", t);
        }
        return null;
    }

    public boolean isAvailable(String groupId, String artifactId, String version) {
        JsonObject searchResult = search(groupId, artifactId, version);

        if (searchResult != null) {
            JsonObject response = searchResult.getJsonObject("response");
            if (response != null) {
                Integer numFound = response.getInteger("numFound");
                if (numFound != null && numFound > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    @Blocking
    public String upload(Name name, String version, Path path) {
        Log.debug("====== mvnpm: Nexus Uploader ======");
        Log.debug("\tUploading " + path + "...");
        byte[] b;
        try {
            b = Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        if (authorization.isPresent()) {
            String a = "Basic " + authorization.get();
            try {
                Response uploadResponse = sonatypeClient.uploadBundle(a, b);

                if (uploadResponse.getStatus() == 201) {
                    String resp = uploadResponse.readEntity(String.class);
                    String repositoryId = resp.substring(resp.lastIndexOf('/') + 1, resp.length() - 3);
                    Log.info("Uploaded bundle " + path + " to staging repo [" + repositoryId + "]");
                    return repositoryId;
                } else {
                    errorHandlingService.handle(name, version,
                            "Error uploading bundle " + path + " - status [" + uploadResponse.getStatus() + "]");
                }
            } catch (Throwable t) {
                errorHandlingService.handle(name, version, "Error uploading bundle " + path, t);
            }
        }

        return null;
    }

    @Blocking
    public RepoStatus status(Name name, String version, String repositoryId) {
        if (authorization.isPresent()) {
            String a = "Basic " + authorization.get();
            try {
                Response statusResponse = sonatypeClient.uploadBundleStatus(a, repositoryId);

                if (statusResponse.getStatus() < 300) {
                    JsonObject resp = statusResponse.readEntity(JsonObject.class);
                    String type = resp.getString("type");
                    return RepoStatus.valueOf(type);
                } else {
                    errorHandlingService.handle(name, version,
                            "Error checking status for staging repo " + repositoryId + " - status ["
                                    + statusResponse.getStatus() + "]");
                }
            } catch (Throwable t) {
                errorHandlingService.handle(name, version, "Error checking status for staging repo " + repositoryId, t);
            }
            return null;
        }
        return null;
    }

    @Blocking
    public boolean release(Name name, String version, String repositoryId) {
        if (authorization.isPresent() && autoRelease) {
            String a = "Basic " + authorization.get();
            try {
                Response promoteResponse = sonatypeClient.releaseToCentral(a, profileId, toPromoteRequest(repositoryId));

                if (promoteResponse.getStatus() < 300) {
                    return true;
                } else {
                    errorHandlingService.handle(name, version,
                            "Error promoting staging repo " + repositoryId + " - status [" + promoteResponse.getStatus() + "]");
                }
            } catch (Throwable t) {
                errorHandlingService.handle(name, version, "Error promoting staging repo " + repositoryId, t);
            }
            return false;
        }
        return true;
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
