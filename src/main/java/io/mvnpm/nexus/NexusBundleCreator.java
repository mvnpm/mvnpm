package io.mvnpm.nexus;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.Constants;
import io.mvnpm.maven.api.BundleCreator;
import io.mvnpm.maven.exceptions.MissingFilesForBundleException;
import io.quarkus.arc.properties.IfBuildProperty;

/**
 * This creates a bundles (pom, jar, -sources, -javadoc) in the format the nexus
 * repository expects.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@ApplicationScoped
@IfBuildProperty(name = "mvnpm.custom.repository.enabled", stringValue = "true")
public class NexusBundleCreator extends BundleCreator implements Constants {

    /**
     * Builds the actual bundle and returns the created files in a path-map.
     *
     * @param groupId The groupId of the project to bundle.
     * @param artifactId The artifactId of the project to bundle.
     * @param version The version of the project to bundle.
     * @return The path-map of created files for a bundle.
     * @throws MissingFilesForBundleException if some file is missing in the
     *         bundle-creation.
     */
    protected List<BundleRecord> buildBundle(String groupId, String artifactId, String version)
            throws MissingFilesForBundleException {
        return getRecordsOf(groupId, artifactId, version);
    }

    /**
     * Creates a map with all wanted paths for seperate asset-assignment.
     *
     * @param parent The parent directory as {@link Path}.
     * @param base The base {@link Path} for the files.
     * @return A {@link HashMap} of the files and corresponding keys.
     */
    protected List<BundleRecord> getRecordsInBundle(Path parent, String base) {
        final List<BundleRecord> records = new ArrayList<>();

        records.add(new BundleRecord(POM, parent.resolve(base + DOT_POM)));
        records.add(new BundleRecord(JAR, parent.resolve(base + DOT_JAR)));
        records.add(new BundleRecord(SOURCES, parent.resolve(base + DASH_SOURCES_DOT_JAR)));
        records.add(new BundleRecord(JAVADOC, parent.resolve(base + DASH_JAVADOC_DOT_JAR)));

        return records;
    }
}
