package org.mvnpm.centralsync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * This checks if a file exists in maven central
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MavenCentralChecker {
    
    @ConfigProperty(name = "mvnpm.local-user-directory")
    private String localUserDir;
    @ConfigProperty(name = "mvnpm.local-m2-directory")
    private String localM2Dir;
    
    
    public boolean isAvailable(String groupId, String artifactId, String version){
        try {
            return isVersionAvailableInMavenCentral(groupId, artifactId, version);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private boolean isVersionAvailableInMavenCentral(String groupId, String artifactId, String versionToCheck) throws IOException, InterruptedException {
        // First check local cache
        boolean local = checkLocal(groupId, artifactId, versionToCheck);
        if(local){
            return true;
        }
        // Next try remove
        boolean remote = checkRemote(groupId, artifactId, versionToCheck);
        if(remote){
            // store cache
            cacheLocal(groupId, artifactId, versionToCheck);
        }
        return remote;
    }

    private boolean checkLocal(String groupId, String artifactId, String versionToCheck){
        String fullPath = getLocalMetadataFilePath(groupId, artifactId, versionToCheck);
        
        Path path = Paths.get(fullPath);
        if(Files.exists(path)){
            try {
                Properties metadata = new Properties();
                metadata.load(Files.newInputStream(path));
                String propVal = metadata.getProperty(AVAILABLE_IN_CENTRAL,FALSE);
                return Boolean.parseBoolean(propVal);
            } catch (IOException ex) {
                Log.error("Error while checking local metadata", ex);
            }
        }
        return false;
    }
    
    private boolean checkRemote(String groupId, String artifactId, String versionToCheck){
        try {
            String url = "https://search.maven.org/solrsearch/select?q=g:%22"
                    + URLEncoder.encode(groupId, StandardCharsets.UTF_8)
                    + "%22+AND+a:%22" + URLEncoder.encode(artifactId, StandardCharsets.UTF_8)
                    + "%22+AND+v:%22" + URLEncoder.encode(versionToCheck, StandardCharsets.UTF_8)
                    + "%22&core=gav&rows=1&wt=json";
            Log.debug("\tChecking remote url " + url);
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(jsonResponse);
                JsonNode responseNode = jsonNode.get("response");
                JsonNode docsNode = responseNode.get("docs");
                boolean available = docsNode.size() > 0;
                Log.debug("\t" + url +" [" + available + "]");
                return available;
            }
            Log.debug("\t" + url +" [false]");
        } catch (IOException | InterruptedException ex) {
            Log.error("Error while checking remote maven central", ex);
        }
        
        return false;
    }
    
    private void cacheLocal(String groupId, String artifactId, String version){
        String fullPath = getLocalMetadataFilePath(groupId, artifactId, version);
        Path path = Paths.get(fullPath);
        Properties metadata = new Properties();
        try {
            if(Files.exists(path)){
                metadata.load(Files.newInputStream(path));
            }else{
                Files.createDirectories(path.getParent());
            }
            metadata.setProperty(AVAILABLE_IN_CENTRAL, TRUE);
            metadata.store(Files.newOutputStream(path), "Last updated on " + getTimeStamp());
        } catch (IOException ex) {
            Log.error("Error while creating local metadata", ex);
        }
    }
    
    private String getLocalMetadataFilePath(String groupId, String artifactId, String version){
        String mvnpmPath = localUserDir + "/" + localM2Dir + "/repository/org/mvnpm/";
        String groupPath = groupId.replaceAll("\\.", "/");
        return mvnpmPath + groupPath + "/" + artifactId + "/" + version + "/mvnpm-metadata.properties";
    }
    
    private String getTimeStamp(){
        return ZonedDateTime
            .now( ZoneId.systemDefault() )
            .format( DateTimeFormatter.ISO_DATE_TIME);
    }
    
    private static final String AVAILABLE_IN_CENTRAL = "available-in-central";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
}
