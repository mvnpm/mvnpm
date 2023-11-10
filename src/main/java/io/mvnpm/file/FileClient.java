package io.mvnpm.file;

import static io.mvnpm.file.FileType.jar;
import static io.mvnpm.file.FileType.pom;
import static io.mvnpm.file.FileType.tgz;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
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

    public byte[] getFileContents(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalFullPath(type, name, version);
        return fetch(type, name, version, localFilePath);
    }

    public byte[] getFileSha1(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalSha1FullPath(type, name, version);
        return fetchSha1(type, name, version, localFilePath);
    }

    public byte[] getFileMd5(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalMd5FullPath(type, name, version);
        return fetchLocal(localFilePath);
    }

    public byte[] getFileAsc(FileType type, Name name, String version) {
        Path localFilePath = fileStore.getLocalAscFullPath(type, name, version);
        return fetchLocal(localFilePath);
    }

    private byte[] fetch(FileType type, Name name, String version, Path localFilePath) {
        boolean local = Files.exists(localFilePath);
        if (local) {
            Log.debug("Serving locally [" + localFilePath + "]");
            return fileStore.readFile(localFilePath);
        } else {
            Log.debug("Serving remotely [" + localFilePath + "]");
            return fetchRemote(type, name, version, localFilePath);
        }
    }

    private byte[] fetchSha1(FileType type, Name name, String version, Path localFilePath) {
        boolean local = Files.exists(localFilePath);
        if (local) {
            Log.debug("Serving locally [" + localFilePath + "]");
            return fileStore.readFile(localFilePath);
        } else {
            Log.debug("Fetching remotely [" + localFilePath + "]");
            Path localFullFilePath = fileStore.getLocalFullPath(type, name, version);
            return fetchRemote(type, name, version, localFullFilePath);
        }
    }

    private byte[] fetchLocal(Path localFilePath) {
        if (Files.exists(localFilePath)) {
            Log.debug("Serving locally [" + localFilePath + "]");
            return fileStore.readFile(localFilePath);
        } else {
            throw new UncheckedIOException(new FileNotFoundException(localFilePath.toString()));
        }
    }

    private byte[] fetchRemote(FileType type, Name name, String version, Path localFilePath) {
        io.mvnpm.npm.model.Package p = npmRegistryFacade.getPackage(name.npmFullName, version);
        switch (type) {
            case tgz -> {
                return tgzClient.fetchRemote(p, localFilePath);
            }
            case jar -> {
                return jarClient.createJar(p, localFilePath);
            }
            case pom -> {
                return pomClient.createPom(p, localFilePath);
            }
            default -> {
                return fileStore.readFile(localFilePath);
            }
        }
    }

}
