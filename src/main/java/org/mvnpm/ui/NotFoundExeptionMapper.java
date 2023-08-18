package org.mvnpm.ui;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.net.URI;
import java.util.List;

/**
 * Make sure the route pages can reload
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Provider
public class NotFoundExeptionMapper implements ExceptionMapper<NotFoundException> {
    
    @Context
    HttpHeaders headers;
    
    @Context
    UriInfo uriInfo;
    
    @Override
    public Response toResponse(NotFoundException exception) {
        return Response.status(404).entity(exception.getMessage()).build();
    }

}
