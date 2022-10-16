package org.mvnpm.file;

import org.mvnpm.file.type.JarClient;
import org.mvnpm.file.type.TgzClient;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.file.AsyncFile;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.mvnpm.file.type.PomClient;

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
    
    public Uni<AsyncFile> streamFile(FileType type, org.mvnpm.npm.model.Package p) {
        String localFileName = fileStore.getLocalFullPath(type, p);
        
        Uni<Boolean> checkIfLocal = vertx.fileSystem().exists(localFileName);
        return checkIfLocal.onItem()
                .transformToUni((local) -> { 
                    return fetch(type, p, localFileName, local);
                });
    }
    
    public Uni<AsyncFile> streamSha1(FileType type, org.mvnpm.npm.model.Package p) {
        String localFileName = fileStore.getLocalShaFullPath(type, p);
        
        Uni<Boolean> checkIfLocal = vertx.fileSystem().exists(localFileName);
        return checkIfLocal.onItem()
                .transformToUni((local) -> { 
                    return fetchSha1(type, p, localFileName, local);
                });
    }
    
    private Uni<AsyncFile> fetch(FileType type, org.mvnpm.npm.model.Package p, String localFileName, Boolean local){
        if(local){
            Log.debug("Serving from cache [" + localFileName + "]");
            return fileStore.readFile(localFileName);
        }else{
            Log.debug("Serving from origin [" + localFileName + "]");
            return fetchOriginal(type, p, localFileName);
        }
    }
    
    private Uni<AsyncFile> fetchSha1(FileType type, org.mvnpm.npm.model.Package p, String localFileName, Boolean local){
        if(local){
            Log.info("Serving from cache [" + localFileName + "]");
            return fileStore.readFile(localFileName);
        }else{
            Log.info("Fetching from origin [" + localFileName + "]");
            String localFullFileName = fileStore.getLocalFullPath(type, p);
            Uni<AsyncFile> fetchOriginal = fetchOriginal(type, p, localFullFileName);
            return fetchOriginal.onItem().transformToUni((downloaded) -> {
                return fileStore.readFile(localFileName);
            });
        }
    }
    
    private Uni<AsyncFile> fetchOriginal(FileType type, org.mvnpm.npm.model.Package p, String localFileName){
        switch (type) {
            case tgz -> {
                return tgzClient.fetchRemote(p, localFileName);
            }
            case jar -> {
                return jarClient.createJar(p, localFileName);
            }
            case pom -> {
                return pomClient.createPom(p, localFileName);
            }
            default -> throw new RuntimeException("Unknown type " + type);
        }
    }
    
}
