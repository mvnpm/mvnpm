package io.mvnpm.mavencentral.sync;

import static io.quarkus.hibernate.orm.panache.PanacheEntityBase.find;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@IdClass(Gav.class)
@NamedQueries({
        @NamedQuery(name = "CentralSyncItem.findByStage", query = "from CentralSyncItem where stage = ?1 order by stageChangeTime LIMIT 999"),
        @NamedQuery(name = "CentralSyncItem.findUploadedButNotReleased", query = "from CentralSyncItem where stage IN ?1 order by stageChangeTime")
})
public class CentralSyncItem extends PanacheEntityBase {
    @Id
    public String groupId;
    @Id
    public String artifactId;
    @Id
    public String version;

    public LocalDateTime startTime;
    public LocalDateTime stageChangeTime;
    public String stagingRepoId;
    public Stage stage;

    public int uploadAttempts = 0;
    public int promotionAttempts = 0;

    public CentralSyncItem() {

    }

    protected CentralSyncItem(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
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

    // TODO: Do this in SQL ?
    public static List<CentralSyncItem> findDistinctGA() {
        Set<String> gaSet = new HashSet<>();
        List<CentralSyncItem> all = CentralSyncItem.findAll().list();
        return all.stream()
                .filter(e -> gaSet.add(e.toGaString()))
                .collect(Collectors.toList());
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

    public String toGaString() {
        return groupId + ":" + artifactId;
    }

    public String toGavString() {
        return toGaString() + ":" + version;
    }

    @Override
    public String toString() {
        return toGavString() + " [" + stage + "]";
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.groupId);
        hash = 17 * hash + Objects.hashCode(this.artifactId);
        hash = 17 * hash + Objects.hashCode(this.version);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CentralSyncItem other = (CentralSyncItem) obj;
        if (!Objects.equals(this.groupId, other.groupId)) {
            return false;
        }
        if (!Objects.equals(this.artifactId, other.artifactId)) {
            return false;
        }
        return Objects.equals(this.version, other.version);
    }
}
