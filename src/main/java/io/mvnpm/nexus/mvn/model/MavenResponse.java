package io.mvnpm.nexus.mvn.model;

import java.util.List;

/**
 * The object representing the maven response of the nexus sonatype rest-API.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public record MavenResponse(
        List<MavenResponseItem> items,
        String continuationToken) {
}
