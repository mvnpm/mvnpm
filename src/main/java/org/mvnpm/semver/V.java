package org.mvnpm.semver;

import org.mvnpm.Constants;
import static org.mvnpm.Constants.DOT;
import static org.mvnpm.Constants.STAR;
import static org.mvnpm.Constants.EX;
import static org.mvnpm.Constants.ESCAPED_DOT;
import static org.mvnpm.Constants.HYPHEN;

public record V(Integer major,Integer minor,Integer patch){
    
    public V(Integer major){
        this(major, null, null);
    }
    public V(Integer major, Integer minor){
        this(major, minor, null);
    }
    
    public static V fromString(String version){
        try {
            if(version.contains(DOT)){
                String parts[] = version.split(ESCAPED_DOT);
                if(parts.length > 2){
                    return new V(toNumber(parts[0]),toNumber(parts[1]),toNumber(parts[2]));
                }else if(parts.length == 2){
                    return new V(toNumber(parts[0]),toNumber(parts[1]));
                }
            }else{
                return new V(toNumber(version));
            }
        }catch(Throwable t){
            throw new InvalidVersionException(version, t);
        }
        throw new InvalidVersionException(version);
    }
    
    private static int toNumber(String part){
        if(part.equals(STAR) || part.equalsIgnoreCase(EX)){
            return Integer.MIN_VALUE;
        }if (part.contains(HYPHEN)){
            part = part.split(HYPHEN)[0]; // Strip out pre-release
        }
        return Integer.parseInt(part);
    }
    
    public V nextMajor(){
        if(this.minor!=null && this.patch!=null){
            if(majorIsX()){
                return new V(1, 0, 0);
            }
            return new V(this.major+1, 0, 0);
        }else if(this.minor!=null && this.patch==null){
            if(majorIsX()){
                return new V(1, 0);
            }
            return new V(this.major+1, 0);
        }else if(this.minor==null && this.patch==null){
            if(majorIsX()){
                return new V(1);
            }
            return new V(this.major+1);
        }
        throw new RuntimeException("Not sure how to get the next major version");
    }
    
    public V nextMinor(){
        if(this.minor!=null && this.patch!=null){
            if(minorIsX()){
                return new V(this.major, 1, 0);
            }
            return new V(this.major, this.minor+1, 0);
        }else if(this.minor!=null && this.patch==null){
            if(minorIsX()){
                return new V(this.major, 1);
            }
            return new V(this.major, this.minor+1);
        }else if(this.minor==null && this.patch==null){
            return new V(this.major, 1);
        }
        throw new RuntimeException("Not sure how to get the next minor version");
    }
    
    public V nextPatch(){
        if(this.minor!=null && this.patch!=null){
            if(patchIsX()){
                return new V(this.major, this.minor, 1);
            }
            return new V(this.major, this.minor, this.patch+1);
        }else if(this.minor!=null && this.patch==null){
            return new V(this.major, this.minor, 1);
        }else if(this.minor==null && this.patch==null){
            return new V(this.major, 0, 1);
        }
        throw new RuntimeException("Not sure how to get the next patch version");
    }
    
    public boolean patchIsX(){
        return patch!=null && patch == Integer.MIN_VALUE;
    }
    
    public boolean minorIsX(){
        return minor!=null && minor == Integer.MIN_VALUE;
    }
    
    public boolean majorIsX(){
        return major!=null && major == Integer.MIN_VALUE;
    }
    
    @Override
    public String toString() {
        if(this.minor !=null && this.patch != null){
            return toString(major) + DOT + toString(minor) + DOT + toString(patch);
        }else if(this.minor !=null && this.patch == null){
            return toString(major) + DOT + toString(minor);
        }else {
            return toString(major);
        }
    }
    
    public String toStringWithCleanedX() {
        if(this.minor !=null && this.patch != null){
            return cleanX(major) + DOT + cleanX(minor) + DOT + cleanX(patch);
        }else if(this.minor !=null && this.patch == null){
            return cleanX(major) + DOT + cleanX(minor);
        }else {
            return cleanX(major);
        }
    }
    
    private String cleanX(Integer v){
        if(v == null)return null;
        if(v == Integer.MIN_VALUE)return Constants.ZERO;
        return String.valueOf(v);
    }
    
    private String toString(Integer x){
        if(x == null)return null;
        if(x == Integer.MIN_VALUE)return Constants.EX;
        return String.valueOf(x);
    }
}
