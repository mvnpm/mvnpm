package io.mvnpm.hosting;

import static io.mvnpm.TestHelpers.readPom;
import static io.mvnpm.hosting.SelfHostedTestFixtures.MAVEN_ARTIFACT_ID;
import static io.mvnpm.hosting.SelfHostedTestFixtures.MAVEN_GROUP_ID;
import static io.mvnpm.hosting.SelfHostedTestFixtures.MAVEN_RELEASE_VERSION;
import static io.mvnpm.hosting.SelfHostedTestFixtures.MAVEN_SNAPSHOT_VERSION;
import static io.mvnpm.hosting.SelfHostedTestFixtures.NPM_PROJECT;
import static io.mvnpm.hosting.SelfHostedTestFixtures.NPM_RELEASE_VERSION;
import static io.mvnpm.hosting.SelfHostedTestFixtures.NPM_SNAPSHOT_VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import jakarta.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;

import io.mvnpm.Constants;
import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.creator.PackageListener;
import io.mvnpm.creator.aliasing.AliasCodec;
import io.mvnpm.creator.type.PomService;
import io.mvnpm.maven.api.BundleCreator.BundleRecord;
import io.mvnpm.maven.api.Gav;
import io.mvnpm.maven.api.MavenFacade;
import io.mvnpm.maven.api.ReleaseStatus;
import io.mvnpm.maven.api.Stage;
import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.Project;
import io.quarkus.logging.Log;

/**
 * <p>
 * Should be the test class to further extend if tests are added.
 * </p>
 *
 * @author Luca Pfaffinger(luca.pfaffinger@gmail.com)
 */
@TestInstance(Lifecycle.PER_CLASS)
public abstract class NexusIntegrationTest implements Constants {

    private static final String ALIAS_ROOT = "mvnpm-test-alias-root";
    private static final String FIRST_ALIAS = "nested-target-via-alias";
    private static final String FIRST_TARGET = "mvnpm-test-alias-target";
    private static final String NESTED_ALIAS = "nested-leaf-via-alias";
    private static final String NESTED_TARGET = "mvnpm-test-alias-leaf";
    private static final String FIRST_ALIAS_ARTIFACT = AliasCodec.encode(FIRST_ALIAS, FIRST_TARGET);
    private static final String NESTED_ALIAS_ARTIFACT = AliasCodec.encode(NESTED_ALIAS, NESTED_TARGET);

    @ConfigProperty(name = "quarkus.rest-client.repository.url")
    String nexusUrl;

    @ConfigProperty(name = "mvnpm.custom.repository.username")
    String nexusUsername;

    @ConfigProperty(name = "mvnpm.custom.repository.password")
    String nexusPassword;

    @ConfigProperty(name = "mvnpm.custom.repository.releases")
    String releaseRepo;

    @ConfigProperty(name = "mvnpm.custom.repository.snapshots")
    String snapshotRepo;

    @ConfigProperty(name = "mvnpm.custom.repository.npm")
    String npmRepo;

    @Inject
    PomService pomService;

    @Inject
    PackageFileLocator packageFileLocator;

    @Inject
    PackageListener packageListener;

    @Inject
    NpmFacade npmFacade;

    @Inject
    MavenFacade mavenFacade;

    @TempDir
    Path tempDir;

    /**
     * Gate for the Testcontainers-backed subclasses: skip (rather than hard-fail) when no Docker
     * daemon is reachable, e.g. on a developer machine or a runner without Docker.
     *
     * <p>
     * This is deliberately a pure-JDK probe (socket/env only). It must NOT touch Testcontainers
     * classes: {@code @EnabledIf} is evaluated by JUnit outside the Quarkus test ClassLoader, where
     * the Testcontainers {@code DockerClientProviderStrategy} SPI is not correctly wired and throws
     * {@link java.util.ServiceConfigurationError}. Testcontainers is only used later, from inside the
     * Quarkus test resource, where it resolves correctly.
     * </p>
     *
     * @return {@code true} if a Docker daemon appears to be available
     */
    static boolean dockerAvailable() {
        final String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost != null && !dockerHost.isBlank()) {
            return true;
        }
        return Files.exists(Path.of("/var/run/docker.sock"));
    }

    @BeforeAll
    void nexusIsAvailable() {
        assertNotNull(nexusUrl);
        assertNotNull(nexusUsername);
        assertNotNull(nexusPassword);

        assertNotNull(releaseRepo);
        assertNotNull(snapshotRepo);
        assertNotNull(npmRepo);

        logInfos(variant());
    }

    @Test
    void testNpmFacadeAndConversion() {
        final Project project = npmFacade.getProject(NPM_PROJECT);

        assertNotNull(project);
        assertNotNull(project.name());

        assertEquals(NPM_PROJECT, project.name().npmFullName);
        assertEquals(Set.of(NPM_RELEASE_VERSION, NPM_SNAPSHOT_VERSION), project.versions());
        assertEquals(NPM_RELEASE_VERSION, project.distTags().latest());

        assertPackage(NPM_RELEASE_VERSION);
        assertPackage(NPM_SNAPSHOT_VERSION);
    }

    @Test
    void testMavenFacade() throws Exception {
        final Gav releaseGav = new Gav(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, MAVEN_RELEASE_VERSION);
        final Gav snapshotGav = new Gav(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, MAVEN_SNAPSHOT_VERSION);

        // Fresh Nexus: neither artifact should exist yet.
        assertFalse(mavenFacade.contains(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, MAVEN_RELEASE_VERSION));
        assertFalse(mavenFacade.contains(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, MAVEN_SNAPSHOT_VERSION));

        final String releaseId = mavenFacade.upload(releaseGav, createMavenBundle(releaseGav));
        final String snapshotId = mavenFacade.upload(snapshotGav, createMavenBundle(snapshotGav));

        assertNotNull(releaseId);
        assertFalse(releaseId.isBlank());

        assertNotNull(snapshotId);
        assertFalse(snapshotId.isBlank());

        awaitMavenContains(releaseGav);
        awaitMavenContains(snapshotGav);
    }

    @Test
    void publishingDoesNotTriggerAnotherUpload() {
        assertEquals(Stage.UPLOADED, mavenFacade.transition(ReleaseStatus.PUBLISHING));
        assertEquals(Stage.RELEASED, mavenFacade.transition(ReleaseStatus.PUBLISHED));
    }

    @Test
    void testNestedNpmAliases() throws Exception {
        final Package root = npmFacade.getPackage(ALIAS_ROOT, "1.0.0");
        assertNotNull(root);

        final Path rootPomPath = tempDir.resolve("alias-root.pom");
        pomService.createAndSavePom(root, rootPomPath);

        final String rootPom = Files.readString(rootPomPath);
        assertFalse(rootPom.contains("npm:"));

        final Model rootModel = readPom(rootPom);
        final Dependency firstAlias = getFromPomService(rootModel, FIRST_ALIAS_ARTIFACT);
        assertNotNull(firstAlias);
        assertEquals("org.mvnpm", firstAlias.getGroupId());

        // expect maven-range after aliasing!
        assertEquals("[1.0.0]", firstAlias.getVersion());

        final Gav firstAliasGav = new Gav(firstAlias.getGroupId(), firstAlias.getArtifactId(), "1.0.0");

        uploadGeneratedArtifact(firstAliasGav);

        final String firstAliasPom = awaitMavenPom(firstAliasGav);
        assertFalse(firstAliasPom.contains("npm:"));

        final Model firstAliasModel = readPom(firstAliasPom);
        assertNull(getFromPomService(firstAliasModel, FIRST_TARGET));

        final Dependency nestedAlias = getFromPomService(firstAliasModel, NESTED_ALIAS_ARTIFACT);
        assertNotNull(nestedAlias);
        assertEquals("org.mvnpm", nestedAlias.getGroupId());

        // expect maven-range after aliasing!
        assertEquals("[1.0.0]", nestedAlias.getVersion());

        final Gav nestedAliasGav = new Gav(nestedAlias.getGroupId(), nestedAlias.getArtifactId(),
                "1.0.0");
        uploadGeneratedArtifact(nestedAliasGav);

        final String nestedAliasPom = awaitMavenPom(nestedAliasGav);
        assertFalse(nestedAliasPom.contains("npm:"));

        final Model nestedModel = readPom(nestedAliasPom);
        assertNull(getFromPomService(nestedModel, NESTED_TARGET));
        assertTrue(nestedModel.getDependencies() == null || nestedModel.getDependencies().isEmpty());
    }

    protected void logInfos(String variant) {
        Log.infof("%s nexus url is: '%s'", variant, nexusUrl);
        Log.infof("%s nexus user is: '%s'", variant, nexusUsername);
        Log.infof("%s nexus release repository is: %s/repository/%s", variant, nexusUrl, releaseRepo);
        Log.infof("%s nexus snapshot repository is: %s/repository/%s", variant, nexusUrl, snapshotRepo);
    }

    private void assertPackage(String version) {
        final Package npmPackage = npmFacade.getPackage(NPM_PROJECT, version);

        assertNotNull(npmPackage);
        assertNotNull(npmPackage.name());

        assertEquals(NPM_PROJECT, npmPackage.name().npmFullName);
        assertEquals(version, npmPackage.version());

        assertNotNull(npmPackage.repository());

        assertTrue(npmPackage.repository().url().startsWith(nexusUrl), "Package must come from the test Nexus");

        assertNotNull(npmPackage.dist());
        assertNotNull(npmPackage.dist().tarball());

        assertTrue(npmPackage.dist().tarball().toString().startsWith(nexusUrl), "Tarball must come from the test Nexus");
    }

    private List<BundleRecord> createMavenBundle(Gav gav) throws IOException {
        final Path directory = tempDir.resolve(gav.getVersion());

        Files.createDirectories(directory);

        final String base = gav.getArtifactId() + "-" + gav.getVersion();
        final Path pom = directory.resolve(base + ".pom");

        Files.writeString(pom,
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <project xmlns="http://maven.apache.org/POM/4.0.0">
                            <modelVersion>4.0.0</modelVersion>
                            <groupId>%s</groupId>
                            <artifactId>%s</artifactId>
                            <version>%s</version>
                        </project>
                        """.formatted(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()), StandardCharsets.UTF_8);
        final Path jar = createJar(directory.resolve(base + ".jar"), "artifact.txt");
        final Path sources = createJar(directory.resolve(base + "-sources.jar"), "source.txt");
        final Path javadoc = createJar(directory.resolve(base + "-javadoc.jar"), "index.html");

        return List.of(
                new BundleRecord(POM, pom),
                new BundleRecord(JAR, jar),
                new BundleRecord(SOURCES, sources),
                new BundleRecord(JAVADOC, javadoc));
    }

    private Path createJar(Path path, String entryName) throws IOException {

        try (final JarOutputStream jar = new JarOutputStream(Files.newOutputStream(path))) {
            jar.putNextEntry(new JarEntry(entryName));
            jar.write("test".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }

        return path;
    }

    private void awaitMavenContains(Gav gav) throws InterruptedException {
        final long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();

        while (System.nanoTime() < deadline) {
            if (mavenFacade.contains(gav.getGroupId(), gav.getArtifactId(), gav.getVersion())) {
                return;
            }

            Thread.sleep(200);
        }

        assertTrue(mavenFacade.contains(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()),
                "Artifact did not become searchable: " + gav);
    }

    /**
     * Up to extending classes to return their specific variant of the nexus image.
     * Uploads a generated artifact.
     *
     * @param gav The {@link Gav} of the artifact to upload.
     * @throws Exception if anything goes wrong
     */
    private void uploadGeneratedArtifact(Gav gav) throws Exception {
        final Path pom = packageFileLocator.getLocalFullPath(FileType.pom, gav.getGroupId(), gav.getArtifactId(),
                gav.getVersion());
        final Path jar = packageFileLocator.getLocalFullPath(FileType.jar, gav.getGroupId(), gav.getArtifactId(),
                gav.getVersion());
        final Path tgz = packageFileLocator.getLocalFullPath(FileType.tgz, gav.getGroupId(), gav.getArtifactId(),
                gav.getVersion());

        assertTrue(Files.exists(pom), "Generated POM does not exist: " + pom);
        assertTrue(Files.exists(jar), "Generated JAR does not exist: " + jar);
        assertTrue(Files.exists(tgz), "Generated TGZ does not exist: " + tgz);

        packageListener.createBundleFiles(pom, jar, tgz, List.of());

        final Path sources = packageFileLocator.getLocalFullPath(FileType.source, gav.getGroupId(), gav.getArtifactId(),
                gav.getVersion());
        final Path javadoc = packageFileLocator.getLocalFullPath(FileType.javadoc, gav.getGroupId(),
                gav.getArtifactId(),
                gav.getVersion());
        final String releaseId = mavenFacade.upload(gav, List.of(new BundleRecord(POM, pom), new BundleRecord(JAR, jar),
                new BundleRecord(SOURCES, sources), new BundleRecord(JAVADOC, javadoc)));

        assertNotNull(releaseId);
        assertFalse(releaseId.isBlank());

        awaitMavenContains(gav);
    }

    /**
     * Waits and verifies that a {@code pom}-file of a given artifact is found.
     *
     * @param gav The {@link Gav} of the artifact for which a {@code pom}-file
     *        should be found
     * @return The body of the latest response for the repository manager
     * @throws Exception if anything goes wrong
     */
    private String awaitMavenPom(Gav gav) throws Exception {
        final String baseUrl = nexusUrl.endsWith("/") ? nexusUrl.substring(0, nexusUrl.length() - 1) : nexusUrl;
        final String path = gav.getGroupId().replace('.', '/') + "/" + gav.getArtifactId() + "/" + gav.getVersion()
                + "/"
                + gav.getArtifactId() + "-" + gav.getVersion() + ".pom";

        final URI uri = URI.create(baseUrl + "/repository/" + releaseRepo + "/" + path);
        final String credentials = Base64.getEncoder()
                .encodeToString((nexusUsername + ":" + nexusPassword).getBytes(StandardCharsets.UTF_8));

        final HttpClient client = HttpClient.newHttpClient();
        final long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        HttpResponse<String> lastResponse = null;

        while (System.nanoTime() < deadline) {
            final HttpRequest request = HttpRequest.newBuilder(uri).header("Authorization", "Basic " + credentials)
                    .GET()
                    .build();

            lastResponse = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (lastResponse.statusCode() == 200) {
                return lastResponse.body();
            }

            Thread.sleep(200);
        }

        throw new AssertionError("POM did not become available at " + uri + "; last HTTP status: "
                + (lastResponse == null ? "no response" : lastResponse.statusCode()));
    }

    /**
     * Resolves the {@link Dependency} via the {@link PomService}.
     *
     * @param model The {@link Model} to resolve the {@link Dependency} from
     * @param artifactId The artifactId of the {@link Dependency} to resolve
     * @return The resolved {@link Dependency}
     */
    private Dependency getFromPomService(Model model, String artifactId) {
        if (model.getDependencies() == null) {
            return null;
        }

        return PomService.resolveDependencies(model).stream().filter(d -> artifactId.equals(d.getArtifactId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Up to extending classes to return their specific variant of the nexus image.
     *
     * <p>
     * <b>NOTE</b>: this is strictly used for logging, the manifest names are hardcoded in the responsible test-classes
     * </p>
     *
     * @return The variant, e.g. <code>community-latest</code>, <code>oss-v3.76.1</code>
     */
    protected abstract String variant();
}
