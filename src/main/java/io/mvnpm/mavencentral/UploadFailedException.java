package io.mvnpm.mavencentral;

public class UploadFailedException extends Exception {

    public UploadFailedException() {
    }

    public UploadFailedException(String message) {
        super(message);
    }

    public UploadFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UploadFailedException(Throwable cause) {
        super(cause);
    }

    public UploadFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
