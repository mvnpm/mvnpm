package io.mvnpm.maven.api;

import java.util.List;

import io.mvnpm.maven.api.BundleCreator.BundleRecord;
import io.mvnpm.maven.exceptions.StatusCheckException;
import io.mvnpm.maven.exceptions.UploadFailedException;
import io.mvnpm.maven.sync.SyncItem;

/**
 * Facade interface for any kind of maven-repository.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public interface MavenFacade {

    /**
     * Should check if the given artifact is already in the maven repository.
     *
     * @param groupId of the dependency
     * @param artifactId of the dependency
     * @param version of the dependency
     * @return <code>true</code> if the dependency is already in the maven repository, <code>false</code>
     *         otherwise
     */
    boolean contains(String groupId, String artifactId, String version);

    /**
     *
     * Should upload the given file to the maven repository and return the release-id
     * to the uploaded file.
     *
     * @param gav the {@link Gav} for a dependency
     * @param records the {@link BundleRecord}s to upload
     * @return the release-id of the uploaded artifact, which can be used to check
     *         the status of the upload
     * @throws UploadFailedException if the upload failed
     */
    String upload(Gav gav, List<BundleRecord> records) throws UploadFailedException;

    /**
     * Checks the status of the given release-id and returns the status.
     *
     * @param csi the central sync item for which the status should be checked
     * @param releaseId the release-id of the upload, which can be used to check the status of the upload
     * @return the status of the upload
     * @throws StatusCheckException if the status check failed
     */
    ReleaseStatus status(SyncItem syncItem, String releaseId) throws StatusCheckException;

    /**
     * Transitions a {@link ReleaseStatus} to according {@link Stage}.
     *
     * @param status The {@link ReleaseStatus} to transition from
     * @return The {@link Stage} which results from transition
     * @throws AssertionError if status is not covered by method
     */
    Stage transition(final ReleaseStatus status) throws AssertionError;
}
