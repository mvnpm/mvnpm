package io.mvnpm.npm;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import io.mvnpm.npm.exceptions.GetPackageException;
import io.mvnpm.npm.model.Project;
import io.mvnpm.npm.model.ProjectInfo;
import io.mvnpm.npm.model.SearchResults;
import io.quarkus.cache.CacheResult;
import io.smallrye.common.annotation.Blocking;

/**
 * Facade on the NPM Registry.
 * Adds caching
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class NpmRegistryFacade {

    @RestClient
    NpmRegistryClient npmRegistryClient;

    /**
     * Fetch full Project from NPM (uncached).
     * Only use when all fields are needed (e.g. REST API serialization).
     */
    @Timeout(unit = ChronoUnit.SECONDS, value = 10)
    @Retry(maxRetries = 1)
    @Blocking
    public Project getProject(String project) {
        Response response = npmRegistryClient.getProject(project);
        if (response.getStatus() < 300) {
            return response.readEntity(Project.class);
        } else {
            throw new WebApplicationException("Error while getting Project for [" + project + "]", response);
        }
    }

    /**
     * Lightweight cached projection: distTags + version strings + lastModified.
     * Drops the large per-version time map, description, homepage, license, name.
     */
    @CacheResult(cacheName = "npm-project-cache")
    @Timeout(unit = ChronoUnit.SECONDS, value = 10)
    @Retry(maxRetries = 1)
    @Blocking
    public ProjectInfo getProjectInfo(String project) {
        return ProjectInfo.from(getProject(project));
    }

    @CacheResult(cacheName = "npm-package-cache")
    @Timeout(unit = ChronoUnit.SECONDS, value = 30)
    @Retry(maxRetries = 1)
    @Blocking
    public io.mvnpm.npm.model.Package getPackage(String project, String version) {
        if (null == version || version.startsWith("git:/") || version.startsWith("git+http")) {
            // We do not support git repos as version. Maybe something we can add later
            version = "*";
        }
        try {
            Response response = npmRegistryClient.getPackage(project, version);
            return response.readEntity(io.mvnpm.npm.model.Package.class);
        } catch (ClientWebApplicationException e) {
            throw new GetPackageException(project, version, e);
        }
    }

    @Timeout(unit = ChronoUnit.SECONDS, value = 30)
    @Blocking
    public SearchResults search(String term, int page) {
        if (page < 0)
            page = 1;
        Response response = npmRegistryClient.search(term, ITEMS_PER_PAGE, page - 1, 0.0, 0.0, 1.0);
        if (response.getStatus() < 300) {
            return response.readEntity(SearchResults.class);
        } else {
            throw new WebApplicationException(response);
        }
    }

    private static final int ITEMS_PER_PAGE = 50; // TODO: Move to config ?
}
