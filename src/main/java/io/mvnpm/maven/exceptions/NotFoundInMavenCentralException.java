package io.mvnpm.maven.exceptions;

public class NotFoundInMavenCentralException extends RuntimeException {
    private final String uri;

    public NotFoundInMavenCentralException(String uri) {
        super("The uri " + uri + " was not found in the Maven central repository.");
        this.uri = uri;
    }
}
