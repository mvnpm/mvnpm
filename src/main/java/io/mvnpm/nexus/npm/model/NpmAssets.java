package io.mvnpm.nexus.npm.model;

import java.util.List;

public record NpmAssets(
        List<NpmAsset> items,
        String continuationToken) {
}
