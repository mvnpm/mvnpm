package org.mvnpm.maven;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mvnpm.Constants;
import org.mvnpm.file.FileClient;
import org.mvnpm.file.FileType;
import org.mvnpm.file.metadata.MetadataAndSha;
import org.mvnpm.file.metadata.MetadataClient;
import org.mvnpm.npm.NpmRegistryFacade;
import org.mvnpm.npm.model.Name;
import org.mvnpm.npm.model.Package;
import org.mvnpm.npm.model.Project;

/**
 * The maven repository endpoint
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 * TODO: Add source jar
 */
@Path("/maven2")
public class MavenRepositoryApi {

    @Inject
    NpmRegistryFacade npmRegistryFacade;
    
    @Inject
    FileClient fileClient; 
    
    @Inject
    MetadataClient metadataClient;
    
    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml")
    @Produces(MediaType.APPLICATION_XML)
    public Uni<Response> getMavenMetadata(@PathParam("ga") String ga){
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        
        Uni<MetadataAndSha> mas = metadataClient.getMetadataAndSha(name);
        return mas.onItem().transform((b) -> {
            return Response.ok(b.data())
                            .build();
        });
    }
    
    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getMavenMetadataSha1(@PathParam("ga") String ga){
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        
        Uni<MetadataAndSha> mas = metadataClient.getMetadataAndSha(name);
        return mas.onItem().transform((s)-> {
            return Response.ok(s.sha1())
                            .build();
        });
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom")
    @Produces(MediaType.APPLICATION_XML)
    public Uni<Response> getPom(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom");
        return getFile(nameVersionType.name(), nameVersionType.version(), FileType.pom);                
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getPomSha1(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom.sha1");
        return getSha1(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getPomMd5(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom.mp5");
        return getMd5(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getPomAsc(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom.asc");
        return getAsc(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar")
    @Produces("application/java-archive")
    public Uni<Response> getJar(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getFile(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJarSha1(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getSha1(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJarMd5(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getMd5(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJarAsc(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getAsc(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar")
    @Produces("application/java-archive")
    public Uni<Response> getSourcesJar(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getFile(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getSourcesJarSha1(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getSha1(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getSourcesJarMd5(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getMd5(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getSourcesJarAsc(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getAsc(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar")
    @Produces("application/java-archive")
    public Uni<Response> getJavadocJar(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getFile(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJavadocJarSha1(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getSha1(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJavadocJarMd5(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getMd5(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJavadocJarAsc(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getAsc(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz")
    @Produces("application/gzip")
    public Uni<Response> getTgz(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getFile(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getTgzSha1(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getSha1(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getTgzMd5(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getMd5(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getTgzAsc(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return getAsc(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }
    
    private Uni<Response> getFile(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            Uni<String> latestVersion = getLatestVersion(fullName);
            return latestVersion.onItem().transformToUni((latest)->{
                return getFile(fullName, latest, type);
            });
        }else {
            Uni<Package> npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                return fileClient.streamFile(type, p).onItem().transform((file) -> {
                    return Response.ok(file).build();
                });
            }); 
        }
    }
    
    private Uni<Response> getSha1(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            Uni<String> latestVersion = getLatestVersion(fullName);
            return latestVersion.onItem().transformToUni((latest)->{
                return getSha1(fullName, latest, type);
            });
        }else {
            Uni<Package> npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                return fileClient.streamSha1(type, p).onItem().transform((file) -> {
                    return Response.ok(file).build();
                });
            }); 
        }
    }
    
    private Uni<Response> getMd5(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            Uni<String> latestVersion = getLatestVersion(fullName);
            return latestVersion.onItem().transformToUni((latest)->{
                return getMd5(fullName, latest, type);
            });
        }else {
            Uni<Package> npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                return fileClient.streamMd5(type, p).onItem().transform((file) -> {
                    return Response.ok(file).build();
                });
            }); 
        }
    }
    
    private Uni<Response> getAsc(Name fullName, String version, FileType type) {
        if(version.equalsIgnoreCase(Constants.LATEST)){
            Uni<String> latestVersion = getLatestVersion(fullName);
            return latestVersion.onItem().transformToUni((latest)->{
                return getAsc(fullName, latest, type);
            });
        }else {
            Uni<Package> npmPackage = npmRegistryFacade.getPackage(fullName.npmFullName(), version);
            
            return npmPackage.onItem().transformToUni((p) -> {
                return fileClient.streamAsc(type, p).onItem().transform((file) -> {
                    return Response.ok(file).build();
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