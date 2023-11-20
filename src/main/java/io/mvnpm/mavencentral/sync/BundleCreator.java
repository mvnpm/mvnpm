package io.mvnpm.mavencentral.sync;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
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
        mavenRespositoryService.getFile(groupId, artifactId, version, FileType.jar);
        Log.debug("\tbundle: Got initial Jar file");
        return buildBundle(groupId, artifactId, version);
    }

    private Path buildBundle(String groupId, String artifactId, String version) {
        List<Path> files = getFiles(groupId, artifactId, version);

        Path parent = fileStore.getLocalDirectory(groupId, artifactId, version);
        String bundlelocation = artifactId + Constants.HYPHEN + version + "-bundle.jar";
        Path bundlePath = parent.resolve(bundlelocation);

        Log.debug("\tBuilding bundle " + bundlePath + "...");

        if (!fileExist(bundlePath)) {
            File bundleFile = bundlePath.toFile();
            try (FileOutputStream fos = new FileOutputStream(bundleFile);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    ZipOutputStream zos = new ZipOutputStream(bos)) {

                for (Path path : files) {
                    ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());
                    zos.putNextEntry(zipEntry);
                    try (InputStream fileInputStream = Files.newInputStream(path)) {
                        int bytesRead;
                        byte[] buffer = new byte[4096];
                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            zos.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error streaming file content", e);
                    }
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

    private List<Path> getFiles(String groupId, String artifactId, String version) {
        // Files that needs to be in the bundle
        try {
            Path parent = fileStore.getLocalDirectory(groupId, artifactId, version);
            String base = artifactId + Constants.HYPHEN + version;
            List<Path> fileNames = getFileNamesInBundle(parent, base);
            List<String> notReady = new ArrayList<>();
            for (Path fileName : fileNames) {
                boolean ready = fileAndContentReady(fileName, 0);
                Log.debug("\tbundle: " + fileName + " [" + ready + "]");
                if (!ready) {
                    notReady.add(fileName.toString());
                }
            }

            if (notReady.isEmpty())
                return fileNames;

            throw new RuntimeException("Files " + notReady + " not ready to bundle. Gave up after multiple attempts");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Check if a file exist and it's not being written to
     *
     * @param fileName
     * @param tryCount
     * @return
     */
    private boolean fileAndContentReady(Path fileName, int tryCount) throws FileNotFoundException {
        boolean isReady = Files.exists(fileName) && !isFileBeingWritten(fileName);
        if (isReady)
            return true;
        if (tryCount > 9)
            throw new FileNotFoundException(fileName.toString());
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ex) {
            Log.error(ex);
        }
        tryCount = tryCount + 1;
        return fileAndContentReady(fileName, tryCount);
    }

    private boolean isFileBeingWritten(Path filePath) {
        try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.WRITE)) {
            // Try to acquire a lock on the entire file.
            FileLock fileLock = fileChannel.tryLock();
            // If the lock is null, then the file is being written by another process.
            return fileLock == null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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
