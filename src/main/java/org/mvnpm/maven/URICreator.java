package org.mvnpm.maven;

import java.net.URI;

public class URICreator {

    private URICreator(){}
    
    public static final URI createURI(String artifactId, String version, String type){
        return URI.create(CONTEXT_ROOT_GROUP_ID + artifactId + SLASH + version + SLASH + artifactId + DASH + version + DOT + type);
    }
    
    private static final String CONTEXT_ROOT_GROUP_ID = "/maven2/org/mvnpm/";
    public static final String SLASH = "/";
    public static final String DOT = ".";
    private static final String DASH = "-";
}
