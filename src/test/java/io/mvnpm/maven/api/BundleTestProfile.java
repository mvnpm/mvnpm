<<<<<<<< HEAD:src/test/java/io/mvnpm/maven/BundleTestProfile.java
package io.mvnpm.maven;
========
package io.mvnpm.maven.api;
>>>>>>>> 57d5c9c (Issue #41655: opening repository API for custom extension):src/test/java/io/mvnpm/maven/api/BundleTestProfile.java

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

public class BundleTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("mvnpm.local-m2-directory", "../src/test/resources");
    }
}
