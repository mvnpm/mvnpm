package io.mvnpm.maven;

import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.maven.api.Namespace;
import io.mvnpm.npm.model.Name;
import io.quarkus.arc.DefaultBean;

@ApplicationScoped
@DefaultBean
public class MvnpmNamespace extends Namespace {

    private static final String INTERNAL_NS = "org.mvnpm.at.mvnpm";

    public boolean isInternal(final String groupId, final String artifactId) {
        return groupId.equals("org.mvnpm.at.mvnpm") || (groupId.equals("org.mvnpm.locked") && artifactId.equals("lit"))
                || // Failed attempt at hardcoding versions
                (groupId.equals("org.mvnpm.locked.at.vaadin") && artifactId.equals("router")) ||
                // Failed attempt at hardcoding versions
                (groupId.equals("org.mvnpm") && artifactId.equals("vaadin-web-components")); // Before we used the @mvnpm namespave
    }

    public boolean isInternalName(final Name name) {
        return INTERNAL_NS.equals(name.mvnGroupId);
    }
}
