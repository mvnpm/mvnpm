package io.mvnpm.maven.api;

import java.io.File;

import jakarta.ws.rs.NotFoundException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.npm.model.Name;

public abstract class Namespace {

    @ConfigProperty(name = "mvnpm.namespace")
    protected String namespace;

    public abstract boolean isInternal(final String groupId, final String artifactId);

    public abstract boolean isInternalName(final Name name);

    /**
     * Checks if requested namespace is available.
     *
     * @param namespace The namespace to check for
     * @throws NotFoundException if namespace is not supported
     */
    public final void check(final String namespace) throws NotFoundException {
        if (!namespace.equals(this.namespace)) {
            throw new NotFoundException();
        }
    }

    public final String toGroupId() {
        return namespace.replace(File.separator, ".");
    }
}
