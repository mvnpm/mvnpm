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
    public CentralSyncItem merge(CentralSyncItem centralSyncItem) {
        if (centralSyncItem.isPersistent()) {
            return centralSyncItem;
        }
        return Panache.getEntityManager().merge(centralSyncItem);
    }

    @Transactional
    public CentralSyncItem findOrCreate(String groupId, String artifactId, String version) {
        return findOrCreate(new Gav(groupId, artifactId, version));
    }

    @Transactional
    public CentralSyncItem findOrCreate(Gav gav) {
        CentralSyncItem existing = CentralSyncItem.findById(gav);
        if (existing != null)
            return existing;

        CentralSyncItem newCentralSyncItem = new CentralSyncItem(gav.getGroupId(), gav.getArtifactId(), gav.getVersion());
        newCentralSyncItem.persist();

        return newCentralSyncItem;
    }

}
