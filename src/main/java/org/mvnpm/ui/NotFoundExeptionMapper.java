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
    
    @Inject
    IndexHtml indexHtml;
    
    @Override
    public Response toResponse(NotFoundException exception) {
        if(isHtmlRequest()){ // We only care about html requests here.
            URI uri = uriInfo.getRequestUri();
            long numberOfSlashes = uri.getPath().chars().filter(ch -> ch == '/').count();
            if(uri.getPath().equals(SLASH) || (uri.getPath().startsWith(SLASH) && numberOfSlashes == 1)){
                return Response.ok(indexHtml.getHomePage(), MediaType.TEXT_HTML).build();
            }
        }
        return Response.status(404).entity(exception.getMessage()).build();
    }
    
    private boolean isHtmlRequest() {
        List<MediaType> acceptableMediaTypes = headers.getAcceptableMediaTypes();
        for(MediaType mt:acceptableMediaTypes){
            if(mt.toString().toLowerCase().startsWith(MediaType.TEXT_HTML.toString().toLowerCase())){
                return true;
            }
        }
        return false;
    }
    
    private static final String SLASH = "/";
}
