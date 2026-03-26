package io.mvnpm.mavencentral.sync;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.logging.Log;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 * Manage the state of the Central Sync Item
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class CentralSyncItemService {

    @Inject
    EventBus bus;

    @Transactional
    public CentralSyncItem changeStage(CentralSyncItem centralSyncItem, Stage stage) {
        Gav gav = new Gav(centralSyncItem.groupId, centralSyncItem.artifactId, centralSyncItem.version);
        // Lock the row to prevent two pods from changing the same item simultaneously
        CentralSyncItem locked = CentralSyncItem.findById(gav, LockModeType.PESSIMISTIC_WRITE);
        if (locked == null) {
            Log.warnf("[MULTI-POD] changeStage: item not found for %s, skipping", gav);
            return null;
        }
        if (locked.stage.equals(stage)) {
            Log.debugf("[MULTI-POD] changeStage: %s already at stage %s, skipping (likely handled by another pod)", gav, stage);
            return locked;
        }
        Log.infof("[MULTI-POD] changeStage: %s %s -> %s", gav, locked.stage, stage);
        // Merge caller's pending field changes (e.g. stagingRepoId, attempt counters)
        centralSyncItem.stage = stage;
        centralSyncItem.stageChangeTime = LocalDateTime.now();
        centralSyncItem = merge(centralSyncItem);
        centralSyncItem.persist();
        if (stage == Stage.RELEASED) {
            SyncedPackage.createIfAbsent(centralSyncItem.groupId, centralSyncItem.artifactId);
        }
        bus.publish("central-sync-item-stage-change", centralSyncItem);
        return centralSyncItem;
    }

    @Transactional
    public CentralSyncItem dependenciesChecked(CentralSyncItem centralSyncItem) {
        centralSyncItem = merge(centralSyncItem);
        centralSyncItem.dependenciesChecked = true;
        centralSyncItem.persist();
        return centralSyncItem;
    }

    @Transactional
    public CentralSyncItem tryErroredItemAgain(CentralSyncItem centralSyncItem) {
        if (centralSyncItem.uploadAttempts > 0) {
            centralSyncItem.uploadAttempts = centralSyncItem.uploadAttempts - 1;
        }
        if (centralSyncItem.promotionAttempts > 0) {
            centralSyncItem.promotionAttempts = centralSyncItem.promotionAttempts - 1;
        }
        return changeStage(centralSyncItem, Stage.PACKAGING);
    }

    @Transactional
    public CentralSyncItem increaseCreationAttempt(CentralSyncItem centralSyncItem) {
        centralSyncItem = merge(centralSyncItem);
        centralSyncItem.increaseCreationAttempt();
        centralSyncItem.persist();
        return centralSyncItem;
    }

    @Transactional
    public CentralSyncItem merge(CentralSyncItem centralSyncItem) {
        if (centralSyncItem.isPersistent()) {
            return centralSyncItem;
        }
        return Panache.getEntityManager().merge(centralSyncItem);
    }

    @Transactional
    public CentralSyncItem claimNextForUpload() {
        @SuppressWarnings("unchecked")
        List<CentralSyncItem> candidates = Panache.getEntityManager()
                .createNativeQuery(
                        "SELECT * FROM centralsyncitem WHERE stage = :init "
                                + "ORDER BY stagechangetime ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
                        CentralSyncItem.class)
                .setParameter("init", Stage.INIT.ordinal())
                .getResultList();
        if (candidates.isEmpty()) {
            return null;
        }
        CentralSyncItem item = candidates.get(0);
        item.stage = Stage.UPLOADING;
        item.stageChangeTime = LocalDateTime.now();
        item.uploadAttempts++;
        item.persist();
        Log.infof("[MULTI-POD] Claimed for upload: %s (attempt %d)", item.toGavString(), item.uploadAttempts);
        return item;
    }

    @Transactional
    public CentralSyncItem claimNextForErrorRetry() {
        @SuppressWarnings("unchecked")
        List<CentralSyncItem> candidates = Panache.getEntityManager()
                .createNativeQuery(
                        "SELECT * FROM centralsyncitem WHERE stage = :error "
                                + "ORDER BY stagechangetime ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
                        CentralSyncItem.class)
                .setParameter("error", Stage.ERROR.ordinal())
                .getResultList();
        if (candidates.isEmpty()) {
            return null;
        }
        CentralSyncItem item = candidates.get(0);
        if (item.uploadAttempts > 0) {
            item.uploadAttempts--;
        }
        if (item.promotionAttempts > 0) {
            item.promotionAttempts--;
        }
        item.stage = Stage.PACKAGING;
        item.stageChangeTime = LocalDateTime.now();
        item.persist();
        Log.infof("[MULTI-POD] Claimed for error retry: %s", item.toGavString());
        return item;
    }

    @Transactional
    public CentralSyncItem claimNextForPackagingCheck() {
        @SuppressWarnings("unchecked")
        List<CentralSyncItem> candidates = Panache.getEntityManager()
                .createNativeQuery(
                        "SELECT * FROM centralsyncitem WHERE stage = :packaging "
                                + "ORDER BY stagechangetime ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
                        CentralSyncItem.class)
                .setParameter("packaging", Stage.PACKAGING.ordinal())
                .getResultList();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(0);
    }

    @Transactional
    public void delete(CentralSyncItem centralSyncItem) {
        centralSyncItem = merge(centralSyncItem);
        centralSyncItem.delete();
    }

    @Transactional
    public CentralSyncItem find(String groupId, String artifactId, String version) {
        return CentralSyncItem.findById(new Gav(groupId, artifactId, version));
    }

    @Transactional
    public CentralSyncItem findOrCreate(String groupId, String artifactId, String version, Stage stage) {
        return CentralSyncItem.findOrCreate(new Gav(groupId, artifactId, version), stage);
    }

}
