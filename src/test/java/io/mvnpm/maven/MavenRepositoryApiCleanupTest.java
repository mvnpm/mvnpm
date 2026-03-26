package io.mvnpm.maven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageCreator;
import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.CentralSyncItemService;
import io.mvnpm.mavencentral.sync.CentralSyncService;
import io.mvnpm.mavencentral.sync.Gav;
import io.mvnpm.mavencentral.sync.Stage;
import io.mvnpm.npm.exceptions.GetPackageException;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.version.InvalidVersionException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class MavenRepositoryApiCleanupTest {

    @Inject
    MavenRepositoryApi api;

    @Inject
    CentralSyncItemService centralSyncItemService;

    @InjectMock
    CentralSyncService centralSyncService;

    @InjectMock
    MavenRepositoryService mavenRepositoryService;

    @InjectMock
    PackageCreator packageCreator;

    @BeforeEach
    @AfterEach
    @Transactional
    void cleanup() {
        CentralSyncItem.deleteAll();
    }

    @Test
    void resolveAndStream_npm404_deletesSyncItem() {
        CentralSyncItem item = insertItem("org.mvnpm", "nonexistent-pkg", "1.0.0");
        Mockito.when(centralSyncService.checkReleaseInDbAndCentral("org.mvnpm", "nonexistent-pkg", "1.0.0", true))
                .thenReturn(item);

        GetPackageException notFound = createGetPackageException(404);
        Mockito.when(mavenRepositoryService.getPath(Mockito.any(Name.class), Mockito.eq("1.0.0"), Mockito.eq(FileType.jar)))
                .thenThrow(notFound);

        NameVersion nv = new NameVersion(NameParser.fromNpmProject("nonexistent-pkg"), "1.0.0");
        assertThrows(GetPackageException.class, () -> api.resolveAndStream(nv, FileType.jar, Optional.empty(),
                mavenRepositoryService::getPath));

        assertNull(findItem("org.mvnpm", "nonexistent-pkg", "1.0.0"),
                "Sync item should be deleted after NPM 404");
    }

    @Test
    void resolveAndStream_npm429_keepsSyncItem() {
        CentralSyncItem item = insertItem("org.mvnpm", "rate-limited-pkg", "1.0.0");
        Mockito.when(centralSyncService.checkReleaseInDbAndCentral("org.mvnpm", "rate-limited-pkg", "1.0.0", true))
                .thenReturn(item);

        GetPackageException rateLimited = createGetPackageException(429);
        Mockito.when(mavenRepositoryService.getPath(Mockito.any(Name.class), Mockito.eq("1.0.0"), Mockito.eq(FileType.jar)))
                .thenThrow(rateLimited);

        NameVersion nv = new NameVersion(NameParser.fromNpmProject("rate-limited-pkg"), "1.0.0");
        assertThrows(GetPackageException.class, () -> api.resolveAndStream(nv, FileType.jar, Optional.empty(),
                mavenRepositoryService::getPath));

        CentralSyncItem found = findItem("org.mvnpm", "rate-limited-pkg", "1.0.0");
        assertEquals(Stage.PACKAGING, found.stage, "Sync item should remain after transient error");
    }

    @Test
    void resolveAndStream_invalidVersion_deletesSyncItem() {
        CentralSyncItem item = insertItem("org.mvnpm", "bad-version-pkg", "not-a-version");
        Mockito.when(centralSyncService.checkReleaseInDbAndCentral("org.mvnpm", "bad-version-pkg", "not-a-version", true))
                .thenReturn(item);

        Mockito.when(mavenRepositoryService.getPath(Mockito.any(Name.class), Mockito.eq("not-a-version"),
                Mockito.eq(FileType.jar)))
                .thenThrow(new InvalidVersionException("not-a-version"));

        NameVersion nv = new NameVersion(NameParser.fromNpmProject("bad-version-pkg"), "not-a-version");
        assertThrows(InvalidVersionException.class, () -> api.resolveAndStream(nv, FileType.jar, Optional.empty(),
                mavenRepositoryService::getPath));

        assertNull(findItem("org.mvnpm", "bad-version-pkg", "not-a-version"),
                "Sync item should be deleted after invalid version");
    }

    @Test
    void resolveAndStream_success_keepsSyncItem() {
        CentralSyncItem item = insertItem("org.mvnpm", "valid-pkg", "1.0.0");
        Mockito.when(centralSyncService.checkReleaseInDbAndCentral("org.mvnpm", "valid-pkg", "1.0.0", true))
                .thenReturn(item);

        Path fakePath = Path.of("target/cache/test-file.jar");
        Mockito.when(mavenRepositoryService.getPath(Mockito.any(Name.class), Mockito.eq("1.0.0"), Mockito.eq(FileType.jar)))
                .thenReturn(fakePath);

        // The streamPath will fail since the file doesn't exist, but the sync item should remain
        try {
            NameVersion nv = new NameVersion(NameParser.fromNpmProject("valid-pkg"), "1.0.0");
            api.resolveAndStream(nv, FileType.jar, Optional.empty(), mavenRepositoryService::getPath);
        } catch (Exception ignored) {
            // File doesn't exist, streaming fails — that's fine
        }

        CentralSyncItem found = findItem("org.mvnpm", "valid-pkg", "1.0.0");
        assertEquals(Stage.PACKAGING, found.stage, "Sync item should remain on success path");
    }

    @Transactional
    CentralSyncItem insertItem(String groupId, String artifactId, String version) {
        return centralSyncItemService.findOrCreate(groupId, artifactId, version, Stage.PACKAGING);
    }

    @Transactional
    CentralSyncItem findItem(String groupId, String artifactId, String version) {
        return CentralSyncItem.findById(new Gav(groupId, artifactId, version));
    }

    private GetPackageException createGetPackageException(int status) {
        Response response = Response.status(status).entity("test error").build();
        ClientWebApplicationException cause = new ClientWebApplicationException("test", response);
        return new GetPackageException("test-project", "1.0.0", cause);
    }
}
