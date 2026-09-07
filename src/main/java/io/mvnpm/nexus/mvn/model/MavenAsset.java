package io.mvnpm.nexus.mvn.model;

import io.mvnpm.nexus.npm.model.Checksums;

/**
 * The asset object for responses of the sonatype nexus rest-API.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
public record MavenAsset(
        String downloadUrl,
        String path,
        String id,
        String repository,
        String format,
        Checksums checksum,
        String contentType,
        String lastModified,
        String lastDownloaded,
        String uploader,
        String uploaderIp,
        long fileSize,
        String blobCreated,
        String blobStoreName,
        MavenDetails maven2) {
}
