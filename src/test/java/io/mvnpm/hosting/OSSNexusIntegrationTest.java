package io.mvnpm.hosting;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIf;

import io.mvnpm.hosting.profiles.OSSNexusTestProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(OSSNexusTestProfile.class)
@Tag("nexus-oss")
@EnabledIf("dockerAvailable")
class OSSNexusIntegrationTest extends NexusIntegrationTest {

    @Override
    protected String variant() {
        return "oss-v3.76.1";
    }
}
