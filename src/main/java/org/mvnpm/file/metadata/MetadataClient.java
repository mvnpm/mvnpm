package org.mvnpm.file.metadata;

import com.github.villadora.semver.SemVer;
import com.github.villadora.semver.Version;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.smallrye.mutiny.Uni;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.mvnpm.file.Sha1Util;
import org.mvnpm.npm.NpmRegistryFacade;
import org.mvnpm.npm.model.Name;
import org.mvnpm.npm.model.Project;

/**
 * Creates a maven-metadata.xml from the NPM Project
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class MetadataClient {
    private final MetadataXpp3Writer metadataXpp3Writer = new MetadataXpp3Writer();
    
    @Inject
    NpmRegistryFacade npmRegistryFacade;
    
    @Inject 
    @CacheName("metadata-cache")
    Cache cache;
    
    public Uni<MetadataAndSha> getMetadataAndSha(Name name){
        CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        if(caffeineCache.keySet().contains(name.npmFullName())){
            CompletableFuture<MetadataAndSha> completableFuture = caffeineCache.getIfPresent(name.npmFullName());
            return Uni.createFrom().completionStage(completableFuture);
        }else{
            Uni<Metadata> metadata = getMetadata(name);
            return metadata.onItem().transformToUni((Metadata m) -> {
                try(StringWriter sw = new StringWriter()){
                    metadataXpp3Writer.write(sw, m);
                    byte[] value = sw.toString().getBytes();
                    String sha1 = Sha1Util.sha1(value);
                    MetadataAndSha mas = new MetadataAndSha(sha1, value);
                    caffeineCache.put(name.npmFullName(), CompletableFuture.completedFuture(mas));
                    return Uni.createFrom().item(mas);
                } catch (IOException ex) {
                    return Uni.createFrom().failure(ex);
                }
            });   
        }
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
    
    public Uni<Versioning> getVersioning(Name name) {
        Uni<Project> project = npmRegistryFacade.getProject(name.npmFullName());
        return project.onItem().transform((p) -> {
            
            Versioning versioning = new Versioning();
            String latest = getLatest(p);
            versioning.setLatest(latest);
            versioning.setRelease(latest);
        
            for(String v:p.versions()){
                // Here ignore invalid and unreleased version 
                if(SemVer.valid(v)){
                    Version semver = SemVer.version(v);
                    String prerelease = semver.getPrerelease();
                    if(prerelease==null || prerelease.isEmpty()){
                        versioning.addVersion(v);
                    }
                }
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
