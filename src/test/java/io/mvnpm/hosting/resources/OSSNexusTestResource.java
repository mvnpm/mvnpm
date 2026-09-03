package io.mvnpm.hosting.resources;

public final class OSSNexusTestResource extends NexusTestResource {

    @Override
    String image() {
        return "sonatype/nexus3:3.76.1";
    }
}
