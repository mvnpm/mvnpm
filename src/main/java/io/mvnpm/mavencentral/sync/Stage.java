package io.mvnpm.mavencentral.sync;

public enum Stage {
    NONE, // 0: Only package info has been requested
    PACKAGING, // 1: Package is being created and signed
    INIT, // 2: Package is created and signed
    UPLOADING, // 3: Package is uploading to central
    UPLOADED, // 4: Package is uploaded in central
    CLOSED, // 5: Package is validated in central
    RELEASING, // 6: Package is being released in central
    RELEASED, // 7: Package is released in central
    ERROR // 8: Package is in an error state
}
