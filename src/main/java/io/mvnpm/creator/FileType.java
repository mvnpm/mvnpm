package io.mvnpm.creator;

import io.mvnpm.Constants;

public enum FileType {
    jar(true),
    tgz(true),
    pom(false),
    source(true),
    javadoc(true),
    importmap(false);

    private final boolean triggerSync;

    FileType(boolean triggerSync) {
        this.triggerSync = triggerSync;
    }

    public boolean triggerSync() {
        return triggerSync;
    }

    public static FileType fromFileName(String fileName) {
        if (fileName.endsWith(Constants.DOT_SHA1)) {
            fileName = fileName.substring(0, fileName.length() - Constants.DOT_SHA1.length());
        } else if (fileName.endsWith(Constants.DOT_ASC)) {
            fileName = fileName.substring(0, fileName.length() - Constants.DOT_ASC.length());
        }
        fileName = fileName.substring(fileName.lastIndexOf(Constants.DOT) + 1);
        return FileType.valueOf(fileName);
    }

    public String getPostString() {
        if (this.equals(FileType.source)) {
            return Constants.DASH_SOURCES_DOT_JAR;
        }
        if (this.equals(FileType.javadoc)) {
            return Constants.DASH_JAVADOC_DOT_JAR;
        }
        if (this.equals(FileType.importmap)) {
            return Constants.DASH_IMPORTMAP_DOT_JSON;
        }
        return Constants.DOT + this.name();
    }
}
