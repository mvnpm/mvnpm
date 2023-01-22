package org.mvnpm.version;

public class InvalidVersionException extends RuntimeException {
    private final String version;
    
    public InvalidVersionException(String version, Throwable t) {
        super(t);
        this.version = version;
    }
    
    public InvalidVersionException(String version) {
        super();
        this.version = version;
    }

    public String getVersion(){
        return version;
    }
    
}
