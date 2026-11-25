package io.mvnpm.npm.model;

import java.io.File;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.mvnpm.Constants;

/**
 * Represent a Name from both NPM and Maven
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@JsonDeserialize(using = NameDeserializer.class)
public class Name {

    public String npmFullName;
    public String npmNamespace;
    public String npmName;
    public String mvnGroupId;
    public String mvnArtifactId;
    public String mvnPath;
    public String displayName;

    public Name() {

    }

    public Name(String npmFullName, String npmNamespace, String npmName, String mvnGroupId, String mvnArtifactId,
            String mvnPath, String displayName) {

        if (npmNamespace == null && npmName == null && mvnGroupId == null && mvnArtifactId == null && mvnPath == null
                && displayName == null) {
            Name parsed = NameParser.fromNpmProject(npmFullName);
            this.npmFullName = parsed.npmFullName;
            this.npmNamespace = parsed.npmNamespace;
            this.npmName = parsed.npmName;
            this.mvnGroupId = parsed.mvnGroupId;
            this.mvnArtifactId = parsed.mvnArtifactId;
            this.mvnPath = parsed.mvnPath;
            this.displayName = parsed.displayName;
        } else {
            this.npmFullName = npmFullName;
            this.npmNamespace = npmNamespace;
            this.npmName = npmName;
            this.mvnGroupId = mvnGroupId;
            this.mvnArtifactId = mvnArtifactId;
            this.mvnPath = mvnPath;
            this.displayName = displayName;
        }
    }

    public Name(String npmFullName) {
        this(npmFullName, null, null, null, null, null, null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof final Name other) {
            return Objects.equals(npmFullName, other.npmFullName) && Objects.equals(npmNamespace, other.npmNamespace)
                    && Objects.equals(npmName, other.npmName) && Objects.equals(mvnGroupId, other.mvnGroupId)
                    && Objects.equals(mvnArtifactId, other.mvnArtifactId) && Objects.equals(mvnPath, other.mvnPath)
                    && Objects.equals(displayName, other.displayName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(npmFullName, npmNamespace, npmName, mvnGroupId, mvnArtifactId, mvnPath, displayName);
    }

    @Override
    public String toString() {
        return this.npmFullName;
    }

    public String toGavString(String version) {
        return this.mvnGroupId + ":" + this.mvnArtifactId + ":" + version;
    }

    public String toPathString(String version) {
        return this.mvnGroupId + "-" + this.mvnArtifactId + "-" + version;
    }

    public String mvnGroupIdPath() {
        return this.mvnGroupId.replaceAll(Constants.ESCAPED_DOT, File.separator);
    }

    public boolean isInternal() {
        return this.mvnGroupId.equals(INTERNAL_NS);
    }

    private static final String INTERNAL_NS = "org.mvnpm.at.mvnpm";
}
