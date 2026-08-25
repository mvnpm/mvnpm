package io.mvnpm.nexus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.creator.utils.FileUtil;
import io.mvnpm.maven.api.BundleCreator;
import io.mvnpm.maven.exceptions.MissingFilesForBundleException;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.logging.Log;

/**
 * This creates a bundles (pom, jar, -sources, -javadoc) in the format the nexus
 * repository expects.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@ApplicationScoped
@IfBuildProperty(name = "mvnpm.nexus-repository.enabled", stringValue = "true")
public class NexusBundleCreator extends BundleCreator implements Constants {

    @Inject
    PackageFileLocator packageFileLocator;

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
        List<BundleRecord> records = getRecordsOf(groupId, artifactId, version);

        Path parent = packageFileLocator.getLocalDirectory(groupId, artifactId, version);
        List<BundleRecord> createdFiles = new ArrayList<>();

        Log.info("\tBuilding bundle files in " + parent + "...");

        for (final BundleRecord record : records) {
            final Path target = parent.resolve(record.path().getFileName().toString());
            Log.info("\tPreparing bundle file " + target + "...");

            if (!Files.exists(target)) {
                final Path temp = FileUtil.getTempFilePathFor(parent);
                try {
                    Files.copy(record.path(), temp, StandardCopyOption.REPLACE_EXISTING);
                } catch (final IOException e) {
                    throw new UncheckedIOException("Error copying file content: " + record.path() + " -> " + temp,
                            e);
                }

                try {
                    FileUtil.forceMoveAtomic(temp, target);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            createdFiles.add(record);
        }

        return createdFiles;
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
