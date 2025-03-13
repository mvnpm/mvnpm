package io.mvnpm.npm.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public record SearchItem(
        String name,
        String description,
        String scope,
        String version,
        List<String> keywords,
        Date date,
        Map<String, String> links,
        Author author,
        Maintainer publisher,
        List<Maintainer> maintainers) {
}
