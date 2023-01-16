package org.mvnpm.semver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.logging.Logger;

import static org.mvnpm.Constants.OR;
import static org.mvnpm.Constants.OR_ESCAPED;
import static org.mvnpm.Constants.SPACE;
import static org.mvnpm.Constants.EX;
import static org.mvnpm.Constants.STAR;
import static org.mvnpm.Constants.DOT;
import static org.mvnpm.Constants.COMMA;
import static org.mvnpm.Constants.ZERO;
import static org.mvnpm.Constants.HYPHEN;
import static org.mvnpm.Constants.TILDE;
import static org.mvnpm.Constants.CARET;
import static org.mvnpm.Constants.OPEN_BLOCK;
import static org.mvnpm.Constants.CLOSE_BLOCK;
import static org.mvnpm.Constants.OPEN_ROUND;
import static org.mvnpm.Constants.CLOSE_ROUND;
import static org.mvnpm.Constants.EMPTY;

/**
 * Convert a npm version to a maven version
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * 
 * see https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
 * see https://github.com/npm/node-semver
 * see https://semver.org/
 */
public class VersionConverter {
    private static final Logger LOG = Logger.getLogger(VersionConverter.class);
    
    private VersionConverter(){}
    
    public static String toMavenString(String versionString){
        try {
            if(null==versionString)versionString=EMPTY;
        
            versionString = versionString.trim();
            String[] orSet;
            if(versionString.contains(OR)){
                orSet = versionString.split(OR_ESCAPED);
            }else{
                orSet = new String[]{versionString};
            }
            return toMavenString(orSet);
        }catch(Throwable t) {
            LOG.warn("Error getting maven version from [" + versionString + "]");
            throw t;
        }
    }
    
    private static String toMavenString(String[] versions){
        List<String> versionList = new ArrayList<>();
        for(String mv:versions){
            mv = mv.trim();
            versionList.add(translateAndConvert(mv));
        }
        return String.join(COMMA, versionList);
    }
    
    private static String translateAndConvert(String version){
        // Translate
        version = translate(version);
        // Convert
        return convert(version);
    }
    
    /**
     * This method checks for alternative range definition and translate them to the common
     * semver format. 
     * @param version
     * @return 
     */
    private static String translate(String version){
        // Hyphen range
        if(version.contains(SPACE + HYPHEN + SPACE)){
            return translateHyphen(version);
        }
        
        // Tilde range
        if(version.startsWith(TILDE)){
            return translateTilde(version);
        }
        
        // Caret range
        if(version.startsWith(CARET)){
            return translateCaret(version);
        }
        
        // X range
        if(!version.contains(SPACE) && (version.contains(STAR) || version.contains(EX) || version.contains(EX.toUpperCase()))){
            return translateX(version);
        }
        
        return version;
    }
    
    /**
     * Translate Hyphen ranges
     * see https://github.com/npm/node-semver#hyphen-ranges-xyz---abc
     * @param version
     * @return 
     */
    private static String translateHyphen(String version){
        
        String[] parts = version.split(SPACE + HYPHEN + SPACE);

        if(parts.length!=2) throw new RuntimeException("Boundary set to big [" + parts.length + "] - expecting 2 (lower and upper). Hyphen version [" + version + "]");
        
        String lowerBoundary = parts[0].trim();
        String upperBoundary = parts[1].trim();
        
        return Operator.greaterThanOrEqualTo.toRangeIndicator() + lowerBoundary + SPACE + Operator.lessThanOrEqualTo .toRangeIndicator() + upperBoundary;
    }
    
    /**
     * Translate Tilde ranges
     * see https://github.com/npm/node-semver#tilde-ranges-123-12-1
     * @param version
     * @return 
     */
    private static String translateTilde(String version){
        version = version.substring(1);
        V v = V.fromString(version);
        
        if(v.minor()!=null){
            V n = v.nextMinor();
            return Operator.greaterThanOrEqualTo.toRangeIndicator() + v.toString() + SPACE + Operator.lessThan.toRangeIndicator() + n.toString();
        }else if(v.minor()==null){
            V n = v.nextMajor();
            return Operator.greaterThanOrEqualTo.toRangeIndicator() + v.toString() + SPACE + Operator.lessThan.toRangeIndicator() + n.toString();
        }
        throw new RuntimeException("Not sure how to translate tilde version " + version);    
    }
    
    /**
     * Translate caret ranges
     * see https://github.com/npm/node-semver#caret-ranges-123-025-004
     * @param version
     * @return 
     */
    private static String translateCaret(String version){
        
        version = version.substring(1);
        V v = V.fromString(version);
        
        if(v.major()>0){
            V n = v.nextMajor();
            return Operator.greaterThanOrEqualTo.toRangeIndicator() + v.toStringWithCleanedX()+ SPACE + Operator.lessThan.toRangeIndicator() + n.toString();
        }else if(v.major()==0 && v.minor()>0){
            V n = v.nextMinor();
            return Operator.greaterThanOrEqualTo.toRangeIndicator() + v.toStringWithCleanedX() + SPACE + Operator.lessThan.toRangeIndicator() + n.toString();
        }else if(v.major()==0 && v.minorIsX()){
            V n = v.nextMajor();
            return Operator.greaterThanOrEqualTo.toRangeIndicator() + v.toStringWithCleanedX() + SPACE + Operator.lessThan.toRangeIndicator() + n.toString();
        }else if(v.major()==0 && v.minor()==0 && notZero(v.patch())){
            V n = v.nextPatch();
            return Operator.greaterThanOrEqualTo.toRangeIndicator() + v.toStringWithCleanedX() + SPACE + Operator.lessThan.toRangeIndicator() + n.toString();
        }else if(v.major()==0 && v.minor()==0 && zeroOrX(v.patch())){
            V n = v.nextMinor();
            return Operator.greaterThanOrEqualTo.toRangeIndicator() + v.toStringWithCleanedX() + SPACE + Operator.lessThan.toRangeIndicator() + n.toString();
        }
        
        throw new RuntimeException("Not sure how to translate caret version " + version);    
    }
    
    /**
     * Translate x ranges
     * see https://github.com/npm/node-semver#x-ranges-12x-1x-12-
     * @param version
     * @return 
     */
    private static String translateX(String version){
        if(version.startsWith(Operator.vee.toRangeIndicator()) || version.startsWith(Operator.equals.toRangeIndicator())){
            version = version.substring(1); // Remove v or =
        }
        V v = V.fromString(version);
        if(v.majorIsX()){
            return Operator.greaterThanOrEqualTo.toRangeIndicator() + ZERO;
        }
        if(v.minorIsX()){
            return Operator.greaterThanOrEqualTo.toRangeIndicator() + v.major() + DOT + ZERO + SPACE + Operator.lessThan.toRangeIndicator() + v.nextMajor().toString();
        }
        if(v.patchIsX()){
            return Operator.greaterThanOrEqualTo.toRangeIndicator() + v.major() + DOT + v.minor() + DOT + ZERO + SPACE + Operator.lessThan.toRangeIndicator() + v.nextMinor().toString();
        }
        
        throw new RuntimeException("Not sure how to translate x version " + version);    
    }
    
    private static boolean zeroOrX(Integer i){
        return i==null || i==0 || i==Integer.MIN_VALUE;
    }
    
    private static boolean notZero(Integer i){
        return i!=null && i>0;
    }
    
    /**
     * This method converts an boundary range or single version to the maven format
     * It expects <= < > >= or exact versions
     * @param version the version string
     * @return the maven string
     */
    private static String convert(String version){
        if(version.contains(SPACE)){
            String[] parts = version.split(SPACE);

            if(parts.length>2) throw new RuntimeException("Boundary set to big [" + parts.length + "] - expecting 2 (lower and upper). Converting version [" + version + "]");

            List<NpmVersion> versionBoundaries = new ArrayList<>();
            for(String part:parts){
                NpmVersion nvPart = NpmVersionParser.parse(part);
                versionBoundaries.add(nvPart);
            }

            Collections.sort(versionBoundaries, (t, t1) -> {
                if((t.operator().isLowerBoundary()) && (t1.operator().isUpperBoundary())){
                    return -1;
                }else if((t1.operator().isLowerBoundary()) && (t.operator().isUpperBoundary())){
                    return 1;
                }
                return 0;
            });

            NpmVersion lowerBoundary = versionBoundaries.get(0);
            NpmVersion upperBoundary = versionBoundaries.get(1);

            if(!lowerBoundary.operator().isLowerBoundary() || !upperBoundary.operator().isUpperBoundary()){
                throw new RuntimeException("Invalid boundaries defined in " + version);
            }

            String l = getLowerBoundary(lowerBoundary);
            String u = getUpperBoundary(upperBoundary);
            return l + COMMA + u;
        } else {
            NpmVersion nv = NpmVersionParser.parse(version);
            return convert(nv);
        }
    }
    
    private static String convert(NpmVersion nv){
        Operator operator = nv.operator();
        Version version = nv.version();
        
        if(operator.isExact() && version.isConcrete()){
            return version.toString();
        } else {
            String l = getLowerBoundary(operator, version);
            String u = getUpperBoundary(operator, version);
            return l + COMMA + u;
        }
    }
    
    private static String getLowerBoundary(NpmVersion nv){
        Operator operator = nv.operator();
        Version version = nv.version();
        return getLowerBoundary(operator, version);
    }
    
    private static String getLowerBoundary(Operator operator, Version version){
        if (!operator.isExact() && operator.equals(Operator.greaterThan)){
            if(version.minor()!=null && version.patch()!=null){
                return OPEN_ROUND + version.major().val() + DOT + version.minor().val() + DOT + version.patch().val();
            }else if(version.minor()!=null && version.patch()==null){
                return OPEN_ROUND + version.major().val() + DOT + version.minor().val();
            }else if(version.minor()==null && version.patch()==null){
                return OPEN_ROUND + version.major().val();
            }  
        } else if (!operator.isExact() && operator.equals(Operator.greaterThanOrEqualTo)){
            if(version.minor()!=null && version.patch()!=null){
                return OPEN_BLOCK + version.major().val() + DOT + version.minor().val() + DOT + version.patch().val();
            }else if(version.minor()!=null && version.patch()==null){
                return OPEN_BLOCK + version.major().val() + DOT + version.minor().val();
            }else if(version.minor()==null && version.patch()==null){
                return OPEN_BLOCK + version.major().val();
            }  
        }
        return OPEN_ROUND; // Not an lower boundary
    }
    
    private static String getUpperBoundary(NpmVersion nv){
        Operator operator = nv.operator();
        Version version = nv.version();
        return getUpperBoundary(operator, version);
    }
    
    private static String getUpperBoundary(Operator operator, Version version){
        if (!operator.isExact() && operator.equals(Operator.lessThan)){
            if(version.minor()!=null && version.patch()!=null){
                return version.major().val() + DOT + version.minor().val() + DOT + version.patch().val() + CLOSE_ROUND;
            }else if(version.minor()!=null && version.patch()==null){
                return version.major().val() + DOT + version.minor().val() + CLOSE_ROUND;
            }else if(version.minor()==null && version.patch()==null){
                return version.major().val() + CLOSE_ROUND;
            }  
        } else if (!operator.isExact() && operator.equals(Operator.lessThanOrEqualTo)){
            if(version.minor()!=null && version.patch()!=null){
                return version.major().val() + DOT + version.minor().val() + DOT + version.patch().val() + CLOSE_BLOCK;
            }else if(version.minor()!=null && version.patch()==null){
                return version.major().val() + DOT + version.minor().val() + CLOSE_BLOCK;
            }else if(version.minor()==null && version.patch()==null){
                return version.major().val() + CLOSE_BLOCK;
            }  
        } 
        return CLOSE_ROUND; // Not an upper boundary
    }
    
    
}
