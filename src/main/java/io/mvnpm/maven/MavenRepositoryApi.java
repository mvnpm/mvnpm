package io.mvnpm.maven;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.file.AsyncFile;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.mvnpm.file.FileType;
import io.mvnpm.file.ImportMapUtil;
import io.mvnpm.file.metadata.MetadataAndHash;
import io.mvnpm.file.metadata.MetadataClient;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.Package;

/**
 * The maven repository endpoint
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@Path("/maven2")
public class MavenRepositoryApi {

    @Inject
    MavenRespositoryService mavenRespositoryService;
    
    @Inject
    NpmRegistryFacade npmRegistryFacade;

    @Inject
    MetadataClient metadataClient;
    
    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml")
    @Produces(MediaType.APPLICATION_XML)
    public Uni<Response> getMavenMetadata(@PathParam("ga") String ga){
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        
        Uni<MetadataAndHash> mah = metadataClient.getMetadataAndHash(name);
        return mah.onItem().transform((b) -> {
            return Response.ok(b.data())
                            .build();
        });
    }
    
    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getMavenMetadataSha1(@PathParam("ga") String ga){
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        
        Uni<MetadataAndHash> mah = metadataClient.getMetadataAndHash(name);
        return mah.onItem().transform((s)-> {
            return Response.ok(s.sha1())
                            .build();
        });
    }
    
    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getMavenMetadataMd5(@PathParam("ga") String ga){
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        
        Uni<MetadataAndHash> mah = metadataClient.getMetadataAndHash(name);
        return mah.onItem().transform((s)-> {
            return Response.ok(s.md5())
                            .build();
        });
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}/package.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Package> getPackageJson(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + "/package.json");
        return npmRegistryFacade.getPackage(nameVersionType.name().npmFullName(), nameVersionType.version());
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}/importmap.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getImportMap(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + "/importmap.json");
        Uni<Package> npmPackage = npmRegistryFacade.getPackage(nameVersionType.name().npmFullName(), nameVersionType.version());
        return npmPackage.onItem().transform((p) -> {
            return Response.ok(ImportMapUtil.createImportMap(p)).build();            
        });
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom")
    @Produces(MediaType.APPLICATION_XML)
    public Uni<Response> getPom(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom");
        return toResponse(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getPomSha1(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom.sha1");
        return toSha1Response(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getPomMd5(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom.mp5");
        return toMd5Response(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.pom.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getPomAsc(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt + ".pom.asc");
        return toAscResponse(nameVersionType.name(), nameVersionType.version(), FileType.pom);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar")
    @Produces("application/java-archive")
    public Uni<Response> getJar(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toResponse(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJarSha1(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toSha1Response(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJarMd5(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toMd5Response(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJarAsc(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toAscResponse(nameVersionType.name(), nameVersionType.version(), FileType.jar);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar")
    @Produces("application/java-archive")
    public Uni<Response> getSourcesJar(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toResponse(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getSourcesJarSha1(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toSha1Response(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getSourcesJarMd5(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toMd5Response(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-sources.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getSourcesJarAsc(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toAscResponse(nameVersionType.name(), nameVersionType.version(), FileType.source);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar")
    @Produces("application/java-archive")
    public Uni<Response> getJavadocJar(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toResponse(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJavadocJarSha1(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toSha1Response(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJavadocJarMd5(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toMd5Response(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}-javadoc.jar.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getJavadocJarAsc(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toAscResponse(nameVersionType.name(), nameVersionType.version(), FileType.javadoc);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz")
    @Produces("application/gzip")
    public Uni<Response> getTgz(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toResponse(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.sha1")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getTgzSha1(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toSha1Response(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.md5")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getTgzMd5(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toMd5Response(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}.tgz.asc")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<Response> getTgzAsc(@PathParam("gavt") String gavt){
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        return toAscResponse(nameVersionType.name(), nameVersionType.version(), FileType.tgz);
    }
    
    private Uni<Response> toResponse(Name fullName, String version, FileType type){
        Uni<AsyncFile> file = mavenRespositoryService.getFile(fullName, version, type);
        return fileToResponse(file);
    }
    
    private Uni<Response> toSha1Response(Name fullName, String version, FileType type){
        Uni<AsyncFile> file = mavenRespositoryService.getSha1(fullName, version, type);
        return fileToResponse(file);
    }
     
    private Uni<Response> toMd5Response(Name fullName, String version, FileType type){
        Uni<AsyncFile> file = mavenRespositoryService.getMd5(fullName, version, type);
        return fileToResponse(file);
    }
    
    private Uni<Response> toAscResponse(Name fullName, String version, FileType type){
        Uni<AsyncFile> file = mavenRespositoryService.getAsc(fullName, version, type);
        return fileToResponse(file);
    }
    
    private Uni<Response> fileToResponse(Uni<AsyncFile> file){
        return file.onItem().transformToUni((f)->{
            return Uni.createFrom().item(Response.ok(f).build());
        });
    }
}