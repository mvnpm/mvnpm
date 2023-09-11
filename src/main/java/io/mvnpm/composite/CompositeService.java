package io.mvnpm.composite;

import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileType;
import io.mvnpm.npm.model.Name;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class CompositeService {

    @Inject
    CompositeCreator compositeCreator;
    
    @Inject
    FileStore fileStore;
    
    public byte[] getFile(Name fullName, String version, FileType type) {
        compositeCreator.buildComposite(fullName.mvnArtifactId(), version);
        Path localFilePath = fileStore.getLocalFullPath(type, fullName, version, Optional.empty());
        return fileStore.readFile(localFilePath);
    }

    public byte[] getFileSha1(Name fullName, String version, FileType type) {
        return getSignedFile(fullName, version, type, "sha1");
    }

    public byte[] getFileMd5(Name fullName, String version, FileType type) {
        return getSignedFile(fullName, version, type, "md5");
    }

    public byte[] getFileAsc(Name fullName, String version, FileType type) {
        return getSignedFile(fullName, version, type, "asc");
    }
    
    private byte[] getSignedFile(Name fullName, String version, FileType type, String signedType){
        Path localFilePath = fileStore.getLocalFullPath(type, fullName, version, Optional.of("." + signedType));
        return fileStore.readFile(localFilePath);
    }
    
    public Map<String, Date> getVersions(Name name){
        try {
            Path groupRoute = fileStore.getGroupRoot(name.mvnGroupIdPath()).resolve(name.mvnArtifactId());
            List<Path> versionDirs = Files.walk(groupRoute)
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());
            Map<String, Date> nameTimeMap = new HashMap<>();
            for(Path versionDir:versionDirs){
                String v = versionDir.getFileName().toString();
                if(!v.equalsIgnoreCase(name.mvnArtifactId())){
                    BasicFileAttributes attr = Files.readAttributes(versionDir, BasicFileAttributes.class);
                    Date lastModified = Date.from(attr.lastModifiedTime().toInstant());
                    nameTimeMap.put(versionDir.getFileName().toString(), lastModified);
                }
            }
            return nameTimeMap;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        
    }
    
    public byte[] getImportMap(Name name, String version){
        Path importmapPath = compositeCreator.getImportMapPath(name, version);
        return fileStore.readFile(importmapPath);
    }
}
