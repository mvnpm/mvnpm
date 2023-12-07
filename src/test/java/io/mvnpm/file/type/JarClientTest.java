package io.mvnpm.file.type;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JarClientTest {

    @Test
    void matcherTest() {
        assertFalse(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "foo.js"));
        assertFalse(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "foo.min.js"));
        assertFalse(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "foo.jpg"));
        assertFalse(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "bar/foo.jpg"));
        assertFalse(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "package.json"));
        assertTrue(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "foo.ts.map"));
        assertTrue(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "foo.d.ts"));
        assertTrue(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "/bar/foo.d.ts"));
        assertTrue(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "/logo.svg"));
        assertTrue(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "/readme.MD"));
        assertTrue(JarClient.matches(JarClient.FILES_TO_EXCLUDE, "/README.MD"));
        assertTrue(JarClient.matches(JarClient.FILES_TO_TGZ, "/foo/bar.d.ts"));
        assertTrue(JarClient.matches(JarClient.FILES_TO_TGZ, "bar.d.ts"));
    }
}
