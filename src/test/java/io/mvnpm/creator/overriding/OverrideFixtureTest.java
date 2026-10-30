package io.mvnpm.creator.overriding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;

class OverrideFixtureTest {

    private static final Path ROOT = Path.of("src/test/resources/npm-packages/overrides");
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void topLevelOverride() throws Exception {
        final Overrides overrides = load(1);
        assertEquals(Map.of(name(1, "d01-n01"), "2.0.0"), flatten(overrides.rootOverrides()));
        assertTrue(overrides.scopedOverrides().isEmpty());
    }

    @Test
    void singleNestedOverride() throws Exception {
        final Overrides overrides = load(2);
        assertTrue(overrides.rootOverrides().isEmpty());
        assertEquals(Map.of(name(2, "d02-n02"), "2.0.0"), scope(overrides, name(2, "d02-n01")));
    }

    @Test
    void nestedAndDotOverride() throws Exception {
        final Overrides overrides = load(3);
        assertEquals("2.0.0", scope(overrides, name(3, "d02-n01")).get(name(3, "d02-n01")));
        assertEquals("2.0.0", scope(overrides, name(3, "d02-n01")).get(name(3, "d02-n02")));
    }

    @Test
    void twoLevelDotAndDollarReference() throws Exception {
        final Overrides overrides = load(4);
        assertEquals("2.0.0", scope(overrides, name(4, "d03-n01")).get(name(4, "d03-n01")));
        assertEquals("2.0.0", scope(overrides, name(4, "d03-n02")).get(name(4, "d03-n02")));
        assertEquals("2.0.0", scope(overrides, name(4, "d03-n02")).get(name(4, "d03-n03")));
    }

    @Test
    void everyDepthUpToTen() throws Exception {
        checkDepths(5, 10);
    }

    @Test
    void everyDepthUpToSixteenIncludingBoundary() throws Exception {
        checkDepths(6, 16);
    }

    @Test
    void conflictingScopedVersionsRemainDistinctUntilNormalization() throws Exception {
        final Overrides overrides = load(7);
        assertEquals("2.0.0", scope(overrides, name(7, "a")).get(name(7, "target")));
        assertEquals("3.0.0", scope(overrides, name(7, "b")).get(name(7, "target")));
    }

    @Test
    void versionOrderingMustUseSemverRatherThanLexicalOrdering() throws Exception {
        assertEquals("2.10.0", flatten(load(8).rootOverrides()).get(name(8, "target")));
        final Overrides overrides = load(15);
        assertEquals("2.9.0", scope(overrides, name(15, "a")).get(name(15, "target")));
        assertEquals("2.10.0", scope(overrides, name(15, "b")).get(name(15, "target")));
    }

    @Test
    void versionQualifiedAncestorIsRecognized() throws Exception {
        final Overrides overrides = load(9);
        assertEquals("2.0.0", scope(overrides, name(9, "anchor@2.0.0")).get(name(9, "target")));
    }

    @Test
    void semverRangeIsPreserved() throws Exception {
        assertEquals("^2.0.0", flatten(load(10).rootOverrides()).get(name(10, "target")));
    }

    @Test
    void installedPackagesOverridesAreNotPromotedToTheRoot() throws Exception {
        final Overrides overrides = load(11);
        assertTrue(overrides.rootOverrides().isEmpty());
        assertTrue(overrides.scopedOverrides().isEmpty());
    }

    @Test
    void rootLevelDotNotationIsRejected() throws Exception {
        assertThrows(OverrideException.class, () -> load(12));
    }

    @Test
    void dollarSelfReferenceUsesDirectDependencySpec() throws Exception {
        final Overrides overrides = load(13);
        assertEquals("1.0.0", flatten(overrides.rootOverrides()).get(name(13, "parent")));
        assertEquals("2.0.0", flatten(overrides.rootOverrides()).get(name(13, "child")));
    }

    @Test
    void nestedSelectorMatchesNonAdjacentAncestors() throws Exception {
        assertEquals("2.0.0", scope(load(14), name(14, "a")).get(name(14, "target")));
    }

    @Test
    void dependencyReferencesPreferProductionOverPeer() throws Exception {
        final OverrideStore store = store("{\"target\":\"$source\"}", Map.of("source", "2.0.0"), Map.of("source", "3.0.0"));
        store.process();
        assertEquals("2.0.0", flatten(store.unload().rootOverrides()).get("target"));
    }

    @Test
    void peerReferencesAreSupportedWhenProductionDependencyIsAbsent() throws Exception {
        final OverrideStore store = store("{\"target\":\"$source\"}", Map.of(), Map.of("source", "3.0.0"));
        store.process();
        assertEquals("3.0.0", flatten(store.unload().rootOverrides()).get("target"));
    }

    @Test
    void missingDollarReferenceFails() throws Exception {
        final OverrideStore store = store("{\"target\":\"$missing\"}", Map.of(), Map.of());
        assertThrows(OverrideException.class, store::process);
    }

    @Test
    void dollarNeverReferencesAnotherOverride() throws Exception {
        final OverrideStore store = store("{\"source\":\"2.0.0\",\"target\":\"$source\"}", Map.of(), Map.of());
        assertThrows(OverrideException.class, store::process);
    }

    @Test
    void incompatibleScopedOverridesAreOmittedFromFlattenedResult() throws Exception {
        assertFalse(flatten(load(7).flattened()).containsKey(name(7, "target")));
        assertFalse(flatten(load(15).flattened()).containsKey(name(15, "target")));
    }

    @Test
    void rootAndScopedOverridesAreFlattenedTogether() throws Exception {
        assertEquals(Map.of(name(16, "root"), "1.5.0", name(16, "child"), "2.0.0"),
                flatten(load(16).flattened()));
    }

    @Test
    void compatibleScopedRangeKeepsConcreteVersion() throws Exception {
        assertEquals(Map.of(name(17, "target"), "2.5.0"), flatten(load(17).flattened()));
    }

    @Test
    void allFixtureTarballsAreReadableAndContainValidPackageMetadata() throws Exception {
        final Path packages = ROOT.resolve("packages");
        assertTrue(Files.isDirectory(packages), "Missing fixture packages: " + packages);
        try (Stream<Path> files = Files.walk(packages)) {
            final var tarballs = files.filter(path -> path.toString().endsWith(".tgz")).toList();
            assertFalse(tarballs.isEmpty(), "No npm tarballs found in " + packages);
            for (final Path tarball : tarballs) {
                boolean found = false;
                try (InputStream input = Files.newInputStream(tarball);
                        GZIPInputStream gzip = new GZIPInputStream(input);
                        TarArchiveInputStream tar = new TarArchiveInputStream(gzip)) {
                    TarArchiveEntry entry;
                    while ((entry = tar.getNextTarEntry()) != null) {
                        if (entry.isFile() && "package/package.json".equals(entry.getName())) {
                            final JsonNode json = mapper.readTree(tar.readNBytes((int) entry.getSize()));
                            assertTrue(json.path("name").isTextual(), "Missing package name in " + tarball);
                            assertTrue(json.path("version").isTextual(), "Missing package version in " + tarball);
                            found = true;
                        }
                    }
                }
                assertTrue(found, "Missing package/package.json in " + tarball);
            }
        }
    }

    private void checkDepths(final int testCase, final int maximum) throws Exception {
        final Overrides overrides = load(testCase);
        for (int depth = 1; depth <= maximum; depth++) {
            final String prefix = "d%02d-".formatted(depth);
            final String leaf = name(testCase, prefix + "n%02d".formatted(depth));
            if (depth == 1) {
                assertEquals("2.0.0", flatten(overrides.rootOverrides()).get(leaf));
            } else {
                final String parent = name(testCase, prefix + "n%02d".formatted(depth - 1));
                assertEquals("2.0.0", scope(overrides, parent).get(leaf), "Override depth " + depth);
            }
        }
    }

    private Overrides load(final int testCase) throws IOException {
        final Path path = ROOT.resolve("fixtures/case-%02d/package.json".formatted(testCase));
        assertTrue(Files.isRegularFile(path), "Missing fixture: " + path);

        final JsonNode manifest = mapper.readTree(path.toFile());
        final Map<String, String> dependencies = new HashMap<>();
        manifest.path("dependencies").properties()
                .forEach(entry -> dependencies.put(entry.getKey(), entry.getValue().asText()));

        final Map<String, String> peers = new HashMap<>();
        manifest.path("peerDependencies").properties().forEach(entry -> peers.put(entry.getKey(), entry.getValue().asText()));

        final OverrideStore store = store(manifest.path("overrides").toString(), dependencies, peers);
        store.process();

        return store.unload();
    }

    private OverrideStore store(final String json, final Map<String, String> dependencies, final Map<String, String> peers)
            throws IOException {
        final JsonNode node = mapper.readTree(json);
        final Map<String, JsonNode> entries = new HashMap<>();
        node.properties().forEach(entry -> entries.put(entry.getKey(), entry.getValue()));

        final OverrideStore store = new OverrideStore(entries);
        store.includeDependencies(names(dependencies));
        store.includePeerDependencies(names(peers));
        // Current setFlags() derives hasDependencies from devDependencies != null.
        // Supply an empty dev map so production dependency references remain testable.
        store.includeDevDependencies(Map.of());

        return store;
    }

    private Map<Name, String> names(final Map<String, String> values) {
        final Map<Name, String> result = new HashMap<>();
        values.forEach((name, version) -> result.put(NameParser.fromNpmProject(name), version));
        return result;
    }

    private Map<String, String> flatten(final Map<Name, String> map) {
        final Map<String, String> result = new TreeMap<>();
        map.forEach((key, value) -> result.put(key.npmFullName, value));
        return result;
    }

    private Map<String, String> scope(final Overrides overrides, final String scope) {
        for (final Map.Entry<Name, Map<Name, String>> entry : overrides.scopedOverrides().entrySet()) {
            if (scope.equals(entry.getKey().npmFullName)) {
                return flatten(entry.getValue());
            }
        }
        return Map.of();
    }

    private String name(final int testCase, final String suffix) {
        return "@mvnpm-fixture/case-%02d-%s".formatted(testCase, suffix);
    }
}
