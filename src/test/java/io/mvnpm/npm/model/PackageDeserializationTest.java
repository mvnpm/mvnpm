package io.mvnpm.npm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class PackageDeserializationTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void bugsWithGithubProtocol() throws Exception {
        String json = "{\"url\": \"github:tanem/react-nprogress/issues\"}";
        Bugs bugs = mapper.readValue(json, Bugs.class);
        assertNotNull(bugs);
        assertEquals("github:tanem/react-nprogress/issues", bugs.url());
    }

    @Test
    void bugsWithHttpUrl() throws Exception {
        String json = "{\"url\": \"https://github.com/tanem/react-nprogress/issues\"}";
        Bugs bugs = mapper.readValue(json, Bugs.class);
        assertNotNull(bugs);
        assertEquals("https://github.com/tanem/react-nprogress/issues", bugs.url());
    }

    @Test
    void bugsAsPlainString() throws Exception {
        // When bugs is a plain string in the package JSON, BugsDeserializer handles it
        String json = "{\"bugs\": \"https://github.com/user/repo/issues\"}";
        Package pkg = mapper.readValue(json, Package.class);
        assertNotNull(pkg);
        assertNotNull(pkg.bugs());
        assertEquals("https://github.com/user/repo/issues", pkg.bugs().url());
    }

    @Test
    void bugsAsPlainStringWithGithubProtocol() throws Exception {
        String json = "{\"bugs\": \"github:user/repo/issues\"}";
        Package pkg = mapper.readValue(json, Package.class);
        assertNotNull(pkg);
        assertNotNull(pkg.bugs());
        assertEquals("github:user/repo/issues", pkg.bugs().url());
    }

    @Test
    void licenseAsString() throws Exception {
        String json = "{\"license\": \"MIT\"}";
        Package pkg = mapper.readValue(json, Package.class);
        assertNotNull(pkg);
        assertNotNull(pkg.license());
        assertEquals("MIT", pkg.license().type());
        assertNull(pkg.license().url());
    }

    @Test
    void licenseAsObject() throws Exception {
        String json = "{\"license\": {\"type\": \"MIT\", \"url\": \"https://opensource.org/licenses/MIT\"}}";
        Package pkg = mapper.readValue(json, Package.class);
        assertNotNull(pkg);
        assertNotNull(pkg.license());
        assertEquals("MIT", pkg.license().type());
        assertEquals("https://opensource.org/licenses/MIT", pkg.license().url());
    }

    @Test
    void homepageWithGithubProtocol() throws Exception {
        String json = "{\"homepage\": \"github:tanem/react-nprogress\"}";
        Package pkg = mapper.readValue(json, Package.class);
        assertNotNull(pkg);
        assertEquals("github:tanem/react-nprogress", pkg.homepage());
    }

    @Test
    void homepageWithHttpUrl() throws Exception {
        String json = "{\"homepage\": \"https://github.com/tanem/react-nprogress\"}";
        Package pkg = mapper.readValue(json, Package.class);
        assertNotNull(pkg);
        assertEquals("https://github.com/tanem/react-nprogress", pkg.homepage());
    }

    @Test
    void timeMapWithUnpublishedObject() throws Exception {
        String json = """
                {
                    "time": {
                        "created": "2025-12-07T09:23:35.715Z",
                        "modified": "2025-12-09T04:03:53.404Z",
                        "90.9.0": "2025-12-07T09:23:35.961Z",
                        "unpublished": {
                            "time": "2025-12-09T04:03:53.404Z",
                            "versions": ["90.9.0", "90.9.5"]
                        }
                    }
                }
                """;
        Project project = mapper.readValue(json, Project.class);
        assertNotNull(project);
        Map<String, String> time = project.time();
        assertNotNull(time);
        assertEquals("2025-12-07T09:23:35.715Z", time.get("created"));
        assertEquals("2025-12-09T04:03:53.404Z", time.get("modified"));
        assertEquals("2025-12-07T09:23:35.961Z", time.get("90.9.0"));
        assertFalse(time.containsKey("unpublished"), "Object entries should be skipped");
    }

    @Test
    void timeMapWithOnlyStrings() throws Exception {
        String json = """
                {
                    "time": {
                        "created": "2025-01-01T00:00:00.000Z",
                        "1.0.0": "2025-01-02T00:00:00.000Z"
                    }
                }
                """;
        Project project = mapper.readValue(json, Project.class);
        assertNotNull(project);
        assertEquals(2, project.time().size());
        assertEquals("2025-01-01T00:00:00.000Z", project.time().get("created"));
        assertEquals("2025-01-02T00:00:00.000Z", project.time().get("1.0.0"));
    }

    @Test
    void peerDependenciesMetaDeserialization() throws Exception {
        String json = """
                {
                    "name": "some-package",
                    "version": "1.0.0",
                    "peerDependencies": {
                        "react": "^17 || ^18",
                        "react-dom": "^17 || ^18"
                    },
                    "peerDependenciesMeta": {
                        "react-dom": {
                            "optional": true
                        }
                    }
                }
                """;
        Package pkg = mapper.readValue(json, Package.class);
        assertNotNull(pkg);
        assertNotNull(pkg.peerDependencies());
        assertEquals(2, pkg.peerDependencies().size());
        assertNotNull(pkg.peerDependenciesMeta());
        assertEquals(1, pkg.peerDependenciesMeta().size());
        assertTrue(pkg.peerDependenciesMeta().get("react-dom").get("optional"));
    }

    @Test
    void isOptionalPeerDependency() throws Exception {
        String json = """
                {
                    "name": "some-package",
                    "version": "1.0.0",
                    "peerDependencies": {
                        "react": "^18",
                        "react-dom": "^18"
                    },
                    "peerDependenciesMeta": {
                        "react-dom": {
                            "optional": true
                        }
                    }
                }
                """;
        Package pkg = mapper.readValue(json, Package.class);

        Name reactDom = NameParser.fromNpmProject("react-dom");
        Name react = NameParser.fromNpmProject("react");

        assertTrue(pkg.isOptionalPeerDependency(reactDom));
        assertFalse(pkg.isOptionalPeerDependency(react));
    }

    @Test
    void isOptionalPeerDependencyWithNoMeta() throws Exception {
        String json = """
                {
                    "name": "some-package",
                    "version": "1.0.0",
                    "peerDependencies": {
                        "react": "^18"
                    }
                }
                """;
        Package pkg = mapper.readValue(json, Package.class);
        assertNull(pkg.peerDependenciesMeta());

        Name react = NameParser.fromNpmProject("react");
        assertFalse(pkg.isOptionalPeerDependency(react));
    }
}
