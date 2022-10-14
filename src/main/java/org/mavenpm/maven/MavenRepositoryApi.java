package org.mavenpm.maven;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import java.io.OutputStream;
import java.net.URL;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mavenpm.maven.xml.PomCreator;
import org.mavenpm.npm.NpmRegistryClient;
import org.mavenpm.file.TarFileClient;
import org.mavenpm.npm.model.Package;
import org.mavenpm.npm.model.Project;

/**
 * The maven repository endpoint
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * TODO: Cache
 */
@Path("/mvn")
public class MavenRepositoryApi {

    @RestClient 
    NpmRegistryClient extensionsService;
    
    @Inject
    PomCreator pomCreator;
    
    @Inject
    TarFileClient tarFileClient; 
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.pom")
    @Produces(MediaType.APPLICATION_XML)
    public Uni<org.mavenpm.npm.model.Package> getPom(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version) {
        
        if(version.equalsIgnoreCase(LATEST)){
            Uni<Project> project = extensionsService.getProject(artifactId);
            return project.onItem()
                    .transformToUni(p -> getPom(artifactId, p.distTags().latest()));
        }else {
            Log.info("Fetching " + artifactId + " " + version + "package details from NPM (pom)");
            return extensionsService.getPackage(artifactId, version);
        }
    }
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.pom.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> getPomSha1(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version) {
        
        if(version.equalsIgnoreCase(LATEST)){
            Uni<Project> project = extensionsService.getProject(artifactId);
            return project.onItem()
                    .transformToUni(p -> getPomSha1(artifactId, p.distTags().latest()));
        }else {
            Uni<Package> npmPackage = extensionsService.getPackage(artifactId, version);
            return npmPackage.onItem().transform(p -> pomCreator.pomSha1(p));
        }
    }
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.tgz")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getTgz(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version) {
        
        if(version.equalsIgnoreCase(LATEST)){
            Uni<Project> project = extensionsService.getProject(artifactId);
            return project.onItem()
                    .transformToUni(p -> getTgz(artifactId, p.distTags().latest()));
        }else {
            Log.info("Fetching " + artifactId + " " + version + " package source from NPM (tgz)");
            Uni<Package> npmPackage = extensionsService.getPackage(artifactId, version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                String filename = tarFileClient.getLocalTgzFileName(p);
                return tarFileClient.streamTgz(p).onItem().transform((file) -> {
                        return Response.ok(file).header(HEADER_CONTENT_DISPOSITION_KEY, HEADER_CONTENT_DISPOSITION_VALUE + "\"" + filename + "\"")
                            .build();
                });
            }); 
        }
    }
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.tgz.sha1")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getTgzSha1(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version) {
        
        if(version.equalsIgnoreCase(LATEST)){
            Uni<Project> project = extensionsService.getProject(artifactId);
            return project.onItem()
                    .transformToUni(p -> getTgz(artifactId, p.distTags().latest()));
        }else {
            Log.info("Fetching " + artifactId + " " + version + " package source sha1 from NPM (tgz.sha1)");
            Uni<Package> npmPackage = extensionsService.getPackage(artifactId, version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                String filename = tarFileClient.getLocalSha1FileName(p);
                return tarFileClient.streamSha1(p).onItem().transform((file) -> {
                        return Response.ok(file).header(HEADER_CONTENT_DISPOSITION_KEY, HEADER_CONTENT_DISPOSITION_VALUE + "\"" + filename + "\"")
                            .build();
                });
            }); 
        }
    }
    
    @GET
    @Path("/org/mavenpm/{artifactId}/{version}/{artifactId}-{version}.jar")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<StreamingOutput> getJar(@PathParam("artifactId") String artifactId, 
                           @DefaultValue(LATEST) @PathParam("version") String version) {
        
        if(version.equalsIgnoreCase(LATEST)){
            Uni<Project> project = extensionsService.getProject(artifactId);
            return project.onItem()
                    .transformToUni(p -> getJar(artifactId, p.distTags().latest()));
        }else {
            Log.info("Fetching " + artifactId + " " + version + "package content from NPM (jar)");
            
            Uni<Package> npmPackage = extensionsService.getPackage(artifactId, version);
            return npmPackage.onItem().transform(p -> jar(p));
        }
    }
    
    // TODO: Jar sha1
    
    
    
    private StreamingOutput jar(org.mavenpm.npm.model.Package p){
        
        URL tarball = p.dist().tarball();
        
        
        return (final OutputStream os) -> {  
            
        };
    }
    
    private static final String LATEST = "latest";
    
    private static final String HEADER_CONTENT_DISPOSITION_KEY = "Content-Disposition";
    private static final String HEADER_CONTENT_DISPOSITION_VALUE = "attachment, filename=";
//    private static final String DASH = "-";
//    private static final String DOT = ".";
//    private static final String TAR = "tar";
}