package io.mvnpm.hosting.profiles;

import io.quarkus.test.junit.QuarkusTestProfile;

public class WiringTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "wiring";
    }
}
