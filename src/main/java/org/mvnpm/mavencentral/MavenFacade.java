package org.mvnpm.mavencentral;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
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
    
    public Uni<JsonObject> search(String groupId, String artifactId, String version){
        String q = String.format(Q_FORMAT, groupId, artifactId, version);
        return searchMavenClient.search(q, CORE, ROWS, WT);
    }
    
    public Uni<Boolean> isAvailable(String groupId, String artifactId, String version){
        Uni<JsonObject> searchResult = search(groupId, artifactId, version);
        return searchResult.onItem().transform((r) -> {
            if(r!=null){
                JsonObject response = r.getJsonObject("response");
                if(response!=null){
                    Integer numFound = response.getInteger("numFound");
                    if(numFound!=null && numFound.intValue()>0){
                        return true;
                    }
                }
            }
            return false;
        });
    }
 
    public Uni<Response> upload(Path path) {
        Log.info("====== mvnpm: Nexus Uploader ======");
        Log.info("\tUploading " + path + "...");
        byte[] b;
        try {
            b = Files.readAllBytes(path);
        } catch (IOException ex) {
            return Uni.createFrom().failure(ex);
        }
        
        if(authorization.isPresent()){
            String a = "Basic " + authorization.get();
            return sonatypeClient.uploadBundle(a, b);    
        }else{
            return Uni.createFrom().item(Response.accepted("Mock upload for " + path + " done").build());
        }
        
    }
    
}
