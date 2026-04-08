package io.mvnpm.mavencentral.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.mvnpm.creator.PackageListener;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.mvnpm.mavencentral.MavenCentralFacade;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.DistTags;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.ProjectInfo;
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

    @InjectMock
    PackageListener packageListener;

    @InjectMock
    MavenCentralFacade mavenCentralFacade;

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
        ProjectInfo info = new ProjectInfo(new DistTags("1.0.0", null),
                Set.of("1.0.0"), tenDaysAgo.toString());
        Mockito.when(npmRegistryFacade.getProjectInfo("lit")).thenReturn(info);

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

    @Test
    void claimNextForUpload_claimsOldestInitItem() {
        createInitItem("org.mvnpm", "first", "1.0.0");
        createInitItem("org.mvnpm", "second", "1.0.0");

        CentralSyncItem claimed = centralSyncItemService.claimNextForUpload();

        assertNotNull(claimed);
        assertEquals("first", claimed.artifactId);
        assertEquals(Stage.UPLOADING, claimed.stage);
        assertEquals(1, claimed.uploadAttempts);
    }

    @Test
    void claimNextForUpload_returnsNullWhenEmpty() {
        CentralSyncItem claimed = centralSyncItemService.claimNextForUpload();
        assertNull(claimed);
    }

    @Test
    void claimNextForUpload_skipsNonInitItems() {
        createItem("org.mvnpm", "uploading-pkg", "1.0.0");
        changeStage("org.mvnpm", "uploading-pkg", "1.0.0", Stage.UPLOADING);

        CentralSyncItem claimed = centralSyncItemService.claimNextForUpload();
        assertNull(claimed);
    }

    @Test
    void claimNextForUpload_sequentialClaimsGetDifferentItems() {
        createInitItem("org.mvnpm", "a", "1.0.0");
        createInitItem("org.mvnpm", "b", "1.0.0");

        CentralSyncItem first = centralSyncItemService.claimNextForUpload();
        CentralSyncItem second = centralSyncItemService.claimNextForUpload();

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first.artifactId, second.artifactId);
    }

    @Transactional
    CentralSyncItem createInitItem(String groupId, String artifactId, String version) {
        return centralSyncItemService.findOrCreate(groupId, artifactId, version, Stage.INIT);
    }

    @Transactional
    void changeStage(String groupId, String artifactId, String version, Stage stage) {
        CentralSyncItem item = CentralSyncItem.findById(new Gav(groupId, artifactId, version));
        if (item != null) {
            item.stage = stage;
            item.persist();
        }
    }

    @Test
    void changeStage_sameStageIsNoOp() {
        CentralSyncItem item = createItem("org.mvnpm", "noop-pkg", "1.0.0");
        changeStage("org.mvnpm", "noop-pkg", "1.0.0", Stage.CLOSED);

        // First change to RELEASED should succeed
        item = reloadItem("org.mvnpm", "noop-pkg", "1.0.0");
        CentralSyncItem result1 = centralSyncItemService.changeStage(item, Stage.RELEASED);
        assertNotNull(result1);
        assertEquals(Stage.RELEASED, result1.stage);

        // Second change to RELEASED should be a no-op (same stage)
        item = reloadItem("org.mvnpm", "noop-pkg", "1.0.0");
        CentralSyncItem result2 = centralSyncItemService.changeStage(item, Stage.RELEASED);
        assertNotNull(result2);
        assertEquals(Stage.RELEASED, result2.stage);
    }

    @Test
    void claimNextForErrorRetry_claimsErrorItem() {
        createInitItem("org.mvnpm", "err-pkg", "1.0.0");
        changeStage("org.mvnpm", "err-pkg", "1.0.0", Stage.ERROR);

        CentralSyncItem claimed = centralSyncItemService.claimNextForErrorRetry();

        assertNotNull(claimed);
        assertEquals("err-pkg", claimed.artifactId);
        assertEquals(Stage.PACKAGING, claimed.stage);
    }

    @Test
    void claimNextForErrorRetry_returnsNullWhenNoErrors() {
        createInitItem("org.mvnpm", "ok-pkg", "1.0.0");

        CentralSyncItem claimed = centralSyncItemService.claimNextForErrorRetry();
        assertNull(claimed);
    }

    @Test
    void claimNextForErrorRetry_sequentialClaimsGetDifferentItems() {
        createInitItem("org.mvnpm", "err-a", "1.0.0");
        changeStage("org.mvnpm", "err-a", "1.0.0", Stage.ERROR);
        createInitItem("org.mvnpm", "err-b", "1.0.0");
        changeStage("org.mvnpm", "err-b", "1.0.0", Stage.ERROR);

        CentralSyncItem first = centralSyncItemService.claimNextForErrorRetry();
        CentralSyncItem second = centralSyncItemService.claimNextForErrorRetry();

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first.artifactId, second.artifactId);
    }

    @Test
    void claimNextForPackagingCheck_claimsPackagingItem() {
        createInitItem("org.mvnpm", "pack-pkg", "1.0.0");
        changeStage("org.mvnpm", "pack-pkg", "1.0.0", Stage.PACKAGING);

        CentralSyncItem claimed = centralSyncItemService.claimNextForPackagingCheck();

        assertNotNull(claimed);
        assertEquals("pack-pkg", claimed.artifactId);
        assertEquals(Stage.PACKAGING, claimed.stage);
    }

    @Test
    void claimNextForPackagingCheck_returnsNullWhenEmpty() {
        CentralSyncItem claimed = centralSyncItemService.claimNextForPackagingCheck();
        assertNull(claimed);
    }

    @Test
    void claimNextForPackagingCheck_skipsNonPackagingItems() {
        createInitItem("org.mvnpm", "init-only", "1.0.0");

        CentralSyncItem claimed = centralSyncItemService.claimNextForPackagingCheck();
        assertNull(claimed);
    }

    @Test
    void claimForErrorRetry_claimsSpecificErrorItem() {
        createInitItem("org.mvnpm", "err-specific", "1.0.0");
        changeStage("org.mvnpm", "err-specific", "1.0.0", Stage.ERROR);
        setAttemptCounters("org.mvnpm", "err-specific", "1.0.0", 3, 2);

        CentralSyncItem claimed = centralSyncItemService.claimForErrorRetry(
                new Gav("org.mvnpm", "err-specific", "1.0.0"));

        assertNotNull(claimed);
        assertEquals("err-specific", claimed.artifactId);
        assertEquals(Stage.PACKAGING, claimed.stage);
        assertEquals(2, claimed.uploadAttempts);
        assertEquals(1, claimed.promotionAttempts);
    }

    @Test
    void claimForErrorRetry_returnsNullForNonErrorItem() {
        createInitItem("org.mvnpm", "not-error", "1.0.0");

        CentralSyncItem claimed = centralSyncItemService.claimForErrorRetry(
                new Gav("org.mvnpm", "not-error", "1.0.0"));

        assertNull(claimed);
    }

    @Test
    void claimForErrorRetry_returnsNullForNonExistentItem() {
        CentralSyncItem claimed = centralSyncItemService.claimForErrorRetry(
                new Gav("org.mvnpm", "ghost", "1.0.0"));

        assertNull(claimed);
    }

    @Test
    void processUpload_ensuresFilesExistBeforeSync() {
        // Create an UPLOADING item (as if just claimed)
        CentralSyncItem item = createInitItem("org.mvnpm", "ensure-files-pkg", "1.0.0");
        changeStage("org.mvnpm", "ensure-files-pkg", "1.0.0", Stage.UPLOADING);
        item = reloadItem("org.mvnpm", "ensure-files-pkg", "1.0.0");

        // Call processNextAction directly (normally triggered by stage-change event)
        continuousSyncService.processNextAction(item);

        // Verify that getPath was called to ensure files exist
        Mockito.verify(mavenRepositoryService).getPath(Mockito.any(Name.class), Mockito.eq("1.0.0"),
                Mockito.any());
        // Verify that createBundleFiles was called for remaining bundle files
        Mockito.verify(packageListener).createBundleFiles(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void processUpload_alreadySynced_marksReleased() {
        // Create an UPLOADING item
        createInitItem("org.mvnpm", "synced-pkg", "2.0.0");
        changeStage("org.mvnpm", "synced-pkg", "2.0.0", Stage.UPLOADING);
        CentralSyncItem item = reloadItem("org.mvnpm", "synced-pkg", "2.0.0");

        // Make getPath throw PackageAlreadySyncedException (package already on Central)
        Mockito.when(mavenRepositoryService.getPath(Mockito.any(Name.class), Mockito.eq("2.0.0"), Mockito.any()))
                .thenThrow(new PackageAlreadySyncedException("test", new Name("synced-pkg"), "2.0.0", null));

        // Process the upload
        continuousSyncService.processNextAction(item);

        // Item should be marked as RELEASED
        CentralSyncItem updated = reloadItem("org.mvnpm", "synced-pkg", "2.0.0");
        assertEquals(Stage.RELEASED, updated.stage);
    }

    @Test
    void resetUpload_movesToErrorAfterTooManyAttempts() {
        createInitItem("org.mvnpm", "stuck-pkg", "1.0.0");
        changeStage("org.mvnpm", "stuck-pkg", "1.0.0", Stage.UPLOADING);
        setAttemptCounters("org.mvnpm", "stuck-pkg", "1.0.0", 9, 0);
        // Make stageChangeTime old enough to be considered stale (>30 min)
        setStageChangeTime("org.mvnpm", "stuck-pkg", "1.0.0", LocalDateTime.now().minusHours(1));

        continuousSyncService.periodicResetUpload();

        CentralSyncItem updated = reloadItem("org.mvnpm", "stuck-pkg", "1.0.0");
        assertEquals(Stage.ERROR, updated.stage, "Item with 10+ attempts should move to ERROR");
    }

    @Test
    void resetUpload_resetsToInitWhenUnderAttemptLimit() {
        createInitItem("org.mvnpm", "retry-pkg", "1.0.0");
        changeStage("org.mvnpm", "retry-pkg", "1.0.0", Stage.UPLOADING);
        setAttemptCounters("org.mvnpm", "retry-pkg", "1.0.0", 2, 0);
        setStageChangeTime("org.mvnpm", "retry-pkg", "1.0.0", LocalDateTime.now().minusHours(1));

        continuousSyncService.periodicResetUpload();

        CentralSyncItem updated = reloadItem("org.mvnpm", "retry-pkg", "1.0.0");
        assertEquals(Stage.INIT, updated.stage, "Item under attempt limit should reset to INIT");
    }

    @Test
    void processUpload_compositeWithoutTgz_passesNullTgz() {
        // Create an UPLOADING item
        createInitItem("org.mvnpm.at.mvnpm", "composite-pkg", "1.0.0");
        changeStage("org.mvnpm.at.mvnpm", "composite-pkg", "1.0.0", Stage.UPLOADING);
        CentralSyncItem item = reloadItem("org.mvnpm.at.mvnpm", "composite-pkg", "1.0.0");

        // getPath returns a non-existent path (simulating composite with no tgz)
        Mockito.when(mavenRepositoryService.getPath(Mockito.any(Name.class), Mockito.eq("1.0.0"), Mockito.any()))
                .thenReturn(Path.of("/tmp/nonexistent-jar.jar"));

        continuousSyncService.processNextAction(item);

        // Verify createBundleFiles was called with null tgz (third argument)
        Mockito.verify(packageListener).createBundleFiles(Mockito.any(), Mockito.any(), Mockito.isNull(), Mockito.any());
    }

    @Transactional
    void setStageChangeTime(String groupId, String artifactId, String version, LocalDateTime time) {
        CentralSyncItem item = CentralSyncItem.findById(new Gav(groupId, artifactId, version));
        if (item != null) {
            item.stageChangeTime = time;
            item.persist();
        }
    }

    @Transactional
    void setAttemptCounters(String groupId, String artifactId, String version,
            int uploadAttempts, int promotionAttempts) {
        CentralSyncItem item = CentralSyncItem.findById(new Gav(groupId, artifactId, version));
        if (item != null) {
            item.uploadAttempts = uploadAttempts;
            item.promotionAttempts = promotionAttempts;
            item.persist();
        }
    }

    @Transactional
    CentralSyncItem reloadItem(String groupId, String artifactId, String version) {
        return CentralSyncItem.findById(new Gav(groupId, artifactId, version));
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
