package org.mvnpm.file;

import io.smallrye.mutiny.Uni;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.file.AsyncFile;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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
                    String localSha1FileName = localFileName + DOT + SHA1;
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
        return localUserDirectory.orElse(CACHE_DIR) + File.separator + 
                DOT + localM2Directory + File.separator +
                REPOSITORY + File.separator + 
                ORG_MVNPM + File.separator +
                p.name() + File.separator + 
                p.version();
    }
    
    public String encode(String s){
        return s.replaceAll(SLASH, UNDERSCORE);
//        try {
//            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
//        } catch (UnsupportedEncodingException ex) {
//            ex.printStackTrace();
//            return s;
//        }
    }
    
    public String getLocalShaFullPath(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalFullPath(type, p) + DOT + SHA1;
    }
    
    public String getLocalFullPath(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalDirectory(p) + File.separator + 
                getLocalFileName(type, p);
    }
    
    public String getLocalFileName(FileType type, org.mvnpm.npm.model.Package p){
        return encode(p.name()) + DASH + p.version() + DOT + type.name();
    }
    
    public String getLocalSha1FileName(FileType type, org.mvnpm.npm.model.Package p){
        return getLocalFileName(type, p) + DOT + SHA1;
    }
    
    private String sha1(byte[] value) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA1);
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
    
    public static final OpenOptions READ_ONLY_OPTIONS = (new OpenOptions()).setCreate(false).setWrite(false);
    public static final String DOT = ".";
    public static final String SHA1 = "sha1";
    private static final String SLASH = "/";
    private static final String UNDERSCORE = "_";
    private static final String DASH = "-";
    private static final String REPOSITORY = "repository";
    private static final String ORG_MVNPM = "org" + File.separator + "mvnpm";
    private static final String USER_HOME = "user.home";
    private static final String CACHE_DIR = System.getProperty(USER_HOME);
}
