package io.mvnpm.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.maven.model.Model;

import io.mvnpm.Constants;
import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageCreator;
import io.mvnpm.creator.composite.CompositeService;
import io.mvnpm.creator.events.DependencyVersionCheckRequest;
import io.mvnpm.creator.type.PomService;
import io.mvnpm.creator.utils.ImportMapUtil;
import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.CentralSyncItemService;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.Project;
import io.mvnpm.version.Version;
import io.mvnpm.version.VersionMatcher;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * The maven repository as a service
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MavenRepositoryService {

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    @Inject
    CompositeService compositeService;

    @Inject
    PackageCreator packageCreator;

    @Inject
    ImportMapUtil importMapUtil;
    @Inject
    private MavenCentralService mavenCentralService;

    @Inject
    private PomService pomService;
    @Inject
    private CentralSyncItemService centralSyncItemService;

    public byte[] getImportMap(NameVersion nameVersion) {
        if (nameVersion.name().isInternal()) {
            try {
                return Files.readAllBytes(compositeService.getImportMap(nameVersion.name(), nameVersion.version()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            Package npmPackage = npmRegistryFacade.getPackage(nameVersion.name().npmFullName, nameVersion.version());
            return importMapUtil.createImportMap(npmPackage);
        }
    }

    public Path getPath(String groupId, String artifactId, String version, FileType type) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        return getPath(name, version, type);
    }

    public Path getOrDownloadFromMavenCentral(Name name, String version, FileType type) {
        try {
            return getPath(name, version, type);
        } catch (PackageAlreadySyncedException e) {
            return mavenCentralService.downloadFromMavenCentral(name, version, type);
        }
    }

    public Path getPath(Name name, String version, FileType type) {
        if (version.equalsIgnoreCase(Constants.LATEST)) {
            String latestVersion = getLatestVersion(name);
            return getPath(name, latestVersion, type);
        } else {
            if (name.isInternal()) {
                return compositeService.getPath(name, version, type);
            } else {
                return packageCreator.getFromCacheOrCreate(type, name, version);
            }
        }
    }

    public Uni<Void> checkDependencies(DependencyVersionCheckRequest req) {
        final CentralSyncItem item = centralSyncItemService.find(req.name().mvnGroupId, req.name().mvnArtifactId,
                req.version());
        if (item == null || !item.alreadyReleased() || item.dependenciesChecked) {
            return Uni.createFrom().nullItem();
        }
        Model model = pomService.readPom(req.pomFile());
        AtomicBoolean error = new AtomicBoolean(false);
        final String reqGavString = req.name().toGavString(req.version());
        return Multi.createFrom().iterable(PomService.resolveDependencies(model))
                .onItem().transformToUniAndConcatenate(d -> Uni.createFrom().item(() -> {
                    final String range = d.getVersion();
                    final Name name = NameParser.fromMavenGA(d.getGroupId(), d.getArtifactId());
                    Project project = npmRegistryFacade.getProject(name.npmFullName);
                    if (project == null) {
                        return null;
                    }
                    final Set<Version> versions = project.versions().stream()
                            .map(Version::fromString)
                            .collect(Collectors.toSet());
                    final Version version = VersionMatcher.selectLatestMatchingVersion(versions, range);
                    return version != null ? new NameVersion(name, version.toString()) : null;
                }).onItem().delayIt().by(Duration.ofSeconds(3)))
                .filter(Objects::nonNull)
                .invoke(n -> {
                    final String depGavString = n.name().toGavString(n.version());
                    Log.infof("Matching dependency version found for package %s -> %s", reqGavString, depGavString);
                    try {
                        getPath(n.name(), n.version(), FileType.jar);
                    } catch (PackageAlreadySyncedException e) {
                        // Do nothing
                    } catch (Exception e) {
                        Log.warnf("Error while syncing matching dependency '%s' because: %s",
                                n.name().toGavString(n.version()), e.getMessage());
                        error.set(true);
                    }
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()) // Runs on worker thread
                .collect().asList()
                .invoke(() -> {
                    if (!error.get()) {
                        Log.infof("Package %s dependencies have been checked.", req.name().toGavString(req.version()));
                        centralSyncItemService.dependenciesChecked(item);
                    }
                }).replaceWithVoid();
    }

    public Path getSha1(String groupId, String artifactId, String version, FileType type) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        return getSha1(name, version, type);
    }

    public Path getSha1(Name name, String version, FileType type) {
        if (version.equalsIgnoreCase(Constants.LATEST)) {
            String latestVersion = getLatestVersion(name);
            return getSha1(name, latestVersion, type);
        } else {
            if (name.isInternal()) {
                return compositeService.getSha1Path(name, version, type);
            } else {
                return packageCreator.getSha1FromCacheOrCreate(type, name, version);
            }
        }
    }

    public Path getMd5(String groupId, String artifactId, String version, FileType type) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        return getMd5(name, version, type);
    }

    public Path getMd5(Name name, String version, FileType type) {
        if (version.equalsIgnoreCase(Constants.LATEST)) {
            String latestVersion = getLatestVersion(name);
            return getMd5(name, latestVersion, type);
        } else {
            if (name.isInternal()) {
                return compositeService.getMd5Path(name, version, type);
            } else {
                return packageCreator.getMd5FromCacheOrCreate(type, name, version);
            }
        }
    }

    public Path getAsc(String groupId, String artifactId, String version, FileType type) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        return getAsc(name, version, type);
    }

    public Path getAsc(Name name, String version, FileType type) {
        if (version.equalsIgnoreCase(Constants.LATEST)) {
            String latestVersion = getLatestVersion(name);
            return getAsc(name, latestVersion, type);
        } else {
            if (name.isInternal()) {
                return compositeService.getAscPath(name, version, type);
            } else {
                return packageCreator.getAscFromCacheOrCreate(type, name, version);
            }
        }
    }

    private String getLatestVersion(Name fullName) {
        Project project = npmRegistryFacade.getProject(fullName.npmFullName);
        return project.distTags().latest();
    }
}
