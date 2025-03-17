package io.mvnpm.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageCreator;
import io.mvnpm.creator.composite.CompositeService;
import io.mvnpm.creator.utils.ImportMapUtil;
import io.mvnpm.mavencentral.sync.CentralSyncService;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.Project;
import io.vertx.mutiny.core.eventbus.EventBus;

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
    CentralSyncService centralSyncService;

    @Inject
    CompositeService compositeService;

    @Inject
    PackageCreator packageCreator;

    @Inject
    ImportMapUtil importMapUtil;

    @Inject
    EventBus bus;

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

    public Path getPath(Name name, String version, FileType type) {
        if (version.equalsIgnoreCase(Constants.LATEST)) {
            String latestVersion = getLatestVersion(name);
            return getPath(name, latestVersion, type);
        } else {
            if (centralSyncService.checkReleaseInDbAndCentral(name.mvnGroupId, name.mvnArtifactId, version, type.triggerSync())
                    .alreadyReleased()) {
                throw packageCreator.newPackageAlreadySyncedException(name, version, type, Optional.empty());
            }
            if (name.isInternal()) {
                return compositeService.getPath(name, version, type);
            } else {
                return packageCreator.getFromCacheOrCreate(type, name, version);
            }
        }
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
            if (centralSyncService.checkReleaseInDbAndCentral(name.mvnGroupId, name.mvnArtifactId, version, type.triggerSync())
                    .alreadyReleased()) {
                throw packageCreator.newPackageAlreadySyncedException(name, version, type, Optional.of(Constants.DOT_SHA1));
            }
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
            if (centralSyncService.checkReleaseInDbAndCentral(name.mvnGroupId, name.mvnArtifactId, version, type.triggerSync())
                    .alreadyReleased()) {
                throw packageCreator.newPackageAlreadySyncedException(name, version, type, Optional.of(Constants.DOT_MD5));
            }
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
            if (centralSyncService.checkReleaseInDbAndCentral(name.mvnGroupId, name.mvnArtifactId, version, type.triggerSync())
                    .alreadyReleased()) {
                throw packageCreator.newPackageAlreadySyncedException(name, version, type, Optional.of(Constants.DOT_ASC));
            }
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
