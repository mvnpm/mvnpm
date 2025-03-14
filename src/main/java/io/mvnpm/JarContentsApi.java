package io.mvnpm;

import static io.mvnpm.Constants.HEADER_CACHE_CONTROL;
import static io.mvnpm.Constants.HEADER_CACHE_CONTROL_IMMUTABLE;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jboss.resteasy.reactive.ResponseHeader;

import io.mvnpm.creator.FileType;
import io.mvnpm.maven.MavenCentralService;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.NameVersion;
import io.mvnpm.maven.UrlPathParser;
import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpResponse;

/**
 * Get the file listing for a jar file
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/api")
public class JarContentsApi {

    private static final Logger LOGGER = Logger.getLogger(JarContentsApi.class.getName());

    @Inject
    MavenRepositoryService mavenRepositoryService;

    @Inject
    MavenCentralService mavenCentralService;

    @GET
    @ResponseHeader(name = HEADER_CACHE_CONTROL, value = HEADER_CACHE_CONTROL_IMMUTABLE)
    @Path("/org/mvnpm/{gavt : (.+)?}.jar")
    public JarLibrary getJar(@PathParam("gavt") String gavt) {
        NameVersion nameVersion = UrlPathParser.parseMavenFile(gavt);
        return loadJarLibrary(nameVersion, FileType.jar);
    }

    @GET
    @ResponseHeader(name = HEADER_CACHE_CONTROL, value = HEADER_CACHE_CONTROL_IMMUTABLE)
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar")
    public JarLibrary getSourcesJar(@PathParam("gavt") String gavt) {
        NameVersion nameVersion = UrlPathParser.parseMavenFile(gavt);
        return loadJarLibrary(nameVersion, FileType.source);
    }

    @GET
    @ResponseHeader(name = HEADER_CACHE_CONTROL, value = HEADER_CACHE_CONTROL_IMMUTABLE)
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar")
    public JarLibrary getJavadocJar(@PathParam("gavt") String gavt) {
        NameVersion nameVersion = UrlPathParser.parseMavenFile(gavt);
        return loadJarLibrary(nameVersion, FileType.javadoc);
    }

    @GET
    @ResponseHeader(name = HEADER_CACHE_CONTROL, value = HEADER_CACHE_CONTROL_IMMUTABLE)
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz")
    public JarLibrary getTgz(@PathParam("gavt") String gavt) {
        NameVersion nameVersion = UrlPathParser.parseMavenFile(gavt);
        return loadTarGzLibrary(nameVersion);
    }

    private JarLibrary loadJarLibrary(NameVersion nameVersion, FileType filetype) {
        try {
            java.nio.file.Path path = mavenRepositoryService.getPath(nameVersion.name(), nameVersion.version(),
                    filetype);
            try (InputStream inputStream = Files.newInputStream(path)) {
                return loadJarLibrary(nameVersion, filetype, inputStream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (PackageAlreadySyncedException e) {
            final Uni<HttpResponse<Buffer>> fromMavenCentral = mavenCentralService.getFromMavenCentral(
                    nameVersion.name(), nameVersion.version(), e.fileName());
            final HttpResponse<Buffer> response = fromMavenCentral.await().indefinitely();
            try (final InputStream is = new ByteArrayInputStream(response.body().getBytes())) {
                return loadJarLibrary(nameVersion, filetype, is);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }
    }

    private JarLibrary loadJarLibrary(NameVersion nameVersion, FileType type, InputStream stream) {

        JarLibrary library = new JarLibrary(nameVersion.name().displayName);
        library.setVersion(nameVersion.version());
        library.setType(type.getPostString());

        Map<String, JarAsset> assetMap = new HashMap<>();
        JarAsset rootAsset = new JarAsset();
        rootAsset.setName("/");
        rootAsset.setFileAsset(false);
        rootAsset.setChildren(new ArrayList<>());
        assetMap.put("/", rootAsset);

        // Open the JAR file using JarInputStream
        try (JarInputStream jarInputStream = new JarInputStream(stream)) {
            JarEntry entry;

            while ((entry = jarInputStream.getNextJarEntry()) != null) {
                String name = entry.getName();
                boolean isFile = !entry.isDirectory();
                String[] parts = name.split("/");
                StringBuilder pathBuilder = new StringBuilder("/");

                JarAsset currentParent = rootAsset;

                // Process each part of the entry's name
                for (int i = 0; i < parts.length - (isFile ? 1 : 0); i++) {
                    pathBuilder.append(parts[i]);
                    String currentPath = pathBuilder.toString() + "/";

                    if (!assetMap.containsKey(currentPath)) {
                        JarAsset newAsset = new JarAsset();
                        newAsset.setName(parts[i]);
                        newAsset.setFileAsset(false);
                        newAsset.setUrlPart(currentPath);
                        newAsset.setChildren(new ArrayList<>());

                        currentParent.getChildren().add(newAsset);
                        assetMap.put(currentPath, newAsset);
                    }

                    currentParent = assetMap.get(currentPath);
                    pathBuilder.append("/");
                }

                // If it's a file, add it to the current parent directory
                if (isFile) {
                    JarAsset fileAsset = new JarAsset();
                    fileAsset.setName(parts[parts.length - 1]);
                    fileAsset.setFileAsset(true);
                    fileAsset.setUrlPart(name);
                    currentParent.getChildren().add(fileAsset);
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load JAR file: " + nameVersion, ex);
        }

        library.setRootAsset(rootAsset);

        return library;
    }

    private JarLibrary loadTarGzLibrary(NameVersion nameVersion) {
        java.nio.file.Path path = mavenRepositoryService.getPath(nameVersion.name(), nameVersion.version(),
                FileType.tgz);

        JarLibrary library = new JarLibrary(nameVersion.name().displayName);
        library.setVersion(nameVersion.version());
        library.setType(FileType.tgz.getPostString());

        Map<String, JarAsset> assetMap = new HashMap<>();
        JarAsset rootAsset = new JarAsset();
        rootAsset.setName("/");
        rootAsset.setFileAsset(false);
        rootAsset.setChildren(new ArrayList<>());
        assetMap.put("/", rootAsset);

        try (FileInputStream fin = new FileInputStream(path.toFile());
                GzipCompressorInputStream gzIn = new GzipCompressorInputStream(fin);
                TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn)) {

            ArchiveEntry entry;

            while ((entry = tarIn.getNextEntry()) != null) {
                if (!tarIn.canReadEntryData(entry))
                    continue;

                String name = entry.getName();
                boolean isFile = !((TarArchiveEntry) entry).isDirectory();
                String[] parts = name.split("/");
                StringBuilder pathBuilder = new StringBuilder("/");

                JarAsset currentParent = rootAsset;

                // Process each part of the entry's name
                for (int i = 0; i < parts.length - (isFile ? 1 : 0); i++) {
                    pathBuilder.append(parts[i]);
                    String currentPath = pathBuilder.toString() + "/";

                    if (!assetMap.containsKey(currentPath)) {
                        JarAsset newAsset = new JarAsset();
                        newAsset.setName(parts[i]);
                        newAsset.setFileAsset(false);
                        newAsset.setUrlPart(currentPath);
                        newAsset.setChildren(new ArrayList<>());

                        currentParent.getChildren().add(newAsset);
                        assetMap.put(currentPath, newAsset);
                    }

                    currentParent = assetMap.get(currentPath);
                    pathBuilder.append("/");
                }

                // If it's a file, add it to the current parent directory
                if (isFile) {
                    JarAsset fileAsset = new JarAsset();
                    fileAsset.setName(parts[parts.length - 1]);
                    fileAsset.setFileAsset(true);
                    fileAsset.setUrlPart(name);
                    currentParent.getChildren().add(fileAsset);
                }
            }
            library.setRootAsset(rootAsset);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load TAR.GZ file: " + path, e);
        }

        return library;
    }

}
