package io.mvnpm.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
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
import io.mvnpm.maven.api.NameVersion;
import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.mvnpm.maven.sync.SyncItem;
import io.mvnpm.maven.sync.SyncItemService;
import io.mvnpm.maven.sync.SyncService;
import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.ProjectInfo;
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
    NpmFacade npmFacade;

    @Inject
    CompositeService compositeService;

    @Inject
    PackageCreator packageCreator;

    @Inject
    ImportMapUtil importMapUtil;

    @Inject
    private MavenService mavenService;

    @Inject
    private PomService pomService;

    @Inject
    private SyncItemService syncItemService;

    @Inject
    private SyncService syncService;

    public byte[] getImportMap(NameVersion nameVersion) {
        if (nameVersion.name().isInternal()) {
            try {
                return Files.readAllBytes(compositeService.getImportMap(nameVersion.name(), nameVersion.version()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            Package npmPackage = npmFacade.getPackage(nameVersion.name().npmFullName, nameVersion.version());
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
            return mavenService.download(name, version, type);
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
        final SyncItem item = syncItemService.find(req.name().mvnGroupId, req.name().mvnArtifactId, req.version());
        if (item == null || !item.alreadyReleased() || item.dependenciesChecked) {
            return Uni.createFrom().nullItem();
        }
        Model model = pomService.readPom(req.pomFile());
        final String reqGavString = req.name().toGavString(req.version());
        return Multi.createFrom().iterable(PomService.resolveDependencies(model)).onItem()
                .transformToUniAndConcatenate(d -> Uni.createFrom().item(() -> {
                    final String range = d.getVersion();
                    final Name name = NameParser.fromMavenGA(d.getGroupId(), d.getArtifactId());
                    ProjectInfo info = npmFacade.getProjectInfo(name.npmFullName);
                    if (info == null) {
                        return null;
                    }
                    final Set<Version> versions = info.versions().stream().map(Version::fromString)
                            .collect(Collectors.toSet());
                    final Version version = VersionMatcher.selectLatestMatchingVersion(versions, range);
                    return version != null ? new NameVersion(name, version.toString()) : null;
                }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).onItem().delayIt()
                        .by(Duration.ofSeconds(1)))
                .filter(Objects::nonNull).emitOn(Infrastructure.getDefaultWorkerPool()).invoke(n -> {
                    final String depGavString = n.name().toGavString(n.version());
                    Log.infof("Matching dependency version found for package %s -> %s", reqGavString, depGavString);
                    // Queue for sync without creating files — files are created at upload time
                    boolean queued = syncService.initializeSync(n.name(), n.version());
                    if (queued) {
                        Log.infof("Dependency '%s' queued for sync", depGavString);
                    } else {
                        Log.warnf("Dependency '%s' already synced or in progress", depGavString);
                    }
                }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool()).collect().asList().invoke(() -> {
                    Log.infof("Package %s dependencies have been checked.", req.name().toGavString(req.version()));
                    syncItemService.dependenciesChecked(item);
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
        ProjectInfo info = npmFacade.getProjectInfo(fullName.npmFullName);
        return info.distTags().latest();
    }
}
