package org.mvnpm.npm.model;

import java.net.URL;
import java.util.List;
import java.util.Date;
import java.util.Map;

public record SearchItem(
        String name,
        String description,
        String scope,
        String version,
        List<String> keywords,
        Date date,
        Map<String,URL> links,
        Author author,
        Maintainer publisher,
        List<Maintainer> maintainers) {
}
