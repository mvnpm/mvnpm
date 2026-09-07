package io.mvnpm.hosting.resources;

import static io.mvnpm.hosting.MavenVersionPolicy.*;
import static io.mvnpm.hosting.MavenWritePolicy.*;
import static io.mvnpm.hosting.NexusTestFixtures.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import io.mvnpm.hosting.NexusTestClient;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public abstract class NexusTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String CUSTOM_REPOSITORY_URL_PROP = "mvnpm.custom.repository.url";

    public static final String USERNAME_PROP = "mvnpm.custom.repository.username";
    public static final String PASSWORD_PROP = "mvnpm.custom.repository.password";

    public static final String RELEASE_REPO_PROP = "mvnpm.custom.repository.releases";
    public static final String SNAPSHOT_REPO_PROP = "mvnpm.custom.repository.snapshots";

    public static final String REST_CLIENT_URL_PROP = "quarkus.rest-client.repository.url";
    public static final String NPM_FALLBACK_URL_PROP = "quarkus.rest-client.npm-registry.url";

    public static final String NPM_REPO_PROP = "mvnpm.custom.repository.npm";

    private static final String ADMIN_USER = "admin";
    private static final String NEW_ADMIN_PASSWORD = "test-password";
    private static final String RELEASES_REPO = "test-releases";
    private static final String SNAPSHOTS_REPO = "test-snapshots";

    private static final String MAVEN_GROUP_REPO = "maven-public";

    private GenericContainer<?> nexusContainer;

    @SuppressWarnings({ "null", "resource" })
    @Override
    public Map<String, String> start() {
        nexusContainer = new GenericContainer<>(DockerImageName.parse(image()))
                .withExposedPorts(8081)
                .waitingFor(
                        Wait.forHttp("/")
                                .forPort(8081)
                                .forStatusCode(200)
                                .withStartupTimeout(Duration.ofMinutes(3)));

        nexusContainer.start();
        String nexusUrl = "http://" + nexusContainer.getHost() + ":" + nexusContainer.getMappedPort(8081);

        changeAdminPassword(nexusUrl, readInitialPassword(), NEW_ADMIN_PASSWORD);
        final NexusTestClient nexus = new NexusTestClient(nexusUrl, ADMIN_USER, NEW_ADMIN_PASSWORD);

        nexus.acceptEulaIfRequired();
        provisionTestRepositories(nexus);
        seedNpmPackages(nexus);

        return Map.ofEntries(
                Map.entry(REST_CLIENT_URL_PROP, nexusUrl),

                // Important: integration tests must fail rather than silently
                // falling back to registry.npmjs.org.
                Map.entry(
                        NPM_FALLBACK_URL_PROP,
                        nexusUrl + "/fallback-must-not-be-used"),

                Map.entry(USERNAME_PROP, ADMIN_USER),
                Map.entry(PASSWORD_PROP, NEW_ADMIN_PASSWORD),

                Map.entry(RELEASE_REPO_PROP, RELEASES_REPO),
                Map.entry(SNAPSHOT_REPO_PROP, SNAPSHOTS_REPO),
                Map.entry(NPM_REPO_PROP, NPM_REPOSITORY));
    }

    private String readInitialPassword() {
        try {
            return nexusContainer.copyFileFromContainer(
                    "/nexus-data/admin.password",
                    inputStream -> new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim());
        } catch (Exception e) {
            throw new IllegalStateException("Could not read Nexus initial admin password", e);
        }
    }

    private void changeAdminPassword(final String nexusUrl, final String currentPassword, final String newPassword) {
        final String credentials = Base64.getEncoder()
                .encodeToString((ADMIN_USER + ":" + currentPassword).getBytes(StandardCharsets.UTF_8));
        final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(nexusUrl + "/service/rest/v1/security/users/admin/change-password"))
                .timeout(Duration.ofSeconds(30)).header("Authorization", "Basic " + credentials)
                .header("Content-Type", "text/plain")
                .PUT(HttpRequest.BodyPublishers.ofString(newPassword))
                .build();

        try {
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Could not change Nexus admin password. " + "HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not communicate with Nexus",
                    e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while changing Nexus password", e);
        }
    }

    private void provisionTestRepositories(NexusTestClient nexus) {
        nexus.createMavenHosted(RELEASES_REPO, RELEASE, ALLOW_ONCE);
        nexus.createMavenHosted(SNAPSHOTS_REPO, SNAPSHOT, ALLOW);
        nexus.createMavenGroup(MAVEN_GROUP_REPO, RELEASES_REPO, SNAPSHOTS_REPO);
        nexus.createNpmHosted(NPM_REPOSITORY, ALLOW_ONCE);
    }

    private void seedNpmPackages(NexusTestClient nexus) {
        nexus.uploadNpmPackage(
                NPM_REPOSITORY,
                "test-mvnpm-package-1.0.0.tgz",
                readFixture(NPM_RELEASE_FIXTURE));

        nexus.awaitNpmPackage(
                NPM_REPOSITORY,
                NPM_SCOPE,
                NPM_NAME,
                NPM_RELEASE_VERSION);

        nexus.uploadNpmPackage(
                NPM_REPOSITORY,
                "test-mvnpm-package-1.0.0-SNAPSHOT.tgz",
                readFixture(NPM_SNAPSHOT_FIXTURE));

        nexus.awaitNpmPackage(
                NPM_REPOSITORY,
                NPM_SCOPE,
                NPM_NAME,
                NPM_SNAPSHOT_VERSION);
    }

    private byte[] readFixture(String resourceName) {
        try (InputStream input = NexusTestResource.class
                .getClassLoader()
                .getResourceAsStream(resourceName)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Missing test fixture: " + resourceName);
            }

            return input.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not read test fixture: " + resourceName,
                    e);
        }
    }

    @Override
    public void stop() {
        if (nexusContainer != null) {
            nexusContainer.stop();
            nexusContainer = null;
        }
    }

    /**
     * Up to implementations to define the exact version.
     *
     * @return The version e.g. <code>sonatype/nexus3:latest</code>
     */
    abstract String image();
}
