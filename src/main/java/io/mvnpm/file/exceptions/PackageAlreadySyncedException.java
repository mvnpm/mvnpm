package io.mvnpm.file.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import io.mvnpm.npm.model.Name;

public class PackageAlreadySyncedException extends WebApplicationException {
    public PackageAlreadySyncedException(Name name) {
        super(Response.status(404).entity("Package '%s' is already synced on Maven Central.".formatted(name.npmFullName))
                .build());
    }
}
