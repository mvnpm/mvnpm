package org.mvnpm.file;

import io.smallrye.mutiny.Uni;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.file.AsyncFile;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mvnpm.Constants;

/**
 * Store for local files.
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class FileStore {

    @Inject
    Vertx vertx;
    
    @ConfigProperty(name = "mvnpm.local-m2-directory", defaultValue = "m2")
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
                    String sha1 = sha1(content);
                    String localSha1FileName = localFileName + Constants.DOT_SHA1;
                    Uni<Void> emptySha1File = vertx.fileSystem().createFile(localSha1FileName);
                    return emptySha1File.onItem().transformToUni((createdSha) -> {
                        Uni<Void> writtenSha = vertx.fileSystem().writeFile(localSha1FileName, Buffer.buffer(sha1));
                        return writtenSha.onItem().transformToUni((doneSha) -> {
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
    
    public String getLocalDirectory(org.mvnpm.npm.model.Package p){
        return localUserDirectory.orElse(Constants.CACHE_DIR) + File.separator + 
                Constants.DOT + localM2Directory + File.separator +
                Constants.REPOSITORY + File.separator +
                p.name().mvnPath() + File.separator +
                p.name().mvnArtifactId() + File.separator + 
                p.version();
    }
    
    public String getLocalShaFullPath(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalFullPath(type, p) + Constants.DOT_SHA1;
    }
    
    public String getLocalFullPath(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalDirectory(p) + File.separator + 
                getLocalFileName(type, p);
    }
    
    public String getLocalFileName(FileType type, org.mvnpm.npm.model.Package p){
        return p.name().mvnArtifactId() + Constants.DASH + p.version() + Constants.DOT + type.name();
    }
    
    public String getLocalSha1FileName(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalFileName(type, p) + Constants.DOT_SHA1;
    }
    
    private String sha1(byte[] value) {
        try {
            MessageDigest md = MessageDigest.getInstance(Constants.SHA1);
            byte[] digest = md.digest(value);
            StringBuilder sb = new StringBuilder(40);
            for (int i = 0; i < digest.length; ++i) {
                sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private static final OpenOptions READ_ONLY_OPTIONS = (new OpenOptions()).setCreate(false).setWrite(false);
}
