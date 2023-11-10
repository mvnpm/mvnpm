package io.mvnpm.npm.model;

import java.io.File;

import jakarta.persistence.Entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.mvnpm.Constants;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

/**
 * Represent a Name from both NPM and Maven
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@JsonDeserialize(using = NameDeserializer.class)
@Entity
public class Name extends PanacheEntity {

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
    public String toString() {
        return this.npmFullName;
    }

    public String toGavString(String version) {
        return this.mvnGroupId + ":" + this.mvnArtifactId + ":" + version;
    }

    public String mvnGroupIdPath() {
        return this.mvnGroupId.replaceAll(Constants.ESCAPED_DOT, File.separator);
    }

    public boolean isInternal() {
        return this.mvnGroupId.equals(INTERNAL_NS);
    }

    private static final String INTERNAL_NS = "org.mvnpm.at.mvnpm";
}
