package io.mvnpm.maven.exceptions;

public class NotFoundInMavenRepoException extends RuntimeException {

    public NotFoundInMavenRepoException(String uri) {
        super("The uri " + uri + " was not found in the Maven central repository.");
    }
}
