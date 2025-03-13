package io.mvnpm.maven.exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.mvnpm.file.FileType;
import io.mvnpm.npm.model.Name;

public class PackageAlreadySyncedException extends RuntimeException {
    private final String fileName;
    private final Name name;
    private final String version;
    private final FileType type;

    public PackageAlreadySyncedException(String fileName, Name name, String version, FileType type) {
        super("Package '%s' is already synced on Maven Central.".formatted(name.toGavString(version)));
        this.fileName = fileName;
        this.name = name;
        this.version = version;
        this.type = type;
    }

    public String fileName() {
        return fileName;
    }

    public Name name() {
        return name;
    }

    public String version() {
        return version;
    }

    public FileType type() {
        return type;
    }

    public Response getErrorResponse() {
        return Response.status(404).entity(getMessage()).type(MediaType.TEXT_PLAIN_TYPE).build();
    }
}
