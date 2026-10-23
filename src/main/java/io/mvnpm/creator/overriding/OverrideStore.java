package io.mvnpm.creator.overriding;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.quarkus.logging.Log;

/**
 * <p>
 * Implements the specification of npm's {@code overrides} property.
 * </p>
 * <p>
 * Note that calls on this class do assert sequentially calling it, as later calls can/will override previously stored
 * version data for a specific {@link Name}.
 * </p>
 * <p>
 *
 * @see <a href=
 *      "https://docs.npmjs.com/cli/v11/configuring-npm/package-json?utm_source=chatgpt.com#overrides">npm-overrides</a>
 *      </p>
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public class OverrideStore {

    private static final int RECURSION_LIMIT = 15;

    private final Map<String, JsonNode> incomingOverrides;

    public OverrideStore(final Map<String, JsonNode> incomingOverrides) {
        this.incomingOverrides = incomingOverrides;
    }

    public OverrideStore(final Set<Map.Entry<String, JsonNode>> incomingOverrides) {
        this.incomingOverrides = new HashMap<>();
        for (final Map.Entry<String, JsonNode> entry : incomingOverrides) {
            this.incomingOverrides.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * The map in which the final results are stored.
     */
    private final Map<Name, String> overrides = new HashMap<>();

    /**
     * The stack for a nested traversal of override-objects.
     */
    private final Stack<Name> nestedStack = new Stack<>();

    public final void process() {
        for (final Map.Entry<String, JsonNode> entry : incomingOverrides.entrySet()) {
            store(entry.getKey(), entry.getValue());
        }
    }

    /**
     * The entry point for storing all overrides found in the parameters.
     *
     * @param name The name for which the override object should be traversed
     * @param node The {@link JsonNode} to traverse
     */
    public final void store(final String name, final JsonNode node) {
        if (node.isObject()) {
            nested(name, node);
        } else if (node.isTextual()) {
            store(name, node.asText());
        } else {
            throw new OverrideException("no other types allowed that object and text as json values!");
        }
    }

    public final Map<Name, String> unload() {
        return overrides;
    }

    /**
     * The (transitively via {@link OverrideStore#store(String, JsonNode)}) recursive method for traversing a nested
     * override object.
     *
     * @param name The name for which an object was found
     * @param node The {@link JsonNode} containing the object
     */
    private final void nested(final String name, final JsonNode node) {
        if (nestedStack.size() < RECURSION_LIMIT) {
            nestedStack.push(NameParser.fromNpmProject(name));
            for (final Map.Entry<String, JsonNode> entry : node.properties()) {
                store(entry.getKey(), entry.getValue());
            }
            Log.infof("successfully traversed nested object for '%s'", nestedStack.pop().npmFullName);
        } else {
            throw new OverrideException("maximum recursion level reached!");
        }
    }

    /**
     * The method to add an override to the store.
     *
     * @param name The {@link Name} to store the version for
     * @param version The version to store
     */
    private final void store(final String name, final String version) {
        final Name toName;
        if (".".equals(name)) {
            if (nestedStack.isEmpty()) {
                throw new OverrideException(
                        "dot-notation for setting version of outer name is not valid at root-override level");
            } else {
                toName = nestedStack.peek();
            }
        } else {
            toName = NameParser.fromNpmProject(name);
        }

        if (version.startsWith("$")) {
            final Name fromName = NameParser.fromNpmProject(version.substring(1));
            copy(toName, fromName);
        } else {
            plain(toName, version);
        }
    }

    /**
     * <p>
     * Handles a plain overrides entry like e.g.:
     * </p>
     * <p>
     * <b>"package": "^1.0.0"</b>
     * </p>
     * <p>
     * Should also be called, when a {@code "."} is encountered in a nested overrides object, with the name of the outer
     * name identifier.
     * </p>
     *
     * @param npmName The {@link Name} of the package
     * @param version The version of named package
     */
    private final void plain(final Name npmName, final String version) {
        Log.infof("found override entry for '%s' with version '%s'...", npmName, version);
        overrides.put(npmName, version);
    }

    /**
     * Operates like the {@code $} in an overrides-object. Which means searching for given {@code fromName}, taking the
     * stored version and also applying the retrieved version for given {@code toName}.
     *
     * @param toName The name for which the version should be applied
     * @param fromName The name from which the version should be taken
     */
    private final void copy(final Name toName, final Name fromName) {
        final String version = overrides.get(fromName);
        if (version == null) {
            throw new OverrideException("cannot copy version from '" + fromName + "' to '" + toName + "', because '"
                    + fromName + "' is not present!");
        }

        Log.infof("copying the version '%s' of '%s' to '%s'...", version, fromName, toName);
        overrides.put(toName, version);
    }
}
