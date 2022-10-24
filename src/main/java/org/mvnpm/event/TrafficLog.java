package org.mvnpm.event;

import io.quarkus.logging.Log;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import org.mvnpm.Constants;

/**
 * Log all traffic to the log file
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class TrafficLog {

    public void in(@ObservesAsync ContainerRequestContext ctx) {
        Log.info("---> " + ctx.getUriInfo().getRequestUri().toString());
    }

    public void out(@ObservesAsync ContainerResponseContext ctx) {
        Log.info("<--- " + ctx.getStatus() + Constants.SPACE + Constants.DASH + Constants.SPACE + ctx.getStatusInfo().getReasonPhrase());
    }
}
