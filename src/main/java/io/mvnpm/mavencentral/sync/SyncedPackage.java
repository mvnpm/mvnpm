package io.mvnpm.mavencentral.sync;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@IdClass(SyncedPackageId.class)
public class SyncedPackage extends PanacheEntityBase {
    @Id
    public String groupId;
    @Id
    public String artifactId;

    public LocalDateTime nextCheck;

    public SyncedPackage() {
    }

    public SyncedPackage(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public static List<SyncedPackage> findBatchToCheck(int batchSize) {
        return find(
                "from SyncedPackage where nextCheck IS NULL or nextCheck < ?1 order by nextCheck ASC NULLS FIRST",
                LocalDateTime.now())
                .page(0, batchSize)
                .list();
    }

    public static void createIfAbsent(String groupId, String artifactId) {
        getEntityManager().createQuery(
                "insert into SyncedPackage (groupId, artifactId) values (:groupId, :artifactId)"
                        + " on conflict(groupId, artifactId) do nothing")
                .setParameter("groupId", groupId)
                .setParameter("artifactId", artifactId)
                .executeUpdate();
    }

    public String toGaString() {
        return groupId + ":" + artifactId;
    }

    @Override
    public String toString() {
        return toGaString() + " [nextCheck=" + nextCheck + "]";
    }
}
