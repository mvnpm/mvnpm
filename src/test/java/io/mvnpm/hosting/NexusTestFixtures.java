package io.mvnpm.hosting;

public final class NexusTestFixtures {
    public static final String NPM_REPOSITORY = "test-npm";

    public static final String NPM_PROJECT = "@test/mvnpm-package";
    public static final String NPM_SCOPE = "@test";
    public static final String NPM_NAME = "mvnpm-package";

    public static final String NPM_RELEASE_VERSION = "1.0.0";
    public static final String NPM_SNAPSHOT_VERSION = "1.0.0-SNAPSHOT";

    public static final String NPM_RELEASE_FIXTURE = "npm-packages/test-mvnpm-package-1.0.0.tgz";

    public static final String NPM_SNAPSHOT_FIXTURE = "npm-packages/test-mvnpm-package-1.0.0-SNAPSHOT.tgz";

    public static final String MAVEN_GROUP_ID = "org.mvnpm.at.test";
    public static final String MAVEN_ARTIFACT_ID = "mvnpm-package";

    public static final String MAVEN_RELEASE_VERSION = NPM_RELEASE_VERSION;
    public static final String MAVEN_SNAPSHOT_VERSION = NPM_SNAPSHOT_VERSION;

    public static final String NEXUS_NPM_SCOPE = "test";

    private NexusTestFixtures() {
    }
}
