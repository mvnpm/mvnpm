package org.mvnpm.file.type;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.mutiny.core.file.AsyncFile;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.mvnpm.Constants;
import org.mvnpm.file.FileClient;
import org.mvnpm.file.FileStore;
import org.mvnpm.file.FileType;
import org.mvnpm.importmap.ImportsDataBinding;
import org.mvnpm.importmap.Location;
import org.mvnpm.importmap.model.Imports;

/**
 * Create the jar from the npm content
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class JarClient {
    
    @Inject
    FileClient fileClient;
    
    @Inject 
    FileStore fileStore;
    
    public Uni<AsyncFile> createJar(org.mvnpm.npm.model.Package p, String localFileName){
        Uni<String> pomFile = fileClient.getFileName(FileType.pom, p);
        Uni<String> tgzFile = fileClient.getFileName(FileType.tgz, p);
        
        Uni<Tuple2<String, String>> inputFiles = Uni.combine().all().unis(pomFile, tgzFile).asTuple();
        
        return inputFiles.onItem().transformToUni((t) -> {
            return jarInput(p, localFileName, t.getItem1(), t.getItem2());
        });        
    }
    
    private Uni<AsyncFile> jarInput(org.mvnpm.npm.model.Package p, String localFileName, String pomFile, String tgzFile){
        
        try (ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(byteOutput)){
            
            // Pom details 
            String pomXmlDir = POM_ROOT + p.name().mvnPath() + Constants.SLASH;
            
            // Pom xml entry
            // TODO: Add dependency to importmap merger
            byte[] pomBytes = Files.readAllBytes(Paths.get(pomFile));
            writeJarEntry(jarOutput, pomXmlDir + POM_DOT_XML, pomBytes);
            
            // Pom properties entry
            writeJarEntry(jarOutput, pomXmlDir + POM_DOT_PROPERTIES, createPomProperties(p));
            
            // Import map
            writeJarEntry(jarOutput, Location.IMPORTMAP_PATH, createImportMap(p));
            
            // Tar contents
            tgzToJar(p, tgzFile, jarOutput);
            
            jarOutput.finish();

            byte[] jarFileContents = byteOutput.toByteArray();

            return fileStore.createFile(p, localFileName, jarFileContents);
            
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    private void tgzToJar(org.mvnpm.npm.model.Package p, String tarFile, JarArchiveOutputStream jarOutput) throws IOException {
        try(InputStream is = new BufferedInputStream(new FileInputStream(tarFile))){
            tgzToJar(p, is,jarOutput);
        }
    }
    
    private void tgzToJar(org.mvnpm.npm.model.Package p, InputStream tarInput, JarArchiveOutputStream jarOutput) throws IOException {
        try (GZIPInputStream inputStream = new GZIPInputStream(tarInput);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(inputStream)) {

            for (TarArchiveEntry entry = tarArchiveInputStream.getNextTarEntry(); entry != null; entry = tarArchiveInputStream
                    .getNextTarEntry()) {
                tgzEntryToJarEntry(p, entry, tarArchiveInputStream, jarOutput);
            }
        }
    }

    private void tgzEntryToJarEntry(org.mvnpm.npm.model.Package p, ArchiveEntry entry, TarArchiveInputStream tar, JarArchiveOutputStream jarOutput) throws IOException {
        String root = MVN_ROOT + getImportMapRoot(p);
        // Let's filter out files we do not need..
        String name = entry.getName();
        
        if(!shouldIgnore(name)){
            
            name = name.replaceFirst(NPM_ROOT, Constants.EMPTY);
            final int bufferSize = 4096;

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
    
    private byte[] createImportMap(org.mvnpm.npm.model.Package p) {
        
        String root = getImportMapRoot(p);
        
        String main = getMain(p);
        Map<String, String> v = new HashMap<>();
        
        v.put(p.name().npmFullName(), root + main);
        v.put(p.name().npmFullName() + Constants.SLASH, root);
        
        Imports imports = new Imports(v);
        
        String importmapJson = ImportsDataBinding.toJson(imports);
        
        return importmapJson.getBytes();
    }
    
    private String getImportMapRoot(org.mvnpm.npm.model.Package p){
        String root = STATIC_ROOT + p.name().npmName();
        if(p.repository()!=null && p.repository().directory()!=null && !p.repository().directory().isEmpty()){
            String d = p.repository().directory();
            if(d.startsWith(PACKAGES + Constants.SLASH)){
                root = d.replaceFirst(PACKAGES + Constants.SLASH, STATIC_ROOT);
            }else if (d.startsWith(PACKAGE + Constants.SLASH)){
                root = d.replaceFirst(PACKAGE + Constants.SLASH, STATIC_ROOT);
            }
        }
        if(!root.endsWith(Constants.SLASH)){
            root = root + Constants.SLASH;
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
    
    private static final String PACKAGES = "packages";
    private static final String PACKAGE = "package";
    
    private static final String NPM_ROOT = PACKAGE + Constants.SLASH;
    private static final String STATIC_ROOT = "/_static/";
    private static final String INDEX_JS = "index.js";
    private static final String MVN_ROOT = "META-INF/resources";
    private static final String POM_ROOT = "META-INF/maven/";
    private static final String POM_DOT_XML = "pom.xml";
    private static final String POM_DOT_PROPERTIES = "pom.properties";
    private static final String POM_DOT_PROPERTIES_COMMENT = "Generated by mvnpm.org";
    
    private static final List<String> FILETYPES_TO_IGNORE = List.of(".md", ".ts", ".ts.map", "/package.json", "/logo.svg"); // Make this configuable per package ?
    
}
