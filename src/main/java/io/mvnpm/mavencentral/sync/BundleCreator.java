package io.mvnpm.mavencentral.sync;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.Constants;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.creator.utils.FileUtil;
import io.mvnpm.mavencentral.exceptions.MissingFilesForBundleException;
import io.quarkus.logging.Log;

/**
 * This creates a bundles (pom, jar, -sources, -javadoc) in the format Nexus expects
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class BundleCreator {

    @Inject
    PackageFileLocator packageFileLocator;

    public Path bundle(String groupId, String artifactId, String version) throws MissingFilesForBundleException {
        Log.debug("====== mvnpm: Nexus Bundler ======");
        return buildBundle(groupId, artifactId, version);
    }

    private Path buildBundle(String groupId, String artifactId, String version) throws MissingFilesForBundleException {
        List<Path> files = getFiles(groupId, artifactId, version);

        Path parent = packageFileLocator.getLocalDirectory(groupId, artifactId, version);
        String bundlelocation = artifactId + Constants.HYPHEN + version + "-bundle.jar";
        Path bundlePath = parent.resolve(bundlelocation);

        Log.debug("\tBuilding bundle " + bundlePath + "...");

        if (!Files.exists(bundlePath)) {
            final Path temp = FileUtil.getTempFilePathFor(bundlePath);
            File bundleFile = temp.toFile();
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
            try {
                FileUtil.forceMoveAtomic(temp, bundlePath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return bundlePath;
    }

    private List<Path> getFiles(String groupId, String artifactId, String version) throws MissingFilesForBundleException {
        // Files that needs to be in the bundle
        Path parent = packageFileLocator.getLocalDirectory(groupId, artifactId, version);
        String base = artifactId + Constants.HYPHEN + version;
        List<Path> fileNames = getFileNamesInBundle(parent, base);
        List<String> notReady = new ArrayList<>();
        for (Path fileName : fileNames) {
            boolean ready = Files.exists(fileName);
            Log.debug("\tbundle: " + fileName + " [" + ready + "]");
            if (!ready) {
                notReady.add(fileName.toString());
            }
        }

        if (notReady.isEmpty())
            return fileNames;

        throw new MissingFilesForBundleException(
                "Some files (%s) are not available yet to build the bundle for '%s:%s:%s' (waiting for next batch)".formatted(
                        notReady, groupId, artifactId, version));
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
