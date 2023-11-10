package io.mvnpm.mavencentral.sync;

public enum Stage {
    NONE, // 0
    INIT, // 1
    UPLOADING, // 2
    UPLOADED, // 3
    CLOSED, // 4
    RELEASING, // 5
    RELEASED, // 6
    ERROR // 7
}
