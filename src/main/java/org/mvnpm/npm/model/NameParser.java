package org.mvnpm.npm.model;

import java.io.File;
import java.util.Arrays;
import org.mvnpm.Constants;

/**
 * Parse a name
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class NameParser {
    
    private NameParser(){}
    
    public static Name fromMavenGA(String groupId, String artifactId){
        
        if(groupId.equals(Constants.ORG_DOT_MVNPM)){
            String mvnPath = groupId.replaceAll("\\.", Constants.SLASH);
            return new Name(artifactId, Constants.EMPTY, artifactId, groupId, artifactId, mvnPath, artifactId);
        }else if(groupId.startsWith(Constants.ORG_DOT_MVNPM + Constants.DOT_AT_DOT)){
                          
            String ns = groupId.replaceFirst(Constants.ESCAPED_ORG_DOT_MVNPM + Constants.ESCAPED_DOT_AT_DOT, Constants.AT);
            String mvnPath = groupId.replaceAll(Constants.ESCAPED_DOT , Constants.SLASH);
            return new Name(ns + Constants.SLASH + artifactId, ns, artifactId, groupId, artifactId, mvnPath, artifactId);
        }else{
            String mvnPath = groupId.replaceAll(Constants.ESCAPED_DOT, Constants.SLASH);
            return new Name(mvnPath + Constants.SLASH + artifactId, mvnPath, artifactId, groupId, artifactId, mvnPath, artifactId);
        }
        
    }
    
    public static Name fromNpmProject(String npmFullName){
        
        String name;
        String npmNamespace;
        String mvnGroupId;
        
        if(npmFullName.contains(Constants.SLASH)){
            // Name (Both NPM and ArtifactId)
            String[] parts = npmFullName.split(Constants.SLASH);
            String[] partsWithoutNamespace = Arrays.copyOfRange(parts, 1, parts.length);
            name = String.join(Constants.SLASH, partsWithoutNamespace);
            
            // NPM Namespace
            npmNamespace = npmFullName.split(Constants.SLASH)[0];
            
            // Mvn GroupId 
            if(npmNamespace.startsWith(Constants.AT)){
                mvnGroupId = Constants.ORG_DOT_MVNPM + npmNamespace.replaceFirst(Constants.AT, Constants.DOT_AT_DOT);
            }else {
                mvnGroupId = Constants.ORG_DOT_MVNPM + Constants.DOT + npmNamespace;
            }
            
        }else {
            // Name (Both NPM and ArtifactId)
            name = npmFullName;
            
            // NPM Namespace
            npmNamespace = Constants.EMPTY;
            
            // Mvn GroupId
            mvnGroupId = Constants.ORG_DOT_MVNPM;            
        }
        
        // Mvn Path
        String mvnPath = mvnGroupId.replaceAll(Constants.ESCAPED_DOT, File.separator);
        
        // DisplayName
        String displayName = npmFullName;
        if(displayName.contains(Constants.AT)){
            displayName = displayName.replace(Constants.AT, Constants.EMPTY);
        }
        if(displayName.contains(Constants.SLASH)){
            displayName = displayName.replace(Constants.SLASH, Constants.SPACE);
        }
        
        return new Name(npmFullName, 
                npmNamespace, 
                name, 
                mvnGroupId,
                name, 
                mvnPath, 
                displayName);
    }

}
