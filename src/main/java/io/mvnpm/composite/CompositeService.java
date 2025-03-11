package io.mvnpm.composite;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileType;
import io.mvnpm.npm.model.Name;

@ApplicationScoped
public class CompositeService {

    @Inject
    CompositeCreator compositeCreator;

    @Inject
    FileStore fileStore;

    public Path getPath(Name fullName, String version, FileType type) {
        return compositeCreator.getOrBuildComposite(fullName.mvnArtifactId, version);
    }

    public Path getSha1Path(Name fullName, String version, FileType type) {
        return fileStore.getLocalFullPath(type, fullName, version, Optional.of(Constants.DOT_SHA1));
    }

    public Path getMd5Path(Name fullName, String version, FileType type) {
        return fileStore.getLocalFullPath(type, fullName, version, Optional.of(Constants.DOT_MD5));
    }

    public Path getAscPath(Name fullName, String version, FileType type) {
        return fileStore.getLocalFullPath(type, fullName, version, Optional.of(Constants.DOT_ASC));
    }

    public Map<String, Date> getVersions(Name name) {
        try {
            Path groupRoute = fileStore.getGroupRoot(name.mvnGroupIdPath()).resolve(name.mvnArtifactId);
            List<Path> versionDirs = Files.walk(groupRoute)
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());
            Map<String, Date> nameTimeMap = new HashMap<>();
            for (Path versionDir : versionDirs) {
                String v = versionDir.getFileName().toString();
                if (!v.equalsIgnoreCase(name.mvnArtifactId)) {
                    BasicFileAttributes attr = Files.readAttributes(versionDir, BasicFileAttributes.class);
                    Date lastModified = Date.from(attr.lastModifiedTime().toInstant());
                    nameTimeMap.put(versionDir.getFileName().toString(), lastModified);
                }
            }
            return sort(nameTimeMap);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

    }

    public Map<String, Date> sort(Map<String, Date> map) {
        List<Map.Entry<String, Date>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Map<String, Date> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Date> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    public Path getImportMap(Name name, String version) {
        return compositeCreator.getImportMapPath(name, version);
    }
}
