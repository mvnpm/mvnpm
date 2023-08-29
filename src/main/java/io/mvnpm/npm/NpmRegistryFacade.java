package io.mvnpm.npm;

import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.mvnpm.npm.model.Project;
import io.mvnpm.npm.model.SearchResults;
import jakarta.ws.rs.core.Response;

/**
 * Facade on the NPM Registry. 
 * Adds caching
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class NpmRegistryFacade {
  
    @RestClient 
    NpmRegistryClient npmRegistryClient;
    
    @CacheResult(cacheName = "npm-project-cache")
    public Project getProject(String project){
        Response response = npmRegistryClient.getProject(project);
        if(response.getStatus()<300){
            return response.readEntity(Project.class);
        }else{
            // TODO: Retry ?
            throw new RuntimeException("Error while fetching project information from NPM [" + project + "] " + response.getStatusInfo());
        }
    }
    
    @CacheResult(cacheName = "npm-package-cache")
    public io.mvnpm.npm.model.Package getPackage(String project, String version){
        Response response = npmRegistryClient.getPackage(project, version);
        if(response.getStatus()<300){
            return response.readEntity(io.mvnpm.npm.model.Package.class);
        }else{
            // TODO: Retry ?
            throw new RuntimeException("Error while fetching package information from NPM [" + project + ":" + version + "] " + response.getStatusInfo());
        }
    }

    public SearchResults search(String term, int page) {
        if(page<0) page = 1;
        Response response = npmRegistryClient.search(term, ITEMS_PER_PAGE, page - 1, 1.0, 0.0, 0.0);
        if(response.getStatus()<300){
            return response.readEntity(SearchResults.class);
        }else{
            // TODO: Retry ?
            throw new RuntimeException("Error while searching package information from NPM [" + term + "] " + response.getStatusInfo());
        }
    }
    
    private static final int ITEMS_PER_PAGE = 50; // TODO: Move to config ?
}
