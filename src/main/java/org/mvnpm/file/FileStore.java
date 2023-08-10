package org.mvnpm.file;

import io.smallrye.mutiny.Uni;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.file.AsyncFile;
import java.io.File;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mvnpm.Constants;
import org.mvnpm.npm.model.Name;

/**
 * Store for local files.
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class FileStore {

    @Inject
    Event<FileStoreEvent> fileStored;
    
    @Inject
    Vertx vertx;
    
    @ConfigProperty(name = "mvnpm.local-m2-directory", defaultValue = ".m2")
    String localM2Directory;
    
    @ConfigProperty(name = "mvnpm.local-user-directory")
    Optional<String> localUserDirectory;
    
    public Uni<AsyncFile> createFile(org.mvnpm.npm.model.Package p, String localFileName, byte[] content){
        Uni<Void> createdDir = vertx.fileSystem().mkdirs(getLocalDirectory(p));
        return createdDir.onItem().transformToUni((createdDirs) -> {
            Uni<Void> emptyFile = vertx.fileSystem().createFile(localFileName);
            return emptyFile.onItem().transformToUni((createdFile) -> {
                Uni<Void> download = vertx.fileSystem().writeFile(localFileName, Buffer.buffer(content));
                return download.onItem().transformToUni((doneDownload) -> {
                    String sha1 = FileUtil.getSha1(content);
                    String localSha1FileName = localFileName + Constants.DOT_SHA1;
                    Uni<Void> emptySha1File = vertx.fileSystem().createFile(localSha1FileName);
                    return emptySha1File.onItem().transformToUni((createdSha) -> {
                        Uni<Void> writtenSha = vertx.fileSystem().writeFile(localSha1FileName, Buffer.buffer(sha1));
                        return writtenSha.onItem().transformToUni((doneSha) -> {
                            fileStored.fireAsync(new FileStoreEvent(p, localFileName));
                            return readFile(localFileName);
                        });
                    });
                });
            });
        });
    }
    
    public Uni<AsyncFile> readFile(String localFileName){
        return vertx.fileSystem().open(localFileName, READ_ONLY_OPTIONS);
    }
    
    
    public String getLocalDirectory(Name name, String version){
        return localUserDirectory.orElse(Constants.CACHE_DIR) + File.separator + 
                localM2Directory + File.separator +
                Constants.REPOSITORY + File.separator +
                name.mvnPath() + File.separator +
                name.mvnArtifactId() + File.separator + 
                version;
    }
    
    public String getLocalDirectory(org.mvnpm.npm.model.Package p){
        return getLocalDirectory(p.name(), p.version());
    }
    
    public String getLocalSha1FullPath(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalFullPath(type, p) + Constants.DOT_SHA1;
    }
    
    public String getLocalMd5FullPath(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalFullPath(type, p) + Constants.DOT_MD5;
    }
    
    public String getLocalAscFullPath(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalFullPath(type, p) + Constants.DOT_ASC;
    }
    
    public String getLocalFullPath(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalDirectory(p) + File.separator + 
                getLocalFileName(type, p);
    }
    
    public String getLocalFileName(FileType type, org.mvnpm.npm.model.Package p){
        return p.name().mvnArtifactId() + Constants.HYPHEN + p.version() + type.getPostString();
    }
    
    public String getLocalSha1FileName(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalFileName(type, p) + Constants.DOT_SHA1;
    }
    
    private static final OpenOptions READ_ONLY_OPTIONS = (new OpenOptions()).setCreate(false).setWrite(false);
}
