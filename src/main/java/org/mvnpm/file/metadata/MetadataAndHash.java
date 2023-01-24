package org.mvnpm.file.metadata;

public record MetadataAndHash(String sha1, String md5, String asc, byte[] data) {

}
