package io.mvnpm.newfile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.zip.GZIPInputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import io.mvnpm.Constants;
import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileStoreEvent;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

/**
 * Create source jar file from tgz
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class SourceService {

    private final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.tgz");

    @Inject
    FileStore fileStore;

    @ConsumeEvent("new-file-created")
    @Blocking
    public void consumeNewFileCreatedEvent(FileStoreEvent fse) {
        if (matcher.matches(fse.filePath().getFileName())) {
            Path tgzFile = fse.filePath();

            Path sourceFile = Path.of(tgzFile.toString().replace(Constants.DOT_TGZ, Constants.DASH_SOURCES_DOT_JAR));
            boolean ok = createJar(tgzFile, sourceFile);
            if (ok) {
                fileStore.touch(fse.name(), fse.version(), sourceFile);
            }
            Log.debug("source created " + fse.filePath() + "[" + ok + "]");
        }
    }

    private boolean createJar(Path tgzFile, Path sourceFile) {

        if (!Files.exists(sourceFile)) {
            synchronized (sourceFile) {
                try {
                    Files.createDirectories(sourceFile.getParent());
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
                try (OutputStream fileOutput = Files.newOutputStream(sourceFile);
                        JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(fileOutput)) {
                    tgzToJar(tgzFile, jarOutput);
                    jarOutput.finish();
                    return true;
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }
        }
        return false;
    }

    private void tgzToJar(Path tarFile, JarArchiveOutputStream jarOutput) throws IOException {
        if (Files.exists(tarFile)) {
            try (InputStream is = new BufferedInputStream(Files.newInputStream(tarFile))) {
                tgzToJar(is, jarOutput);
            }
        }
    }

    private void tgzToJar(InputStream tarInput, JarArchiveOutputStream jarOutput) throws IOException {
        try (GZIPInputStream inputStream = new GZIPInputStream(tarInput);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream)) {

            for (TarArchiveEntry entry = tarArchiveInputStream.getNextTarEntry(); entry != null; entry = tarArchiveInputStream
                    .getNextTarEntry()) {
                tgzEntryToJarEntry(entry, tarArchiveInputStream, jarOutput);
            }
        }
    }

    private void tgzEntryToJarEntry(ArchiveEntry entry, TarArchiveInputStream tar, JarArchiveOutputStream jarOutput)
            throws IOException {

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(baos, bufferSize)) {
            IOUtils.copy(tar, bos, bufferSize);
            bos.flush();
            baos.flush();
            writeJarEntry(jarOutput, entry.getName(), baos.toByteArray());
        }

    }

    private void writeJarEntry(JarArchiveOutputStream jarOutput, String filename, byte[] filecontents) throws IOException {
        JarArchiveEntry entry = new JarArchiveEntry(filename);
        entry.setSize(filecontents.length);
        jarOutput.putArchiveEntry(entry);
        jarOutput.write(filecontents);
        jarOutput.closeArchiveEntry();
    }

    private final int bufferSize = 4096;

}
