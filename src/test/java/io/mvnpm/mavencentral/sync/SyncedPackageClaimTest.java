package io.mvnpm.mavencentral.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SyncedPackageClaimTest {

    @BeforeEach
    @Transactional
    void cleanup() {
        SyncedPackage.deleteAll();
    }

    @Test
    @Transactional
    void claimBatchPicksDueItemsOnly() {
        LocalDateTime now = LocalDateTime.now();
        // Due items: null nextCheck and past nextCheck
        insertPackage("org.mvnpm", "lit", null);
        insertPackage("org.mvnpm", "vue", now.minusHours(1));
        insertPackage("org.mvnpm", "react", now.minusDays(1));
        // Not due: future nextCheck
        insertPackage("org.mvnpm", "angular", now.plusHours(2));
        insertPackage("org.mvnpm", "svelte", now.plusDays(1));

        LocalDateTime claimUntil = now.plusHours(1);
        int claimed = SyncedPackage.claimBatch(10, claimUntil);

        assertEquals(3, claimed);

        List<SyncedPackage> claimedItems = SyncedPackage.findClaimed(claimUntil);
        assertEquals(3, claimedItems.size());

        Set<String> claimedArtifacts = claimedItems.stream()
                .map(p -> p.artifactId)
                .collect(Collectors.toSet());
        assertTrue(claimedArtifacts.contains("lit"));
        assertTrue(claimedArtifacts.contains("vue"));
        assertTrue(claimedArtifacts.contains("react"));
    }

    @Test
    @Transactional
    void claimBatchRespectsLimit() {
        insertPackage("org.mvnpm", "a", null);
        insertPackage("org.mvnpm", "b", null);
        insertPackage("org.mvnpm", "c", null);

        LocalDateTime claimUntil = LocalDateTime.now().plusHours(1);
        int claimed = SyncedPackage.claimBatch(2, claimUntil);

        assertEquals(2, claimed);
    }

    @Test
    @Transactional
    void claimBatchReturnsZeroWhenNothingDue() {
        insertPackage("org.mvnpm", "future", LocalDateTime.now().plusDays(1));

        LocalDateTime claimUntil = LocalDateTime.now().plusHours(1);
        int claimed = SyncedPackage.claimBatch(10, claimUntil);

        assertEquals(0, claimed);
    }

    @Test
    @Transactional
    void doubleClaimDoesNotReclaimAlreadyClaimed() {
        insertPackage("org.mvnpm", "lit", null);
        insertPackage("org.mvnpm", "vue", null);

        LocalDateTime claim1 = LocalDateTime.now().plusHours(1);
        int firstClaim = SyncedPackage.claimBatch(10, claim1);
        assertEquals(2, firstClaim);

        // Second claim should find nothing — items already have future nextCheck
        LocalDateTime claim2 = LocalDateTime.now().plusHours(2);
        int secondClaim = SyncedPackage.claimBatch(10, claim2);
        assertEquals(0, secondClaim);
    }

    private void insertPackage(String groupId, String artifactId, LocalDateTime nextCheck) {
        SyncedPackage pkg = new SyncedPackage(groupId, artifactId);
        pkg.nextCheck = nextCheck;
        pkg.persist();
    }
}
