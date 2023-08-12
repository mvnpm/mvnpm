package org.mvnpm.centralsync;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.file.AsyncFile;
import jakarta.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
    
    @ConfigProperty(name = "mvnpm.bundle-creator-timeout", defaultValue = "30000") // 30 seconds
    int timeout;
    
    public Path bundle(Name name, String version){
        Log.debug("====== mvnpm: Nexus Bundler ======");
        downloadFiles(name, version);
        Path bundleLocation = buildBundle(name, version); // TODO: Allow passing in output path ?
        Log.debug("Bundle " + bundleLocation + " created successful");
        return bundleLocation;
    }
    
    /**
     * We only need to download the pom and the jar.
     * Other files are being created once the pom and jar is downloaded
    */
    private void downloadFiles(Name name, String version){
        Log.debug("\tDownloading files for " + name.displayName() + " " + version + "...");
        
        Uni<AsyncFile> pomFile = mavenRespositoryService.getFile(name, version, FileType.pom);
        pomFile.await().atMost(Duration.ofSeconds(30));
        
        Uni<AsyncFile> jarFile = mavenRespositoryService.getFile(name, version, FileType.jar);
        jarFile.await().atMost(Duration.ofSeconds(30));
    }    
    
    private Path buildBundle(Name name, String version) {
        String parent = fileStore.getLocalDirectory(name, version);
        String bundlelocation = name.mvnArtifactId() + Constants.HYPHEN + version + "-bundle.jar";
        Path bundlePath = Paths.get(parent, bundlelocation);
        
        Log.debug("\tBuilding bundle " + bundlePath + "...");
        
        List<Path> files = new ArrayList<>() ;
        Stack<String> filesQ = new Stack<>();
        filesQ.addAll(getFiles(name, version));
        // We need to wait until all files are available. 
        long startTime = System.currentTimeMillis();
        while(!filesQ.isEmpty()){
            
            String f = filesQ.peek();
            Path path = Paths.get(parent,f);
            if(Files.exists(path) && Files.isReadable(path) && Files.isWritable(path)){
                files.add(path);
                filesQ.pop();
                Log.debug("\t\t" + f + "[OK]");
            }else{
                Log.debug("\t\t" + f + "[KO]");
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ex) {    
                }
            }
            long elapsed = System.currentTimeMillis()-startTime;
            
            if (elapsed>timeout)
                throw new RuntimeException("Timeout while building " + bundlelocation);
        }
        
        File bundleFile = bundlePath.toFile();
        try (FileOutputStream fos = new FileOutputStream(bundleFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            for (Path file : files) {
                addToZipFile(file, zos);
            }
            
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return bundlePath;
    }

    private void addToZipFile(Path path, ZipOutputStream zos) throws IOException {
        File file = path.toFile();
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            ZipEntry zipEntry = new ZipEntry(file.getName());
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }

            zos.closeEntry();
        }
    }

    private List<String> getFiles(Name name, String version){
        String base = name.mvnArtifactId() + Constants.HYPHEN + version;
        return List.of(
                base + Constants.DOT_POM,
                base + Constants.DOT_POM + Constants.DOT_ASC,
                base + Constants.DOT_JAR,
                base + Constants.DOT_JAR + Constants.DOT_ASC,
                base + Constants.DASH_SOURCES_DOT_JAR,
                base + Constants.DASH_SOURCES_DOT_JAR +  Constants.DOT_ASC,
                base + Constants.DASH_JAVADOC_DOT_JAR,
                base + Constants.DASH_JAVADOC_DOT_JAR +  Constants.DOT_ASC
        );
    }
}
