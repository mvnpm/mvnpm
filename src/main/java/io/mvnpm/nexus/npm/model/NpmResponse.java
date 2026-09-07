package io.mvnpm.nexus.npm.model;

import java.util.List;

public record NpmResponse(
        List<NpmResponseItem> items,
        String continuationToken) {
}
