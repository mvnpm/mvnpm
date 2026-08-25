package io.mvnpm.nexus.mvn.upload;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.Constants;

@ApplicationScoped
public final class NexusRepositoryProducer implements Constants {

    private static final String REPOSITORY = "repository";

    @ConfigProperty(name = "quarkus.rest-client.nexus-repository.url")
    private String basePath;

    @ConfigProperty(name = "mvnpm.nexus.mvn-repository.releases")
    private String releaseRepository;

    @ConfigProperty(name = "mvnpm.nexus.mvn-repository.snapshots")
    private String snapshotsRepository;

    @ConfigProperty(name = "mvnpm.nexus.username")
    private String userName;

    @ConfigProperty(name = "mvnpm.nexus.password")
    private String password;

    @Produces
    @Dependent
    @Snapshots
    public final RemoteRepository snapshotsRepository() {
        return createRepository("snapshots", repositoryPath(snapshotsRepository));
    }

    @Produces
    @Dependent
    @Releases
    public final RemoteRepository releasesRepository() {
        return createRepository("releases", repositoryPath(releaseRepository));
    }

    private final RemoteRepository createRepository(String id, String url) {
        Authentication authentication = new AuthenticationBuilder().addUsername(userName).addPassword(password).build();
        return new RemoteRepository.Builder(id, "default", url).setAuthentication(authentication).build();
    }

    private final String repositoryPath(final String repo) {
        return basePath + SLASH + REPOSITORY + repo + SLASH;
    }
}
