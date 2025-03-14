package io.mvnpm.creator.type;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JarServiceTest {

    @Test
    void matcherTest() {
        assertFalse(JarService.matches(JarService.FILES_TO_EXCLUDE, "foo.js"));
        assertFalse(JarService.matches(JarService.FILES_TO_EXCLUDE, "foo.min.js"));
        assertFalse(JarService.matches(JarService.FILES_TO_EXCLUDE, "foo.jpg"));
        assertFalse(JarService.matches(JarService.FILES_TO_EXCLUDE, "bar/foo.jpg"));
        assertFalse(JarService.matches(JarService.FILES_TO_EXCLUDE, "package.json"));
        assertTrue(JarService.matches(JarService.FILES_TO_EXCLUDE, "foo.ts.map"));
        assertTrue(JarService.matches(JarService.FILES_TO_EXCLUDE, "foo.d.ts"));
        assertTrue(JarService.matches(JarService.FILES_TO_EXCLUDE, "/bar/foo.d.ts"));
        assertTrue(JarService.matches(JarService.FILES_TO_EXCLUDE, "/logo.svg"));
        assertTrue(JarService.matches(JarService.FILES_TO_EXCLUDE, "/readme.MD"));
        assertTrue(JarService.matches(JarService.FILES_TO_EXCLUDE, "/README.MD"));
        assertTrue(JarService.matches(JarService.FILES_TO_TGZ, "/foo/bar.d.ts"));
        assertTrue(JarService.matches(JarService.FILES_TO_TGZ, "bar.d.ts"));
    }

}
