package io.mvnpm.mavencentral.sync;

import io.mvnpm.maven.NameVersionType;
import io.mvnpm.npm.model.Name;
import java.time.LocalDateTime;
import java.util.Objects;

public class CentralSyncItem {
    private LocalDateTime startTime;
    private LocalDateTime stageChangeTime;
    private String stagingRepoId;
    private Stage stage;
    private NameVersionType nameVersionType;

    public CentralSyncItem(){
        
    }
    
    public CentralSyncItem(Name name, String version) {
        this.nameVersionType = new NameVersionType(name, version);
        this.startTime = LocalDateTime.now();
        this.stage = Stage.INIT;
        this.stageChangeTime = LocalDateTime.now();
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getStageChangeTime() {
        return stageChangeTime;
    }

    public void setStageChangeTime(LocalDateTime stageChangeTime) {
        this.stageChangeTime = stageChangeTime;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
        this.stageChangeTime = LocalDateTime.now();
    }

    public NameVersionType getNameVersionType() {
        return nameVersionType;
    }

    public void setNameVersionType(NameVersionType nameVersionType) {
        this.nameVersionType = nameVersionType;
    }

    public String getStagingRepoId() {
        return stagingRepoId;
    }

    public void setStagingRepoId(String stagingRepoId) {
        this.stagingRepoId = stagingRepoId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.nameVersionType);
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
        return Objects.equals(this.nameVersionType, other.nameVersionType);
    }

    
    

    
    
}
