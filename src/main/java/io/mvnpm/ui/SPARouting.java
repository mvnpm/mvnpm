package io.mvnpm.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.vertx.ext.web.Router;

@ApplicationScoped
public class SPARouting {

    public void init(@Observes Router router) {
        router.get("/*").handler(rc -> {
            final String path = rc.normalizedPath();
            if (path.startsWith("/package/") || path.startsWith("/search/")) {
                rc.reroute("/");
            } else {
                rc.next();
            }
        });
    }
}
