package io.mvnpm.maven.exceptions;

public class NotFoundInRepositoryException extends RuntimeException {
    private final String uri;

    public NotFoundInRepositoryException(String uri) {
        super("The uri " + uri + " was not found in the maven repository.");
        this.uri = uri;
    }
}
