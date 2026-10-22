package io.mvnpm.hosting;

/**
 * The class containing all fixtures needed when testing against self-hosted
 * repository managers.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public final class SelfHostedTestFixtures {

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

    // testing aliasing (npm:<package>@<version>)
    public static final String NPM_ALIAS_ROOT = "mvnpm-test-alias-root";
    public static final String NPM_ALIAS_ROOT_VERSION = "1.0.0";
    public static final String NPM_ALIAS_ROOT_FIXTURE = "npm-packages/mvnpm-test-alias-root-1.0.0.tgz";

    public static final String NPM_ALIAS_TARGET = "mvnpm-test-alias-target";
    public static final String NPM_ALIAS_TARGET_VERSION = "1.0.0";
    public static final String NPM_ALIAS_TARGET_FIXTURE = "npm-packages/mvnpm-test-alias-target-1.0.0.tgz";

    public static final String NPM_ALIAS_LEAF = "mvnpm-test-alias-leaf";
    public static final String NPM_ALIAS_LEAF_VERSION = "1.0.0";
    public static final String NPM_ALIAS_LEAF_FIXTURE = "npm-packages/mvnpm-test-alias-leaf-1.0.0.tgz";

    private SelfHostedTestFixtures() {
    }
}
