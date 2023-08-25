package io.mvnpm.ui;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ApplicationScoped
public class SPARouting {
    private static final String[] PATH_PREFIXES = { "/q/", "/maven2", "/api" };
    private static final Predicate<String> FILE_NAME_PREDICATE = Pattern.compile(".+\\.[a-zA-Z0-9]+$").asMatchPredicate();

    public void init(@Observes Router router) {
        router.get("/*").handler(rc -> {
            final String path = rc.normalizedPath();
            if (!path.equals("/")
                    && Stream.of(PATH_PREFIXES).noneMatch(path::startsWith)
                    && !FILE_NAME_PREDICATE.test(path)) {
                rc.reroute("/");
            } else {
                rc.next();
            }
        });
    }
}