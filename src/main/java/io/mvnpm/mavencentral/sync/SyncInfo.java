package io.mvnpm.mavencentral.sync;

import io.mvnpm.npm.model.Name;

public class SyncInfo {
    public Name name;
    public String version;
    public boolean inCentral;
    public boolean inStaging;

    public SyncInfo() {
    }

    public SyncInfo(Name name, String version, boolean inCentral, boolean inStaging) {
        this.name = name;
        this.version = version;
        this.inCentral = inCentral;
        this.inStaging = inStaging;
    }

    public boolean canSync() {
        return !this.inCentral && !this.inStaging;
    }

}
