package org.mvnpm.npm;

import io.quarkus.cache.CacheResult;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mvnpm.npm.model.Project;
import org.mvnpm.npm.model.SearchResults;

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
    public Uni<Project> getProject(String project){
        return npmRegistryClient.getProject(project);
    }
    
    @CacheResult(cacheName = "npm-package-cache")
    public Uni<org.mvnpm.npm.model.Package> getPackage(String project, String version){
        return npmRegistryClient.getPackage(project, version);
    }

    public Uni<SearchResults> search(String term, int page) {
        if(page<0) page = 1;
        return npmRegistryClient.search(term, ITEMS_PER_PAGE, page - 1, 1.0, 0.0, 0.0);
    }
    
    private static final int ITEMS_PER_PAGE = 50; // TODO: Move to config ?
}
