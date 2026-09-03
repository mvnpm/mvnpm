package io.mvnpm.nexus.tooling;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.nexus.npm.model.NpmAssets;
import io.mvnpm.nexus.npm.model.NpmResponse;
import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.Project;
import io.mvnpm.npm.model.SearchResults;

/**
 * The tool to guarantee that the a properly constructed {@link Package}-,
 * {@link Project}- or {@link SearchResults}-object is returned when requested
 * through the nexus implementation.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@ApplicationScoped
public final class TypeConversionTool {

    @Inject
    NpmAssetsTooling assetsTooling;

    @Inject
    NpmSearchTooling searchTooling;

    /**
     * Noop constructor
     */
    TypeConversionTool() {
    }

    /**
     * Creates the fluent API object for converting {@link NpmAssets} to
     * {@link Package} (or {@link Project}).
     *
     * @param assets The parsed {@link NpmAssets} to convert.
     * @return An instance of the fluent API object.
     */
    public final NpmAssetsTooling from(final NpmAssets assets) {
        return assetsTooling.with(assets);
    }

    /**
     * Creates the fluent API object for converting {@link NpmResponse} to
     * {@link SearchResults}.
     *
     * @param response The parsed {@link NpmResponse} to convert.
     * @return An instance of the fluent API object.
     */
    public final NpmSearchTooling from(final NpmResponse response) {
        return searchTooling.with(response);
    }
}
