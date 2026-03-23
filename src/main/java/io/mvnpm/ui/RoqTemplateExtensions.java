package io.mvnpm.ui;

import java.util.Comparator;
import java.util.List;

import io.quarkiverse.roq.frontmatter.runtime.model.Page;
import io.quarkus.qute.TemplateExtension;
import io.vertx.core.json.JsonObject;

@TemplateExtension
public class RoqTemplateExtensions {

    static List<Page> sortedByNav(List<Page> pages) {
        return pages.stream()
                .filter(p -> p.data() != null && p.data().containsKey("nav"))
                .sorted(Comparator.comparingInt(p -> {
                    Object nav = p.data().getValue("nav");
                    if (nav instanceof JsonObject navObj) {
                        return navObj.getInteger("order", Integer.MAX_VALUE);
                    }
                    return Integer.MAX_VALUE;
                }))
                .toList();
    }
}
