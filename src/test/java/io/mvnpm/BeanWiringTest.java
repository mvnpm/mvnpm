package io.mvnpm;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.mvnpm.hosting.profiles.WiringTestProfile;
import io.mvnpm.maven.api.MavenFacade;
import io.mvnpm.npm.api.NpmFacade;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

/**
 * Very blatantly tests if the {@link DefaultBean} and {@link IfBuildProperty} wiring works as expected.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@QuarkusTest
@TestProfile(WiringTestProfile.class)
public class BeanWiringTest {

    @Inject
    NpmFacade npmTestFacade;

    @Inject
    MavenFacade mavenTestFacade;

    @Test
    void testNpmWiring() {
        assertThrows(UnsupportedOperationException.class, () -> npmTestFacade.getProject(null));
        assertThrows(UnsupportedOperationException.class, () -> npmTestFacade.getProjectInfo(null));
        assertThrows(UnsupportedOperationException.class, () -> npmTestFacade.getPackage(null, null));
        assertThrows(UnsupportedOperationException.class, () -> npmTestFacade.search(null, 0));
    }

    @Test
    void testMavenWiring() {
        assertThrows(UnsupportedOperationException.class, () -> mavenTestFacade.contains(null, null, null));
        assertThrows(UnsupportedOperationException.class, () -> mavenTestFacade.status(null, null));
        assertThrows(UnsupportedOperationException.class, () -> mavenTestFacade.upload(null, null));
        assertThrows(UnsupportedOperationException.class, () -> mavenTestFacade.transition(null));
    }
}
