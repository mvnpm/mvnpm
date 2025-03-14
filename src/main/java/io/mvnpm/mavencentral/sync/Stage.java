package io.mvnpm.mavencentral.sync;

public enum Stage {
    NONE, // 0: Package is request and will be created
    INIT, // 1: Package is created and signed
    UPLOADING, // 2: Package is uploading to central
    UPLOADED, // 3: Package is uploaded in central
    CLOSED, // 4: Package is validated in central
    RELEASING, // 5: Package is being released in central
    RELEASED, // 6: Package is released in central
    ERROR // 7: Package is in an error state
}
