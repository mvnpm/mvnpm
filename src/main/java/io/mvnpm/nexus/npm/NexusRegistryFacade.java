package io.mvnpm.nexus.npm;

import java.time.temporal.ChronoUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import io.mvnpm.nexus.npm.model.NpmAssets;
import io.mvnpm.nexus.npm.model.NpmResponse;
import io.mvnpm.nexus.tooling.TypeConversionTool;
import io.mvnpm.npm.NpmRegistryClient;
import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.Project;
import io.mvnpm.npm.model.ProjectInfo;
import io.mvnpm.npm.model.SearchResults;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.Blocking;

/**
 * The {@link NpmFacade}-implementation on the NPM registry of a configured nexus repository. Works like the
 * {@link NpmRegistryClient} but searches in the nexus-registry.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@ApplicationScoped
@IfBuildProperty(name = "mvnpm.nexus-repository.enabled", stringValue = "true")
public class NexusRegistryFacade implements NpmFacade {

    @Inject
    @RestClient
    NexusRegistryClient nexusClient;

    @Inject
    @RestClient
    NpmRegistryClient npmRegistry;

    @Inject
    TypeConversionTool conversionTool;

    @ConfigProperty(name = "mvnpm.nexus.npm-repository")
    String repository;

    @Timeout(unit = ChronoUnit.SECONDS, value = 10)
    @Retry(maxRetries = 3)
    @Blocking
    public Project getProject(String project) {
        Name name = NameParser.fromNpmProject(project);
        Response response = nexusClient.searchAsset(repository, "npm", name.npmNamespace.replace("@", ""), name.npmName,
                null);
        if (response.getStatus() < 300) {
            final NpmAssets assets = response.readEntity(NpmAssets.class);
            if (assets.items().size() >= 1) {
                Log.infof("obtaining project '%s' via nexus...", project);
                return TypeConversionTool.from(assets).toProject();
            }
        }

        Log.infof("obtaining project '%s' via official npm-registry...", project);
        response = npmRegistry.getProject(project);
        if (response.getStatus() < 300) {
            return response.readEntity(Project.class);
        }
        throw new WebApplicationException("Error while getting project for [" + project + "]", response);
    }

    @CacheResult(cacheName = "npm-project-cache")
    @Timeout(unit = ChronoUnit.SECONDS, value = 10)
    @Retry(maxRetries = 1)
    @Blocking
    public ProjectInfo getProjectInfo(String project) {
        return ProjectInfo.from(getProject(project));
    }

    @CacheResult(cacheName = "npm-package-cache")
    @Blocking
    public Package getPackage(String project, String version) {
        if (null == version || version.startsWith("git:/") || version.startsWith("git+http")) {
            // We do not support git repos as version. Maybe something we can add later
            version = "*";
        }

        final Name name = NameParser.fromNpmProject(project);
        Response response = name.npmNamespace.isEmpty()
                ? nexusClient.searchAsset(repository, "npm", name.npmName, version)
                : nexusClient.searchAsset(repository, "npm", name.npmNamespace.replace("@", ""), name.npmName, version);
        if (response.getStatus() < 300) {
            final NpmAssets assets = response.readEntity(NpmAssets.class);
            if (assets.items().size() >= 1) {
                Log.infof("Obtaining package [%s:%s:%s] via nexus!", name.npmNamespace, name.npmName, version);
                return TypeConversionTool.from(assets).toPackage(name, version);
            }
        }

        Log.infof("Obtaining package [%s:%s:%s] via official npm-registry...", name.npmNamespace, name.npmName,
                version);
        response = npmRegistry.getPackage(project, version);
        if (response.getStatus() < 300) {
            return response.readEntity(Package.class);
        }
        throw new ClientWebApplicationException(
                String.format("Error while getting package [%s:%s:%s]!", name.npmNamespace, name.npmName, version),
                response);
    }

    @Blocking
    public SearchResults search(String term, int page) {
        // NOTE: page current is ignored, because on nexus a continuationToken is used for pagination
        final Response response = nexusClient.searchComponent(null, term, null, "npm", null, null, null, null, null,
                null, null, null);

        if (response.getStatus() < 300) {
            final NpmResponse npmResponse = response.readEntity(NpmResponse.class);
            if (npmResponse.items().size() >= 1) {
                return TypeConversionTool.from(npmResponse).toSearchResults();
            }
        }

        Log.infof("Failed to obtain searched component for term '%s' from given nexus-client!", term, response);
        throw new WebApplicationException(response);
    }
}
