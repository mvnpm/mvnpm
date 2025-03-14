package io.mvnpm.mavencentral.exceptions;

public class PromotionException extends Exception {

    public PromotionException() {
    }

    public PromotionException(String message) {
        super(message);
    }

    public PromotionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PromotionException(Throwable cause) {
        super(cause);
    }

    public PromotionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
