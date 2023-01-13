package org.mvnpm.semver;

/**
 * Represent a parsed NPM version
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public record NpmVersion(
        Operator operator,
        Version version){

}
