package io.mvnpm.nexus.mvn.upload;

import java.nio.file.Path;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.repository.RemoteRepository;

import io.mvnpm.Constants;
import io.mvnpm.maven.api.Gav;
import io.mvnpm.maven.api.BundleCreator.BundleRecord;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;

@ApplicationScoped
public final class MavenArtifactUploader implements Constants {

    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession session;

    private final RemoteRepository releasesRepository;
    private final RemoteRepository snapshotsRepository;

    @Inject
    public MavenArtifactUploader(BootstrapMavenContext mvnCtx, @Releases RemoteRepository releasesRepository,
            @Snapshots RemoteRepository snapshotsRepository) throws BootstrapMavenException {
        this.repositorySystem = mvnCtx.getRepositorySystem();
        this.session = mvnCtx.getRepositorySystemSession();
        this.releasesRepository = releasesRepository;
        this.snapshotsRepository = snapshotsRepository;
    }

    public final DeployResult upload(Gav gav, List<BundleRecord> files) throws Exception {
        final RemoteRepository repository = gav.getVersion().endsWith("-SNAPSHOT") ? snapshotsRepository
                : releasesRepository;
        final DeployRequest request = new DeployRequest().setRepository(repository).setArtifacts(
                files.stream().map(entry -> createArtifact(gav, entry.classifier(), entry.path())).toList());

        return repositorySystem.deploy(session, request);
    }

    private final Artifact createArtifact(final Gav gav, String classifier, final Path file) {
        String type = JAR;
        if (POM.equals(classifier)) {
            type = POM;
        }

        if (POM.equals(classifier) || JAR.equals(classifier)) {
            // the key is the classifier, but officially POM and JAR do not have a classifier, therefore:
            classifier = null;
        }

        return new DefaultArtifact(gav.getGroupId(), gav.getArtifactId(), classifier, type, gav.getVersion())
                .setFile(file.toFile());
    }
}
