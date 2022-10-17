package org.mvnpm.npm.model;

import java.util.List;

public record SearchResults(
        List<SearchResult> objects,
        int total) {
}
