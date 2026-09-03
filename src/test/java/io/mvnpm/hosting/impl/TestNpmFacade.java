package io.mvnpm.hosting.impl;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.Project;
import io.mvnpm.npm.model.ProjectInfo;
import io.mvnpm.npm.model.SearchResults;

/**
 * A test-implementation of the {@link NpmFacade} for smoke testing bean-selection.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@Alternative
@ApplicationScoped
public class TestNpmFacade implements NpmFacade {

    @Override
    public Project getProject(String project) {
        throw new UnsupportedOperationException("Unimplemented method 'getProject'");
    }

    @Override
    public ProjectInfo getProjectInfo(String project) {
        throw new UnsupportedOperationException("Unimplemented method 'getProjectInfo'");
    }

    @Override
    public Package getPackage(String project, String version) {
        throw new UnsupportedOperationException("Unimplemented method 'getPackage'");
    }

    @Override
    public SearchResults search(String term, int page) {
        throw new UnsupportedOperationException("Unimplemented method 'search'");
    }

}
