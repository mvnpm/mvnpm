package io.mvnpm.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.file.exceptions.PackageNotCreatedException;
import io.mvnpm.file.type.JarClient;
import io.mvnpm.file.type.PomClient;
import io.mvnpm.file.type.TgzClient;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.mvnpm.newfile.HashService;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.quarkus.logging.Log;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 * Get the jar or tgz from either local file system or from it's origin
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class FileClient {

    @Inject
    TgzClient tgzClient;

    @Inject
    JarClient jarClient;

    @Inject
    PomClient pomClient;

    @Inject
    FileStore fileStore;

    @Inject
    HashService hashService;

    @Inject
    EventBus bus;

    @Inject
    MavenRepositoryService mavenRepositoryService;

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    public PackageAlreadySyncedException newPackageAlreadySyncedException(Name name, String version, FileType type,
            Optional<String> dotSigned) {
        return new PackageAlreadySyncedException(fileStore.getLocalFileName(type, name, version, dotSigned), name, version,
                type);
    }

    public Path getFilePath(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalFullPath(type, name, version);
        return localOrFetchRemote(type, name, version, localFilePath);
    }

    public Path getFileSha1(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalSha1FullPath(type, name, version);
        return requireExists("sha1", type, name, version, localFilePath);
    }

    public Path getFileMd5(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalMd5FullPath(type, name, version);
        return requireExists("md5", type, name, version, localFilePath);
    }

    public Path getFileAsc(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalAscFullPath(type, name, version);
        return requireExists("asc", type, name, version, localFilePath);
    }

    private Path requireExists(String label, FileType type, Name name, String version, Path localFilePath) {
        if (Files.exists(localFilePath)) {
            return localOrFetchRemote(type, name, version, localFilePath);
        }
        throw new PackageNotCreatedException(name, type, label);
    }

    private Path localOrFetchRemote(FileType type, Name name, String version, Path localFilePath) {

        boolean local = Files.exists(localFilePath);
        if (local) {
            Log.debug("Serving locally [" + localFilePath + "]");
            return localFilePath;
        } else {
            Log.debug("Serving remotely [" + localFilePath + "]");
            return fetchRemoteAndStore(type, name, version, localFilePath);
        }
    }

    private Path fetchRemoteAndStore(FileType type, Name name, String version,
            Path localFilePath) {
        io.mvnpm.npm.model.Package p = npmRegistryFacade.getPackage(name.npmFullName, version);
        switch (type) {
            case tgz -> tgzClient.fetchRemoteAndSave(p, localFilePath);
            case jar -> createAndSaveJar(localFilePath, p);
            case pom -> pomClient.createAndSavePom(p, localFilePath); // Only create the POM to avoid extra processing
            default -> throw new PackageNotCreatedException(name, type, version);
        }
        return localFilePath;
    }

    private void createAndSaveJar(Path jarPath, io.mvnpm.npm.model.Package p) {
        Path pomPath = mavenRepositoryService.getPath(p.name(), p.version(), FileType.pom);
        Path tgzPath = mavenRepositoryService.getPath(p.name(), p.version(), FileType.tgz);
        jarClient.createAndSaveJar(p, jarPath, pomPath, tgzPath);
        hashService.createHashes(jarPath);
        bus.send(NewJarEvent.EVENT_NAME,
                new NewJarEvent(pomPath, jarPath, tgzPath, List.of(), p.name(), p.version()));

    }
}
