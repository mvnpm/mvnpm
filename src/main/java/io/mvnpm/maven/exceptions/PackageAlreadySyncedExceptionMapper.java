package io.mvnpm.maven.exceptions;

import java.time.Duration;
import java.util.List;

import io.mvnpm.maven.MavenCentralService;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class PackageAlreadySyncedExceptionMapper implements ExceptionMapper<PackageAlreadySyncedException> {

    @Context
    private RoutingContext routingContext;

    @Inject
    private MavenCentralService mavenCentralService;

    @Override
    public Response toResponse(PackageAlreadySyncedException exception) {
        final List<String> proxy = routingContext.queryParam("proxy");
        final List<String> redirect = routingContext.queryParam("redirect");
        if (redirect.size() == 1 && redirect.get(0).equals("true")) {
            return Response
                    .seeOther(mavenCentralService.getUri(exception.name(), exception.version(), exception.fileName()))
                    .build();
        }
        if (proxy.size() == 1 && proxy.get(0).equals("true")) {
            return mavenCentralService.proxyMavenRequest(exception.name(), exception.version(), exception.fileName())
                    .await().atMost(Duration.ofSeconds(10));
        }
        return exception.getErrorResponse();
    }

}
