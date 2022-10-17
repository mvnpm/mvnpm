package org.mvnpm.maven;

import io.smallrye.mutiny.Uni;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mvnpm.file.FileClient;
import org.mvnpm.file.FileStore;
import org.mvnpm.file.FileType;
import org.mvnpm.npm.NpmRegistryClient;
import org.mvnpm.npm.model.Package;
import org.mvnpm.npm.model.Project;

/**
 * The maven repository endpoint
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * TODO: Add source jar
 */
@Path("/maven2")
public class MavenRepositoryApi {

    @RestClient 
    NpmRegistryClient npmRegistryClient;
    
    @Inject
    FileClient fileClient; 
    
    @Inject 
    FileStore fileStore;
    
    @GET
    @Path("/org/mvnpm/{artifactIdVersionType : (.+)?}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getAny(@PathParam("artifactIdVersionType") String artifactIdVersionType){
        
        String[] parts = artifactIdVersionType.split(URICreator.SLASH);
        
        if(parts.length>3){
            int numberOfPartsInName = ((parts.length - 3)/2) + 1;
            String[] nameParts = new String[numberOfPartsInName];
            for (int i = 0; i < numberOfPartsInName; i++) {
                nameParts[i] = parts[i];
            }
            String name = String.join(URICreator.SLASH, nameParts);
            String version = parts[numberOfPartsInName];
            String filename = parts[parts.length-1];
            if(isSha1Request(filename)){
                filename = filename.substring(0, filename.lastIndexOf(URICreator.DOT));
                String type = filename.substring(filename.lastIndexOf(URICreator.DOT) + 1);
                return getSha1(name, version, FileType.valueOf(type));
            } else {
                String type = filename.substring(filename.lastIndexOf(URICreator.DOT) + 1);
                return getFile(name, version, FileType.valueOf(type));                
            }
        } else if (parts.length == 3) {
            String name = parts[0];
            String version = parts[1];
            String filename = parts[2];
            if(isSha1Request(filename)){
                filename = filename.substring(0, filename.lastIndexOf(URICreator.DOT));
                String type = filename.substring(filename.lastIndexOf(URICreator.DOT) + 1);            
                return getSha1(name, version, FileType.valueOf(type));
            }else{
                String type = filename.substring(filename.lastIndexOf(URICreator.DOT) + 1);            
                return getFile(name, version, FileType.valueOf(type));
            }
        }
        
        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    private boolean isSha1Request(String filename){
        return filename.endsWith(URICreator.DOT + SHA1);
    }
    
    private Uni<Response> getFile(String artifactId, String version, FileType type) {
        if(version.equalsIgnoreCase(LATEST)){
            return redirectToLatest(artifactId, type.name());
        }else {
            Uni<Package> npmPackage = npmRegistryClient.getPackage(NameCreator.toName(artifactId), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                String filename = fileStore.getLocalFileName(type, p);
                return fileClient.streamFile(type, p).onItem().transform((file) -> {
                        return Response.ok(file).header(HEADER_CONTENT_DISPOSITION_KEY, HEADER_CONTENT_DISPOSITION_VALUE + "\"" + filename + "\"")
                            .build();
                });
            }); 
        }
    }
    
    private Uni<Response> getSha1(String artifactId, String version, FileType type) {
        if(version.equalsIgnoreCase(LATEST)){
            return redirectToLatest(artifactId, type.name() + URICreator.DOT + SHA1);
        }else {
            Uni<Package> npmPackage = npmRegistryClient.getPackage(NameCreator.toName(artifactId), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                String filename = fileStore.getLocalSha1FileName(type, p);
                return fileClient.streamSha1(type, p).onItem().transform((file) -> {
                        return Response.ok(file).header(HEADER_CONTENT_DISPOSITION_KEY, HEADER_CONTENT_DISPOSITION_VALUE + "\"" + filename + "\"")
                            .build();
                });
            }); 
        }
    }
    
    private Uni<Response> redirectToLatest(String artifactId, String type){
        Uni<String> version = getLatestVersion(artifactId);
        return version.onItem().transform((latest) -> {
            return Response.temporaryRedirect(URICreator.createURI(artifactId, latest, type)).build();
        });
    }
    
    private Uni<String> getLatestVersion(String artifactId){
        Uni<Project> project = npmRegistryClient.getProject(NameCreator.toName(artifactId));
        return project.onItem()
                .transform((p) -> {
                    return p.distTags().latest();
                });
    }
    
    private static final String SHA1 = "sha1";
    private static final String LATEST = "latest";
    private static final String HEADER_CONTENT_DISPOSITION_KEY = "Content-Disposition";
    private static final String HEADER_CONTENT_DISPOSITION_VALUE = "attachment, filename=";
}