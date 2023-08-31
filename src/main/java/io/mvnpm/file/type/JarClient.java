package io.mvnpm.file.type;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
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
import io.mvnpm.file.FileType;
import io.mvnpm.file.ImportMapUtil;
import io.mvnpm.importmap.Location;
import io.mvnpm.maven.MavenRespositoryService;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/**
 * Create the jar from the npm content
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class JarClient {

    @Inject 
    FileStore fileStore;
    
    @Inject
    MavenRespositoryService mavenRespositoryService;
    
    public Path createEmptyJar(Path forJar, String replaceJarWith){
        Path emptyFile = Paths.get(forJar.toString().replace(Constants.DOT_JAR, replaceJarWith));
        if(!Files.exists(emptyFile)){
            synchronized (emptyFile) {
                try (OutputStream fileOutput = Files.newOutputStream(emptyFile);
                    JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(fileOutput)){
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
    
    public byte[] createJar(io.mvnpm.npm.model.Package p, Path localFilePath){
        byte[] pomBytes = mavenRespositoryService.getFile(p.name(), p.version(), FileType.pom);
        byte[] tgzBytes = mavenRespositoryService.getFile(p.name(), p.version(), FileType.tgz);
        return jarInput(p, localFilePath, pomBytes, tgzBytes);
    }
    
    private byte[] jarInput(io.mvnpm.npm.model.Package p, Path localFilePath, byte[] pomBytes, byte[] tgzBytes){
        
        try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(byteOutput)){
            
            // Pom details 
            String pomXmlDir = POM_ROOT + p.name().mvnGroupId() + Constants.SLASH + p.name().mvnArtifactId() + Constants.SLASH;
            
            // Pom xml entry
            writeJarEntry(jarOutput, pomXmlDir + POM_DOT_XML, pomBytes);
            
            // Pom properties entry
            writeJarEntry(jarOutput, pomXmlDir + POM_DOT_PROPERTIES, createPomProperties(p));
            
            // Import map TODO: Chage to new structure
            writeJarEntry(jarOutput, Location.IMPORTMAP_PATH, ImportMapUtil.createImportMap(p));
            
            // Tar contents
            tgzToJar(p, tgzBytes, jarOutput);
            
            jarOutput.finish();

            byte[] jarFileContents = byteOutput.toByteArray();

            return fileStore.createFile(p, localFilePath, jarFileContents);
            
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void tgzToJar(io.mvnpm.npm.model.Package p, byte[] tgzBytes, JarArchiveOutputStream jarOutput) throws IOException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(tgzBytes);
                GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(byteArrayInputStream);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(gzipInputStream);) {

            for (TarArchiveEntry entry = tarArchiveInputStream.getNextTarEntry(); entry != null; entry = tarArchiveInputStream
                    .getNextTarEntry()) {
                tgzEntryToJarEntry(p, entry, tarArchiveInputStream, jarOutput);
            }
        }
    }

    private void tgzEntryToJarEntry(io.mvnpm.npm.model.Package p, ArchiveEntry entry, TarArchiveInputStream tar, JarArchiveOutputStream jarOutput) throws IOException {
        String root = MVN_ROOT + ImportMapUtil.getImportMapRoot(p);
        // Let's filter out files we do not need..
        String name = entry.getName();
        
        if(!shouldIgnore(name)){
            
            name = name.replaceFirst(NPM_ROOT, Constants.EMPTY);
            
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    BufferedOutputStream bos = new BufferedOutputStream(baos, bufferSize)) {
                    IOUtils.copy(tar, bos, bufferSize);
                    bos.flush();
                    baos.flush();
                    writeJarEntry(jarOutput, root + name, baos.toByteArray());
            }
        }
    }
    
    private boolean shouldIgnore(String name){
        for(String end:FILETYPES_TO_IGNORE){
            if(name.endsWith(end) || name.endsWith(end.toUpperCase())){
                return true;
            }
        }
        return false;
    }
    
    private byte[] createPomProperties(io.mvnpm.npm.model.Package p) throws IOException{
        Properties properties = new Properties();
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            properties.setProperty(Constants.GROUP_ID, p.name().mvnGroupId());
            properties.setProperty(Constants.ARTIFACT_ID, p.name().mvnArtifactId());
            properties.setProperty(Constants.VERSION, p.version());
            properties.store(baos, POM_DOT_PROPERTIES_COMMENT);
            return baos.toByteArray();
        }
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
    private static final String MVN_ROOT = "META-INF/resources";
    private static final String POM_ROOT = "META-INF/maven/";
    private static final String POM_DOT_XML = "pom.xml";
    private static final String POM_DOT_PROPERTIES = "pom.properties";
    private static final String POM_DOT_PROPERTIES_COMMENT = "Generated by mvnpm.org";
    private final int bufferSize = 4096;
    private static final List<String> FILETYPES_TO_IGNORE = List.of(".md", ".ts", ".ts.map", "/logo.svg"); // Make this configuable per package ?
    private static final String CONTENTS = "No contents since this is a JavaScript Project";
}
