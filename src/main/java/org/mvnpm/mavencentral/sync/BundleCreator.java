package org.mvnpm.mavencentral.sync;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.file.AsyncFile;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.mvnpm.Constants;
import org.mvnpm.file.FileStore;
import org.mvnpm.file.FileType;
import org.mvnpm.maven.MavenRespositoryService;
import org.mvnpm.npm.model.Name;

/**
 * This creates a bundles (pom, jar, -sources, -javadoc) in the format Nexus expects
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class BundleCreator {
    
    @Inject
    FileStore fileStore;
    
    @Inject
    MavenRespositoryService mavenRespositoryService;
    
    public Uni<Path> bundle(Name name, String version){
        Log.info("====== mvnpm: Nexus Bundler ======");
        // First get the jar, as the jar will create the pom, and
        // other files are being created once the pom and jar is downloaded
        Uni<AsyncFile> jarFile = mavenRespositoryService.getFile(name, version, FileType.jar);
        return jarFile.onItem().transformToUni((t) -> {
            Log.info("\tbundle: Got initial Jar file");
            return buildBundle(t, name, version);
        });
    }
    
    private Uni<Path> buildBundle(AsyncFile jarFile,Name name, String version) {
        Uni<Map<String, byte[]>> files = getFiles(jarFile, name, version);
        return files.onItem().transformToUni((t) -> {
            String parent = fileStore.getLocalDirectory(name, version);
            String bundlelocation = name.mvnArtifactId() + Constants.HYPHEN + version + "-bundle.jar";
            Path bundlePath = Paths.get(parent, bundlelocation);
        
            Log.info("\tBuilding bundle " + bundlePath + "...");
            
            File bundleFile = bundlePath.toFile();
            try (FileOutputStream fos = new FileOutputStream(bundleFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 ZipOutputStream zos = new ZipOutputStream(bos)) {

                for (Map.Entry<String, byte[]> file : t.entrySet()) {
                    Path path = Paths.get(file.getKey());
                    ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());
                    zos.putNextEntry(zipEntry);
                    zos.write(file.getValue());
                    zos.closeEntry();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            
            return Uni.createFrom().item(bundlePath);
        });        
    }

    private Uni<byte[]> read(Uni<AsyncFile> af) {
        return af.onItem().transformToUni((t) -> {
            return read(t);
        });
    }
    
    private Uni<byte[]> read(AsyncFile af) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Uni<Void> flushed = af.exceptionHandler(t ->
                                System.out.println("Failure while reading the file: " + t)
                            ).handler(buffer -> {
                                try {
                                    baos.write(buffer.getBytes());
                                } catch (IOException ex) {
                                    throw new UncheckedIOException(ex);
                                }
                            }).flush();
        
        return flushed.onItem().transform((t) -> {
            return baos.toByteArray();
        });
    }
    
    private Uni<Map<String, byte[]>> getFiles(AsyncFile jarFile, Name name, String version){
        // Files that needs to be in the bundle
        String parent = fileStore.getLocalDirectory(name, version);
        
        String base = parent + File.separator + name.mvnArtifactId() + Constants.HYPHEN + version;
        String jarFileName = base + Constants.DOT_JAR;
        List<String> fileNames = getFileNamesInBundle(base);
        List<Uni<byte[]>> files = new ArrayList<>();
        for(String fileName:fileNames){
            if(fileName.equals(jarFileName)){
                Uni<byte[]> content = read(jarFile);
                files.add(content);
                Log.info("\tbundle: " + fileName + " [already]");
            }else{
                Uni<byte[]> content = read(fileStore.readFile(fileName));
                files.add(content);
                Log.info("\tbundle: " + fileName + " [ok]");
            }
        }
        
        return Uni.combine()
            .all().unis(files).combinedWith(
                    listOfResponses -> {
                        Log.info("All bundle files available !");
                        return Map.of(fileNames.get(0),
                                (byte[])listOfResponses.get(0), 
                                fileNames.get(1),
                                (byte[])listOfResponses.get(1), 
                                fileNames.get(2),
                                (byte[])listOfResponses.get(2), 
                                fileNames.get(3),
                                (byte[])listOfResponses.get(3), 
                                fileNames.get(4),
                                (byte[])listOfResponses.get(4), 
                                fileNames.get(5),
                                (byte[])listOfResponses.get(5), 
                                fileNames.get(6),
                                (byte[])listOfResponses.get(6), 
                                fileNames.get(7),
                                (byte[])listOfResponses.get(7));
                    }
            );
    }
    
    private List<String> getFileNamesInBundle(String base){
        List<String> fileNames = List.of(
            base + Constants.DOT_POM,
            base + Constants.DOT_POM + Constants.DOT_ASC,
            base + Constants.DOT_JAR,
            base + Constants.DOT_JAR + Constants.DOT_ASC,
            base + Constants.DASH_SOURCES_DOT_JAR,
            base + Constants.DASH_SOURCES_DOT_JAR +  Constants.DOT_ASC,
            base + Constants.DASH_JAVADOC_DOT_JAR,
            base + Constants.DASH_JAVADOC_DOT_JAR +  Constants.DOT_ASC);
        
        return fileNames;
    }
}
