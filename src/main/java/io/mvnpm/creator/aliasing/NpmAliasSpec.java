package io.mvnpm.creator.aliasing;

/**
 * A helper class for to deconstruct a npm-alias.
 *
 * @param targetName The name of a npm-alias target
 * @param selector The version selector
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public record NpmAliasSpec(String targetName, String selector) {

    /**
     * Parses {@link NpmAliasSpec} from a raw npm-alias {@link String} in the
     * format...
     * <p>
     * <b><i>npm:@&ltnamespace&gt/&ltpackage&gt@&ltversion&gt</i></b>
     * </p>
     * <p>
     * or
     * </p>
     * <p>
     * <b><i>npm:&ltpackage&gt@&ltversion&gt</i></b>
     * </p>
     *
     * @param raw The {@link String} to parse
     * @return A constructed {@link NpmAliasSpec} with name and selector
     */
    public static NpmAliasSpec parse(String raw) {
        if (raw == null || !raw.startsWith("npm:")) {
            throw new IllegalArgumentException("Not an npm alias: " + raw);
        }
        String spec = raw.substring(4);
        int at = spec.lastIndexOf('@');
        String name = at > 0 ? spec.substring(0, at) : spec;
        String selector = at > 0 ? spec.substring(at + 1) : "*";

        if (name.isBlank() || selector.isBlank()
                || (name.startsWith("@") && (name.indexOf('/') <= 1 || name.endsWith("/")))
                || (!name.startsWith("@") && name.contains("/"))) {
            throw new IllegalArgumentException("Invalid npm alias: " + raw);
        }
        return new NpmAliasSpec(name, selector);
    }
}
