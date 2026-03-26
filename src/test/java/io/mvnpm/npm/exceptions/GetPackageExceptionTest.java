package io.mvnpm.npm.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;

class GetPackageExceptionTest {

    @Test
    void isNotFoundReturnsTrueFor404() {
        GetPackageException ex = createWithStatus(404);
        assertTrue(ex.isNotFound());
    }

    @Test
    void isNotFoundReturnsFalseFor429() {
        GetPackageException ex = createWithStatus(429);
        assertFalse(ex.isNotFound());
    }

    @Test
    void isNotFoundReturnsFalseFor500() {
        GetPackageException ex = createWithStatus(500);
        assertFalse(ex.isNotFound());
    }

    @Test
    void preservesOriginalStatusCode() {
        GetPackageException ex = createWithStatus(429);
        assertEquals(429, ex.getResponse().getStatus());
    }

    private GetPackageException createWithStatus(int status) {
        Response response = Response.status(status).entity("error body").build();
        ClientWebApplicationException cause = new ClientWebApplicationException("error", response);
        return new GetPackageException("test-project", "1.0.0", cause);
    }
}
