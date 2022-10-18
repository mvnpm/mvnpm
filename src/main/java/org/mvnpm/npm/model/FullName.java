package org.mvnpm.npm.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represent a Name from both NPM and Maven
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@JsonDeserialize(using = FullNameDeserializer.class)
public class FullName {

    private String npmFullName; 
    private String npmNamespace; 
    private String npmName; 
    private String mvnGroupId; 
    private String mvnArtifactId;
    private String mvnPath;
    private String displayName;

    public FullName() {
    }

    public FullName(String npmFullName) {
        FullName parsed = FullNameParser.parse(npmFullName);
        this.npmFullName = parsed.npmFullName();
        this.npmNamespace = parsed.npmNamespace();
        this.npmName = parsed.npmName();
        this.mvnGroupId = parsed.mvnGroupId();
        this.mvnArtifactId = parsed.mvnArtifactId();
        this.mvnPath = parsed.mvnPath();
        this.displayName = parsed.displayName();
    }
    
    public FullName(String npmFullName, String npmNamespace, String npmName, String mvnGroupId, String mvnArtifactId, String mvnPath, String displayName) {
        this.npmFullName = npmFullName;
        this.npmNamespace = npmNamespace;
        this.npmName = npmName;
        this.mvnGroupId = mvnGroupId;
        this.mvnArtifactId = mvnArtifactId;
        this.mvnPath = mvnPath;
        this.displayName = displayName;
    }

    public String npmFullName() {
        return npmFullName;
    }

    public String npmNamespace() {
        return npmNamespace;
    }

    public String npmName() {
        return npmName;
    }

    public String mvnGroupId() {
        return mvnGroupId;
    }

    public String mvnArtifactId() {
        return mvnArtifactId;
    }

    public String mvnPath() {
        return mvnPath;
    }

    public String displayName() {
        return displayName;
    }
}