package org.mvnpm.maven;

import org.mvnpm.file.FileType;
import org.mvnpm.npm.model.FullName;

public record NameVersionType(FullName name, String version, FileType type, boolean sha1) {

}
