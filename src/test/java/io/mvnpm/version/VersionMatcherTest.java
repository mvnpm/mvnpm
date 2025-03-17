package io.mvnpm.version;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

class VersionMatcherTest {
    @ParameterizedTest
    @CsvFileSource(resources = "/versions-matcher.csv", numLinesToSkip = 1)
    void testSelectLatestMatchingVersionFromCSV(String mavenRange, String expectedVersion, String versionStrings) {
        Set<Version> versions = new HashSet<>();
        for (String versionString : versionStrings.split(",")) {
            versions.add(Version.fromString(versionString));
        }
        Version latest = VersionMatcher.selectLatestMatchingVersion(versions, mavenRange);
        if (expectedVersion.equals("null")) {
            assertNull(latest);
            return;
        }
        assertNotNull(latest);
        assertEquals(expectedVersion, latest.toString());
    }
}
