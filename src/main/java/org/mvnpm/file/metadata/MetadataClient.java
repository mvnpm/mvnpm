package org.mvnpm.file.metadata;

import io.smallrye.mutiny.Uni;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.enterprise.context.ApplicationScoped;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.mvnpm.file.Sha1Util;
import org.mvnpm.npm.NpmRegistryClient;
import org.mvnpm.npm.model.Name;
import org.mvnpm.npm.model.Project;

/**
 * Creates a maven-metadata.xml from the NPM Project
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MetadataClient {
    private final MetadataXpp3Writer metadataXpp3Writer = new MetadataXpp3Writer();
    
    @RestClient 
    NpmRegistryClient extensionsService;
    
    // TODO: cache
    
    public Uni<String> getMetadataSha1(Name name){
        Uni<byte[]> metadataString = getMetadataBytes(name);
        return metadataString.onItem().transform((s -> {
            return Sha1Util.sha1(s);
        }));
    }
    
    public Uni<byte[]> getMetadataBytes(Name name){
        Uni<Metadata> metadata = getMetadata(name);
        return metadata.onItem().transformToUni((m) -> {
            try(StringWriter sw = new StringWriter()){
               metadataXpp3Writer.write(sw, m);
               return Uni.createFrom().item(sw.toString().getBytes());
            } catch (IOException ex) {
                return Uni.createFrom().failure(ex);
            }
        });   
    }
    
    private Uni<Metadata> getMetadata(Name name){
        Uni<Versioning> versioning = getVersioning(name);
        return versioning.onItem().transform((v) -> {
            
            Metadata metadata = new Metadata();
            metadata.setGroupId(name.mvnGroupId());
            metadata.setArtifactId(name.mvnArtifactId());
            metadata.setVersioning(v);

            return metadata;
        });
    }
    
    private Uni<Versioning> getVersioning(Name name) {
        Uni<Project> project = extensionsService.getProject(name.npmFullName());
        return project.onItem().transform((p) -> {
            
            Versioning versioning = new Versioning();
            String latest = getLatest(p);
            versioning.setLatest(latest);
            versioning.setRelease(latest);
        
            for(String v:p.versions()){
                versioning.addVersion(v);
            }
        
            String timestamp = new SimpleDateFormat(TIME_STAMP_FORMAT).format(new Date());
            versioning.setLastUpdated(timestamp);
        
            return versioning;
        });
        
    }
    
    private String getLatest(Project p){
        return p.distTags().latest();
    }
    
    private static final String TIME_STAMP_FORMAT = "yyyyMMddHHmmss";
    
}
