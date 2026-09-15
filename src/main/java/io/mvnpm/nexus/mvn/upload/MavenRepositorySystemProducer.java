package io.mvnpm.nexus.mvn.upload;

import java.nio.file.Path;
import java.util.HashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.http.impl.auth.AuthSchemeBase;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.RFC2617Scheme;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

import io.mvnpm.creator.PackageFileLocator;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Produces the Aether {@link RepositorySystem} used to deploy artifacts to a self-hosted repository.
 * <p>
 * {@link RepositorySystemSupplier} wires the resolver with plain constructors instead of a JSR-330
 * container, so nothing here needs Sisu class-path scanning. That matters for the native image: the
 * quarkus-maven-resolver extension this replaces built the same object graph from a STATIC_INIT
 * recorder, which runs inside the image builder and deadlocked on Sisu's bean loader.
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
@IfBuildProperty(name = "mvnpm.custom.repository.enabled", stringValue = "true")
// The resolver's HTTP transport caches the negotiated auth scheme by serializing it, so in a native image
// every request to the repository logs a ClassNotFoundException and re-does the 401 challenge.
@RegisterForReflection(serialization = true, targets = { BasicScheme.class, RFC2617Scheme.class,
        AuthSchemeBase.class, HashMap.class })
public final class MavenRepositorySystemProducer {

    @Inject
    PackageFileLocator packageFileLocator;

    @Produces
    @Singleton
    public RepositorySystem repositorySystem() {
        return new RepositorySystemSupplier().get();
    }

    /**
     * Deploys still stage through a local repository, so point it at the cache directory mvnpm
     * already owns rather than the build user's ~/.m2.
     */
    @Produces
    @Singleton
    public RepositorySystemSession repositorySystemSession(RepositorySystem repositorySystem) {
        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        final Path localRepository = packageFileLocator.getCacheDir();
        session.setLocalRepositoryManager(
                repositorySystem.newLocalRepositoryManager(session, new LocalRepository(localRepository.toFile())));
        return session;
    }

    void shutdown(@Disposes RepositorySystem repositorySystem) {
        repositorySystem.shutdown();
    }
}
