package io.mvnpm.creator.aliasing;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * <p>
 * Stable and reversible maven artifact helper.
 * </p>
 * <p>
 * Uses {@link Base64} to en/de-code.
 * </p>
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public final class AliasCodec {

    /**
     * Fixed marker for generating alias-artifactId
     */
    private static final String MARKER = "-ALIASED-v1-";

    private AliasCodec() {
    }

    /**
     * Checks if a given artifactId is an alias.
     *
     * @param artifactId The artifactId to check
     * @return {@code true} if the given artifactId is an alias-artifact,
     *         {@code false} if not
     */
    public static boolean isAlias(String artifactId) {
        return artifactId != null && artifactId.contains(MARKER);
    }

    /**
     * Encodes a public artifactId for a target-package to use as alias-artifactId.
     *
     * @param publicArtifactId The artifactId of the alias
     * @param targetName The target name
     * @return A {@link Base64} encoded version of given artifactId for the target
     *         name
     */
    public static String encode(String publicArtifactId, String targetName) {
        return publicArtifactId + MARKER + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(targetName.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Determines if a given artifactId even is of an alias, and if so, returns the
     * decoded name of the original target.
     *
     * @param artifactId The artifactId to inspect
     * @return The original target name
     */
    public static String targetName(String artifactId) {
        int i = artifactId.lastIndexOf(MARKER);
        if (i < 0) {
            throw new IllegalArgumentException("Not an alias: " + artifactId);
        }
        return new String(Base64.getUrlDecoder().decode(artifactId.substring(i + MARKER.length())),
                StandardCharsets.UTF_8);
    }

    /**
     * Determines if a given artifactId is of an alias, and if so, returns the
     * actual public artifactId.
     *
     * @param artifactId The artifactId to inspect
     * @return The public artifactId
     */
    public static String publicArtifactId(String artifactId) {
        int i = artifactId.lastIndexOf(MARKER);
        if (i < 0) {
            throw new IllegalArgumentException("Not an alias: " + artifactId);
        }
        return artifactId.substring(0, i);
    }
}
