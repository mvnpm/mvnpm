package io.mvnpm.hosting.resources;

public final class CommunityLatestNexusTestResource extends NexusTestResource {

    @Override
    String image() {
        return "sonatype/nexus3:latest";
    }

}
