package org.mvnpm.maven.filter;

import io.vertx.core.http.HttpMethod;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Return 404 for things we know we do not have
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class KnownNotFoundFilter {

    @ServerRequestFilter
    public Optional<RestResponse<Void>> getFilter(ContainerRequestContext ctx) {
        // only allow GET methods for now
        
        if(ctx.getMethod().equals(HttpMethod.GET.name()) && ctx.getUriInfo().getPath().startsWith(MAVEN_2)) {
            String path = ctx.getUriInfo().getPath();
            if(isKnownNotFound(path)){
                return Optional.of(RestResponse.notFound());
            }else{
                return Optional.empty();
                
            }
        } else {
            return Optional.empty();
        }
    }
    
    private boolean isKnownNotFound(String path){
        for(String known : KNOWN_PATHS){
            if(path.startsWith(known)){
                return true;
            }
        }
        return false;
    }
    
    private static final List<String> KNOWN_PATHS = List.of("/maven2/org/mvnpm/importmap/");
    private static final String MAVEN_2 = "/maven2";
}