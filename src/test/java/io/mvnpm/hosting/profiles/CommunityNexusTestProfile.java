package io.mvnpm.hosting.profiles;

import java.util.List;

import io.mvnpm.hosting.resources.CommunityLatestNexusTestResource;
import io.quarkus.test.junit.QuarkusTestProfile;

public class CommunityNexusTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "test,nexus";
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(CommunityLatestNexusTestResource.class));
    }

    @Override
    public boolean disableGlobalTestResources() {
        return true;
    }
}
