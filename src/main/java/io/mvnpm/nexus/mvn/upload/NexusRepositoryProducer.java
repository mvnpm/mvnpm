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

    @ConfigProperty(name = "quarkus.rest-client.repository.url")
    String basePath;

    @ConfigProperty(name = "mvnpm.custom.repository.releases")
    String releaseRepository;

    @ConfigProperty(name = "mvnpm.custom.repository.snapshots")
    String snapshotsRepository;

    @ConfigProperty(name = "mvnpm.custom.repository.username")
    String userName;

    @ConfigProperty(name = "mvnpm.custom.repository.password")
    String password;

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
        return basePath + SLASH + REPOSITORY + SLASH + repo + SLASH;
    }
}
