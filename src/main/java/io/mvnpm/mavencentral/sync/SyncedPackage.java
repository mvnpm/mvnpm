package io.mvnpm.mavencentral.sync;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@IdClass(SyncedPackageId.class)
@Table(indexes = {
        @Index(columnList = "nextCheck")
})
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

    /**
     * Atomically claim a batch of packages due for checking by setting their nextCheck
     * to a temporary future value. Must be called within a transaction.
     * Returns the number of claimed items.
     */
    public static int claimBatch(int batchSize, LocalDateTime claimUntil) {
        LocalDateTime now = LocalDateTime.now();
        return getEntityManager().createNativeQuery(
                "UPDATE syncedpackage SET nextcheck = :claimUntil "
                        + "WHERE (groupid, artifactid) IN ("
                        + "  SELECT groupid, artifactid FROM syncedpackage "
                        + "  WHERE nextcheck IS NULL OR nextcheck < :now "
                        + "  ORDER BY nextcheck ASC NULLS FIRST "
                        + "  LIMIT :limit"
                        + ") AND (nextcheck IS NULL OR nextcheck < :now)")
                .setParameter("claimUntil", claimUntil)
                .setParameter("now", now)
                .setParameter("limit", batchSize)
                .executeUpdate();
    }

    public static List<SyncedPackage> findClaimed(LocalDateTime claimUntil) {
        return find("from SyncedPackage where nextCheck = ?1", claimUntil).list();
    }

    public static void createIfAbsent(String groupId, String artifactId) {
        getEntityManager().createNativeQuery(
                "INSERT INTO syncedpackage (groupid, artifactid) VALUES (:groupId, :artifactId)"
                        + " ON CONFLICT (groupid, artifactid) DO NOTHING")
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
