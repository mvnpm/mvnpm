package io.mvnpm.maven.sync;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import io.mvnpm.maven.api.Gav;
import io.mvnpm.maven.api.Stage;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.logging.Log;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 * Manage the state of the Central Sync Item
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class SyncItemService {

    @Inject
    EventBus bus;

    @Transactional
    public SyncItem changeStage(SyncItem syncItem, Stage stage) {
        Gav gav = new Gav(syncItem.groupId, syncItem.artifactId, syncItem.version);
        // Lock the row to prevent two pods from changing the same item simultaneously
        SyncItem locked = SyncItem.findById(gav, LockModeType.PESSIMISTIC_WRITE);
        if (locked == null) {
            Log.warnf("[MULTI-POD] changeStage: item not found for %s, skipping", gav);
            return null;
        }
        if (locked.stage.equals(stage)) {
            Log.debugf("[MULTI-POD] changeStage: %s already at stage %s, skipping (likely handled by another pod)", gav,
                    stage);
            return locked;
        }
        Log.infof("[MULTI-POD] changeStage: %s %s -> %s", gav, locked.stage, stage);
        // Merge caller's pending field changes (e.g. releaseId, attempt counters)
        syncItem.stage = stage;
        syncItem.stageChangeTime = LocalDateTime.now();
        syncItem = merge(syncItem);
        syncItem.persist();
        if (stage == Stage.RELEASED) {
            SyncedPackage.createIfAbsent(syncItem.groupId, syncItem.artifactId);
        }
        bus.publish("sync-item-stage-change", syncItem);
        return syncItem;
    }

    @Transactional
    public SyncItem dependenciesChecked(SyncItem syncItem) {
        syncItem = merge(syncItem);
        syncItem.dependenciesChecked = true;
        syncItem.persist();
        return syncItem;
    }

    @Transactional
    public SyncItem claimForErrorRetry(Gav gav) {
        SyncItem item = SyncItem.findById(gav, LockModeType.PESSIMISTIC_WRITE);
        if (item == null || item.stage != Stage.ERROR) {
            return null;
        }
        return applyErrorRetry(item);
    }

    @Transactional
    public SyncItem increaseCreationAttempt(SyncItem syncItem) {
        syncItem = merge(syncItem);
        syncItem.increaseCreationAttempt();
        syncItem.persist();
        return syncItem;
    }

    @Transactional
    public SyncItem merge(SyncItem syncItem) {
        if (syncItem.isPersistent()) {
            return syncItem;
        }
        return Panache.getEntityManager().merge(syncItem);
    }

    @Transactional
    public SyncItem claimNextForUpload() {
        @SuppressWarnings("unchecked")
        List<SyncItem> candidates = Panache.getEntityManager()
                .createNativeQuery("SELECT * FROM centralsyncitem WHERE stage = :init "
                        + "ORDER BY stagechangetime ASC LIMIT 1 FOR UPDATE SKIP LOCKED", SyncItem.class)
                .setParameter("init", Stage.INIT.ordinal()).getResultList();
        if (candidates.isEmpty()) {
            return null;
        }
        SyncItem item = candidates.get(0);
        item.stage = Stage.UPLOADING;
        item.stageChangeTime = LocalDateTime.now();
        item.uploadAttempts++;
        item.persist();
        Log.infof("[MULTI-POD] Claimed for upload: %s (attempt %d)", item.toGavString(), item.uploadAttempts);
        return item;
    }

    @Transactional
    public SyncItem claimNextForErrorRetry() {
        @SuppressWarnings("unchecked")
        List<SyncItem> candidates = Panache.getEntityManager()
                .createNativeQuery("SELECT * FROM centralsyncitem WHERE stage = :error "
                        + "ORDER BY stagechangetime ASC LIMIT 1 FOR UPDATE SKIP LOCKED", SyncItem.class)
                .setParameter("error", Stage.ERROR.ordinal()).getResultList();
        if (candidates.isEmpty()) {
            return null;
        }
        return applyErrorRetry(candidates.get(0));
    }

    private SyncItem applyErrorRetry(SyncItem item) {
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
    public SyncItem claimNextForPackagingCheck() {
        @SuppressWarnings("unchecked")
        List<SyncItem> candidates = Panache.getEntityManager()
                .createNativeQuery("SELECT * FROM centralsyncitem WHERE stage = :packaging "
                        + "ORDER BY stagechangetime ASC LIMIT 1 FOR UPDATE SKIP LOCKED", SyncItem.class)
                .setParameter("packaging", Stage.PACKAGING.ordinal()).getResultList();
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(0);
    }

    @Transactional
    public void delete(SyncItem syncItem) {
        syncItem = merge(syncItem);
        syncItem.delete();
    }

    @Transactional
    public SyncItem find(String groupId, String artifactId, String version) {
        return SyncItem.findById(new Gav(groupId, artifactId, version));
    }

    @Transactional
    public SyncItem findOrCreate(String groupId, String artifactId, String version, Stage stage) {
        return SyncItem.findOrCreate(new Gav(groupId, artifactId, version), stage);
    }

}
