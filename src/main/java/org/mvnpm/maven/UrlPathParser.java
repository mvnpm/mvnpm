package org.mvnpm.maven;

import java.util.Arrays;
import org.mvnpm.Constants;
import org.mvnpm.npm.model.Name;
import org.mvnpm.npm.model.NameParser;

/**
 * Parse a url path
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class UrlPathParser {
    
    private UrlPathParser(){}
    
    public static Name parseMavenMetaDataXml(String fullName){
        
        if(fullName.startsWith(Constants.AT_SLASH)){
            fullName = fullName.replaceFirst(Constants.AT_SLASH, Constants.AT);
        }
        
        return NameParser.parse(fullName);
    }
    
    public static NameVersionType parseMavenFile(String urlPath){
        String[] parts = urlPath.split(Constants.SLASH);
        
        // We need at least 3 (name / version / filename)
        if(parts.length<3){
            throw new RuntimeException("Invalid Url Path [" + urlPath + "]");
        }
        
        // Start from the back
        String version = parts[parts.length-2]; // version
        String[] nameParts = Arrays.copyOfRange(parts, 0, parts.length - 2); // groupid and artifactId
        
        String fullName = String.join(Constants.SLASH, nameParts);
        if(fullName.startsWith(Constants.AT_SLASH)){
            fullName = fullName.replaceFirst(Constants.AT_SLASH, Constants.AT);
        }
        
        return new NameVersionType(NameParser.parse(fullName), version);
    }
}
