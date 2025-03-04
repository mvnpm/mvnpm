package io.mvnpm.maven.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class InvalidUrlPathException extends WebApplicationException {
    public InvalidUrlPathException(String urlPath) {
        super(Response.status(Response.Status.BAD_REQUEST).entity("Invalid Url Path [" + urlPath + "]").build());
    }
}
