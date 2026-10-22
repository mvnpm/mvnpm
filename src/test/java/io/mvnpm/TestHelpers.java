package io.mvnpm;

import java.io.StringReader;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import io.mvnpm.creator.type.PomService;

/**
 * Helpful methods for testing would be redundant.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public final class TestHelpers {

    private TestHelpers() {
    }

    /**
     * Read given pom content via {@link MavenXpp3Reader}.
     *
     * @param pom The pom content to reac
     * @return A {@link Model} of given pom
     * @throws Exception
     */
    public static Model readPom(String pom) throws Exception {
        return new MavenXpp3Reader()
                .read(new StringReader(pom));
    }

    /**
     * Uses the {@link PomService} to resolve dependencies of a given model and
     * filter for given artifactId.
     *
     * @param model The {@link Model} to resolve for
     * @param artifactId The artifactId to filter for
     * @return The first matching {@link Dependency} in the model, or
     *         <code>null</code> if none found
     */
    public static Dependency dependencyFrom(Model model, String artifactId) {
        return PomService.resolveDependencies(model)
                .stream()
                .filter(d -> artifactId.equals(d.getArtifactId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the <code>maven2</code>-path to the given {@link Dependency}. The
     * extension is for certain file-types of a dependency
     *
     * @param dependency The {@link Dependency} to construct the path for
     * @param extension The file-extension
     * @return The fully constructed <code>maven2</code>-path
     */
    public static String mavenPath(Dependency dependency, String extension) {
        final String groupPath = dependency.getGroupId().replace('.', '/');

        return "/maven2/" + groupPath + "/" + dependency.getArtifactId() + "/" + dependency.getVersion() + "/"
                + dependency.getArtifactId() + "-" + dependency.getVersion() + "." + extension;
    }
}
