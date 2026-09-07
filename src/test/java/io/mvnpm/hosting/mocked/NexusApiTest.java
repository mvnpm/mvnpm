package io.mvnpm.hosting.mocked;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.mvnpm.Constants.SLASH;
import static io.mvnpm.hosting.NexusTestFixtures.MAVEN_ARTIFACT_ID;
import static io.mvnpm.hosting.NexusTestFixtures.MAVEN_GROUP_ID;
import static io.mvnpm.hosting.NexusTestFixtures.MAVEN_RELEASE_VERSION;
import static io.mvnpm.hosting.NexusTestFixtures.MAVEN_SNAPSHOT_VERSION;
import static io.mvnpm.hosting.NexusTestFixtures.NEXUS_NPM_SCOPE;
import static io.mvnpm.hosting.NexusTestFixtures.NPM_NAME;
import static io.mvnpm.hosting.NexusTestFixtures.NPM_PROJECT;
import static io.mvnpm.hosting.NexusTestFixtures.NPM_RELEASE_FIXTURE;
import static io.mvnpm.hosting.NexusTestFixtures.NPM_RELEASE_VERSION;
import static io.mvnpm.hosting.NexusTestFixtures.NPM_REPOSITORY;
import static io.mvnpm.hosting.NexusTestFixtures.NPM_SNAPSHOT_FIXTURE;
import static io.mvnpm.hosting.NexusTestFixtures.NPM_SNAPSHOT_VERSION;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import io.mvnpm.hosting.profiles.NexusApiMockTestProfile;
import io.mvnpm.hosting.resources.NexusApiMockTestResource;
import io.mvnpm.maven.api.MavenFacade;
import io.mvnpm.maven.api.ReleaseStatus;
import io.mvnpm.maven.api.Stage;
import io.mvnpm.maven.sync.SyncItem;
import io.mvnpm.nexus.mvn.NexusMavenFacade;
import io.mvnpm.nexus.npm.NexusRegistryFacade;
import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.Project;
import io.mvnpm.npm.model.SearchResults;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * A mock-test of the nexus-api.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@QuarkusTest
@TestProfile(NexusApiMockTestProfile.class)
class NexusApiTest {

    private static final String ASSET_SEARCH = "/service/rest/v1/search/assets";
    private static final String COMPONENT_SEARCH = "/service/rest/v1/search";
    private static final String RELEASE_REPOSITORY = "test-releases";
    private static final String SNAPSHOT_REPOSITORY = "test-snapshots";

    private static final String FALLBACK_PROJECT = "fallback-package";
    private static final String FALLBACK_VERSION = "9.9.9";
    private static final String FALLBACK_PATH = "/fallback/";
    private static final String FALLBACK_PATH_RGX = FALLBACK_PATH + ".*";

    @Inject
    NpmFacade npmFacade;

    @Inject
    MavenFacade mavenFacade;

    private WireMockServer server;

    @BeforeEach
    void resetWireMock() {
        server = NexusApiMockTestResource.server();
        server.resetAll();

        assertInstanceOf(NexusRegistryFacade.class, npmFacade, "NexusApiTest must use NexusRegistryFacade");
        assertInstanceOf(NexusMavenFacade.class, mavenFacade, "NexusApiTest must use NexusMavenFacade");
    }

    // ---------------------------------------------------------------------
    // npm: getProject
    // ---------------------------------------------------------------------

    @Test
    void getProjectUsesNameAndScopeAndReturnsAllVersions() {
        server.stubFor(npmAssetSearch(NPM_NAME, null, NEXUS_NPM_SCOPE)
                .willReturn(jsonResponse(npmAssetsResponse(
                        npmAsset(
                                NPM_PROJECT,
                                NPM_RELEASE_VERSION,
                                server.baseUrl()
                                        + "/unused/release.tgz",
                                NPM_NAME + "/-/release.tgz"),
                        npmAsset(
                                NPM_PROJECT,
                                NPM_SNAPSHOT_VERSION,
                                server.baseUrl()
                                        + "/unused/snapshot.tgz",
                                NPM_NAME + "/-/snapshot.tgz")))));

        final Project project = npmFacade.getProject(NPM_PROJECT);

        assertNotNull(project);
        assertNotNull(project.name());
        assertNotNull(project.distTags());

        assertEquals(NPM_PROJECT, project.name().npmFullName);
        assertEquals(Set.of(NPM_RELEASE_VERSION, NPM_SNAPSHOT_VERSION), project.versions());
        assertEquals(NPM_RELEASE_VERSION, project.distTags().latest());
        server.verify(1, npmAssetSearchRequest(NPM_NAME, null, NEXUS_NPM_SCOPE));

        /*
         * Nexus contained the package, therefore the official npm fallback
         * must not have been contacted.
         */
        server.verify(0, getRequestedFor(urlPathMatching(FALLBACK_PATH_RGX)));
    }

    @Test
    void getProjectWithoutNexusResultFallsBackToNpmRegistry() {
        server.stubFor(npmAssetSearch(FALLBACK_PROJECT, null, null)
                .willReturn(jsonResponse(emptyItems())));

        server.stubFor(get(urlPathEqualTo(FALLBACK_PATH + FALLBACK_PROJECT))
                .willReturn(jsonResponse(fallbackProjectJson())));

        final Project project = npmFacade.getProject(FALLBACK_PROJECT);

        assertNotNull(project);
        assertNotNull(project.name());

        assertEquals(FALLBACK_PROJECT, project.name().npmFullName);
        assertEquals(Set.of(FALLBACK_VERSION), project.versions());
        assertEquals(FALLBACK_VERSION, project.distTags().latest());

        /*
         * This also verifies the unscoped-package behavior:
         * npm.scope must not be sent.
         */
        server.verify(1, npmAssetSearchRequest(FALLBACK_PROJECT, null, null));
        server.verify(1, getRequestedFor(urlPathEqualTo(FALLBACK_PATH + FALLBACK_PROJECT)));
    }

    // ---------------------------------------------------------------------
    // npm: getPackage
    // ---------------------------------------------------------------------

    @Test
    void getReleasePackageUsesExactNexusCoordinates() {
        final String tarballPath = "/tarballs/mvnpm-package-1.0.0.tgz";

        stubTarball(tarballPath, NPM_RELEASE_FIXTURE);
        server.stubFor(npmAssetSearch(NPM_NAME, NPM_RELEASE_VERSION, NEXUS_NPM_SCOPE)
                .willReturn(jsonResponse(npmAssetsResponse(npmAsset(
                        NPM_PROJECT,
                        NPM_RELEASE_VERSION,
                        "http://nexus-internal:8081"
                                + tarballPath,
                        NPM_NAME
                                + "/-/"
                                + "mvnpm-package-1.0.0.tgz")))));

        final Package npmPackage = npmFacade.getPackage(NPM_PROJECT, NPM_RELEASE_VERSION);

        assertNexusPackage(npmPackage, NPM_RELEASE_VERSION, tarballPath);

        server.verify(1, npmAssetSearchRequest(NPM_NAME, NPM_RELEASE_VERSION, NEXUS_NPM_SCOPE));
        server.verify(0, getRequestedFor(urlPathMatching(FALLBACK_PATH + ".*")));
    }

    @Test
    void getSnapshotPackageUsesExactNexusCoordinates() {
        final String tarballPath = "/tarballs/mvnpm-package-1.0.0-SNAPSHOT.tgz";

        stubTarball(tarballPath, NPM_SNAPSHOT_FIXTURE);

        server.stubFor(npmAssetSearch(NPM_NAME, NPM_SNAPSHOT_VERSION, NEXUS_NPM_SCOPE)
                .willReturn(jsonResponse(npmAssetsResponse(npmAsset(
                        NPM_PROJECT,
                        NPM_SNAPSHOT_VERSION,
                        "http://nexus-internal:8081"
                                + tarballPath,
                        NPM_NAME
                                + "/-/"
                                + "mvnpm-package-1.0.0-SNAPSHOT.tgz")))));

        final Package npmPackage = npmFacade.getPackage(NPM_PROJECT, NPM_SNAPSHOT_VERSION);

        assertNexusPackage(npmPackage, NPM_SNAPSHOT_VERSION, tarballPath);

        server.verify(1, npmAssetSearchRequest(NPM_NAME, NPM_SNAPSHOT_VERSION, NEXUS_NPM_SCOPE));
        server.verify(0, getRequestedFor(urlPathMatching(FALLBACK_PATH_RGX)));
    }

    @Test
    void getPackageWithoutNexusResultFallsBackToNpmRegistry() {
        server.stubFor(npmAssetSearch(FALLBACK_PROJECT, FALLBACK_VERSION, null)
                .willReturn(jsonResponse(emptyItems())));

        server.stubFor(get(urlPathEqualTo(FALLBACK_PATH + FALLBACK_PROJECT + "/" + FALLBACK_VERSION))
                .willReturn(jsonResponse(fallbackPackageJson())));

        final Package npmPackage = npmFacade.getPackage(FALLBACK_PROJECT, FALLBACK_VERSION);

        assertNotNull(npmPackage);
        assertNotNull(npmPackage.name());

        assertEquals(FALLBACK_PROJECT, npmPackage.name().npmFullName);
        assertEquals(FALLBACK_VERSION, npmPackage.version());

        server.verify(1, npmAssetSearchRequest(FALLBACK_PROJECT, FALLBACK_VERSION, null));
        server.verify(1, getRequestedFor(urlPathEqualTo(FALLBACK_PATH + FALLBACK_PROJECT + SLASH + FALLBACK_VERSION)));
    }

    // ---------------------------------------------------------------------
    // npm: search
    // ---------------------------------------------------------------------

    @Test
    void searchUsesNpmKeywordSearch() {
        final String term = "mvnpm";
        server.stubFor(npmComponentSearch(term).willReturn(jsonResponse(
                """
                        {
                          "items": [
                            {
                              "id": "component-1",
                              "repository": "test-npm",
                              "format": "npm",
                              "group": "@test",
                              "name": "mvnpm-package",
                              "version": "1.0.0",
                              "assets": []
                            },
                            {
                              "id": "component-2",
                              "repository": "test-npm",
                              "format": "npm",
                              "group": "@test",
                              "name": "another-package",
                              "version": "2.0.0",
                              "assets": []
                            }
                          ],
                          "continuationToken": null
                        }
                        """)));

        final SearchResults results = npmFacade.search(term, 0);

        assertNotNull(results);

        assertEquals(2, results.total());
        assertEquals(2, results.objects().size());
        assertEquals("mvnpm-package", results.objects().get(0).item().name());
        assertEquals("1.0.0", results.objects().get(0).item().version());

        server.verify(1, npmComponentSearchRequest(term));
    }

    @Test
    void searchWithNoResultsThrows() {
        final String term = "definitely-not-present";
        server.stubFor(npmComponentSearch(term).willReturn(jsonResponse(emptyItems())));
        assertThrows(WebApplicationException.class, () -> npmFacade.search(term, 0));
        server.verify(1, npmComponentSearchRequest(term));
    }

    // ---------------------------------------------------------------------
    // Maven: contains
    // ---------------------------------------------------------------------

    @Test
    void containsReleaseUsesVersionAndReleaseRepository() {
        stubMavenSearch(RELEASE_REPOSITORY, MAVEN_RELEASE_VERSION, false, true);

        final boolean contained = mavenFacade.contains(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, MAVEN_RELEASE_VERSION);

        /*
         * Verify before asserting the result so a regression in the actual
         * HTTP request produces a useful WireMock failure message.
         */
        server.verify(1, mavenSearchRequest(RELEASE_REPOSITORY, MAVEN_RELEASE_VERSION, false));
        assertTrue(contained);
    }

    @Test
    void containsSnapshotUsesBaseVersionAndSnapshotRepository() {
        stubMavenSearch(SNAPSHOT_REPOSITORY, MAVEN_SNAPSHOT_VERSION, true, true);

        final boolean contained = mavenFacade.contains(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, MAVEN_SNAPSHOT_VERSION);

        server.verify(1, mavenSearchRequest(SNAPSHOT_REPOSITORY, MAVEN_SNAPSHOT_VERSION, true));
        assertTrue(contained);
    }

    @Test
    void containsReturnsFalseWhenNexusHasNoMatchingArtifact() {
        final String version = "7.7.7";
        stubMavenSearch(RELEASE_REPOSITORY, version, false, false);

        final boolean contained = mavenFacade.contains(MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, version);
        server.verify(1, mavenSearchRequest(RELEASE_REPOSITORY, version, false));
        assertFalse(contained);
    }

    // ---------------------------------------------------------------------
    // Maven: status
    // ---------------------------------------------------------------------

    @Test
    void statusIsPublishedWhenArtifactExists() throws Exception {
        final String version = "2.0.0";
        stubMavenSearch(RELEASE_REPOSITORY, version, false, true);

        final SyncItem syncItem = syncItem(version, Stage.UPLOADED);
        assertEquals(ReleaseStatus.PUBLISHED, mavenFacade.status(syncItem, "ignored-release-id"));
    }

    @Test
    void statusIsPublishingWhenArtifactIsMissingButItemIsInProgress() throws Exception {
        final String version = "2.0.1";
        stubMavenSearch(RELEASE_REPOSITORY, version, false, false);

        final SyncItem syncItem = syncItem(version, Stage.UPLOADED);
        assertEquals(ReleaseStatus.PUBLISHING, mavenFacade.status(syncItem, "ignored-release-id"));
    }

    @Test
    void statusIsPendingWhenArtifactIsMissingAndItemHasNotStarted() throws Exception {
        final String version = "2.0.2";
        stubMavenSearch(RELEASE_REPOSITORY, version, false, false);

        final SyncItem syncItem = syncItem(version, Stage.INIT);
        assertEquals(ReleaseStatus.PENDING, mavenFacade.status(syncItem, "ignored-release-id"));
    }

    @Test
    void statusIsFailedWhenArtifactIsMissingAndItemIsInError() throws Exception {
        final String version = "2.0.3";
        stubMavenSearch(RELEASE_REPOSITORY, version, false, false);

        final SyncItem syncItem = syncItem(version, Stage.ERROR);
        assertEquals(ReleaseStatus.FAILED, mavenFacade.status(syncItem, "ignored-release-id"));
    }

    // ---------------------------------------------------------------------
    // Maven: state transition
    // ---------------------------------------------------------------------

    @Test
    void transitionsDoNotCauseAlreadyUploadedArtifactToBeUploadedAgain() {
        assertAll(
                () -> assertEquals(Stage.UPLOADED, mavenFacade.transition(ReleaseStatus.PENDING)),
                () -> assertEquals(Stage.UPLOADED, mavenFacade.transition(ReleaseStatus.VALIDATING)),
                () -> assertEquals(Stage.UPLOADED, mavenFacade.transition(ReleaseStatus.VALIDATED)),
                () -> assertEquals(Stage.UPLOADED, mavenFacade.transition(ReleaseStatus.PUBLISHING)),
                () -> assertEquals(Stage.RELEASED, mavenFacade.transition(ReleaseStatus.PUBLISHED)),
                () -> assertEquals(Stage.ERROR, mavenFacade.transition(ReleaseStatus.FAILED)));
    }

    // ---------------------------------------------------------------------
    // Helpers: npm
    // ---------------------------------------------------------------------

    private MappingBuilder npmAssetSearch(String name, String version, String scope) {
        MappingBuilder request = get(urlPathEqualTo(ASSET_SEARCH))
                .withQueryParam("repository", equalTo(NPM_REPOSITORY))
                .withQueryParam("format", equalTo("npm"))
                .withQueryParam("name", equalTo(name));

        if (version == null) {
            request = request.withQueryParam("version", absent());
        } else {
            request = request.withQueryParam("version", equalTo(version));
        }

        if (scope == null) {
            request = request.withQueryParam("npm.scope", absent());
        } else {
            request = request.withQueryParam("npm.scope", equalTo(scope));
        }

        return request;
    }

    private RequestPatternBuilder npmAssetSearchRequest(String name, String version, String scope) {
        RequestPatternBuilder request = getRequestedFor(urlPathEqualTo(ASSET_SEARCH))
                .withQueryParam("repository", equalTo(NPM_REPOSITORY))
                .withQueryParam("format", equalTo("npm"))
                .withQueryParam("name", equalTo(name));

        if (version == null) {
            request = request.withQueryParam("version", absent());
        } else {
            request = request.withQueryParam("version", equalTo(version));
        }

        if (scope == null) {
            request = request.withQueryParam("npm.scope", absent());
        } else {
            request = request.withQueryParam("npm.scope", equalTo(scope));
        }

        return request;
    }

    private MappingBuilder npmComponentSearch(String term) {
        return get(urlPathEqualTo(COMPONENT_SEARCH))
                .withQueryParam("repository", equalTo(NPM_REPOSITORY))
                .withQueryParam("format", equalTo("npm"))
                .withQueryParam("name", absent())
                .withQueryParam("npm.scope", absent())
                .withQueryParam("npm.keywords", equalTo(term));
    }

    private RequestPatternBuilder npmComponentSearchRequest(String term) {
        return getRequestedFor(urlPathEqualTo(COMPONENT_SEARCH))
                .withQueryParam("repository", equalTo(NPM_REPOSITORY))
                .withQueryParam("format", equalTo("npm"))
                .withQueryParam("name", absent())
                .withQueryParam("npm.scope", absent())
                .withQueryParam("npm.keywords", equalTo(term));
    }

    private void stubTarball(String path, String fixture) {
        server.stubFor(get(urlPathEqualTo(path)).willReturn(
                aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/gzip")
                        .withBody(readFixture(fixture))));
    }

    private void assertNexusPackage(Package npmPackage, String expectedVersion, String expectedTarballPath) {
        assertNotNull(npmPackage);
        assertNotNull(npmPackage.name());
        assertEquals(NPM_PROJECT, npmPackage.name().npmFullName);
        assertEquals(expectedVersion, npmPackage.version());
        assertNotNull(npmPackage.repository());
        assertEquals("npm", npmPackage.repository().type());
        assertEquals(server.baseUrl() + expectedTarballPath, npmPackage.repository().url());
        assertNotNull(npmPackage.dist());
        assertNotNull(npmPackage.dist().tarball());
        assertEquals(server.baseUrl() + expectedTarballPath, npmPackage.dist().tarball().toString());
    }

    private String npmAssetsResponse(String... assets) {
        return """
                {
                  "items": [
                    %s
                  ],
                  "continuationToken": null
                }
                """.formatted(String.join(",\n", assets));
    }

    private String npmAsset(String project, String version, String downloadUrl, String path) {
        return """
                {
                  "downloadUrl": "%s",
                  "path": "%s",
                  "id": "asset-%s",
                  "repository": "%s",
                  "format": "npm",
                  "checksum": {
                    "sha1": "0123456789012345678901234567890123456789",
                    "md5": "01234567890123456789012345678901"
                  },
                  "contentType": "application/gzip",
                  "fileSize": 1234,
                  "npm": {
                    "name": "%s",
                    "version": "%s"
                  }
                }
                """.formatted(downloadUrl, path, version, NPM_REPOSITORY, project, version);
    }

    private String fallbackProjectJson() {
        return """
                {
                  "name": "%s",
                  "description": "Fallback project",
                  "dist-tags": {
                    "latest": "%s",
                    "next": null
                  },
                  "versions": {
                    "%s": {}
                  }
                }
                """.formatted(FALLBACK_PROJECT, FALLBACK_VERSION, FALLBACK_VERSION);
    }

    private String fallbackPackageJson() {
        return """
                {
                  "_id": "%s@%s",
                  "name": "%s",
                  "version": "%s",
                  "description": "Fallback package",
                  "dist": {
                    "shasum": "0123456789012345678901234567890123456789",
                    "tarball": "https://example.invalid/%s-%s.tgz",
                    "fileCount": 1,
                    "unpackedSize": 100
                  }
                }
                """.formatted(FALLBACK_PROJECT, FALLBACK_VERSION, FALLBACK_PROJECT, FALLBACK_VERSION, FALLBACK_PROJECT,
                FALLBACK_VERSION);
    }

    // ---------------------------------------------------------------------
    // Helpers: Maven
    // ---------------------------------------------------------------------

    private void stubMavenSearch(String repository, String version, boolean snapshot, boolean found) {
        final MappingBuilder request = mavenSearch(repository, version, snapshot);
        final String response = found ? mavenFoundResponse(repository, version, snapshot) : emptyItems();

        server.stubFor(request.willReturn(jsonResponse(response)));
    }

    private MappingBuilder mavenSearch(String repository, String version, boolean snapshot) {
        MappingBuilder request = get(urlPathEqualTo(COMPONENT_SEARCH))
                .withQueryParam("repository", equalTo(repository))
                .withQueryParam("format", equalTo("maven2"))
                .withQueryParam("maven.groupId", equalTo(MAVEN_GROUP_ID))
                .withQueryParam("maven.artifactId", equalTo(MAVEN_ARTIFACT_ID));

        if (snapshot) {
            request = request.withQueryParam("version", absent())
                    .withQueryParam("maven.baseVersion", equalTo(version));
        } else {
            request = request.withQueryParam("version", equalTo(version))
                    .withQueryParam("maven.baseVersion", absent());
        }

        return request;
    }

    private RequestPatternBuilder mavenSearchRequest(String repository, String version, boolean snapshot) {
        RequestPatternBuilder request = getRequestedFor(urlPathEqualTo(COMPONENT_SEARCH))
                .withQueryParam("repository", equalTo(repository))
                .withQueryParam("format", equalTo("maven2"))
                .withQueryParam("maven.groupId", equalTo(MAVEN_GROUP_ID))
                .withQueryParam("maven.artifactId", equalTo(MAVEN_ARTIFACT_ID));

        if (snapshot) {
            request = request.withQueryParam("version", absent())
                    .withQueryParam("maven.baseVersion", equalTo(version));
        } else {
            request = request.withQueryParam("version", equalTo(version))
                    .withQueryParam("maven.baseVersion", absent());
        }

        return request;
    }

    private String mavenFoundResponse(String repository, String version, boolean snapshot) {
        final String storedVersion;
        if (snapshot) {
            /*
             * This is deliberately NOT "1.0.0-SNAPSHOT".
             *
             * It models the unique version actually stored by Nexus after
             * Maven snapshot deployment.
             */
            storedVersion = version.replace("-SNAPSHOT", "-20260902.120000-1");
        } else {
            storedVersion = version;
        }

        return """
                {
                  "items": [
                    {
                      "id": "maven-component-1",
                      "repository": "%s",
                      "format": "maven2",
                      "group": "%s",
                      "name": "%s",
                      "version": "%s",
                      "assets": []
                    }
                  ],
                  "continuationToken": null
                }
                """.formatted(repository, MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, storedVersion);
    }

    private SyncItem syncItem(String version, Stage stage) {
        final SyncItem syncItem = new SyncItem();

        syncItem.groupId = MAVEN_GROUP_ID;
        syncItem.artifactId = MAVEN_ARTIFACT_ID;
        syncItem.version = version;
        syncItem.stage = stage;

        return syncItem;
    }

    private ResponseDefinitionBuilder jsonResponse(String body) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private String emptyItems() {
        return """
                {
                  "items": [],
                  "continuationToken": null
                }
                """;
    }

    private byte[] readFixture(String resourceName) {
        try (InputStream input = NexusApiTest.class.getClassLoader().getResourceAsStream(resourceName)) {

            if (input == null) {
                throw new IllegalStateException("Missing test fixture: " + resourceName);
            }

            return input.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read test fixture: " + resourceName, e);
        }
    }
}
