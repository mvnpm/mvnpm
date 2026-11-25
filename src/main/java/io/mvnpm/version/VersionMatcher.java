package io.mvnpm.version;

import static java.util.function.Predicate.not;

import java.util.Comparator;
import java.util.Set;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

public final class VersionMatcher {

    /**
     * Selects the latest version from a {@link Set} of {@link Version}s which matches given {@link MavenRange}.
     *
     * @param versions The given {@link Set} of {@link Version}s
     * @param mavenRange The {@link MavenRange} to match
     * @return The resulting, latest {@link Version}
     */
    public static Version selectLatestMatchingVersion(Set<Version> versions, String mavenRange) {
        try {
            if (!mavenRange.matches("^[(\\[].+[)\\]]$")) {
                return selectLatestMatchingVersion(versions, "[" + mavenRange + "]");
            }
            VersionRange range = VersionRange.createFromVersionSpec(mavenRange);
            return versions.stream()
                    // We only match versions without qualifier
                    .filter(not(Version::hasQualifier))
                    .filter(v -> range.containsVersion(new DefaultArtifactVersion(v.toString())))
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        } catch (InvalidVersionSpecificationException e) {
            return null;
        }
    }
}
