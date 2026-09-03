package io.mvnpm.maven.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.maven.exceptions.MissingFilesForBundleException;
import io.quarkus.logging.Log;

/**
 * An interface for the creation of bundles in the format which is expected by a
 * repository-manager.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public abstract class BundleCreator {

    @Inject
    protected PackageFileLocator packageFileLocator;

    /**
     * A helper class for combining the classifier which is uploaded, and the
     * path to the file.
     *
     * @param classifier The classifier of the given file-{@link Path}
     * @param path The {@link Path} to the file
     */
    public final record BundleRecord(String classifier, Path path) {
    }

    /**
     * Calls the bundle method and returns the {@link Path} to the resulting
     * bundle.
     *
     * @param groupId of the dependency to bundle
     * @param artifactId of the dependency to bundle
     * @param version of the dependency to bundle
     * @return the {@link Path} to the bundled dependency
     * @throws MissingFilesForBundleException if files are missing
     */
    public List<BundleRecord> bundle(String groupId, String artifactId, String version)
            throws MissingFilesForBundleException {
        Log.debug("====== mvnpm: Bundler ======");
        return buildBundle(groupId, artifactId, version);
    }

    /**
     * Should build the bundle for given dependency.
     *
     * @param groupId of the dependency to bundle
     * @param artifactId of the dependency to bundle
     * @param version of the dependency to bundle
     * @return the {@link List} with all {@link BundleRecord}s to the bundled in
     *         the dependency
     * @throws MissingFilesForBundleException if files are missing
     */
    protected abstract List<BundleRecord> buildBundle(String groupId, String artifactId, String version)
            throws MissingFilesForBundleException;

    /**
     * Should return a list of {@link BundleRecord}s containing the <code>base</code>
     * parameter.
     *
     * @param parent {@link Path} from which file-names should be searched
     * @param base {@link String} which should be contained in returned
     *        {@link Path} file-names
     * @return a {@link List} of {@link BundleRecord}s where file-names matched the base
     *         {@link String}
     */
    protected abstract List<BundleRecord> getRecordsInBundle(Path parent, String base);

    /**
     * Should return a {@link List} of {@link Path}s to all files for a given
     * dependency.
     *
     * @param groupId of the dependency to bundle
     * @param artifactId of the dependency to bundle
     * @param version of the dependency to bundle
     * @return a {@link List} of {@link BundleRecord}s for the files contained
     *         in a dependency
     * @throws MissingFilesForBundleException if files are missing
     */
    protected List<BundleRecord> getRecordsOf(String groupId, String artifactId, String version)
            throws MissingFilesForBundleException {
        // Files that needs to be in the bundle
        Path parent = packageFileLocator.getLocalDirectory(groupId, artifactId, version);
        String base = artifactId + Constants.HYPHEN + version;
        List<BundleRecord> records = getRecordsInBundle(parent, base);
        List<String> notReady = new ArrayList<>();
        for (BundleRecord bundleRecord : records) {
            boolean ready = Files.exists(bundleRecord.path());
            Log.debug("\tbundle: " + bundleRecord.path() + " [" + ready + "]");
            if (!ready) {
                notReady.add(bundleRecord.path().toString());
            }
        }

        if (notReady.isEmpty())
            return records;

        throw new MissingFilesForBundleException(
                "Some files (%s) are not available yet to build the bundle for '%s:%s:%s' (waiting for next batch)"
                        .formatted(notReady, groupId, artifactId, version));
    }
}
