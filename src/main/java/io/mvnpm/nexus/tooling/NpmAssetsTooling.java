package io.mvnpm.nexus.tooling;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mvnpm.Constants;
import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.creator.type.TgzService;
import io.mvnpm.nexus.npm.NexusRegistryClient;
import io.mvnpm.nexus.npm.model.NpmAsset;
import io.mvnpm.nexus.npm.model.NpmAssets;
import io.mvnpm.npm.model.Dist;
import io.mvnpm.npm.model.DistTags;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.Project;
import io.mvnpm.npm.model.Repository;
import io.mvnpm.version.Version;
import io.quarkus.logging.Log;

/**
 * Assists in fluent API of {@link TypeConversionTool} when searching for
 * specific packages/projects.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@Dependent
public final class NpmAssetsTooling implements Constants {

    @Inject
    TgzService tgzService;

    @Inject
    PackageFileLocator packageFileLocator;

    @Inject
    @RestClient
    NexusRegistryClient nexusClient;

    @ConfigProperty(name = "quarkus.rest-client.repository.url")
    String nexusBaseUrl;

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE,
            false);
    private NpmAssets assets;

    /**
     * Noop constructor
     */
    NpmAssetsTooling() {
    }

    /**
     * The entry-point to the {@link NpmAssets} tooling chain.
     *
     * @param assets The {@link NpmAssets} which should be converted.
     * @return {@code this} instance with set {@code assets}
     */
    public final NpmAssetsTooling with(final NpmAssets assets) {
        this.assets = assets;
        return this;
    }

    /**
     * Returns a properly constructed {@link Package} via {@link NpmAssetsTooling#toPackage(NpmAsset)}.
     *
     * @param expectedName The {@link Name} of searched package
     * @param expectedVersion The expected version {@link String} of searched package
     * @return A {@link Package} object constructed from the filtered results of the nexus response
     * @throws TypeConversionException if no matching {@link NpmAsset} is found
     */
    public final Package toPackage(final Name expectedName, final String expectedVersion)
            throws TypeConversionException {
        final NpmAsset asset = assets.items().stream()
                .filter(item -> expectedName.npmFullName.equals(item.npm().name()))
                .filter(item -> expectedVersion.equals(item.npm().version())).findFirst()
                .orElseThrow(() -> new TypeConversionException(
                        "No exact npm asset found for " + expectedName.npmFullName + "@" + expectedVersion));

        return toPackage(asset);
    }

    /**
     * Returns a properly constructed {@link Package} object on basis of the parsed
     * {@link NpmAssets}.
     *
     * @param asset the {@link NpmAsset} to convert into a {@link Package}.
     * @return The constructed {@link Package}-object.
     * @throws TypeConversionException if an exception occured in the process.
     */
    private Package toPackage(final NpmAsset asset) throws TypeConversionException {
        Log.infof("The path of found package is '%s'", asset.path());

        final Map<Name, String> dependencies = new HashMap<>();
        final Map<Name, String> peerDependencies = new HashMap<>();
        final Map<String, Map<String, Boolean>> peerDependencyMeta = new HashMap<>();

        final Name name = new Name(asset.npm().name());
        final Path localFilePath = packageFileLocator.getLocalFullPath(FileType.tgz, name, asset.npm().version());
        final URL tarballUrl = externallyReachableDownloadUrl(asset.downloadUrl());

        Log.debugf("Nexus advertised npm asset URL '%s'; downloading via '%s'", asset.downloadUrl(), tarballUrl);

        try (final InputStream tarball = nexusClient.download(tarballUrl.toString())) {
            tgzService.save(tarball, localFilePath);
        } catch (final Exception e) {
            throw new TypeConversionException("Could not download Nexus npm asset from " + tarballUrl, e);
        }

        try {
            readDependencies(localFilePath, dependencies, peerDependencies, peerDependencyMeta);
        } catch (final Exception e) {
            Log.debugf("No dependencies found, continuing without dependencies.");
        }

        Log.debugf("dependency-map is:\n%s", dependencies);
        Log.debugf("peer dependency-map is:\n%s", peerDependencies);
        Log.debugf("metadata of peer dependencies is:\n%s", peerDependencyMeta);

        final Repository repo = new Repository(asset.format(), tarballUrl.toString(), asset.path());
        final Dist dist = new Dist(null, asset.checksum().sha1(), tarballUrl, 1, asset.fileSize(), null);

        return new Package(asset.id(), name, asset.npm().version(), null, null, null, null, repo, null, null, null,
                asset.format(), null, dependencies, peerDependencies, peerDependencyMeta, dist);
    }

    /**
     * Returns a properly constructed {@link Project} object on basis of the parsed
     * {@link NpmAssets}.
     *
     * @return The constructed {@link Project}-object
     * @throws TypeConversionException if an exception occured in the process
     */
    public final Project toProject() throws TypeConversionException {
        final List<NpmAsset> sortedAssets = assets.items().stream().sorted(versionComparator.reversed()).toList();
        final Set<String> versions = new HashSet<>();
        final Name name = new Name(assets.items().get(0).npm().name());
        sortedAssets.forEach(asset -> {
            if (name.npmFullName.equals(asset.npm().name())) {
                versions.add(asset.npm().version());
            }
        });

        final DistTags distTags = new DistTags(sortedAssets.get(0).npm().version(), null);
        return new Project(name, null, distTags, null, null, versions, null);
    }

    /**
     * Rebase an asset URL returned by Nexus onto the configured Nexus origin.
     *
     * Search responses can expose the server's internal/container origin in
     * {@code downloadUrl}. That URL is not necessarily reachable by this process
     * (for example, Testcontainers maps Nexus' 8081 to a random host port). The
     * path still identifies the correct repository asset, so preserve it while
     * using the configured REST-client scheme/authority.
     */
    private URL externallyReachableDownloadUrl(final String advertisedDownloadUrl) throws TypeConversionException {
        try {
            final URI advertised = URI.create(advertisedDownloadUrl);
            final URI configured = URI.create(nexusBaseUrl);

            if (configured.getScheme() == null || configured.getRawAuthority() == null) {
                throw new IllegalArgumentException("Configured Nexus URL is not absolute: " + nexusBaseUrl);
            }

            String path = advertised.getRawPath();
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("Nexus asset URL has no path: " + advertisedDownloadUrl);
            }

            final String configuredPath = configured.getRawPath();
            if (configuredPath != null && !configuredPath.isBlank()
                    && !SLASH.equals(configuredPath)
                    && !path.startsWith(stripTrailingSlash(configuredPath) + SLASH)) {

                path = stripTrailingSlash(configuredPath) + (path.startsWith(SLASH) ? path : SLASH + path);
            }

            final StringBuilder normalized = new StringBuilder().append(configured.getScheme())
                    .append(URL_DELIMITER)
                    .append(configured.getRawAuthority())
                    .append(path.startsWith(SLASH) ? path : SLASH + path);

            if (advertised.getRawQuery() != null) {
                normalized.append('?').append(advertised.getRawQuery());
            }

            return URI.create(normalized.toString()).toURL();
        } catch (final IllegalArgumentException | MalformedURLException e) {
            throw new TypeConversionException(
                    "Could not construct externally reachable Nexus asset URL from " + advertisedDownloadUrl, e);
        }
    }

    /**
     * Strips trailing slashes from given values.
     *
     * @param value The value to strip
     * @return The stripped value
     */
    private static String stripTrailingSlash(final String value) {
        int end = value.length();
        while (end > 1 && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(0, end);
    }

    /**
     * Helper method to read the dependencies, peer-dependencies and optional dependencies of a project
     * into the given {@link Map}s.
     *
     * @param localFilePath The {@link Path} to the tarball
     * @param deps The dependency {@link Map} to fill
     * @param peerDeps The peer-dependency {@link Map} to fill
     * @throws Exception if an exception occured in the process
     */
    private final void readDependencies(final Path localFilePath, final Map<Name, String> deps,
            final Map<Name, String> peerDeps, final Map<String, Map<String, Boolean>> peerDepsMeta)
            throws IOException {

        try (final InputStream tgz = Files.newInputStream(localFilePath);
                final GzipCompressorInputStream gzip = new GzipCompressorInputStream(tgz);
                final TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {

            TarArchiveEntry entry;

            while ((entry = tar.getNextEntry()) != null) {
                // npm convention: package/package.json
                if (!entry.isDirectory() && entry.getName().equals("package/package.json")) {
                    JsonNode root = MAPPER.readTree(tar);
                    extractDeps(root, "dependencies", deps);
                    extractDeps(root, "peerDependencies", peerDeps);
                    extractDepsMeta(root, peerDepsMeta);
                    return;
                }
            }
        }

        throw new IllegalStateException("package.json not found in tarball: " + localFilePath);
    }

    /**
     * Helper method of extracting a field of a {@link JsonNode}.
     *
     * @param root The root {@link JsonNode}.
     * @param field The field which should be extracted.
     * @param target The {@link Map} into which the field should be extracted.
     */
    private final void extractDeps(final JsonNode root, final String field, final Map<Name, String> target) {
        final JsonNode deps = root.get(field);
        if (deps == null || !deps.isObject()) {
            return;
        }

        final Set<Map.Entry<String, JsonNode>> fields = deps.properties();
        for (final Map.Entry<String, JsonNode> entry : fields) {
            final Name name = new Name(entry.getKey());
            final String version = entry.getValue().asText();

            Log.infof("adding [%s:%s] as dependency for field '%s'...", name, version, field);
            target.put(name, version);
        }
    }

    /**
     * Helper method of extracting dependency-meta fields of a {@link JsonNode}.
     *
     * @param root The root {@link JsonNode}.
     * @param field The field which should be extracted.
     * @param target The {@link Map} into which the field should be extracted.
     */
    private final void extractDepsMeta(final JsonNode root, final Map<String, Map<String, Boolean>> target) {
        final JsonNode depsMeta = root.get("peerDependenciesMeta");
        if (depsMeta == null || !depsMeta.isObject()) {
            return;
        }

        final Set<Map.Entry<String, JsonNode>> fields = depsMeta.properties();
        for (final Map.Entry<String, JsonNode> entry : fields) {
            final String packageName = entry.getKey();
            final Map<String, Boolean> meta = new HashMap<>();
            for (final Map.Entry<String, JsonNode> prop : entry.getValue().properties()) {
                final String propName = prop.getKey();
                final Boolean value = prop.getValue().asBoolean();
                meta.put(propName, value);
                Log.infof("adding metadata [%s : %s] for package '%s'...", propName, value, packageName);
            }
            target.put(packageName, meta);
        }
    }

    /**
     * A version comparator for {@link NpmAsset} which compares on basis of {@link Version#compareTo(Version)}.
     */
    private static final Comparator<NpmAsset> versionComparator = (asset1, asset2) -> {
        final Version version1 = Version.fromString(asset1.npm().version());
        final Version version2 = Version.fromString(asset2.npm().version());

        return version1.compareTo(version2);
    };
}
