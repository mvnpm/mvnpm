package org.mvnpm.maven.filter;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

/**
 * Fire event on maven requests
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class TrafficFilter {

    @Inject
    Event<ContainerRequestContext> in;
    
    @Inject
    Event<ContainerResponseContext> out;
    
    @ServerRequestFilter(preMatching = true)
    public void inFilter(ContainerRequestContext ctx) {
        in.fireAsync(ctx);
    }
    
    @ServerResponseFilter
    public void outFilter(ContainerResponseContext ctx) {
        out.fireAsync(ctx);
    }    
}