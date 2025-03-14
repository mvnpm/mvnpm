package io.mvnpm.mavencentral.exceptions;

public class StatusCheckException extends Exception {

    public StatusCheckException() {
    }

    public StatusCheckException(String message) {
        super(message);
    }

    public StatusCheckException(String message, Throwable cause) {
        super(message, cause);
    }

    public StatusCheckException(Throwable cause) {
        super(cause);
    }

    public StatusCheckException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
