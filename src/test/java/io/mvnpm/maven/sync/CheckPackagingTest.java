package io.mvnpm.maven.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
import io.mvnpm.maven.api.Gav;
import io.mvnpm.maven.api.Stage;
import io.mvnpm.npm.exceptions.GetPackageException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class CheckPackagingTest {

    @Inject
    ContinuousSyncService continuousSyncService;

    @Inject
    SyncItemService syncItemService;

    @InjectMock
    PackageCreator packageCreator;

    @InjectMock
    SyncService syncService;

    @BeforeEach
    @AfterEach
    @Transactional
    void cleanup() {
        SyncItem.deleteAll();
    }

    @Test
    void checkPackaging_npm404_deletesItem() {
        // Insert a PACKAGING item
        SyncItem item = insertPackagingItem("org.mvnpm", "nonexistent-pkg", "1.0.0");

        // Mock canProcessSync to return true
        Mockito.when(syncService.canProcessSync(Mockito.any())).thenReturn(true);

        // Mock packageCreator to throw GetPackageException with 404
        GetPackageException notFound = createGetPackageException(404);
        Mockito.when(packageCreator.getFromCacheOrCreate(Mockito.eq(FileType.jar), Mockito.any(), Mockito.eq("1.0.0")))
                .thenThrow(notFound);

        continuousSyncService.checkPackaging();

        // Item should be deleted from DB
        SyncItem found = findItem("org.mvnpm", "nonexistent-pkg", "1.0.0");
        assertNull(found, "PACKAGING item with NPM 404 should be deleted");
    }

    @Test
    void checkPackaging_npm429_doesNotDeleteItem() {
        // Insert a PACKAGING item
        insertPackagingItem("org.mvnpm", "rate-limited-pkg", "1.0.0");

        // Mock canProcessSync to return true
        Mockito.when(syncService.canProcessSync(Mockito.any())).thenReturn(true);

        // Mock packageCreator to throw GetPackageException with 429 (rate limited)
        GetPackageException rateLimited = createGetPackageException(429);
        Mockito.when(packageCreator.getFromCacheOrCreate(Mockito.eq(FileType.jar), Mockito.any(), Mockito.eq("1.0.0")))
                .thenThrow(rateLimited);

        continuousSyncService.checkPackaging();

        // Item should NOT be deleted — it's a transient error
        SyncItem found = findItem("org.mvnpm", "rate-limited-pkg", "1.0.0");
        assertEquals(Stage.PACKAGING, found.stage, "PACKAGING item with NPM 429 should remain");
    }

    @Transactional
    SyncItem insertPackagingItem(String groupId, String artifactId, String version) {
        return syncItemService.findOrCreate(groupId, artifactId, version, Stage.PACKAGING);
    }

    @Transactional
    SyncItem findItem(String groupId, String artifactId, String version) {
        return SyncItem.findById(new Gav(groupId, artifactId, version));
    }

    private GetPackageException createGetPackageException(int status) {
        Response response = Response.status(status).entity("test error").build();
        ClientWebApplicationException cause = new ClientWebApplicationException("test", response);
        return new GetPackageException("test-project", "1.0.0", cause);
    }
}
