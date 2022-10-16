package org.mvnpm.file.type;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.file.AsyncFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Properties;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.mvnpm.file.FileClient;
import org.mvnpm.file.FileStore;
import org.mvnpm.file.FileType;

/**
 * Create the jar from the npm content
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * 
 * TODO: Generate importmap
 */
@ApplicationScoped
public class JarClient {

    @Inject
    Vertx vertx;
    
    @Inject
    FileClient fileClient;
    
    @Inject 
    FileStore fileStore;
    
    public Uni<AsyncFile> createJar(org.mvnpm.npm.model.Package p, String localFileName){
        
        Uni<Buffer> pomContent = getFileContent(p, FileType.pom);
        Uni<Buffer> tgzContent = getFileContent(p, FileType.tgz);
        
        Uni<Tuple2<Buffer, Buffer>> inputContent = Uni.combine().all().unis(pomContent, tgzContent).asTuple();
        
        return inputContent.onItem().transformToUni((t) -> {
            return tgzToJar(p, localFileName, t.getItem1(), t.getItem2());
        });
        
    }
    
    private Uni<AsyncFile> tgzToJar(org.mvnpm.npm.model.Package p, String localFileName, Buffer pomBuffer, Buffer tgzBuffer){
            
        try (GzipCompressorInputStream gzInput = new GzipCompressorInputStream(new ByteArrayInputStream(tgzBuffer.getBytes()));
                TarArchiveInputStream tarInput = new TarArchiveInputStream(gzInput);
                ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(byteOutput)){

            // Pom details 
            String pomXmlDir = POM_ROOT + p.name() + SLASH;
            
            // Pom xml entry
            writeJarEntry(jarOutput, pomXmlDir + POM_DOT_XML, pomBuffer.getBytes());
            
            // Pom properties entry
            writeJarEntry(jarOutput, pomXmlDir + POM_DOT_PROPERTIES, createPomProperties(p));
            
            TarArchiveEntry tarEntry;
            try {
                while ((tarEntry = tarInput.getNextTarEntry()) != null) {
                    String name = tarEntry.getName();
                    if (tarEntry.isDirectory()) {
                        continue;
                    }

                    name = name.replaceFirst(NPM_ROOT, MVN_ROOT + p.name()  + SLASH);
                    // TODO: Allow filtering from config ? Filter out unwanted files
                    try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
                        IOUtils.copy(tarInput, baos);
                        writeJarEntry(jarOutput, name, baos.toByteArray());
                    }catch(EOFException eofe){
                        // Just continue
                    }
                }
            }catch(EOFException e) {
                // Just continue
            }

            jarOutput.finish();

            byte[] jarFileContents = byteOutput.toByteArray();

            return fileStore.createFile(p, localFileName, jarFileContents);

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        
    }
    
    private byte[] createPomProperties(org.mvnpm.npm.model.Package p) throws IOException{
        Properties properties = new Properties();
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            properties.setProperty(GROUP_ID, ORG_MVNPM);
            properties.setProperty(ARTIFACT_ID, p.name());
            properties.setProperty(VERSION, p.version());
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
    
    private Uni<Buffer> getFileContent(org.mvnpm.npm.model.Package p, FileType type){
        Uni<AsyncFile> pomFile = fileClient.streamFile(type, p);
        return pomFile.onItem().transformToUni((pomContent) -> {
            return pomContent.toMulti().toUni();
        });
    }
    
    private static final String ORG_MVNPM = "org.mvnpm";
    private static final String VERSION = "version";
    private static final String GROUP_ID = "groupId";
    private static final String ARTIFACT_ID = "artifactId";
    
    private static final String SLASH = "/";
    private static final String NPM_ROOT = "package/";
    private static final String MVN_ROOT = "META-INF/resources/_static/";
    private static final String POM_ROOT = "META-INF/maven/" + ORG_MVNPM + "/";
    private static final String POM_DOT_XML = "pom.xml";
    private static final String POM_DOT_PROPERTIES = "pom.properties";
    private static final String POM_DOT_PROPERTIES_COMMENT = "Generated by mvnpm.org";
    
}
