package io.mvnpm.maven.api;

import static io.mvnpm.Constants.JAR;
import static io.mvnpm.Constants.JAVADOC;
import static io.mvnpm.Constants.POM;
import static io.mvnpm.Constants.SOURCES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.mvnpm.maven.api.BundleCreator.BundleRecord;
import io.mvnpm.nexus.NexusBundleCreator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(CustomBundleTestProfile.class)
class CustomBundleCreatorTest {

    @Inject
    BundleCreator bundleCreator;

    @Test
    void testBundleCreation() throws Exception {
        final String groupId = "org.mvnpm";
        final String artifactId = "lit";
        final String version = "3.2.1";

        assertTrue(
                bundleCreator instanceof NexusBundleCreator,
                "Custom repository profile should use NexusBundleCreator");

        final List<BundleRecord> bundleRecords = bundleCreator.bundle(
                groupId,
                artifactId,
                version);

        assertEquals(
                4,
                bundleRecords.size(),
                "Nexus bundle should contain exactly four artifacts");

        final Map<String, BundleRecord> byClassifier = bundleRecords.stream()
                .collect(Collectors.toMap(
                        BundleRecord::classifier,
                        Function.identity()));

        assertEquals(
                Set.of(POM, JAR, SOURCES, JAVADOC),
                byClassifier.keySet());

        assertRecord(
                byClassifier.get(POM),
                "lit-3.2.1.pom");

        assertRecord(
                byClassifier.get(JAR),
                "lit-3.2.1.jar");

        assertRecord(
                byClassifier.get(SOURCES),
                "lit-3.2.1-sources.jar");

        assertRecord(
                byClassifier.get(JAVADOC),
                "lit-3.2.1-javadoc.jar");
    }

    private void assertRecord(BundleRecord bundleRecord, String expectedFileName) {
        assertNotNull(bundleRecord);

        final Path path = bundleRecord.path();
        assertNotNull(path);
        assertTrue(Files.exists(path), "Expected file to exist: " + path);
        assertTrue(Files.isRegularFile(path), "Expected regular file: " + path);
        assertEquals(expectedFileName, path.getFileName().toString());
    }
}
