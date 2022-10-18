package org.mvnpm.event;

import io.quarkus.logging.Log;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;

/**
 * Log all traffic to the log file
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class TrafficLog {

    public void in(@ObservesAsync ContainerRequestContext ctx) {
        String remoteIp = getClientIP(ctx);
        Log.info("---> [" + remoteIp + "] " + ctx.getUriInfo().getRequestUri().toString());
    }

    public void out(@ObservesAsync ContainerResponseContext ctx) {
        String remoteIp = getClientIP(ctx);
        Log.info("<--- [" + remoteIp + "] " + ctx.getStatus() + " - " + ctx.getStatusInfo().getReasonPhrase());
    }
    
    private String getClientIP(ContainerResponseContext ctx){
        String remoteAddr = ctx.getHeaderString(X_FORWARDED_FOR);
        if(remoteAddr!=null && !remoteAddr.isEmpty()){
            return remoteAddr;
        }
        return UNKNOWN;
    }
    
    private String getClientIP(ContainerRequestContext ctx){
        String remoteAddr = ctx.getHeaderString(X_FORWARDED_FOR);
        if(remoteAddr!=null && !remoteAddr.isEmpty()){
            return remoteAddr;
        }
        return UNKNOWN;
    }
    
    private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
    private static final String UNKNOWN = "unknown";
}
