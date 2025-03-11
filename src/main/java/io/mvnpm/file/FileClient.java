package io.mvnpm.file;

import java.io.IOException;
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
    EventBus bus;

    @Inject
    NpmRegistryFacade npmRegistryFacade;

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
            try {
                final Path tempDirectory = Files.createTempDirectory("jar-" + name.toPathString(version));
                final Path tempPath = tempDirectory.resolve(localFilePath.getFileName().toString());
                final Path targetDirectory = localFilePath.getParent();
                return fetchRemoteAndStore(tempDirectory, targetDirectory, type, name, version, tempPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }

    private Path fetchRemoteAndStore(Path tempDirectory, Path targetDirectory, FileType type, Name name, String version,
            Path localFilePath) {
        io.mvnpm.npm.model.Package p = npmRegistryFacade.getPackage(name.npmFullName, version);
        switch (type) {
            case tgz, jar -> createPomTgzAndJar(tempDirectory, targetDirectory, name, version, localFilePath, p);
            case pom -> pomClient.createAndSavePom(p, localFilePath); // Only create the POM to avoid extra processing
        }
        return localFilePath;
    }

    private void createPomTgzAndJar(Path tempDirectory, Path targetDirectory, Name name, String version, Path localFilePath,
            io.mvnpm.npm.model.Package p) {
        Path jarPath = tempDirectory.resolve(
                fileStore.getLocalFileName(FileType.jar, name.mvnArtifactId, version, Optional.empty()));
        Path pomPath = tempDirectory.resolve(
                fileStore.getLocalFileName(FileType.pom, name.mvnArtifactId, version, Optional.empty()));
        Path tgzPath = tempDirectory.resolve(
                fileStore.getLocalFileName(FileType.tgz, name.mvnArtifactId, version, Optional.empty()));
        pomClient.createAndSavePom(p, pomPath);
        tgzClient.fetchRemoteAndSave(p, tgzPath);
        jarClient.createAndSaveJar(p, jarPath, pomPath, tgzPath);
        bus.send(NewJarEvent.EVENT_NAME,
                new NewJarEvent(tempDirectory, pomPath, jarPath, tgzPath, List.of(), targetDirectory, p.name(), p.version()));
    }
}
