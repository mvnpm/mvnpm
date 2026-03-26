package io.mvnpm.npm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}