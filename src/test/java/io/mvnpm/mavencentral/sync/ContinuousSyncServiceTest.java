package io.mvnpm.mavencentral.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.DistTags;
import io.mvnpm.npm.model.Project;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class ContinuousSyncServiceTest {

    @Inject
    ContinuousSyncService continuousSyncService;

    @Inject
    CentralSyncItemService centralSyncItemService;

    @InjectMock
    NpmRegistryFacade npmRegistryFacade;

    @InjectMock
    MavenRepositoryService mavenRepositoryService;

    @BeforeEach
    @Transactional
    void cleanup() {
        SyncedPackage.deleteAll();
        CentralSyncItem.deleteAll();
    }

    @Test
    void nextCheckInterval_activePackage() {
        assertEquals(Duration.ofHours(4), ContinuousSyncService.nextCheckInterval(2));
    }

    @Test
    void nextCheckInterval_recentPackage() {
        assertEquals(Duration.ofHours(12), ContinuousSyncService.nextCheckInterval(15));
    }

    @Test
    void nextCheckInterval_moderatePackage() {
        assertEquals(Duration.ofDays(1), ContinuousSyncService.nextCheckInterval(90));
    }

    @Test
    void nextCheckInterval_olderPackage() {
        assertEquals(Duration.ofDays(3), ContinuousSyncService.nextCheckInterval(365));
    }

    @Test
    void nextCheckInterval_abandonedPackage() {
        assertEquals(Duration.ofDays(30), ContinuousSyncService.nextCheckInterval(2000));
    }

    @Test
    void nextCheckInterval_boundaries() {
        assertEquals(Duration.ofHours(4), ContinuousSyncService.nextCheckInterval(0));
        assertEquals(Duration.ofHours(4), ContinuousSyncService.nextCheckInterval(6));
        assertEquals(Duration.ofHours(12), ContinuousSyncService.nextCheckInterval(7));
        assertEquals(Duration.ofHours(12), ContinuousSyncService.nextCheckInterval(29));
        assertEquals(Duration.ofDays(1), ContinuousSyncService.nextCheckInterval(30));
        assertEquals(Duration.ofDays(1), ContinuousSyncService.nextCheckInterval(179));
        assertEquals(Duration.ofDays(3), ContinuousSyncService.nextCheckInterval(180));
        assertEquals(Duration.ofDays(3), ContinuousSyncService.nextCheckInterval(1824));
        assertEquals(Duration.ofDays(30), ContinuousSyncService.nextCheckInterval(1825));
    }

    @Test
    void checkAllUpdatesNextCheck() {
        // Insert a due package
        insertPackage("org.mvnpm", "lit", null);

        // Mock NPM to return a project modified 10 days ago (→ 12h interval)
        Instant tenDaysAgo = Instant.now().minus(Duration.ofDays(10));
        Project project = new Project(null, "lit", new DistTags("1.0.0", null),
                null, null, Set.of("1.0.0"), Map.of("modified", tenDaysAgo.toString()));
        Mockito.when(npmRegistryFacade.getProject("lit")).thenReturn(project);

        // MavenRepositoryService is mocked — getPath returns null by default (no-op)

        continuousSyncService.checkAll();

        // Verify nextCheck was set (should be ~12h from now)
        SyncedPackage updated = findPackage("org.mvnpm", "lit");
        assertNotNull(updated, "SyncedPackage should still exist");
        assertNotNull(updated.nextCheck, "nextCheck should be set after checkAll");
        Duration untilNextCheck = Duration.between(LocalDateTime.now(), updated.nextCheck);
        assertTrue(untilNextCheck.toHours() >= 11, "nextCheck should be at least 11h from now, was: " + untilNextCheck);
        assertTrue(untilNextCheck.toHours() <= 13, "nextCheck should be at most 13h from now, was: " + untilNextCheck);
    }

    @Test
    void checkAllSkipsFutureItems() {
        // Insert a package not yet due
        insertPackage("org.mvnpm", "vue", LocalDateTime.now().plusDays(1));

        continuousSyncService.checkAll();

        // nextCheck should remain unchanged (still ~1 day in future)
        SyncedPackage pkg = findPackage("org.mvnpm", "vue");
        assertNotNull(pkg);
        Duration untilNextCheck = Duration.between(LocalDateTime.now(), pkg.nextCheck);
        assertTrue(untilNextCheck.toHours() >= 22, "Future package nextCheck should not change");
    }

    @Test
    void changeStageToReleasedCreatesSyncedPackage() {
        // Create a CentralSyncItem and move it to RELEASED
        CentralSyncItem item = createItem("org.mvnpm", "released-pkg", "2.0.0");
        centralSyncItemService.changeStage(item, Stage.RELEASED);

        // SyncedPackage should be auto-created
        SyncedPackage pkg = findPackage("org.mvnpm", "released-pkg");
        assertNotNull(pkg, "SyncedPackage should be created when item reaches RELEASED");
    }

    @Transactional
    CentralSyncItem createItem(String groupId, String artifactId, String version) {
        return centralSyncItemService.findOrCreate(groupId, artifactId, version, Stage.UPLOADED);
    }

    @Transactional
    void insertPackage(String groupId, String artifactId, LocalDateTime nextCheck) {
        SyncedPackage pkg = new SyncedPackage(groupId, artifactId);
        pkg.nextCheck = nextCheck;
        pkg.persist();
    }

    @Transactional
    SyncedPackage findPackage(String groupId, String artifactId) {
        return SyncedPackage.findById(new SyncedPackageId(groupId, artifactId));
    }
}
