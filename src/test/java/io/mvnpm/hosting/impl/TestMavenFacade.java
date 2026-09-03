package io.mvnpm.hosting.impl;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import io.mvnpm.maven.api.BundleCreator.BundleRecord;
import io.mvnpm.maven.api.Gav;
import io.mvnpm.maven.api.MavenFacade;
import io.mvnpm.maven.api.ReleaseStatus;
import io.mvnpm.maven.api.Stage;
import io.mvnpm.maven.exceptions.StatusCheckException;
import io.mvnpm.maven.exceptions.UploadFailedException;
import io.mvnpm.maven.sync.SyncItem;

/**
 * A test-implementation of the {@link MavenFacade} for smoke testing bean-selection.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@Alternative
@ApplicationScoped
public class TestMavenFacade implements MavenFacade {

    @Override
    public boolean contains(String groupId, String artifactId, String version) {
        throw new UnsupportedOperationException("Unimplemented method 'contains'");
    }

    @Override
    public String upload(Gav gav, List<BundleRecord> records) throws UploadFailedException {
        throw new UnsupportedOperationException("Unimplemented method 'upload'");
    }

    @Override
    public ReleaseStatus status(SyncItem syncItem, String releaseId) throws StatusCheckException {
        throw new UnsupportedOperationException("Unimplemented method 'status'");
    }

    @Override
    public Stage transition(ReleaseStatus status) throws AssertionError {
        throw new UnsupportedOperationException("Unimplemented method 'transition'");
    }

}
