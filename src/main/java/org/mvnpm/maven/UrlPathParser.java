package org.mvnpm.maven;

import java.util.Arrays;
import org.mvnpm.Constants;
import org.mvnpm.file.FileType;
import org.mvnpm.npm.model.NameParser;

/**
 * Parse a url path
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class UrlPathParser {
    
    private UrlPathParser(){}
    
    public static NameVersionType parse(String urlPath){
        String[] parts = urlPath.split(Constants.SLASH);
        
        // We need at least 3 (name / version / filename)
        if(parts.length<3){
            throw new RuntimeException("Invalid Url Path [" + urlPath + "]");
        }
        
        // Start from the back
        String filename = parts[parts.length-1]; // filename
        String version = parts[parts.length-2]; // version
        String[] nameParts = Arrays.copyOfRange(parts, 0, parts.length - 2); // groupid and artifactId
        
        boolean isSha1 = isSha1Request(filename);
        FileType fileType = FileType.fromFileName(filename);
        
        String fullName = String.join(Constants.SLASH, nameParts);
        if(fullName.startsWith(Constants.AT_SLASH)){
            fullName = fullName.replaceFirst(Constants.AT_SLASH, Constants.AT);
        }
        
        return new NameVersionType(NameParser.parse(fullName), version, fileType, isSha1);
    }
    
    private static boolean isSha1Request(String filename){
        return filename.endsWith(Constants.DOT_SHA1);
    }
}
