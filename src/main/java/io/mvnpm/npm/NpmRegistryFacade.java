package io.mvnpm.npm;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.mvnpm.npm.model.Project;
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

    @CacheResult(cacheName = "npm-project-cache")
    @Timeout(unit = ChronoUnit.SECONDS, value = 10)
    @Retry(maxRetries = 3)
    @Blocking
    public Project getProject(String project) {
        Response response = npmRegistryClient.getProject(project);
        if (response.getStatus() < 300) {
            return response.readEntity(Project.class);
        } else {
            throw new WebApplicationException("Error while getting Project for [" + project + "]", response);
        }
    }

    @CacheResult(cacheName = "npm-package-cache")
    @Blocking
    public io.mvnpm.npm.model.Package getPackage(String project, String version) {
        if (null == version || version.startsWith("git:/") || version.startsWith("git+http")) {
            // We do not support git repos as version. Maybe something we can add later
            version = "*";
        }

        Response response = npmRegistryClient.getPackage(project, version);
        if (response.getStatus() < 300) {
            return response.readEntity(io.mvnpm.npm.model.Package.class);
        } else {
            throw new WebApplicationException("Error while getting Package for [" + project + "] version [" + version + "]",
                    response);
        }
    }

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
