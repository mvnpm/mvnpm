package io.mvnpm.npm.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GetPackageExceptionTest {

    @ParameterizedTest
    @ValueSource(ints = { 400, 404, 405, 410 })
    void isPermanentlyUnavailable_permanentErrors(int status) {
        GetPackageException ex = createWithStatus(status);
        assertTrue(ex.isPermanentlyUnavailable(),
                "Status " + status + " should be permanently unavailable");
    }

    @ParameterizedTest
    @ValueSource(ints = { 401, 403, 429, 500, 502, 503 })
    void isPermanentlyUnavailable_retriableErrors(int status) {
        GetPackageException ex = createWithStatus(status);
        assertFalse(ex.isPermanentlyUnavailable(),
                "Status " + status + " should NOT be permanently unavailable");
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