package io.mvnpm.mavencentral.sync;

import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import io.quarkus.hibernate.orm.panache.Panache;
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
        if (!centralSyncItem.stage.equals(stage)) {
            centralSyncItem = merge(centralSyncItem);
            centralSyncItem.stage = stage;
            centralSyncItem.stageChangeTime = LocalDateTime.now();
            centralSyncItem.persist();
            bus.publish("central-sync-item-stage-change", centralSyncItem);
        }
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
    public CentralSyncItem find(String groupId, String artifactId, String version) {
        return CentralSyncItem.findById(new Gav(groupId, artifactId, version));
    }

    @Transactional
    public CentralSyncItem findOrCreate(String groupId, String artifactId, String version, Stage stage) {
        return CentralSyncItem.findOrCreate(new Gav(groupId, artifactId, version), stage);
    }

}
