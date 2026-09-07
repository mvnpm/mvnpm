package io.mvnpm.nexus.tooling;

import org.jboss.resteasy.reactive.ClientWebApplicationException;

/**
 * A {@link ClientWebApplicationException} for type conversion of the nexus implementation.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public class TypeConversionException extends ClientWebApplicationException {

    public TypeConversionException() {
    }

    public TypeConversionException(String message) {
        super(message);
    }

    public TypeConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeConversionException(Throwable cause) {
        super(cause);
    }
}
