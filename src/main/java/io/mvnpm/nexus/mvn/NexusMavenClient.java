package io.mvnpm.nexus.mvn;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * The main client for sonatype nexus.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@RegisterRestClient(configKey = "nexus-repository")
public interface NexusMavenClient {

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
     *        values are encapsulated in
     *        {@link io.mvnpm.nexus.formats.Package.Format}. If
     *        null, all formats are searched.
     * @param group The group to search for. This is only applicable for
     *        maven packages. If null, all groups are searched.
     * @param name The name to search for. This is only applicable for
     *        maven packages. If null, all names are searched.
     * @param version The version to search for. If null, all versions
     *        are searched.
     * @param versionFlag The version flag to search for. If null, all version
     *        flags are searched.
     * @param groupId The maven groupId to search for. If null, all
     *        groupIds
     *        are searched.
     * @param artifactId The maven artifactId to search for. If null, all
     *        artifactIds are searched.
     * @param baseVersion The maven baseVersion to search for. If null, all
     *        baseVersions are
     *        searched.
     * @param extension The maven extension to search for. If null, all
     *        extensions are
     *        searched.
     * @param classifier The maven classifier to search for. If null, all
     *        classifiers are
     *        searched.
     * @return the raw response from the nexus repository
     */
    @GET
    @Path("/service/rest/v1/search")
    Response search(@QueryParam("continuationToken") String continuationToken, @QueryParam("q") String keyword,
            @QueryParam("repository") String repository, @QueryParam("format") String format, @QueryParam("group") String group,
            @QueryParam("name") String name, @QueryParam("version") String version,
            @QueryParam("prerelease") String versionFlag, @QueryParam("maven.groupId") String groupId,
            @QueryParam("maven.artifactId") String artifactId, @QueryParam("maven.baseVersion") String baseVersion,
            @QueryParam("maven.extension") String extension, @QueryParam("maven.classifier") String classifier);
}
