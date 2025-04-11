package io.mvnpm.mavencentral.sync;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class BundleTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "mvnpm.local-m2-directory", "../src/test/resources");
    }
}
