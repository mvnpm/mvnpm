package io.mvnpm.hosting;

import org.junit.jupiter.api.Tag;

import io.mvnpm.hosting.profiles.CommunityNexusTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CommunityNexusTestProfile.class)
@Tag("nexus-community-latest")
class CommunityNexusIntegrationTest extends NexusIntegrationTest {

    @Override
    protected String variant() {
        return "community-latest";
    }
}
