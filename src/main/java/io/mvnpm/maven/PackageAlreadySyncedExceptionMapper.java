package io.mvnpm.maven;

import java.time.Duration;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.vertx.ext.web.RoutingContext;

@Provider
public class PackageAlreadySyncedExceptionMapper implements ExceptionMapper<PackageAlreadySyncedException> {

    @Context
    private RoutingContext routingContext;

    @Inject
    private MavenCentralService proxyService;

    @Override
    public Response toResponse(PackageAlreadySyncedException exception) {
        final List<String> proxy = routingContext.queryParam("proxy");
        if (proxy.size() == 1 && proxy.get(0).equals("true")) {
            return proxyService.proxyMavenRequest(exception.name(), exception.version(), exception.fileName()).await()
                    .atMost(Duration.ofSeconds(10));
        }
        return exception.getErrorResponse();
    }

}
