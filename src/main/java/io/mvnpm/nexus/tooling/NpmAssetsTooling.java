package io.mvnpm.nexus.tooling;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
public final class NpmAssetsTooling {

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE,
            false);
    private final NpmAssets assets;

    NpmAssetsTooling(final NpmAssets assets) {
        this.assets = assets;
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

        URI tarballUri;
        URL tarballUrl;
        final Map<Name, String> dependencies = new HashMap<>();
        final Map<Name, String> peerDependencies = new HashMap<>();
        final Map<String, Map<String, Boolean>> peerDependencyMeta = new HashMap<>();

        try {
            tarballUri = URI.create(asset.downloadUrl());
            tarballUrl = tarballUri.toURL();
        } catch (final MalformedURLException e) {
            throw new TypeConversionException(e);
        }

        try {
            readDependencies(tarballUri, dependencies, peerDependencies, peerDependencyMeta);
        } catch (final Exception e) {
            Log.debugf("No dependencies found, continuing without dependencies.");
        }

        Log.debugf("dependency-map is:\n%s", dependencies);
        Log.debugf("peer dependency-map is:\n%s", peerDependencies);
        Log.debugf("metadata of peer dependencies is:\n%s", peerDependencyMeta);

        final Name name = new Name(asset.npm().name());
        final Repository repo = new Repository(asset.format(), asset.downloadUrl(), asset.path());
        final Dist dist = new Dist(null, null, tarballUrl, 1, asset.fileSize(), null);

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
        Log.infof("all versions found are: %s", sortedAssets.stream().map(asset -> asset.npm().version()).toList());

        final Set<String> versions = new HashSet<>();
        final Name name = new Name(assets.items().get(0).npm().name());
        sortedAssets.stream().forEach(asset -> {
            if (name.npmFullName.equals(asset.npm().name())) {
                versions.add(asset.npm().version());
            }
        });

        final DistTags distTags = new DistTags(sortedAssets.get(sortedAssets.size() - 1).npm().version(), null);
        return new Project(name, null, distTags, null, null, versions, null);
    }

    /**
     * Helper method to read the dependencies, peer-dependencies and optional dependencies of a project
     * into the given {@link Map}s.
     *
     * @param tarballUri The {@link URI} to the tarball
     * @param deps The dependency {@link Map} to fill
     * @param peerDeps The peer-dependency {@link Map} to fill
     * @throws Exception if an exception occured in the process
     */
    private final void readDependencies(final URI tarballUri, final Map<Name, String> deps,
            final Map<Name, String> peerDeps, final Map<String, Map<String, Boolean>> peerDepsMeta) throws Exception {
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder(tarballUri).GET().build();
        final HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (final InputStream body = response.body();
                final GzipCompressorInputStream gzip = new GzipCompressorInputStream(body);
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

        throw new IllegalStateException("package.json not found in tarball: " + tarballUri);
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
