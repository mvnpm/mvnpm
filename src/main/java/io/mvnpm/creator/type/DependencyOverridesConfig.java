package io.mvnpm.creator.type;

import java.util.Map;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

/**
 * Forces the version of a dependency in every generated pom, like the npm `overrides` field.
 * Keyed by npm package name, the value is an npm version or range, e.g.
 * <code>mvnpm.overrides."@hotwired/stimulus"=^3.2.0</code>
 */
@ConfigMapping(prefix = "mvnpm.overrides")
public interface DependencyOverridesConfig {

    @WithParentName
    Map<String, String> versions();
}
