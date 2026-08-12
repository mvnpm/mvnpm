package io.mvnpm.mavencentral;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.Constants;
import io.mvnpm.creator.utils.FileUtil;
import io.mvnpm.maven.api.BundleCreator;
import io.mvnpm.maven.exceptions.MissingFilesForBundleException;
import io.quarkus.arc.DefaultBean;
import io.quarkus.logging.Log;

/**
 * This creates a bundles (pom, jar, -sources, -javadoc) in the format Nexus expects
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
@DefaultBean
public class CentralBundleCreator extends BundleCreator {

    protected List<BundleRecord> buildBundle(String groupId, String artifactId, String version)
            throws MissingFilesForBundleException {
        List<BundleRecord> records = getRecordsOf(groupId, artifactId, version);

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

                String basePath = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/";

                for (BundleRecord record : records) {
                    final Path path = record.path();
                    String zipEntryName = basePath + path.getFileName();
                    Log.debug("\tAdding to bundle: " + zipEntryName);

                    ZipEntry zipEntry = new ZipEntry(zipEntryName);
                    zos.putNextEntry(zipEntry);
                    try (InputStream fileInputStream = Files.newInputStream(path)) {
                        int bytesRead;
                        byte[] buffer = new byte[4096];
                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            zos.write(buffer, 0, bytesRead);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Error streaming file content: " + path, e);
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
        return List.of(new BundleRecord("", bundlePath));
    }

    protected List<BundleRecord> getRecordsInBundle(Path parent, String base) {
        List<Path> fileNames = List.of(parent.resolve(base + Constants.DOT_POM),
                parent.resolve(base + Constants.DOT_POM + Constants.DOT_ASC),
                parent.resolve(base + Constants.DOT_POM + Constants.DOT_MD5),
                parent.resolve(base + Constants.DOT_POM + Constants.DOT_SHA1), parent.resolve(base + Constants.DOT_JAR),
                parent.resolve(base + Constants.DOT_JAR + Constants.DOT_ASC),
                parent.resolve(base + Constants.DOT_JAR + Constants.DOT_MD5),
                parent.resolve(base + Constants.DOT_JAR + Constants.DOT_SHA1),
                parent.resolve(base + Constants.DASH_SOURCES_DOT_JAR),
                parent.resolve(base + Constants.DASH_SOURCES_DOT_JAR + Constants.DOT_ASC),
                parent.resolve(base + Constants.DASH_SOURCES_DOT_JAR + Constants.DOT_MD5),
                parent.resolve(base + Constants.DASH_SOURCES_DOT_JAR + Constants.DOT_SHA1),
                parent.resolve(base + Constants.DASH_JAVADOC_DOT_JAR),
                parent.resolve(base + Constants.DASH_JAVADOC_DOT_JAR + Constants.DOT_ASC),
                parent.resolve(base + Constants.DASH_JAVADOC_DOT_JAR + Constants.DOT_MD5),
                parent.resolve(base + Constants.DASH_JAVADOC_DOT_JAR + Constants.DOT_SHA1));
        return fileNames.stream().map(fileName -> new BundleRecord(null, fileName)).toList();
    }
}
