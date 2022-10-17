package org.mvnpm.maven;

// Maven's arifactId can not handle @ and /
public class NameCreator {

    private NameCreator(){}
    
    public static final String toDisplayName(String name){
        if(name.contains(AT)){
            name = name.replace(AT, EMPTY);
        }
        if(name.contains(SLASH)){
            name = name.replace(SLASH, SPACE);
        }
        
        return name;
    }
    
    public static final String toArtifactId(String name){
        if(name.contains(AT)){
            name = name.replaceAll(AT, AT_ENCODED);
        }
        if(name.contains(SLASH)){
            name = name.replaceAll(SLASH, SLASH_ENCODED);
        }
        return name;
    }
    
    public static final String toName(String artifactId){
        if(artifactId.contains(AT_ENCODED)){
            artifactId = artifactId.replaceAll(AT_ENCODED,AT);
        }
        if(artifactId.contains(SLASH_ENCODED)){
            artifactId = artifactId.replaceAll(SLASH_ENCODED, SLASH);
        }
        return artifactId;    
    }
    
    private static final String AT = "@";
    private static final String SLASH = "/";
    private static final String AT_ENCODED = "_a_";
    private static final String SLASH_ENCODED = "_._";
    private static final String SPACE = " ";
    private static final String EMPTY = "";
}
