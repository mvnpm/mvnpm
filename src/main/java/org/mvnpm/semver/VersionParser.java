package org.mvnpm.semver;

/**
 * Parse a semver string
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class VersionParser {

    private VersionParser(){}
    
    public static Version parse(String semVer){
        
        // Null Check
        if(semVer == null) return null;
        
        // Empty Check
        semVer = semVer.trim();        
        if(semVer.isEmpty()) return null;
        
        if(!semVer.contains(DOT)){
            return new Version(semVer);
        }else {
            String parts[] = semVer.split(ESCAPED_DOT);
            if(parts.length>2){
                return new Version(parts[0],parts[1], parts[2]);
            }else{
                return new Version(parts[0],parts[1]);
            }
            // TODO: Add other parts here.
            
        }
        
    }
    
    
    private static final String DOT = ".";
    private static final String ESCAPED_DOT = "\\.";
}
