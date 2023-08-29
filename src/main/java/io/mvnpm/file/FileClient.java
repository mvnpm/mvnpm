package io.mvnpm.file;

import io.mvnpm.file.type.JarClient;
import io.mvnpm.file.type.TgzClient;
import io.quarkus.logging.Log;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.mvnpm.file.type.PomClient;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Get the jar or tgz from either local file system or from it's origin
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class FileClient {

    @Inject
    Vertx vertx;
    
    @Inject
    TgzClient tgzClient;
    
    @Inject
    JarClient jarClient;
    
    @Inject 
    PomClient pomClient;
    
    @Inject
    FileStore fileStore;
    
    public byte[] getFileContents(FileType type, io.mvnpm.npm.model.Package p) {
        Path localFilePath = fileStore.getLocalFullPath(type, p);
        return fetch(type, p, localFilePath);
    }
    
    public byte[] getFileSha1(FileType type, io.mvnpm.npm.model.Package p) {
        Path localFilePath = fileStore.getLocalSha1FullPath(type, p);
        return fetchSha1(type, p, localFilePath);
    }
    
    public byte[] getFileMd5(FileType type, io.mvnpm.npm.model.Package p) {
        Path localFilePath = fileStore.getLocalMd5FullPath(type, p);
        return fetchLocal(localFilePath);
    }
    
    public byte[] getFileAsc(FileType type, io.mvnpm.npm.model.Package p) {
        Path localFilePath = fileStore.getLocalAscFullPath(type, p);
        return fetchLocal(localFilePath);
    }
    
    private byte[] fetch(FileType type, io.mvnpm.npm.model.Package p, Path localFilePath){
        boolean local = Files.exists(localFilePath);
        if(local){
            Log.debug("Serving locally [" + localFilePath + "]");
            return fileStore.readFile(localFilePath);
        }else{
            Log.debug("Serving remotely [" + localFilePath + "]");
            return fetchRemote(type, p, localFilePath);
        }
    }
    
    private byte[] fetchSha1(FileType type, io.mvnpm.npm.model.Package p, Path localFilePath){
        boolean local = Files.exists(localFilePath);
        if(local){
            Log.debug("Serving locally [" + localFilePath + "]");
            return fileStore.readFile(localFilePath);
        }else{
            Log.debug("Fetching remotely [" + localFilePath + "]");
            Path localFullFilePath = fileStore.getLocalFullPath(type, p);
            return fetchRemote(type, p, localFullFilePath);
        }
    }
    
    private byte[] fetchLocal(Path localFilePath){
        if(Files.exists(localFilePath)){
            Log.debug("Serving locally [" + localFilePath + "]");
            return fileStore.readFile(localFilePath);
        }else{
            throw new UncheckedIOException(new FileNotFoundException(localFilePath.toString()));
        }
    }
    
    private byte[] fetchRemote(FileType type, io.mvnpm.npm.model.Package p, Path localFilePath){
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
            default -> throw new RuntimeException("Unknown type " + type);
        }
    }
    
}
