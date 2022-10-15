package org.mavenpm.maven;

import io.smallrye.mutiny.Uni;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mavenpm.file.FileClient;
import org.mavenpm.file.FileStore;
import org.mavenpm.file.FileType;
import org.mavenpm.npm.NpmRegistryClient;
import org.mavenpm.npm.model.Package;
import org.mavenpm.npm.model.Project;

/**
 * The maven repository endpoint
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * TODO: Add source jar
 */
@Path("/mvn")
public class MavenRepositoryApi {

    @RestClient 
    NpmRegistryClient extensionsService;
    
    @Inject
    FileClient fileClient; 
    
    @Inject 
    FileStore fileStore;
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.tgz")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getTgz(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version){
        return getFile(artifactId, version, FileType.tgz);
    }
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.tgz.sha1")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getTgzSha1(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version){
        return getSha1(artifactId, version, FileType.tgz);
    }
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.jar")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getJar(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version){
        return getFile(artifactId, version, FileType.jar);
    }
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.jar.sha1")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getJarSha1(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version){
        return getSha1(artifactId, version, FileType.jar);
    }
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.pom")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getPom(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version){
        return getFile(artifactId, version, FileType.pom);
    }
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.pom.sha1")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getPomSha1(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version){
        return getSha1(artifactId, version, FileType.pom);
    }
    
    private Uni<Response> getFile(String artifactId, String version, FileType type) {
        if(version.equalsIgnoreCase(LATEST)){
            Uni<Project> project = extensionsService.getProject(artifactId);
            return project.onItem()
                    .transformToUni(p -> getFile(artifactId, p.distTags().latest(), type));
        }else {
            Uni<Package> npmPackage = extensionsService.getPackage(artifactId, version);
            
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
            Uni<Project> project = extensionsService.getProject(artifactId);
            return project.onItem()
                    .transformToUni(p -> getFile(artifactId, p.distTags().latest(), type));
        }else {
            Uni<Package> npmPackage = extensionsService.getPackage(artifactId, version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                String filename = fileStore.getLocalSha1FileName(type, p);
                return fileClient.streamSha1(type, p).onItem().transform((file) -> {
                        return Response.ok(file).header(HEADER_CONTENT_DISPOSITION_KEY, HEADER_CONTENT_DISPOSITION_VALUE + "\"" + filename + "\"")
                            .build();
                });
            }); 
        }
    }
    
    private static final String LATEST = "latest";
    private static final String HEADER_CONTENT_DISPOSITION_KEY = "Content-Disposition";
    private static final String HEADER_CONTENT_DISPOSITION_VALUE = "attachment, filename=";
}