package io.mvnpm.file.type;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import io.mvnpm.Constants;
import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileType;
import io.mvnpm.file.FileUtil;
import io.mvnpm.file.ImportMapUtil;
import io.mvnpm.importmap.Location;
import io.mvnpm.maven.MavenRepositoryService;

/**
 * Create the jar from the npm content
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class JarClient {
    @Inject
    FileStore fileStore;

    @Inject
    MavenRepositoryService mavenRepositoryService;

    public Path createEmptyJar(Path forJar, String replaceJarWith) {
        Path emptyFile = Paths.get(forJar.toString().replace(Constants.DOT_JAR, replaceJarWith));
        if (!Files.exists(emptyFile)) {
            synchronized (emptyFile) {
                try (OutputStream fileOutput = Files.newOutputStream(emptyFile);
                        JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(fileOutput)) {
                    emptyJar(jarOutput);
                    jarOutput.finish();
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        }
        return emptyFile;
    }

    private void emptyJar(JarArchiveOutputStream jarOutput) throws IOException {
        JarArchiveEntry entry = new JarArchiveEntry("README.md");
        byte[] filecontents = CONTENTS.getBytes();
        entry.setSize(filecontents.length);
        jarOutput.putArchiveEntry(entry);
        jarOutput.write(filecontents);
        jarOutput.closeArchiveEntry();
    }

    public void createAndSaveJar(io.mvnpm.npm.model.Package p, Path localFilePath) {
        Path pomPath = mavenRepositoryService.getPath(p.name(), p.version(), FileType.pom);
        Path tgzPath = mavenRepositoryService.getPath(p.name(), p.version(), FileType.tgz);
        jarInput(p, localFilePath, pomPath, tgzPath);
    }

    private void jarInput(io.mvnpm.npm.model.Package p, Path localFilePath, Path pomPath, Path tgzPath) {
        FileUtil.createDirectories(localFilePath);
        try (OutputStream fileOutput = Files.newOutputStream(localFilePath);
                JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(fileOutput)) {

            // Pom details
            String pomXmlDir = POM_ROOT + p.name().mvnGroupId + Constants.SLASH + p.name().mvnArtifactId + Constants.SLASH;

            // Pom xml entry
            writeJarEntry(jarOutput, pomXmlDir + POM_DOT_XML, pomPath);

            // Pom properties entry
            writeJarEntry(jarOutput, pomXmlDir + POM_DOT_PROPERTIES, createPomProperties(p));

            // Import map
            writeJarEntry(jarOutput, Location.IMPORTMAP_PATH, ImportMapUtil.createImportMap(p));

            // Tar contents
            tgzToJar(p, tgzPath, jarOutput);

            jarOutput.finish();

            fileStore.touch(p.name(), p.version(), localFilePath);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void tgzToJar(io.mvnpm.npm.model.Package p, Path tgzPath, JarArchiveOutputStream jarOutput) throws IOException {

        try (InputStream tgzInputStream = Files.newInputStream(tgzPath);
                GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(tgzInputStream);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream);) {
            final Map<String, byte[]> toTgz = new LinkedHashMap<>();
            for (TarArchiveEntry entry = tarArchiveInputStream.getNextTarEntry(); entry != null; entry = tarArchiveInputStream
                    .getNextTarEntry()) {
                tgzEntryToJarEntry(p, entry, tarArchiveInputStream, toTgz, jarOutput);
            }
            if (!toTgz.isEmpty()) {
                final byte[] bytes = tarGz(toTgz);
                writeJarEntry(jarOutput, MVNPM_MORE_ARCHIVE, bytes);
            }
        }
    }

    private void tgzEntryToJarEntry(io.mvnpm.npm.model.Package p, ArchiveEntry entry, TarArchiveInputStream tar,
            Map<String, byte[]> toTgz,
            JarArchiveOutputStream jarOutput) throws IOException {
        String importMapRoot = ImportMapUtil.getImportMapRoot(p);
        // Let's filter out files we do not need..
        String name = entry.getName();
        final boolean shouldAdd = !matches(FILES_TO_EXCLUDE, name);
        final boolean shouldTgz = matches(FILES_TO_TGZ, name);
        if (shouldAdd || shouldTgz) {
            name = name.replaceFirst(NPM_ROOT, Constants.EMPTY);
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    BufferedOutputStream bos = new BufferedOutputStream(baos, bufferSize)) {
                IOUtils.copy(tar, bos, bufferSize);
                bos.flush();
                baos.flush();
                if (shouldAdd) {
                    writeJarEntry(jarOutput, MVN_ROOT + importMapRoot + name, baos.toByteArray());
                } else {
                    // We don't add the META-INF because the tgz is already in META-INF
                    toTgz.put("resources" + importMapRoot + name, baos.toByteArray());
                }
            }
        }
    }

    private byte[] tarGz(Map<String, byte[]> toCompress) throws IOException {
        // Step 1, 2 and 3: Create tar archive from map
        ByteArrayOutputStream tarOutput = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarArchiveOutputStream = new ArchiveStreamFactory()
                .createArchiveOutputStream(ArchiveStreamFactory.TAR, tarOutput)) {
            tarArchiveOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (Map.Entry<String, byte[]> entry : toCompress.entrySet()) {
                TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getKey());
                tarEntry.setSize(entry.getValue().length);
                tarArchiveOutputStream.putArchiveEntry(tarEntry);
                tarArchiveOutputStream.write(entry.getValue());
                tarArchiveOutputStream.closeArchiveEntry();
            }
        } catch (ArchiveException e) {
            throw new RuntimeException(e);
        }

        // Step 4: Compress tar archive to tar.gz
        ByteArrayOutputStream gzipOutput = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(gzipOutput)) {
            gzipOutputStream.write(tarOutput.toByteArray());
        }

        return gzipOutput.toByteArray();
    }

    static boolean matches(List<String> suffixes, String name) {
        for (String suffix : suffixes) {
            if (name.toLowerCase().endsWith(suffix.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private byte[] createPomProperties(io.mvnpm.npm.model.Package p) throws IOException {
        Properties properties = new Properties();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            properties.setProperty(Constants.GROUP_ID, p.name().mvnGroupId);
            properties.setProperty(Constants.ARTIFACT_ID, p.name().mvnArtifactId);
            properties.setProperty(Constants.VERSION, p.version());
            properties.setProperty(Constants.MVNPM_PACKAGING_VERSION_KEY, Constants.MVNPM_PACKAGING_VERSION);
            properties.store(baos, POM_DOT_PROPERTIES_COMMENT);
            return baos.toByteArray();
        }
    }

    private void writeJarEntry(JarArchiveOutputStream jarOutput, String filename, Path path) throws IOException {
        JarArchiveEntry entry = new JarArchiveEntry(filename);
        entry.setSize(Files.size(path));
        jarOutput.putArchiveEntry(entry);
        try (InputStream fileInputStream = Files.newInputStream(path)) {
            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                jarOutput.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error jarring file content for " + path, e);
        }
        jarOutput.closeArchiveEntry();
    }

    private void writeJarEntry(JarArchiveOutputStream jarOutput, String filename, byte[] filecontents) throws IOException {
        JarArchiveEntry entry = new JarArchiveEntry(filename);
        entry.setSize(filecontents.length);
        jarOutput.putArchiveEntry(entry);
        jarOutput.write(filecontents);
        jarOutput.closeArchiveEntry();
    }

    private static final String PACKAGE = "package";
    private static final String NPM_ROOT = PACKAGE + Constants.SLASH;
    private static final String META_INF = "META-INF";
    private static final String MVN_ROOT = META_INF + "/resources";
    private static final String POM_ROOT = META_INF + "/maven/";
    public static final String MVNPM_MORE_ARCHIVE = META_INF + "/.more" + Constants.DOT_TGZ;
    private static final String POM_DOT_XML = "pom.xml";
    private static final String POM_DOT_PROPERTIES = "pom.properties";
    private static final String POM_DOT_PROPERTIES_COMMENT = "Generated by mvnpm.org";
    private final int bufferSize = 4096;

    // Files to add in a tgz compressed file in the jar
    static final List<String> FILES_TO_TGZ = List.of(".d.ts");

    // Excluded files which won't be added to the jar (unless gzipped)
    static final List<String> FILES_TO_EXCLUDE = List.of(".md", ".ts", ".ts.map", "/logo.svg");
    private static final String CONTENTS = "No contents since this is a JavaScript Project";
}
