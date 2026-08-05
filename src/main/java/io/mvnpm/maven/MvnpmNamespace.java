package io.mvnpm.maven;

import io.mvnpm.maven.api.Namespace;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@DefaultBean
public class MvnpmNamespace extends Namespace {

    public boolean isInternal(final String groupId, final String artifactId) {
        return groupId.equals("org.mvnpm.at.mvnpm") || (groupId.equals("org.mvnpm.locked") && artifactId.equals("lit"))
                || // Failed attempt at hardcoding versions
                (groupId.equals("org.mvnpm.locked.at.vaadin") && artifactId.equals("router")) ||
                // Failed attempt at hardcoding versions
                (groupId.equals("org.mvnpm") && artifactId.equals("vaadin-web-components")); // Before we used the @mvnpm namespave
    }
}
