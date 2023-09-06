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
    
    @ConfigProperty(name = "mvnpm.sonatype.autorelease")
    boolean autoRelease;
    
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
                    return repositoryId;
                }else{
                    Log.error("Error uploading bundle " + path + " - status [" + uploadResponse.getStatus() + "]");
                }
            }catch(Throwable t) {
                Log.error("Error uploading bundle " + path + " - " + t.getMessage());
            }
        }
        
        return null;
    }
    
    public RepoStatus status(String repositoryId){
        if(authorization.isPresent()){
            String a = "Basic " + authorization.get();
            try {
                Response statusResponse = sonatypeClient.uploadBundleStatus(a, repositoryId);
            
                if(statusResponse.getStatus()<299){
                    JsonObject resp = statusResponse.readEntity(JsonObject.class);
                    String type = resp.getString("type");
                    return RepoStatus.valueOf(type);        
                }else{
                    Log.error("Error checking status for staging repo " + repositoryId + " - status [" + statusResponse.getStatus() + "]");
                }
            }catch(Throwable t) {
                Log.error("Error checking status for staging repo " + repositoryId + " - " + t.getMessage());
            }
            return null;
        }
        return null;
    }
    
    public boolean close(String repositoryId){
        if(authorization.isPresent()){
            String a = "Basic " + authorization.get();
            try {
                JsonObject data = JsonObject.of("description", "Closed by mvnpm.org", "stagedRepositoryId", repositoryId);
                JsonObject closeRequest = JsonObject.of("data", data);
                Response closeResponse = sonatypeClient.closeUploadBundle(a, profileId, closeRequest);
            
                if(closeResponse.getStatus()<299){
                    String resp = closeResponse.readEntity(String.class);
                    Log.info("Closed staging repo " + repositoryId + " [" + resp + "]");
                    return true;
                }else{
                    Log.error("Error closing staging repo " + repositoryId + " - status [" + closeResponse.getStatus() + "]");
                }
            }catch(Throwable t) {
                Log.error("Error closing staging repo " + repositoryId + " - " + t.getMessage());
            }
            return false;
        }
        return true;
    }
    
    public boolean release(String repositoryId){
        if(authorization.isPresent() && autoRelease){
            String a = "Basic " + authorization.get();
            try {
                JsonObject data = JsonObject.of("description", "Released by mvnpm.org", "stagedRepositoryId", repositoryId);
                JsonObject promoteRequest = JsonObject.of("data", data);
                Response promoteResponse = sonatypeClient.releaseToCentral(a, profileId, promoteRequest);
            
                if(promoteResponse.getStatus()<299){
                    return true;
                }else{
                    Log.error("Error promoting staging repo " + repositoryId + " - status [" + promoteResponse.getStatus() + "]");
                }
            }catch(Throwable t) {
                Log.error("Error promoting staging repo " + repositoryId + " - " + t.getMessage());
            }
            return false;
        }
        return true;
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
    
    public boolean drop(String stagingRepoId) {
        if(authorization.isPresent()){
            try {
                String a = "Basic " + authorization.get();
                Response dropResponse = sonatypeClient.dropStagingProfileRepo(a, stagingRepoId);
                return dropResponse.getStatus()==204;
            }catch(Throwable t){
                Log.error(t.getMessage());
                return false;
            }
        }
        return false;
    }

    public void dropAll() {
        JsonObject stagingProfileRepos = getStagingProfileRepos();
        
        JsonArray data = stagingProfileRepos.getJsonArray("data");
        data.forEach((t) -> {
            JsonObject repository = (JsonObject)t;
            String repositoryId = repository.getString("repositoryId");
            drop(repositoryId);
        });
    }
    
}
