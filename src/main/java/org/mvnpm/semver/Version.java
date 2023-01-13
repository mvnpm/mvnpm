package org.mvnpm.semver;
import static org.mvnpm.Constants.DOT;

/**
 * Represent a SemVer string
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public record Version(
        Part major,
        Part minor,
        Part patch
    ){
    
    public Version(String major) {
        this(new Part(major), null, null);
    }
    
    public Version(String major, String minor) {
        this(new Part(major), new Part(minor), null);
    }
    
    public Version(String major, String minor, String patch) {
        this(new Part(major), new Part(minor), new Part(patch));
    }
    
    public boolean isConcrete(){
        return this.major.isConcrete() && (this.minor == null || this.minor.isConcrete()) && (this.patch == null || this.patch.isConcrete());
    }
    
    @Override
    public String toString() {
        
        if(this.minor !=null && this.patch != null){
            return major.val() + DOT + minor.val() + DOT + patch.val();
        }else if(this.minor !=null && this.patch == null){
            return major.val() + DOT + minor.val();
        }else {
            return major.val();
        }
    }
}
