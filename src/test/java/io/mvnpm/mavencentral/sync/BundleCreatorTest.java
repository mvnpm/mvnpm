package io.mvnpm.mavencentral.sync;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import io.mvnpm.Constants;
import io.mvnpm.mavencentral.MavenCentralFacade;
import io.mvnpm.mavencentral.exceptions.UploadFailedException;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(BundleTestProfile.class)
@EnabledIfEnvironmentVariable(named = "MVNPM_SONATYPE_AUTHORIZATION", matches = ".*")
public class BundleCreatorTest {

    @Inject
    BundleCreator bundleCreator;

    @Inject
    MavenCentralFacade mavenCentralFacade;

    @Test
    public void testBundleUpload() throws Exception {
        String groupId = "org.mvnpm";
        String artifactId = "lit";
        String version = "3.2.1";

        Path bundlePath = bundleCreator.bundle(groupId, artifactId, version);

        assertNotNull(bundlePath, "Bundle path should not be null");
        assertTrue(Files.exists(bundlePath), "Bundle file should exist");
        assertTrue(Files.isRegularFile(bundlePath), "Bundle should be a regular file");

        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        Path target = tempDir.resolve(bundlePath.getFileName());

        Files.move(bundlePath, target, StandardCopyOption.REPLACE_EXISTING);

        // Verify bundle contents
        Set<String> entries = new HashSet<>();
        try (InputStream is = Files.newInputStream(target);
                ZipInputStream zis = new ZipInputStream(is)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.add(entry.getName());
                assertNotNull(entry.getName(), "Zip entry name should not be null");
            }
        }

        assertFalse(entries.isEmpty(), "Bundle should contain entries");

        String basePath = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/";
        String baseFile = artifactId + Constants.HYPHEN + version;

        assertTrue(entries.contains(basePath + baseFile + Constants.DOT_POM), "Missing pom.xml");
        assertTrue(entries.contains(basePath + baseFile + Constants.DOT_POM + Constants.DOT_ASC), "Missing pom.asc");
        assertTrue(entries.contains(basePath + baseFile + Constants.DOT_POM + Constants.DOT_SHA1), "Missing pom.sha1");
        assertTrue(entries.contains(basePath + baseFile + Constants.DOT_POM + Constants.DOT_MD5), "Missing pom.md5");

        assertTrue(entries.contains(basePath + baseFile + Constants.DOT_JAR), "Missing jar");
        assertTrue(entries.contains(basePath + baseFile + Constants.DOT_JAR + Constants.DOT_ASC), "Missing jar.asc");
        assertTrue(entries.contains(basePath + baseFile + Constants.DOT_JAR + Constants.DOT_SHA1), "Missing jar.sha1");
        assertTrue(entries.contains(basePath + baseFile + Constants.DOT_JAR + Constants.DOT_MD5), "Missing jar.md5");

        assertTrue(entries.contains(basePath + baseFile + Constants.DASH_SOURCES_DOT_JAR), "Missing sources jar");
        assertTrue(entries.contains(basePath + baseFile + Constants.DASH_SOURCES_DOT_JAR + Constants.DOT_ASC),
                "Missing sources jar.asc");
        assertTrue(entries.contains(basePath + baseFile + Constants.DASH_SOURCES_DOT_JAR + Constants.DOT_SHA1),
                "Missing sources jar.sha1");
        assertTrue(entries.contains(basePath + baseFile + Constants.DASH_SOURCES_DOT_JAR + Constants.DOT_MD5),
                "Missing sources jar.md5");

        assertTrue(entries.contains(basePath + baseFile + Constants.DASH_JAVADOC_DOT_JAR), "Missing javadoc jar");
        assertTrue(entries.contains(basePath + baseFile + Constants.DASH_JAVADOC_DOT_JAR + Constants.DOT_ASC),
                "Missing javadoc jar.asc");
        assertTrue(entries.contains(basePath + baseFile + Constants.DASH_JAVADOC_DOT_JAR + Constants.DOT_SHA1),
                "Missing javadoc jar.sha1");
        assertTrue(entries.contains(basePath + baseFile + Constants.DASH_JAVADOC_DOT_JAR + Constants.DOT_MD5),
                "Missing javadoc jar.md5");

        // Now upload

        String uploadId = null;
        try {
            uploadId = mavenCentralFacade.upload(target);
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
