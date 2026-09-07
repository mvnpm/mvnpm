package io.mvnpm.hosting.profiles;

import java.util.Set;

import io.mvnpm.hosting.impl.TestMavenFacade;
import io.mvnpm.hosting.impl.TestNpmFacade;
import io.quarkus.test.junit.QuarkusTestProfile;

public class WiringTestProfile implements QuarkusTestProfile {

    @Override
    public String getConfigProfile() {
        return "test,wiring";
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(TestNpmFacade.class, TestMavenFacade.class);
    }
}
