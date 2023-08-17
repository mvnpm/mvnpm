package org.mvnpm.file;

import io.smallrye.mutiny.Uni;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.file.AsyncFile;
import java.io.File;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
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
    EventBus bus;
    
    @Inject
    Vertx vertx;
    
    @ConfigProperty(name = "mvnpm.local-m2-directory", defaultValue = ".m2")
    String localM2Directory;
    
    @ConfigProperty(name = "mvnpm.local-user-directory")
    Optional<String> localUserDirectory;
    
    public Uni<List<String>> getArtifactRoots() {
        String mvnpmRoot = getMvnpmRoot();
        return findArtifactRoots(mvnpmRoot);
    }
    
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
                            bus.publish("new-file-created", new FileStoreEvent(p, localFileName));
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
    
    public Uni<Boolean> exist(String localFileName){
        return vertx.fileSystem().exists(localFileName);
    }
    
    public String getLocalDirectory(Name name, String version){
        return getGroupRoot(name.mvnPath()) +
                name.mvnArtifactId() + File.separator + 
                version;
    }
    
    public String getMvnpmRoot(){
        return getGroupRoot(Constants.ORG_SLASH_MVNPM);
    }
    
    public String getGroupRoot(String groupId){
        return localUserDirectory.orElse(Constants.CACHE_DIR) + File.separator + 
                localM2Directory + File.separator +
                Constants.REPOSITORY + File.separator +
                groupId + File.separator;
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
    
    private Uni<List<String>> findArtifactRoots(String directoryPath) {
        List<String> roots = new ArrayList<>();
        try {
            Files.walkFileTree(Paths.get(directoryPath), EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    if(isJar(fileName) && !isBundle(fileName) && !isJavadoc(fileName) && !isSources(fileName)){
                        Path versionRoot = file.toAbsolutePath().getParent();
                        String artifactRoot = versionRoot.getParent().toAbsolutePath().toString();
                        if(!roots.contains(artifactRoot)){
                            roots.add(artifactRoot);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return Uni.createFrom().item(roots);
    }
    
    private boolean isBundle(String fileName){
        return fileName.endsWith("-bundle.jar");
    }
    
    private boolean isJavadoc(String fileName){
        return fileName.endsWith("-javadoc.jar");
    }
    
    private boolean isSources(String fileName){
        return fileName.endsWith("-sources.jar");
    }
    
    private boolean isJar(String fileName){
        return fileName.endsWith(".jar");
    }
    
    private static final OpenOptions READ_ONLY_OPTIONS = (new OpenOptions()).setCreate(false).setWrite(false);
}
