package io.mvnpm.nexus.npm;

import java.io.InputStream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.quarkus.rest.client.reactive.ClientBasicAuth;
import io.quarkus.rest.client.reactive.Url;

/**
 * The npm client for self-hosted sonatype nexus.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@RegisterRestClient(configKey = "repository")
@ClientBasicAuth(username = "${mvnpm.custom.repository.username}", password = "${mvnpm.custom.repository.password}")
public interface NexusRegistryClient {

    /**
     * Searches for npm components in Nexus Repository.
     *
     * <p>
     * This method calls the Nexus Repository Search API:
     * {@code GET /service/rest/v1/search}.
     * </p>
     *
     * <p>
     * All query parameters are optional. Parameters with a {@code null} value
     * are omitted from the request.
     * </p>
     *
     * <p>
     * For scoped npm packages, Nexus represents the npm scope as the component
     * group. For example, the npm package {@code @test/mvnpm-package} has the
     * scope/group {@code @test} and the package name {@code mvnpm-package}.
     * The npm-specific {@code npm.scope} criterion can also be used to search
     * by scope.
     * </p>
     *
     * <p>
     * The response is returned unparsed. The caller is responsible for
     * processing the JSON response and, when present, using the returned
     * continuation token to retrieve subsequent pages.
     * </p>
     *
     * @param continuationToken token returned by a previous search request.
     *        Use {@code null} when requesting the first page.
     * @param keyword free-text search query ({@code q}). Nexus searches
     *        component metadata and coordinates for the supplied term.
     *        Use {@code null} to omit keyword searching.
     * @param repository name of the Nexus repository to search.
     *        Use {@code null} to search all repositories accessible to the
     *        authenticated user.
     * @param format repository format. For this npm client this should be
     *        {@code "npm"}. Supplying the format is recommended when
     *        {@code repository} is not specified.
     * @param group component group/namespace. For a scoped npm package this
     *        corresponds to its npm scope, for example {@code @test}.
     *        Use {@code null} to omit this criterion.
     * @param name npm package name. For example, for
     *        {@code @test/mvnpm-package}, this is {@code mvnpm-package}.
     *        Use {@code null} to omit this criterion.
     * @param version npm package version, for example
     *        {@code 1.0.0-SNAPSHOT}. Use {@code null} to omit this criterion.
     * @param scope npm scope ({@code npm.scope}), for example {@code @test}.
     *        Use {@code null} to omit this criterion.
     * @param author npm package author ({@code npm.author}).
     *        Use {@code null} to omit this criterion.
     * @param description npm package description ({@code npm.description}).
     *        Use {@code null} to omit this criterion.
     * @param keywords npm package keywords ({@code npm.keywords}).
     *        Use {@code null} to omit this criterion.
     * @param license npm package license ({@code npm.license}).
     *        Use {@code null} to omit this criterion.
     *
     * @return the raw HTTP response returned by Nexus Repository
     */
    @GET
    @Path("/service/rest/v1/search")
    Response searchComponent(
            @QueryParam("repository") String repository,
            @QueryParam("format") String format,
            @QueryParam("name") String name,
            @QueryParam("npm.scope") String scope,
            @QueryParam("npm.keywords") String keywords);

    /**
     * Searches for npm assets in Nexus Repository.
     *
     * <p>
     * This method calls the Nexus Repository Asset Search API:
     * {@code GET /service/rest/v1/search/assets}.
     * </p>
     *
     * <p>
     * Nexus exposes the same search criteria for asset searches as for
     * component searches. The difference is the result type: this endpoint
     * returns matching assets rather than components.
     * </p>
     *
     * <p>
     * An npm component normally corresponds to the published package version,
     * while an asset represents the stored file belonging to that component,
     * typically the package {@code .tgz}.
     * </p>
     *
     * <p>
     * All query parameters are optional. Parameters with a {@code null} value
     * are omitted from the request.
     * </p>
     *
     * <p>
     * The response is returned unparsed. The caller is responsible for
     * processing the JSON response and, when present, using the returned
     * continuation token to retrieve subsequent pages.
     * </p>
     *
     * @param continuationToken token returned by a previous search request.
     *        Use {@code null} when requesting the first page.
     * @param keyword free-text search query ({@code q}). Use {@code null} to
     *        omit keyword searching.
     * @param repository name of the Nexus repository to search.
     *        Use {@code null} to search all repositories accessible to the
     *        authenticated user.
     * @param format repository format. For this npm client this should be
     *        {@code "npm"}. Supplying the format is recommended when
     *        {@code repository} is not specified.
     * @param group component group/namespace. For a scoped npm package this
     *        corresponds to its npm scope, for example {@code @test}.
     *        Use {@code null} to omit this criterion.
     * @param name npm package name. For example, for
     *        {@code @test/mvnpm-package}, this is {@code mvnpm-package}.
     *        Use {@code null} to omit this criterion.
     * @param version npm package version, for example
     *        {@code 1.0.0-SNAPSHOT}. Use {@code null} to omit this criterion.
     * @param scope npm scope ({@code npm.scope}), for example {@code @test}.
     *        Use {@code null} to omit this criterion.
     * @param author npm package author ({@code npm.author}).
     *        Use {@code null} to omit this criterion.
     * @param description npm package description ({@code npm.description}).
     *        Use {@code null} to omit this criterion.
     * @param keywords npm package keywords ({@code npm.keywords}).
     *        Use {@code null} to omit this criterion.
     * @param license npm package license ({@code npm.license}).
     *        Use {@code null} to omit this criterion.
     *
     * @return the raw HTTP response returned by Nexus Repository
     */
    @GET
    @Path("/service/rest/v1/search/assets")
    Response searchAsset(
            @QueryParam("repository") String repository,
            @QueryParam("format") String format,
            @QueryParam("name") String name,
            @QueryParam("version") String version,
            @QueryParam("npm.scope") String scope);

    /**
     * Downloads a Nexus asset using the same authenticated REST client used for
     * search. The URL is supplied dynamically because Search API responses contain
     * an absolute download URL.
     *
     * @param url externally reachable Nexus asset URL
     * @return response body stream
     */
    @GET
    InputStream download(@Url String url);
}
