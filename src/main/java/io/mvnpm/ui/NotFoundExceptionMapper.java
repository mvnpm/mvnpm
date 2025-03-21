package io.mvnpm.ui;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Make sure the route pages can reload
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {
        return Response.status(404).entity(exception.getMessage()).build();
    }

}
