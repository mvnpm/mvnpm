package io.mvnpm.mavencentral.sync;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileType;
import io.mvnpm.maven.MavenRespositoryService;
import io.quarkus.logging.Log;

/**
 * This creates a bundles (pom, jar, -sources, -javadoc) in the format Nexus expects
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class BundleCreator {

    @Inject
    FileStore fileStore;

    @Inject
    MavenRespositoryService mavenRespositoryService;

    public Path bundle(String groupId, String artifactId, String version) {
        Log.debug("====== mvnpm: Nexus Bundler ======");
        // First get the jar, as the jar will create the pom, and
        // other files are being created once the pom and jar is downloaded
        byte[] jarFile = mavenRespositoryService.getFile(groupId, artifactId, version, FileType.jar);
        Log.debug("\tbundle: Got initial Jar file");
        return buildBundle(jarFile, groupId, artifactId, version);
    }

    private Path buildBundle(byte[] jarFile, String groupId, String artifactId, String version) {
        Map<Path, byte[]> files = getFiles(jarFile, groupId, artifactId, version);

        Path parent = fileStore.getLocalDirectory(groupId, artifactId, version);
        String bundlelocation = artifactId + Constants.HYPHEN + version + "-bundle.jar";
        Path bundlePath = parent.resolve(bundlelocation);

        Log.debug("\tBuilding bundle " + bundlePath + "...");

        if (!fileExist(bundlePath)) {
            File bundleFile = bundlePath.toFile();
            try (FileOutputStream fos = new FileOutputStream(bundleFile);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    ZipOutputStream zos = new ZipOutputStream(bos)) {

                for (Map.Entry<Path, byte[]> file : files.entrySet()) {
                    Path path = file.getKey();
                    ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());
                    zos.putNextEntry(zipEntry);
                    zos.write(file.getValue());
                    zos.closeEntry();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return bundlePath;
    }

    private boolean fileExist(Path bundlePath) {
        try {
            return Files.exists(bundlePath) && Files.size(bundlePath) > 0;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private Map<Path, byte[]> getFiles(byte[] jarFile, String groupId, String artifactId, String version) {
        // Files that needs to be in the bundle
        try {
            Path parent = fileStore.getLocalDirectory(groupId, artifactId, version);
            String base = artifactId + Constants.HYPHEN + version;
            Path jarFileName = parent.resolve(base + Constants.DOT_JAR);
            List<Path> fileNames = getFileNamesInBundle(parent, base);
            Map<Path, byte[]> files = new HashMap<>();
            for (Path fileName : fileNames) {
                if (fileName.equals(jarFileName)) {
                    files.put(fileName, jarFile);
                    Log.debug("\tbundle: " + fileName + " [already]");
                } else {
                    byte[] content = waitForContent(fileName, 0);
                    files.put(fileName, content);
                    Log.debug("\tbundle: " + fileName + " [ok]");
                }
            }
            return files;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private byte[] waitForContent(Path fileName, int tryCount) throws IOException {
        if (Files.exists(fileName)) {
            return Files.readAllBytes(fileName);
        } else {
            if (tryCount > 9)
                throw new FileNotFoundException(fileName.toString());
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ex) {
                Log.error(ex);
            }
            tryCount = tryCount + 1;
            return waitForContent(fileName, tryCount);
        }
    }

    private List<Path> getFileNamesInBundle(Path parent, String base) {
        List<Path> fileNames = List.of(
                parent.resolve(base + Constants.DOT_POM),
                parent.resolve(base + Constants.DOT_POM + Constants.DOT_ASC),
                parent.resolve(base + Constants.DOT_JAR),
                parent.resolve(base + Constants.DOT_JAR + Constants.DOT_ASC),
                parent.resolve(base + Constants.DASH_SOURCES_DOT_JAR),
                parent.resolve(base + Constants.DASH_SOURCES_DOT_JAR + Constants.DOT_ASC),
                parent.resolve(base + Constants.DASH_JAVADOC_DOT_JAR),
                parent.resolve(base + Constants.DASH_JAVADOC_DOT_JAR + Constants.DOT_ASC));

        return fileNames;
    }
}
