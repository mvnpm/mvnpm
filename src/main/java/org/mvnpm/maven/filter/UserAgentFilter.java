package org.mvnpm.maven.filter;

import io.quarkus.logging.Log;
import io.vertx.core.http.HttpMethod;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Only allow user agents for maven (TODO: Test gradle and others)
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class UserAgentFilter {

    
    @ConfigProperty(name = "mvnpm.filter-useragent", defaultValue = "true")
    boolean filterUserAgent;
    
    @ServerRequestFilter
    public Optional<RestResponse<Void>> getFilter(ContainerRequestContext ctx) {
        if(filterUserAgent){
            // only allow GET methods for now
        
            if(ctx.getMethod().equals(HttpMethod.GET.name()) && ctx.getUriInfo().getPath().startsWith(MAVEN_2)) {
                String ua = ctx.getHeaderString(USER_AGENT);
                if(isAllowedUserAgent(ua)){
                    return Optional.empty();
                }else{
                    return Optional.of(RestResponse.status(444, NO_RESPONSE));
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
    
    private boolean isAllowedUserAgent(String ua){
        for(String allowed : ALLOWED_USER_AGENTS){
            if(ua.startsWith(allowed)){
                return true;
            }
        }
        Log.warn("# USER-AGENT NOT ALLOWED [" + ua + "]");
        return false;
    }
    
    private static final List<String> ALLOWED_USER_AGENTS = List.of("m2e", "netBeans", "Apache-Maven"); // TODO: Test other IDEs and build tools
    private static final String USER_AGENT = "User-Agent";
    private static final String NO_RESPONSE = "No Response";
    private static final String MAVEN_2 = "/maven2";
}