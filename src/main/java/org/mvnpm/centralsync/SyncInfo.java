package org.mvnpm.centralsync;

public class SyncInfo {
    public boolean inCentral;
    public boolean inStaging;
    
    public SyncInfo() {
    }

    public SyncInfo(boolean inCentral, boolean inStaging) {
        this.inCentral = inCentral;
        this.inStaging = inStaging;
    }
    
}
