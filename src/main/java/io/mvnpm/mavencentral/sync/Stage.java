package io.mvnpm.mavencentral.sync;

public enum Stage {
    NONE,
    INIT,
    UPLOADING,
    UPLOADED,
    CLOSED,
    RELEASING,
    RELEASED,
    ERROR
}
