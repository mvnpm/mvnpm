package io.mvnpm.mavencentral.sync;

import static io.quarkus.hibernate.orm.panache.PanacheEntityBase.find;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import io.mvnpm.npm.model.Name;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.logging.Log;

@Entity
@NamedQueries({
        @NamedQuery(name = "CentralSyncItem.findByStage", query = "from CentralSyncItem where stage = ?1 order by stageChangeTime LIMIT 999"),
        @NamedQuery(name = "CentralSyncItem.findUploadedButNotReleased", query = "from CentralSyncItem where stage IN ?1 order by stageChangeTime")
})
public class CentralSyncItem extends PanacheEntity {
    public LocalDateTime startTime;
    public LocalDateTime stageChangeTime;
    public String stagingRepoId;
    public Stage stage;
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH, CascadeType.DETACH })
    public Name name;
    public String version;
    public int uploadAttempts = 0;
    public int promotionAttempts = 0;

    public CentralSyncItem() {

    }

    public CentralSyncItem(Name name, String version) {
        this.name = name;
        this.version = version;
        this.startTime = LocalDateTime.now();
        this.stage = Stage.NONE;
        this.stageChangeTime = LocalDateTime.now();
    }

    public static List<CentralSyncItem> findByStage(Stage stage) {
        return find("#CentralSyncItem.findByStage", stage).list();
    }

    public static List<CentralSyncItem> findNotReleased() {
        List<Stage> uploadedButNotReleased = Arrays.asList(Stage.UPLOADED, Stage.CLOSED, Stage.RELEASING);
        return find("#CentralSyncItem.findUploadedButNotReleased", uploadedButNotReleased).list();
    }

    public static CentralSyncItem findByGAV(String groupId, String artifactId, String version) {
        Name name = Name.find("mvnGroupId = ?1 and mvnArtifactId = ?2", groupId, artifactId).firstResult();
        if (name == null)
            return null;
        List<CentralSyncItem> list = CentralSyncItem.find("name = ?1 and version = ?2", name, version).list();
        if (list == null || list.isEmpty())
            return null;
        if (list.size() > 1) {
            // TODO: Clean up, find latest and delete others ?
            Log.error("Multiple GAV entries found for [" + groupId + ":" + artifactId + ":" + version + "]");
            return null;
        }
        return list.get(0);
    }

    public boolean isInProgress() {
        return this.stage.equals(Stage.CLOSED)
                || this.stage.equals(Stage.RELEASING)
                || this.stage.equals(Stage.UPLOADED)
                || this.stage.equals(Stage.UPLOADING);
    }

    public boolean isInError() {
        return this.stage.equals(Stage.ERROR);
    }

    public boolean alreadyRealeased() {
        return this.stage.equals(Stage.RELEASED);
    }

    public void increaseUploadAttempt() {
        this.uploadAttempts = this.uploadAttempts + 1;
    }

    public void increasePromotionAttempt() {
        this.promotionAttempts = this.promotionAttempts + 1;
    }

    @Override
    public String toString() {
        return name.mvnGroupId + ":" + name.mvnArtifactId + ":" + version + " [" + stage + "]";
    }
}
