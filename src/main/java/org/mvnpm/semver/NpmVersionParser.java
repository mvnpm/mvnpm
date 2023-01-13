package org.mvnpm.semver;

import java.util.Arrays;
import static org.mvnpm.Constants.SPACE;

/**
 * Parse a semver string from npm
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class NpmVersionParser {

    private static final Operator[] OPERATORS = Operator.values();
    
    private NpmVersionParser(){}
    
    public static NpmVersion parse(String npm){
        // Null Check
        if(npm == null) return null;
        
        // Empty Check
        npm = npm.trim();        
        if(npm.isEmpty()) return null;
        
        if(!npm.contains(SPACE)){
            Operator operator = getOperator(npm);
            String semVersionPart = npm.substring(operator.toRangeIndicator().length());
            Version semVer = VersionParser.parse(semVersionPart);
            return new NpmVersion(operator, semVer);
        }
        throw new RuntimeException("Unexpected npm version [" + npm + "]");
    }
    
    private static Operator getOperator(String npm){
        
        for(Operator o:OPERATORS){
            if(npm.startsWith(o.toRangeIndicator())){
                return o;
            }
        }
        
        // Unknown Operator
        throw new RuntimeException("Unknown operator in " + npm);
    }
    
    static {
        // Order by longest first, so that we not not match > to something like >=
        Arrays.sort(OPERATORS, (a, b)->Integer.compare(b.toRangeIndicator().length(), a.toRangeIndicator().length()));
    }
}
