package io.mvnpm.nexus.npm.model;

public record NpmAsset(
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
        NpmDetails npm) {
}