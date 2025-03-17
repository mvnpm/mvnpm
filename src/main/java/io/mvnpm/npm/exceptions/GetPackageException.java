package io.mvnpm.npm.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.ClientWebApplicationException;

public class GetPackageException extends WebApplicationException {
    public GetPackageException(String project, String version, ClientWebApplicationException cause) {
        super(getMessage(project, version, cause), Response.status(404).entity(getMessage(project, version, cause)).build());
    }

    private static String getMessage(String project, String version, ClientWebApplicationException cause) {
        return "Error while getting Package for [" + project + "] version [" + version
                + "] because: " + cause.getResponse().readEntity(String.class);
    }
}
