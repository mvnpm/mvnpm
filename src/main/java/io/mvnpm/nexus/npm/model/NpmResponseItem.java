package io.mvnpm.nexus.npm.model;

import java.util.List;

public record NpmResponseItem(
        String id,
        String repository,
        String format,
        String group,
        String name,
        String version,
        List<NpmAsset> assets) {
}
