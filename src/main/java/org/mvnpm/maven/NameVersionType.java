package org.mvnpm.maven;

import org.mvnpm.file.FileType;
import org.mvnpm.npm.model.Name;

public record NameVersionType(Name name, String version, FileType type, boolean sha1) {

}
