package io.mvnpm.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.netty.handler.codec.http.HttpStatusClass;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class SPARouting {

    public void init(@Observes Router router) {
        router.get("/*").handler(rc -> {
            // Workaround for https://github.com/quarkusio/quarkus/issues/54272
            rc.addHeadersEndHandler(v -> {
                if (!HttpStatusClass.SUCCESS.contains(rc.response().getStatusCode())) {
                    rc.response().headers().remove("Cache-Control");
                }
            });
            final String path = rc.normalizedPath();
            if (path.startsWith("/package/") || path.startsWith("/search/")) {
                rc.reroute("/");
            } else {
                rc.next();
            }
        });
    }
}
