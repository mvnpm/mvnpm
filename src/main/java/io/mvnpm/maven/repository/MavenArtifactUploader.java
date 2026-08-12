package io.mvnpm.maven.repository;

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
import io.mvnpm.maven.api.BundleCreator.BundleRecord;
import io.mvnpm.maven.api.Gav;
import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;

/**
 * The bean to upload maven artifacts via the {@link BootstrapMavenContext}.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@ApplicationScoped
@IfBuildProperty(name = "mvnpm.custom.repository.enabled", stringValue = "true")
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

    /**
     * Uploads the given artifact and its {@link BundleRecord}s.
     *
     * @param gav The {@link Gav} of the artifact to upload
     * @param records The {@link BundleRecord}s to upload for given artifact
     * @return The resulting {@link DeployResult} of the method
     *         {@link RepositorySystem#deploy(RepositorySystemSession, DeployRequest)}
     * @throws Exception if something went wrong
     */
    public final DeployResult upload(Gav gav, List<BundleRecord> records) throws Exception {
        final RemoteRepository repository = gav.getVersion().endsWith("-SNAPSHOT") ? snapshotsRepository
                : releasesRepository;
        final DeployRequest request = new DeployRequest().setRepository(repository)
                .setArtifacts(records.stream().map(record -> createArtifact(gav, record)).toList());

        return repositorySystem.deploy(session, request);
    }

    /**
     * Uses given {@link Gav} and {@link BundleRecord#classifier()}/{@link BundleRecord#path()} to create a {@link Artifact}.
     *
     * @param gav The artifacts dependency information encapsulated in {@link Gav}
     * @param record The {@link BundleRecord} to create an {@link Artifact} for
     * @return The resulting {@link Artifact}
     */
    private final Artifact createArtifact(final Gav gav, final BundleRecord record) {
        String type = JAR;
        String classifier = record.classifier();
        if (POM.equals(classifier)) {
            type = POM;
        }

        if (POM.equals(classifier) || JAR.equals(classifier)) {
            // the key is the classifier, but officially POM and JAR do not have a classifier, therefore:
            classifier = null;
        }

        return new DefaultArtifact(gav.getGroupId(), gav.getArtifactId(), classifier, type, gav.getVersion())
                .setFile(record.path().toFile());
    }
}
