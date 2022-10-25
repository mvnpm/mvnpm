package org.mvnpm.maven;

import io.smallrye.mutiny.Uni;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.mvnpm.Constants;
import org.mvnpm.file.FileClient;
import org.mvnpm.file.FileStore;
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
    FileStore fileStore;
    
    @Inject
    MetadataClient metadataClient;
    
    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getMavenMetadata(@PathParam("ga") String ga){
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        
        Uni<MetadataAndSha> mas = metadataClient.getMetadataAndSha(name);
        return mas.onItem().transform((b) -> {
            return Response.ok(b.data()).header(Constants.HEADER_CONTENT_DISPOSITION_KEY, Constants.HEADER_CONTENT_DISPOSITION_VALUE + Constants.DOUBLE_QUOTE + MAVEN_META_DATA_XML + Constants.DOUBLE_QUOTE)
                            .build();
        });
        
    }
    
    @GET
    @Path("/org/mvnpm/{ga : (.+)?}/maven-metadata.xml.sha1")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getMavenMetadataSha1(@PathParam("ga") String ga){
        Name name = UrlPathParser.parseMavenMetaDataXml(ga);
        
        Uni<MetadataAndSha> mas = metadataClient.getMetadataAndSha(name);
        return mas.onItem().transform((s)-> {
            return Response.ok(s.sha1()).header(Constants.HEADER_CONTENT_DISPOSITION_KEY, Constants.HEADER_CONTENT_DISPOSITION_VALUE + Constants.DOUBLE_QUOTE + MAVEN_META_DATA_XML + Constants.SHA1 + Constants.DOUBLE_QUOTE)
                            .build();
        });
    }
    
    @GET
    @Path("/org/mvnpm/{gavt : (.+)?}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Uni<Response> getAny(@PathParam("gavt") String gavt){
        
        NameVersionType nameVersionType = UrlPathParser.parseMavenFile(gavt);
        
        if(nameVersionType.sha1()){
            return getSha1(nameVersionType.name(), nameVersionType.version(), nameVersionType.type());
        } else {
            return getFile(nameVersionType.name(), nameVersionType.version(), nameVersionType.type());                
        }
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
                String filename = fileStore.getLocalFileName(type, p);
                return fileClient.streamFile(type, p).onItem().transform((file) -> {
                        return Response.ok(file).header(Constants.HEADER_CONTENT_DISPOSITION_KEY, Constants.HEADER_CONTENT_DISPOSITION_VALUE + Constants.DOUBLE_QUOTE + filename + Constants.DOUBLE_QUOTE)
                            .build();
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
                String filename = fileStore.getLocalSha1FileName(type, p);
                return fileClient.streamSha1(type, p).onItem().transform((file) -> {
                        return Response.ok(file).header(Constants.HEADER_CONTENT_DISPOSITION_KEY, Constants.HEADER_CONTENT_DISPOSITION_VALUE + Constants.DOUBLE_QUOTE + filename + Constants.DOUBLE_QUOTE)
                            .build();
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
    
    private static final String MAVEN_META_DATA_XML = "maven-metadata.xml";
}