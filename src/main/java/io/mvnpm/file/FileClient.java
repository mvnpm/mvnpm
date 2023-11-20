package io.mvnpm.file;

import static io.mvnpm.file.FileType.jar;
import static io.mvnpm.file.FileType.pom;
import static io.mvnpm.file.FileType.tgz;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.file.type.JarClient;
import io.mvnpm.file.type.PomClient;
import io.mvnpm.file.type.TgzClient;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.quarkus.logging.Log;

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
    NpmRegistryFacade npmRegistryFacade;

    public Path getFilePath(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalFullPath(type, name, version);
        return localOrFetchRemote(type, name, version, localFilePath);
    }

    public Path getFileSha1(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalSha1FullPath(type, name, version);
        return fetchLocalWhenReady(localFilePath);
    }

    public Path getFileMd5(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalMd5FullPath(type, name, version);
        return fetchLocalWhenReady(localFilePath);
    }

    public Path getFileAsc(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalAscFullPath(type, name, version);
        return fetchLocalWhenReady(localFilePath);
    }

    private Path localOrFetchRemote(FileType type, Name name, String version, Path localFilePath) {

        boolean local = Files.exists(localFilePath);
        if (local) {
            Log.debug("Serving locally [" + localFilePath + "]");
            return fetchLocalWhenReady(localFilePath);
        } else {
            Log.debug("Serving remotely [" + localFilePath + "]");
            return fetchRemoteAndStore(type, name, version, localFilePath);
        }
    }

    private Path fetchLocalWhenReady(Path localFilePath) {
        try {
            boolean readyForUse = FileUtil.isReadyForUse(localFilePath);
            if (readyForUse)
                return localFilePath;
            throw new RuntimeException(localFilePath.toString() + " not ready for use");
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Path fetchRemoteAndStore(FileType type, Name name, String version, Path localFilePath) {
        io.mvnpm.npm.model.Package p = npmRegistryFacade.getPackage(name.npmFullName, version);
        switch (type) {
            case tgz -> {
                tgzClient.fetchRemoteAndSave(p, localFilePath);
            }
            case jar -> {
                jarClient.createAndSaveJar(p, localFilePath);
            }
            case pom -> {
                pomClient.createAndSavePom(p, localFilePath);
            }
        }
        return fetchLocalWhenReady(localFilePath);
    }
}
