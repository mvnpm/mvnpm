package io.mvnpm.file.exceptions;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.mvnpm.file.FileType;
import io.mvnpm.npm.model.Name;

public class PackageNotCreatedException extends WebApplicationException {
    public PackageNotCreatedException(Name name, FileType type, String label) {
        super(Response.status(404).type(MediaType.TEXT_PLAIN_TYPE).entity(
                "Package '%s' is not available either because it hasn't been generated or because it does not exists. Fetch the jar file first to generate the %s %s."
                        .formatted(name.npmFullName, type.name(), label))
                .build());
    }
}
