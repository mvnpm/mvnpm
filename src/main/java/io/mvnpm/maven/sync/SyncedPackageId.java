<<<<<<<< HEAD:src/main/java/io/mvnpm/maven/sync/SyncedPackageId.java
package io.mvnpm.maven.sync;
========
package io.mvnpm.maven.api;
>>>>>>>> 57d5c9c (Issue #41655: opening repository API for custom extension):src/main/java/io/mvnpm/maven/api/SyncedPackageId.java

import java.io.Serializable;
import java.util.Objects;

public class SyncedPackageId implements Serializable {
    private String groupId;
    private String artifactId;

    public SyncedPackageId() {
    }

    public SyncedPackageId(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        SyncedPackageId other = (SyncedPackageId) obj;
        return Objects.equals(groupId, other.groupId) && Objects.equals(artifactId, other.artifactId);
    }
}
