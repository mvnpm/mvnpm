package org.mvnpm.file.type;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.json.Json;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.file.AsyncFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import org.mvnpm.importmap.model.Imports;

/**
 * Create the jar from the npm content
 * @author Phillip Kruger (phillip.kruger@gmail.com)
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
            
            // Import map
            writeJarEntry(jarOutput, MVN_ROOT + p.name() + SLASH + IMPORT_MAP, createImportMap(p));
            
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
    
    private byte[] createImportMap(org.mvnpm.npm.model.Package p) {
        
        String root = getImportMapRoot(p);
        
        String main = getMain(p);
        Map<String, String> v = new HashMap<>();
        
        v.put(p.name(), root + main);
        v.put(p.name() + SLASH, root);
        
        Imports imports = new Imports(v);
        
        String importmapJson = Json.encode(imports);
        
        return importmapJson.getBytes();
    }
    
    private String getImportMapRoot(org.mvnpm.npm.model.Package p){
        String root = STATIC_ROOT + p.name();
        if(p.repository()!=null && p.repository().directory()!=null && !p.repository().directory().isEmpty()){
            String d = p.repository().directory();
            if(d.startsWith(PACKAGES + SLASH)){
                root = d.replaceFirst(PACKAGES + SLASH, STATIC_ROOT);
            }else if (d.startsWith(PACKAGE + SLASH)){
                root = d.replaceFirst(PACKAGE + SLASH, STATIC_ROOT);
            }
        }
        if(!root.endsWith(SLASH)){
            root = root + SLASH;
        }
        
        // TODO: Validate that the folder exist ?
        // Else search for the first "main" / "module" in the tree ?
        return root;
    }
    
    private String getMain(org.mvnpm.npm.model.Package p){
        if(p.module()!=null && !p.module().isEmpty()){
            return p.module();
        }else if(p.main()!=null && !p.main().isBlank()){
            return p.main();
        }
        
        // Default
        return INDEX_JS;
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
    
    private static final String IMPORT_MAP = "importmap.json";
    private static final String PACKAGES = "packages";
    private static final String PACKAGE = "package";
    private static final String SLASH = "/";
    private static final String NPM_ROOT = PACKAGE + SLASH;
    private static final String STATIC_ROOT = "/_static/";
    private static final String INDEX_JS = "index.js";
    private static final String MVN_ROOT = "META-INF/resources" + STATIC_ROOT;
    private static final String POM_ROOT = "META-INF/maven/" + ORG_MVNPM + "/";
    private static final String POM_DOT_XML = "pom.xml";
    private static final String POM_DOT_PROPERTIES = "pom.properties";
    private static final String POM_DOT_PROPERTIES_COMMENT = "Generated by mvnpm.org";
    
}
