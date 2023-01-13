package org.mvnpm.event;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
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
        Log.info("<--- " + ctx.getStatus() + Constants.SPACE + Constants.HYPHEN + Constants.SPACE + ctx.getStatusInfo().getReasonPhrase());
    }
}
