package org.mvnpm.newfile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.mvnpm.Constants;

import org.mvnpm.file.FileStoreEvent;
import org.mvnpm.file.FileUtil;

/**
 * Create source jar file from tgz
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class SourceService {

    public void newFileCreated(@ObservesAsync FileStoreEvent fse) {
        String tgzFile = fse.fileName();
        if(tgzFile.endsWith(Constants.DOT_TGZ)){
            
            String outputFile = tgzFile.replace(Constants.DOT_TGZ, Constants.DASH_SOURCES_DOT_JAR);
            boolean ok = createJar(tgzFile, outputFile);
            if(ok){
                FileUtil.createSha1(outputFile);
                FileUtil.createMd5(outputFile);
                FileUtil.createAsc(outputFile);
            }
            
        }
    }
    
    private boolean createJar(String tgzFile, String outputFile){
        Path f = Paths.get(outputFile);
        if(!Files.exists(f)){
            synchronized (f) {
                try (FileOutputStream fileOutput = new FileOutputStream(outputFile);
                    JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(fileOutput)){
                    tgzToJar(tgzFile, jarOutput);
                    jarOutput.finish();
                    return true;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return false;
    }
    
    private void tgzToJar(String tarFile, JarArchiveOutputStream jarOutput) throws IOException {
        try(InputStream is = new BufferedInputStream(new FileInputStream(tarFile))){
            tgzToJar( is,jarOutput);
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
    
    private void tgzEntryToJarEntry(ArchiveEntry entry, TarArchiveInputStream tar, JarArchiveOutputStream jarOutput) throws IOException {
        
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
