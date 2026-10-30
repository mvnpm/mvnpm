package io.mvnpm.creator.overriding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mvnpm.creator.type.PomService;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class OverridePomTest {

    private static final Path FIXTURES = Path.of("src/test/resources/npm-packages/overrides/fixtures");
    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    PomService pomService;

    @Test
    void topLevelOverrideAppearsInGeneratedDependencyManagement() throws Exception {
        assertManaged(1, Map.of("@mvnpm-fixture/case-01-d01-n01", "2.0.0"));
        assertManaged(8, Map.of("@mvnpm-fixture/case-08-target", "2.10.0"));
    }

    @Test
    void nestedOnlyOverridesAlsoProducesManagement() throws Exception {
        // PomService gates flattening on rootOverrides being nonempty.
        assertManaged(2, Map.of("@mvnpm-fixture/case-02-d02-n02", "2.0.0"));
        assertManaged(4, Map.of("@mvnpm-fixture/case-04-d03-n01", "2.0.0", "@mvnpm-fixture/case-04-d03-n02", "2.0.0",
                "@mvnpm-fixture/case-04-d03-n03", "2.0.0"));
    }

    @Test
    void incompatibleScopedVersionsAreNotSelectedArbitrarily() throws Exception {
        assertManaged(7, Map.of());
        assertManaged(15, Map.of());
    }

    @Test
    void rootAndNestedOverridesProduceManagement() throws Exception {
        assertManaged(16, Map.of("@mvnpm-fixture/case-16-root", "1.5.0",
                "@mvnpm-fixture/case-16-child", "2.0.0"));
    }

    @Test
    void compatibleScopedRangeKeepsRootVersion() throws Exception {
        assertManaged(17, Map.of("@mvnpm-fixture/case-17-target", "2.5.0"));
    }

    private void assertManaged(final int testCase, final Map<String, String> expected) throws Exception {
        final JsonNode json = mapper.readTree(FIXTURES.resolve("case-%02d/package.json".formatted(testCase)).toFile());
        final Map<Name, String> dependencies = new HashMap<>();
        json.path("dependencies").properties()
                .forEach(entry -> dependencies.put(NameParser.fromNpmProject(entry.getKey()), entry.getValue().asText()));

        final Map<String, JsonNode> entries = new HashMap<>();
        json.path("overrides").properties().forEach(entry -> entries.put(entry.getKey(), entry.getValue()));

        final OverrideStore store = new OverrideStore(entries);
        final io.mvnpm.npm.model.Package pkg = new io.mvnpm.npm.model.Package(null, new Name(json.path("name").asText()),
                "1.0.0", null, null, null, null, null, null, null, null, null, null, dependencies, Map.of(), Map.of(), Map.of(),
                Map.of(), store, null);
        final Path output = Files.createTempDirectory("mvnpm-override-pom-").resolve("generated.pom");
        pomService.createAndSavePom(pkg, output);

        final Model model = pomService.readPom(output);
        if (expected.isEmpty()) {
            assertTrue(model.getDependencyManagement() == null
                    || model.getDependencyManagement().getDependencies().isEmpty(),
                    "Unexpected managed dependencies in case " + testCase);
            return;
        }
        assertNotNull(model.getDependencyManagement(), "Missing dependencyManagement in case " + testCase);

        final Map<String, String> actual = new HashMap<>();
        for (final Dependency dependency : model.getDependencyManagement().getDependencies()) {
            actual.put(dependency.getGroupId() + ":" + dependency.getArtifactId(), dependency.getVersion());
        }
        assertEquals(expected.size(), actual.size(), "Unexpected dependencyManagement entries in case " + testCase);
        for (final Map.Entry<String, String> entry : expected.entrySet()) {
            final Name name = NameParser.fromNpmProject(entry.getKey());
            assertTrue(actual.containsKey(name.mvnGroupId + ":" + name.mvnArtifactId), "Unmanaged override " + entry.getKey());
            assertEquals(entry.getValue(), actual.get(name.mvnGroupId + ":" + name.mvnArtifactId),
                    "Wrong managed version for " + entry.getKey());
        }
    }
}
