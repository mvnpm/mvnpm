package io.mvnpm.maven.exceptions;

public class MavenCentralRequestError extends RuntimeException {
    private final String uri;
    private final int errorCode;

    public MavenCentralRequestError(String uri, int errorCode) {
        super("Maven central request error: %d %s".formatted(errorCode, uri));
        this.uri = uri;
        this.errorCode = errorCode;
    }
}
