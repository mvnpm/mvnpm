package io.mvnpm.maven.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.Constants;
import io.quarkus.arc.properties.IfBuildProperty;

/**
 * The producer bean for creating both the release- and snapshot-{@link RemoteRepository}.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@ApplicationScoped
@IfBuildProperty(name = "mvnpm.custom.repository.enabled", stringValue = "true")
public final class RepositoryProducer implements Constants {

    private static final String REPOSITORY = "repository";

    @ConfigProperty(name = "quarkus.rest-client.repository.url")
    private String basePath;

    @ConfigProperty(name = "mvnpm.custom.mvn-repository.username")
    private String userName;

    @ConfigProperty(name = "mvnpm.custom.mvn-repository.password")
    private String password;

    @ConfigProperty(name = "mvnpm.custom.mvn-repository.releases")
    private String releaseRepository;

    @ConfigProperty(name = "mvnpm.custom.mvn-repository.snapshots")
    private String snapshotsRepository;

    @Produces
    @Dependent
    @Snapshots
    @IfBuildProperty(name = "mvnpm.custom.repository.enabled", stringValue = "true")
    public final RemoteRepository snapshotsRepository() {
        return createRepository("snapshots", repositoryPath(snapshotsRepository));
    }

    @Produces
    @Dependent
    @Releases
    @IfBuildProperty(name = "mvnpm.custom.repository.enabled", stringValue = "true")
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
