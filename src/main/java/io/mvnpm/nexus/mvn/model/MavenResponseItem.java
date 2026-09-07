package io.mvnpm.nexus.mvn.model;

import java.util.List;

/**
 * The object representing an response item of the nexus sonatype rest-API.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public record MavenResponseItem(
        String id,
        String repository,
        String format,
        String group,
        String name,
        String version,
        List<MavenAsset> assets) {
}
