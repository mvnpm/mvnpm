package io.mvnpm.hosting;

import static io.mvnpm.hosting.NexusTestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.io.TempDir;

import io.mvnpm.Constants;
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
     *
     * <p>
     * <b>NOTE</b>: this is strictly used for logging, the manifest names are hardcoded in the responsible test-classes
     * </p>
     *
     * @return The variant, e.g. <code>community-latest</code>, <code>oss-v3.76.1</code>
     */
    protected abstract String variant();
}
