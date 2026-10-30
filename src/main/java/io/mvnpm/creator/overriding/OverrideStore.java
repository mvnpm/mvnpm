package io.mvnpm.creator.overriding;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.fasterxml.jackson.databind.JsonNode;

import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Package;
import io.quarkus.logging.Log;

/**
 * <p>
 * Implements the specification of npm's {@code overrides} property.
 * </p>
 *
 * <p>
 *
 * @see
 *      <a href=
 *      "https://docs.npmjs.com/cli/v11/configuring-npm/package-json#overrides">link to npm's overrides documentation</a>
 *      </p>
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public final class OverrideStore {

    /**
     * Limits the possible recursion of the {@code overrides}-property in npm's {@code package.json}.
     */
    private static final int RECURSION_LIMIT = 15;

    /**
     * Stores the incoming overrides from constructions.
     */
    private final Map<String, JsonNode> incomingOverrides;

    /**
     * The map in which the scoped results are stored.
     */
    private final Map<Name, Map<Name, String>> scopedOverrides = new HashMap<>();

    /**
     * The map for root-level overrides.
     */
    private final Map<Name, String> rootOverrides = new HashMap<>();

    /**
     * The stack for a nested traversal of override-objects.
     */
    private final Stack<Name> nestedStack = new Stack<>();

    /**
     * Used for resolving a {@code $} in an override-version.
     */
    private Map<Name, String> dependencies = null;

    /**
     * Used for resolving a {@code $} in an override-version.
     */
    private Map<Name, String> peerDependencies = null;

    /**
     * Used for resolving a {@code $} in an override-version.
     */
    private Map<Name, String> devDependencies = null;

    /**
     * Used for resolving a {@code $} in an override-version.
     */
    private Map<Name, String> optionalDependencies = null;

    /* Flags to avoid repeated checks */
    private boolean hasDependencies = false;
    private boolean hasPeerDependencies = false;
    private boolean hasDevDependencies = false;
    private boolean hasOptionalDependencies = false;

    /**
     * Constructor for the {@link OverrideDeserializer}.
     *
     * @param incomingOverrides The incoming overrides {@link JsonNode}
     */
    public OverrideStore(final Map<String, JsonNode> incomingOverrides) {
        this.incomingOverrides = incomingOverrides;
    }

    /**
     * Constructor helping the manual construction for self-hosted instances.
     *
     * @param incomingOverrides The incoming overrides {@link Set} of {@link Map.Entry}
     */
    public OverrideStore(final Set<Map.Entry<String, JsonNode>> incomingOverrides) {
        this.incomingOverrides = new HashMap<>();
        for (final Map.Entry<String, JsonNode> entry : incomingOverrides) {
            this.incomingOverrides.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Needs to be called before {@link OverrideStore#process()} is called to take {@code dependencies} of the given
     * {@link Package} into account when
     * resolving override entries.
     *
     * @param pkg The {@link Package} to take into account
     */
    public void includeDependenciesOf(final Package pkg) {
        this.dependencies = pkg.dependencies();
        this.peerDependencies = pkg.peerDependencies();
        this.devDependencies = pkg.devDependencies();
        this.optionalDependencies = pkg.optionalDependencies();
    }

    /**
     * Needs to be called before {@link OverrideStore#process()} is called to take {@code dependencies} into account when
     * resolving override entries.
     *
     * @param dependencies The {@link Map} of the dependencies to take into account
     */
    public void includeDependencies(final Map<Name, String> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Needs to be called before {@link OverrideStore#process()} is called to take {@code peerDependencies} into account when
     * resolving override entries.
     *
     * @param peerDependencies The {@link Map} of the dependencies to take into account
     */
    public void includePeerDependencies(final Map<Name, String> peerDependencies) {
        this.peerDependencies = peerDependencies;
    }

    /**
     * Needs to be called before {@link OverrideStore#process()} is called to take {@code devDependencies} into account when
     * resolving override entries.
     *
     * @param devDependencies The {@link Map} of the dependencies to take into account
     */
    public void includeDevDependencies(final Map<Name, String> devDependencies) {
        this.devDependencies = devDependencies;
    }

    /**
     * Needs to be called before {@link OverrideStore#process()} is called to take {@code optionalDependencies} into account
     * when
     * resolving override entries.
     *
     * @param optionalDependencies The {@link Map} of the dependencies to take into account
     */
    public void includeOptionalDependencies(final Map<Name, String> optionalDependencies) {
        this.optionalDependencies = optionalDependencies;
    }

    /**
     * The method which triggers the storing of actual override-rules for maven.
     */
    public void process() {
        if (dependencies == null && peerDependencies == null && devDependencies == null && optionalDependencies == null) {
            throw new OverrideException("without any dependencies, a $ cannot be resolved in the override-entries");
        }
        setFlags();
        Log.infof("processing stored overrides...");
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
    private void store(final String name, final JsonNode node) {
        if (node.isObject()) {
            nested(name, node);
        } else if (node.isTextual()) {
            store(name, node.asText());
        } else {
            throw new OverrideException("no other types allowed that object and text as json values!");
        }
    }

    /**
     * Unloads {@link OverrideStore#rootOverrides} and {@link OverrideStore#scopedOverrides} into an {@link Overrides} object.
     *
     * @return The constructed {@link Overrides} object
     */
    public Overrides unload() {
        return new Overrides(rootOverrides, scopedOverrides);
    }

    /**
     * The (transitively via {@link OverrideStore#store(String, JsonNode)}) recursive method for traversing a nested
     * override object.
     *
     * @param name The name for which an object was found
     * @param node The {@link JsonNode} containing the object
     */
    private void nested(final String name, final JsonNode node) {
        if (nestedStack.size() < RECURSION_LIMIT) {
            nestedStack.push(NameParser.fromNpmProject(name));
            for (final Map.Entry<String, JsonNode> entry : node.properties()) {
                store(entry.getKey(), entry.getValue());
            }
            final int level = nestedStack.size();
            Log.debugf("successfully traversed nested object on level '%s' for '%s'", level, nestedStack.pop().npmFullName);
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
    private void store(final String name, final String version) {
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
            final Name fromName = NameParser.fromNpmProject(version.substring(version.indexOf("$") + 1));
            Log.debugf("copying version for '%s'...", fromName);
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
     * Respects scoping via the {@link OverrideStore#nestedStack}.
     * </p>
     *
     * @param name The {@link Name} of the package
     * @param version The version of named package
     */
    private void plain(final Name name, final String version) {
        if (nestedStack.isEmpty()) {
            Log.debugf("found root-level override entry for '%s' with version '%s'...", name, version);
            rootOverrides.put(name, version);
        } else {
            Log.debugf("found an override entry on nested level [%s] for '%s' with version '%s'...", nestedStack.size(), name,
                    version);
            scoped(name, version);
        }
    }

    /**
     * Adds a plain override-entry which is scoped to current scope in {@link OverrideStore#nestedStack}.
     *
     * @param name The {@link Name} of the entry
     * @param version The version of the entry
     */
    private void scoped(final Name name, final String version) {
        final Map<Name, String> scoped;
        final Name scope = nestedStack.peek();

        if (scopedOverrides.get(scope) != null) {
            scoped = scopedOverrides.get(scope);
            if (scoped.containsKey(name)) {
                throw new OverrideException("scoped overrides of same package are not permitted!");
            }
            scoped.put(name, version);
            return;
        } else {
            scoped = new HashMap<>();
        }

        scoped.put(name, version);
        scopedOverrides.put(scope, scoped);
    }

    /**
     * Operates like the {@code $} in an overrides-object. Which means searching for given {@code fromName}, taking the
     * stored version and also applying the retrieved version for given {@code toName}.
     *
     * @param to The name for which the version should be applied
     * @param from The name from which the version should be taken
     */
    private void copy(final Name to, final Name from) {
        final String version = getOccuring(from);
        if (version == null) {
            Log.errorf("did not find any occurence of '%s' in any dependencies...", from);
            throw new OverrideException("cannot copy version from '" + from + "' to '" + to + "', because '"
                    + from + "' is not present in neither dependencies nor peerDependencies");
        }

        Log.debugf("copying the version '%s' of '%s' to '%s'...", version, from, to);
        plain(to, version);
    }

    /**
     * Get, if present, the version already occuring in {@code dependencies} or {@code peerDependencies}.
     *
     * @param from The {@link Name} to search the version for
     * @return The found version, or {@code null} if none was found
     */
    private String getOccuring(final Name from) {
        if (hasDependencies && dependencies.containsKey(from)) {
            Log.infof("found '%s' in dependencies...", from);
            return dependencies.get(from);
        } else if (hasPeerDependencies && peerDependencies.containsKey(from)) {
            Log.infof("found '%s' in peerDependencies...", from);
            return peerDependencies.get(from);
        } else if (hasDevDependencies && devDependencies.containsKey(from)) {
            Log.infof("found '%s' in devDependencies...", from);
            return devDependencies.get(from);
        } else if (hasOptionalDependencies && optionalDependencies.containsKey(from)) {
            Log.infof("found '%s' in optionalDependencies...", from);
            return optionalDependencies.get(from);
        }

        throw new OverrideException("no version found for '" + from + "' any dependency collection!");
    }

    /**
     * Sets all flags regarding dependencies, to avoid repeated null-checks.
     */
    private void setFlags() {
        hasDependencies = dependencies != null;
        hasPeerDependencies = peerDependencies != null;
        hasDevDependencies = devDependencies != null;
        hasOptionalDependencies = optionalDependencies != null;
    }
}
