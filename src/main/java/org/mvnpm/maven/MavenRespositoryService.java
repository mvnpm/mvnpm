
package org.mvnpm.maven;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.file.AsyncFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.mvnpm.Constants;
import org.mvnpm.file.FileClient;
import org.mvnpm.file.FileType;
import org.mvnpm.npm.NpmRegistryFacade;
import org.mvnpm.npm.model.Name;
import org.mvnpm.npm.model.Project;

/**
 * The maven repository as a service
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MavenRespositoryService {
    
    @Inject
    NpmRegistryFacade npmRegistryFacade;
    
    @Inject
    FileClient fileClient; 
    
    public Uni<AsyncFile> getFile(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            Uni<String> latestVersion = getLatestVersion(fullName);
            return latestVersion.onItem().transformToUni((latest)->{
                return getFile(fullName, latest, type);
            });
        }else {
            Uni<org.mvnpm.npm.model.Package> npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                return fileClient.streamFile(type, p).onItem().transform((file) -> {
                    return file;
                });
            }); 
        }
    }
    
    public Uni<AsyncFile> getSha1(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            Uni<String> latestVersion = getLatestVersion(fullName);
            return latestVersion.onItem().transformToUni((latest)->{
                return getSha1(fullName, latest, type);
            });
        }else {
            Uni<org.mvnpm.npm.model.Package> npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                return fileClient.streamSha1(type, p).onItem().transform((file) -> {
                    return file;
                });
            }); 
        }
    }
    
    public Uni<AsyncFile> getMd5(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            Uni<String> latestVersion = getLatestVersion(fullName);
            return latestVersion.onItem().transformToUni((latest)->{
                return getMd5(fullName, latest, type);
            });
        }else {
            Uni<org.mvnpm.npm.model.Package> npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                return fileClient.streamMd5(type, p).onItem().transform((file) -> {
                    return file;
                });
            }); 
        }
    }
    
    public Uni<AsyncFile> getAsc(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            Uni<String> latestVersion = getLatestVersion(fullName);
            return latestVersion.onItem().transformToUni((latest)->{
                return getAsc(fullName, latest, type);
            });
        }else {
            Uni<org.mvnpm.npm.model.Package> npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                return fileClient.streamAsc(type, p).onItem().transform((file) -> {
                    return file;
                });
            }); 
        }
    }
    
    private Uni<String> getLatestVersion(Name fullName){
        Uni<Project> project = npmRegistryFacade.getProject(fullName.npmFullName());
        return project.onItem()
                .transform((p) -> {
                    return p.distTags().latest();
                });
    }
}
