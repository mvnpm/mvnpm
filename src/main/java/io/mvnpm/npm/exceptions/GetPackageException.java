package io.mvnpm.npm.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.ClientWebApplicationException;

public class GetPackageException extends WebApplicationException {
    private final int npmStatusCode;

    public GetPackageException(String project, String version, ClientWebApplicationException cause) {
        super(getMessage(project, version, cause),
                Response.status(cause.getResponse().getStatus())
                        .entity(getMessage(project, version, cause)).build());
        this.npmStatusCode = cause.getResponse().getStatus();
    }

    /**
     * Returns true if the NPM error is permanent and the package will never be fetchable.
     */
    public boolean isPermanentlyUnavailable() {
        return npmStatusCode == 400 // bad request (invalid package name)
                || npmStatusCode == 404 // not found
                || npmStatusCode == 405 // method not allowed (e.g. platform-specific @esbuild packages)
                || npmStatusCode == 410; // gone (unpublished)
    }

    private static String getMessage(String project, String version, ClientWebApplicationException cause) {
        return "Error while getting Package for [" + project + "] version [" + version
                + "] because: " + cause.getResponse().readEntity(String.class);
    }
}
