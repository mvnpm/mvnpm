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
import io.quarkus.logging.Log;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mvnpm.Constants;

/**
 * This checks if a file exists in maven central
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MavenCentralChecker {
    
    @ConfigProperty(name = "mvnpm.local-user-directory")
    String localUserDir;
    @ConfigProperty(name = "mvnpm.local-m2-directory", defaultValue = ".m2")
    String localM2Dir;
    
    public boolean isAvailable(String groupId, String artifactId, String version){
        try {
            return isVersionAvailableInMavenCentral(groupId, artifactId, version);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public boolean isStaged(String groupId, String artifactId, String version){
        MetadataService metadataService = new MetadataService(localUserDir, localM2Dir, groupId, artifactId, version);
        return metadataService.isTrue(Constants.STAGED_TO_OSS);
    }
    
    private boolean isVersionAvailableInMavenCentral(String groupId, String artifactId, String version) throws IOException, InterruptedException {
        // First check local cache
        MetadataService metadataService = new MetadataService(localUserDir, localM2Dir, groupId, artifactId, version);
        boolean local = metadataService.isTrue(Constants.AVAILABLE_IN_CENTRAL);
        if(local){
            return true;
        }
        // Next try remove
        boolean remote = checkRemote(groupId, artifactId, version);
        if(remote){
            // store cache
            metadataService.set(Constants.AVAILABLE_IN_CENTRAL, Constants.TRUE);
        }
        return remote;
    }
    
    private boolean checkRemote(String groupId, String artifactId, String version){
        try {
            String url = "https://search.maven.org/solrsearch/select?q=g:%22"
                    + URLEncoder.encode(groupId, StandardCharsets.UTF_8)
                    + "%22+AND+a:%22" + URLEncoder.encode(artifactId, StandardCharsets.UTF_8)
                    + "%22+AND+v:%22" + URLEncoder.encode(version, StandardCharsets.UTF_8)
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
}
