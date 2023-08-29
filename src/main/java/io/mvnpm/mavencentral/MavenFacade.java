package io.mvnpm.mavencentral;

import io.quarkus.logging.Log;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Facade on the Maven Search API
 * TODO: Add caching
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MavenFacade {
  
    private static final String Q_FORMAT = "g:%s AND a:%s AND v:%s";
    private static final String CORE = "gav";
    private static final String ROWS = "1";
    private static final String WT = "json";
    
    @RestClient 
    SearchMavenClient searchMavenClient;
    
    @RestClient
    SonatypeClient sonatypeClient; 
    
    @ConfigProperty(name = "mvnpm.sonatype.authorization")
    Optional<String> authorization;
    
    @ConfigProperty(name = "mvnpm.sonatype.profileId", defaultValue = "473ee06cf882e")
    String profileId;
    
    public JsonObject search(String groupId, String artifactId, String version){
        String q = String.format(Q_FORMAT, groupId, artifactId, version);
        Response searchResponse = searchMavenClient.search(q, CORE, ROWS, WT);
        if(searchResponse.getStatus()<300){
            return searchResponse.readEntity(JsonObject.class);
        }
        return null;
    }
    
    public boolean isAvailable(String groupId, String artifactId, String version){
        JsonObject searchResult = search(groupId, artifactId, version);
        
        if(searchResult!=null){
            JsonObject response = searchResult.getJsonObject("response");
            if(response!=null){
                Integer numFound = response.getInteger("numFound");
                if(numFound!=null && numFound.intValue()>0){
                    return true;
                }
            }
        }
        
        return false;
    }
 
    public String upload(Path path) {
        Log.debug("====== mvnpm: Nexus Uploader ======");
        Log.debug("\tUploading " + path + "...");
        byte[] b;
        try {
            b = Files.readAllBytes(path);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        
        if(authorization.isPresent()){
            String a = "Basic " + authorization.get();
            try {
                Response uploadResponse = sonatypeClient.uploadBundle(a, b);
            
                if(uploadResponse.getStatus()==201){
                    String resp = uploadResponse.readEntity(String.class);
                    String repositoryId = resp.substring(resp.lastIndexOf('/') + 1, resp.length()-3);
                    Log.info("Uploaded bundle " + path + " to staging repo [" + repositoryId + "]");
                    
                    // Now close
                    JsonObject data = JsonObject.of("description", "Closed by mvnpm.org", "stagedRepositoryId", repositoryId);
                    String post = JsonObject.of("data", data).encode();
                    Log.info("post data = " + post);
                    Response closeResponse = sonatypeClient.closeUploadBundle(a, profileId, post);
                    
                    Log.info("CLOSING STATUS [" + closeResponse.getStatus() + "]");
                    Log.info("CLOSING DATA   [" + closeResponse.readEntity(String.class) + "]");
                    
                    return repositoryId;
                }else{
                    Log.error("Error uploading bundle " + path + " - status code [" + uploadResponse.getStatus() + "]");
                }
            }catch(Throwable t) {
                Log.error("Error uploading bundle " + path + " - " + t.getMessage());
            }
        }
        
        return null;
    }
    
    public JsonObject getStagingProfileRepos(){
        if(authorization.isPresent()){
            String a = "Basic " + authorization.get();
            Response stagingProfileRepos = sonatypeClient.getStagingProfileRepos(a, profileId);
            if(stagingProfileRepos.getStatus()<300){
                return stagingProfileRepos.readEntity(JsonObject.class);
            }else{
                throw new RuntimeException("Could not get staging profile repos [" + stagingProfileRepos.getStatus() + " - " + stagingProfileRepos.getStatusInfo().getReasonPhrase() +"]");
            }
        }
        return null;
    }
    
    public boolean dropStagingProfileRepo(String stagingRepoId) {
        if(authorization.isPresent()){
            String a = "Basic " + authorization.get();
            Response dropResponse = sonatypeClient.dropStagingProfileRepo(a, stagingRepoId);
            return dropResponse.getStatus()==204;
        }
        return false;
    }

    public void dropStagingProfileRepos() {
        JsonObject stagingProfileRepos = getStagingProfileRepos();
        
        JsonArray data = stagingProfileRepos.getJsonArray("data");
        data.forEach((t) -> {
            JsonObject repository = (JsonObject)t;
            String repositoryId = repository.getString("repositoryId");
            dropStagingProfileRepo(repositoryId);
        });
    }
    
}
