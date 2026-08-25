package io.mvnpm.nexus.npm;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * The npm client for self-hosted sonatype nexus.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@RegisterRestClient(configKey = "nexus-registry")
public interface NexusRegistryClient {

    /**
     * GET method for generic search in nexus repository.
     * This is a very generic search, that can be used for maven and npm packages.
     * The results are not parsed, but returned as a raw response.
     * The caller is responsible for parsing the response.
     *
     * The search is based on the following query parameters:
     *
     * @param continuationToken The continuation token for pagination. If null, the
     *        first page is returned.
     * @param keyword The keyword to search for. This is a free text
     *        search, that can be used to search for packages by
     *        name, description, etc. If null, all packages are
     *        returned.
     * @param repository The repository to search in. If null, all
     *        repositories are searched.
     * @param format The format of the packages to search for. Possible
     *        values are "npm". If null, all formats are searched.
     * @param group The group to search for. This is only applicable for
     *        maven packages. If null, all groups are searched.
     * @param name The name to search for. This is only applicable for
     *        maven packages. If null, all names are searched.
     * @param version The version to search for. If null, all versions
     *        are searched.
     * @param scope The npm scope to search for. If null, all scopes are
     *        searched.
     * @param author The npm author to search for. If null, all authors
     *        are searched.
     * @param description The npm description to search for. If null, all
     *        descriptions are searched.
     * @param keywords The npm keywords to search for. If null, all
     *        keywords
     *        are searched.
     * @param license The npm license to search for. If null, all licenses
     *        are searched.
     * @return the raw response from the nexus repository
     */
    @GET
    @Path("/service/rest/v1/search")
    Response searchComponent(@QueryParam("continuationToken") String continuationToken, @QueryParam("q") String keyword,
            @QueryParam("repository") String repository, @QueryParam("format") String format, @QueryParam("group") String group,
            @QueryParam("name") String name, @QueryParam("version") String version, @QueryParam("npm.scope") String scope,
            @QueryParam("npm.author") String author, @QueryParam("npm.description") String description,
            @QueryParam("npm.keywords") String keywords, @QueryParam("npm.license") String license);

    /**
     * GET method for generic search in nexus repository.
     * This is a very generic search, that can be used for maven and npm packages.
     * The results are not parsed, but returned as a raw response.
     * The caller is responsible for parsing the response.
     *
     * The search is based on the following query parameters:
     *
     * @param repository The repository to search in. If null, all
     *        repositories are searched.
     * @param format The format of the packages to search for. Possible
     *        values are encapsulated in
     *        {@link io.mvnpm.nexus.formats.Package.Format}. If
     *        null, all formats are searched.
     * @param group The group to search for. This is only applicable for
     *        maven packages. If null, all groups are searched.
     * @param name The name to search for. This is only applicable for
     *        maven packages. If null, all names are searched.
     * @param version The version to search for. If null, all versions
     *        are searched.
     * @return the raw response from the nexus repository
     */
    @GET
    @Path("/service/rest/v1/search/assets")
    Response searchAsset(@QueryParam("repository") String repository, @QueryParam("format") String format,
            @QueryParam("group") String group, @QueryParam("name") String name, @QueryParam("version") String version);

    /**
     * GET method for generic search in nexus repository.
     * This is a very generic search, that can be used for maven and npm packages.
     * The results are not parsed, but returned as a raw response.
     * The caller is responsible for parsing the response.
     *
     * The search is based on the following query parameters:
     *
     * @param repository The repository to search in. If null, all
     *        repositories are searched.
     * @param format The format of the packages to search for. Possible
     *        values are encapsulated in
     *        {@link io.mvnpm.nexus.formats.Package.Format}. If
     *        null, all formats are searched.
     * @param name The name to search for. This is only applicable for
     *        maven packages. If null, all names are searched.
     * @param version The version to search for. If null, all versions
     *        are searched.
     * @return the raw response from the nexus repository
     */
    @GET
    @Path("/service/rest/v1/search/assets")
    Response searchAsset(@QueryParam("repository") String repository, @QueryParam("format") String format,
            @QueryParam("name") String name, @QueryParam("version") String version);
}
