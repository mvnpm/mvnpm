package io.mvnpm.maven.exceptions;

public class MavenRequestError extends RuntimeException {
    private final String uri;
    private final int errorCode;

    public MavenRequestError(String uri, int errorCode) {
        super("Maven request error: %d %s".formatted(errorCode, uri));
        this.uri = uri;
        this.errorCode = errorCode;
    }
}
