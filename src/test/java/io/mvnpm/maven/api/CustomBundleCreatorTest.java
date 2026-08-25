package io.mvnpm.maven.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.mvnpm.Constants;
import io.mvnpm.maven.api.BundleCreator.BundleRecord;
import io.mvnpm.maven.exceptions.UploadFailedException;
import io.mvnpm.nexus.NexusBundleCreator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(BundleTestProfile.class)
public class CustomBundleCreatorTest {

    @Inject
    NexusBundleCreator bundleCreator;

    @Inject
    MavenFacade mavenFacade;

    @Test
    public void testBundleUpload() throws Exception {
        String groupId = "org.mvnpm";
        String artifactId = "lit";
        String version = "3.2.1";

        List<BundleRecord> bundleRecords = bundleCreator.bundle(groupId, artifactId, version);
        assertTrue(bundleRecords.size() > 1, "Bundle map has to have more than 1 entry!");
        for (final BundleRecord record : bundleRecords) {
            final Path bundlePath = record.path();
            assertNotNull(bundlePath, "Bundle path should not be null");
            assertTrue(Files.exists(bundlePath), "Bundle file should exist");
            assertTrue(Files.isRegularFile(bundlePath), "Bundle should be a regular file");

            Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
            Path target = tempDir.resolve(bundlePath.getFileName());

            Files.move(bundlePath, target, StandardCopyOption.REPLACE_EXISTING);
        }

        // Verify bundle contents
        Set<String> entries = bundleRecords.stream().map(record -> record.path().toString())
                .collect(Collectors.toSet());
        assertFalse(entries.isEmpty(), "Bundle should contain entries");

        String basePath = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/";
        String baseFile = artifactId + Constants.HYPHEN + version;

        assertTrue(entries.contains(basePath + baseFile + Constants.DOT_POM), "Missing pom.xml");
        assertTrue(entries.contains(basePath + baseFile + Constants.DOT_JAR), "Missing jar");
        assertTrue(entries.contains(basePath + baseFile + Constants.DASH_SOURCES_DOT_JAR),
                "Missing sources jar");
        assertTrue(entries.contains(basePath + baseFile + Constants.DASH_JAVADOC_DOT_JAR),
                "Missing javadoc jar");

        // Now upload

        String uploadId = null;
        try {
            uploadId = mavenFacade.upload(new Gav(groupId, artifactId, version), bundleRecords);
        } catch (UploadFailedException e) {
            e.printStackTrace();
            fail("Upload failed: " + e.getMessage());
        }

        // Then
        assertNotNull(uploadId, "UploadId should not be null");
        assertFalse(uploadId.isEmpty(), "UploadId should not be empty");

        System.out.println("Upload successful! Upload ID: " + uploadId);
    }

}
