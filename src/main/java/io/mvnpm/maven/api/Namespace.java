package io.mvnpm.maven.api;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.ws.rs.NotFoundException;

public abstract class Namespace {

    @ConfigProperty(name = "mvnpm.namespace")
    protected String namespace;

    public abstract boolean isInternal(final String groupId, final String artifactId);

    /**
     * Checks if requested namespace is available.
     * 
     * @param namespace The namespace to check for
     * @throws NotFoundException if namespace is not supported
     */
    public void check(final String namespace) throws NotFoundException {
        if (!namespace.equals(this.namespace)) {
            throw new NotFoundException();
        }
    }
}
