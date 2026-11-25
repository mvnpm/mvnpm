package io.mvnpm.creator.overriding;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.mvnpm.npm.model.Name;
import io.mvnpm.version.Version;
import io.mvnpm.version.VersionConverter;
import io.mvnpm.version.VersionMatcher;

/**
 * A encapsulation object for override collections. Output is flattened, so if competing version-ranges occur, further override
 * rules have to be applied manually by package-maintainers.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public final class Overrides {

    private final Map<Name, String> rootOverrides;
    private final Map<Name, Map<Name, String>> scopedOverrides;

    private final Map<Name, String> flattened = new HashMap<>();

    public Overrides(final Map<Name, String> rootOverrides, final Map<Name, Map<Name, String>> scopedOverrides) {
        this.rootOverrides = rootOverrides;
        this.scopedOverrides = scopedOverrides;
    }

    /**
     * In maven, scoped dependencies can not be supported via plain {@code <dependencyManagement>}-entries. Therefore, the two
     * maps ({@link Overrides#rootOverrides} and {@link Overrides#scopedOverrides}) are flattened to a 1 dimensional map. This
     * happens according to following rule(s):
     *
     * <ol>
     * <li>
     * All entries of {@link Overrides#rootOverrides} are added to {@link Overrides#flattened}.
     * </li>
     * <li>
     * Then, all entries of {@link Overrides#scopedOverrides} are iterated. If any of those entries has a matching {@link Name}
     * already in {@link Overrides#flattened}, a matching version is searched.
     * </li>
     * <li>
     * If no matching version is found between the entry contained and the entry in the iteration, the dependency is removed.
     * This will result in a maven resolution error and will need attention from package-maintainers.
     * </li>
     * </ol>
     *
     * @return The flattened map {@link Overrides#flattened}
     */
    public final Map<Name, String> flattened() {
        flattened.putAll(rootOverrides);
        for (final Map.Entry<Name, Map<Name, String>> scopedOverride : scopedOverrides.entrySet()) {
            // on iteration, we do not respect the key-name as scoped dependencies are not supported in maven
            for (final Map.Entry<Name, String> scopeEntry : scopedOverride.getValue().entrySet()) {
                putOrRemove(scopeEntry.getKey(), scopeEntry.getValue());
            }
        }
        return flattened;
    }

    /**
     * Helper method for finding a latest matching version for given versions, or omit the alredy found.
     *
     * @param key The {@like Name} for which a version should be chosen
     * @param version The version to compare
     */
    private void putOrRemove(final Name name, final String version) {
        if (flattened.containsKey(name)) {
            // look for matching - if none, remove entry to let maven throw resolution error
            final Version toMatch = Version.fromString(flattened.get(name));
            final String toCompare = VersionConverter.convert(version);

            final Version match = VersionMatcher.selectLatestMatchingVersion(Set.of(toMatch), toCompare);
            if (match == null) {
                flattened.remove(name);
            } else {
                flattened.put(name, match.toString());
            }
        } else {
            flattened.put(name, version);
        }
    }

    /**
     * Record-like getter.
     *
     * @return {@link Overrides#rootOverrides}
     */
    public final Map<Name, String> rootOverrides() {
        return rootOverrides;
    }

    /**
     * Record-like getter.
     *
     * @return {@link Overrides#scopedOverrides}
     */
    public final Map<Name, Map<Name, String>> scopedOverrides() {
        return scopedOverrides;
    }
}
