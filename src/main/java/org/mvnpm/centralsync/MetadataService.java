package org.mvnpm.centralsync;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import io.quarkus.logging.Log;
import java.io.UncheckedIOException;

/**
 * Keeps some metadata about
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class MetadataService {
    
    private final String localUserDir;
    private final String localM2Dir;
    
    private final Properties metadata;
    private final Path path;
    
    public MetadataService(String localUserDir, String localM2Dir, String groupId, String artifactId, String version) {
        this.localUserDir = localUserDir;
        this.localM2Dir = localM2Dir;
        this.path = getLocalMetadataFilePath(groupId, artifactId, version);
        this.metadata = getMetadata();
    }
    
    public boolean isTrue(String key){
        if(has(key)){
            String value = get(key);
            return Boolean.parseBoolean(get(key));
        }
        return false;
    }
    
    public boolean has(String key){
        return metadata.containsKey(key);
    }
    
    public String get(String key){
        return metadata.getProperty(key);
    }
    
    public void set(String key, String value){
        metadata.setProperty(key, value);
        try {
            if(!Files.exists(this.path.getParent())){
                Files.createDirectories(this.path.getParent());
            }
            metadata.store(Files.newOutputStream(this.path), "Last updated on " + getTimeStamp());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    private Properties getMetadata(){
        Properties md = new Properties();
        if(Files.exists(path)){
            try {
                md.load(Files.newInputStream(path));
            } catch (IOException ex) {
                Log.error("Error while checking local metadata", ex);
            }
        }
        return md;
    }
    
    private Path getLocalMetadataFilePath(String groupId, String artifactId, String version){
        String mvnpmPath = localUserDir + "/" + localM2Dir + "/repository/";
        String groupPath = groupId.replaceAll("\\.", "/");
        String fullPath = mvnpmPath + "/" + groupPath + "/" + artifactId + "/" + version + "/mvnpm-metadata.properties";
        return Paths.get(fullPath);
    }
    
    private String getTimeStamp(){
        return ZonedDateTime
            .now( ZoneId.systemDefault() )
            .format( DateTimeFormatter.ISO_DATE_TIME);
    }
    
}
