package io.mvnpm.maven;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.mvnpm.composite.CompositeService;
import io.mvnpm.file.FileClient;
import io.mvnpm.file.FileType;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;

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
    FileClient fileClient;

    public Path getPath(String groupId, String artifactId, String version, FileType type) {
        Name name = NameParser.fromMavenGA(groupId, artifactId);
        return getPath(name, version, type);
    }

    public Path getPath(Name name, String version, FileType type) {
        if (version.equalsIgnoreCase(Constants.LATEST)) {
            String latestVersion = getLatestVersion(name);
            return getPath(name, latestVersion, type);
        } else if (name.isInternal()) {
            return compositeService.getPath(name, version, type);
        } else {
            return fileClient.getFilePath(type, name, version);
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
        } else if (name.isInternal()) {
            return compositeService.getSha1Path(name, version, type);
        } else {
            return fileClient.getFileSha1(type, name, version);
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
        } else if (name.isInternal()) {
            return compositeService.getMd5Path(name, version, type);
        } else {
            return fileClient.getFileMd5(type, name, version);
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
        } else if (name.isInternal()) {
            return compositeService.getAscPath(name, version, type);
        } else {
            return fileClient.getFileAsc(type, name, version);
        }
    }

    private String getLatestVersion(Name fullName) {
        Project project = npmRegistryFacade.getProject(fullName.npmFullName);
        return project.distTags().latest();
    }
}
