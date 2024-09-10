package io.mvnpm;

import static io.mvnpm.Constants.HEADER_CACHE_CONTROL;
import static io.mvnpm.Constants.HEADER_CACHE_CONTROL_IMMUTABLE;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jboss.resteasy.reactive.ResponseHeader;

import io.mvnpm.file.FileType;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.NameVersionType;
import io.mvnpm.maven.UrlPathParser;

/**
 * Get the file listing for a jar file
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/api")
public class JarContentsApi {

    @Inject
    MavenRepositoryService mavenRepositoryService;

    @GET
    @ResponseHeader(name = HEADER_CACHE_CONTROL, value = HEADER_CACHE_CONTROL_IMMUTABLE)
    @Path("/org/mvnpm/{gavt : (.+)?}.jar")
    public JarLibrary getJar(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return loadJarLibrary(nameVersionType, FileType.jar);
    }

    @GET
    @ResponseHeader(name = HEADER_CACHE_CONTROL, value = HEADER_CACHE_CONTROL_IMMUTABLE)
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar")
    public JarLibrary getSourcesJar(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return loadJarLibrary(nameVersionType, FileType.source);
    }

    @GET
    @ResponseHeader(name = HEADER_CACHE_CONTROL, value = HEADER_CACHE_CONTROL_IMMUTABLE)
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar")
    public JarLibrary getJavadocJar(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return loadJarLibrary(nameVersionType, FileType.javadoc);
    }

    @GET
    @ResponseHeader(name = HEADER_CACHE_CONTROL, value = HEADER_CACHE_CONTROL_IMMUTABLE)
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz")
    public JarLibrary getTgz(@PathParam("gavt") String gavt) {
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return loadTarGzLibrary(nameVersionType);
    }

    private JarLibrary loadJarLibrary(NameVersionType nameVersionType, FileType filetype) {
        java.nio.file.Path path = mavenRepositoryService.getPath(nameVersionType.name(), nameVersionType.version(),
                filetype);

        JarLibrary library = new JarLibrary(nameVersionType.name().displayName);
        library.setVersion(nameVersionType.version());
        library.setType(filetype.getPostString());

        try (JarFile jarFile = new JarFile(path.toString())) {
            Map<String, JarAsset> assetMap = new HashMap<>();

            // Create a root asset
            JarAsset rootAsset = new JarAsset();
            rootAsset.setName("/");
            rootAsset.setFileAsset(false);
            rootAsset.setChildren(new ArrayList<>());
            assetMap.put("/", rootAsset);

            // Iterate through the entries of the jar file
            jarFile.stream().forEach(entry -> {
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
            });

            library.setRootAsset(rootAsset);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return library;
    }

    private JarLibrary loadTarGzLibrary(NameVersionType nameVersionType) {
        java.nio.file.Path path = mavenRepositoryService.getPath(nameVersionType.name(), nameVersionType.version(),
                FileType.tgz);

        JarLibrary library = new JarLibrary(nameVersionType.name().displayName);
        library.setVersion(nameVersionType.version());
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
                if (!tarIn.canReadEntryData(entry)) {
                    continue;
                }
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
            e.printStackTrace();
        }

        return library;
    }

}
