package io.mvnpm.npm.model;

import java.net.URL;
import java.util.List;

public record Dist(String integrity,
        String shasum,
        URL tarball,
        int fileCount,
        long unpackedSize,
        List<Signature> signatures) {
}
