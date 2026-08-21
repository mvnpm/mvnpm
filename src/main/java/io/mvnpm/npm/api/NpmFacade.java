package io.mvnpm.npm.api;

import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.Project;
import io.mvnpm.npm.model.ProjectInfo;
import io.mvnpm.npm.model.SearchResults;

/**
 * The interface for any facade on any npm repository.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public interface NpmFacade {

    /**
     * Should retrieve a project with all found versions.
     *
     * @param project The project to find
     * @return The {@link Project} with all found versions
     */
    public Project getProject(String project);

    /**
     * Should retrieve a lightweight cached projection: distTags + version strings + lastModified.
     * Drops the large per-version time map, description, homepage, license, name.
     *
     * @param project The project of which a {@link ProjectInfo} should be constructed
     * @return The {@link ProjectInfo} of found project
     */
    public ProjectInfo getProjectInfo(String project);

    /**
     * Should retrieve a project-package for a specific version.
     *
     * @param project The project to find
     * @param version The version of the project to find
     * @return The package corresponding to given project and version
     */
    public Package getPackage(String project, String version);

    /**
     * Should search the registry and return the found results.
     *
     * @param term The term to search for
     * @param page The page to search on
     * @return The found {@link SearchResults}
     */
    public SearchResults search(String term, int page);
}
