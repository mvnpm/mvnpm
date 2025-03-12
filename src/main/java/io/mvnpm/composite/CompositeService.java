package io.mvnpm.composite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileType;
import io.mvnpm.file.FileUtil;
import io.mvnpm.maven.MavenCentralService;
import io.mvnpm.npm.model.Name;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

@ApplicationScoped
public class CompositeService {

    @Inject
    CompositeCreator compositeCreator;

    @Inject
    FileStore fileStore;

    @Inject
    MavenCentralService mavenCentralService;

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
        final Path importMapPath = compositeCreator.getImportMapPath(name, version);
        if (!Files.exists(importMapPath)) {
            final HttpResponse<Buffer> response = mavenCentralService.getFromMavenCentral(name, version,
                    fileStore.getLocalFileName(FileType.jar, name, version, Optional.empty()))
                    .await().atMost(Duration.ofSeconds(10));
            createImportMapFromJar(response.bodyAsBuffer().getBytes(), importMapPath);
        }
        return importMapPath;
    }

    private void createImportMapFromJar(byte[] jar, Path importMapPath) {
        try (InputStream inputStream = new ByteArrayInputStream(jar);
                JarInputStream inputJar = new JarInputStream(inputStream)) {
            // Add all entries from the input JAR to the merged JAR
            JarEntry entry;
            while ((entry = inputJar.getNextJarEntry()) != null) {
                if ("META-INF/importmap.json".equals(entry.getName())) {
                    final String importMap = compositeCreator.getEntryContent(inputJar);
                    Files.createDirectories(importMapPath.getParent());
                    FileUtil.writeAtomic(importMapPath, importMap);
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
