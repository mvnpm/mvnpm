package io.mvnpm.maven;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.mvnpm.Constants;
import io.mvnpm.composite.CompositeService;
import io.mvnpm.file.FileClient;
import io.mvnpm.file.FileType;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.Project;

/**
 * The maven repository as a service
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MavenRespositoryService {
    
    @Inject
    NpmRegistryFacade npmRegistryFacade;
    
    @Inject
    CompositeService compositeService;
    
    @Inject
    FileClient fileClient; 
    
    public byte[] getFile(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            String latestVersion = getLatestVersion(fullName);
            return getFile(fullName, latestVersion, type);
        }else if(fullName.isInternal()){
            return compositeService.getFile(fullName, version, type);
        }else{
            io.mvnpm.npm.model.Package npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            return fileClient.getFileContents(type, npmPackage);
        }
    }
    
    public byte[] getSha1(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            String latestVersion = getLatestVersion(fullName);
            return getSha1(fullName, latestVersion, type);
        }else if(fullName.isInternal()){
            return compositeService.getFileSha1(fullName, version, type);
        }else {
            io.mvnpm.npm.model.Package npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            return fileClient.getFileSha1(type, npmPackage);
        }
    }
    
    public byte[] getMd5(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            String latestVersion = getLatestVersion(fullName);
            return getMd5(fullName, latestVersion, type);
        }else if(fullName.isInternal()){
            return compositeService.getFileMd5(fullName, version, type);    
        }else {
            io.mvnpm.npm.model.Package npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            return fileClient.getFileMd5(type, npmPackage);
        }
    }
    
    public byte[] getAsc(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            String latestVersion = getLatestVersion(fullName);
            return getAsc(fullName, latestVersion, type);
        }else if(fullName.isInternal()){
            return compositeService.getFileAsc(fullName, version, type);        
        }else {
            io.mvnpm.npm.model.Package npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            return fileClient.getFileAsc(type, npmPackage);
        }
    }
    
    private String getLatestVersion(Name fullName){
        Project project = npmRegistryFacade.getProject(fullName.npmFullName());
        return project.distTags().latest();
    }
}
