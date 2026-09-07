package io.mvnpm.maven.api;

import java.util.Map;
import java.util.Set;

import io.mvnpm.hosting.impl.TestMavenFacade;
import io.mvnpm.hosting.impl.TestNpmFacade;
import io.quarkus.test.junit.QuarkusTestProfile;

public class CustomBundleTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        String testResourcesDirectory = System.getProperty("test.resources.directory");

        if (testResourcesDirectory == null || testResourcesDirectory.isBlank()) {
            throw new IllegalStateException("System property 'test.resources.directory' is not configured");
        }

        return Map.of("mvnpm.local-m2-directory", testResourcesDirectory);
    }

    @Override
    public String getConfigProfile() {
        return "test,wiring";
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(TestNpmFacade.class, TestMavenFacade.class);
    }
}
