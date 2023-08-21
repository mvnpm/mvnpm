package io.mvnpm.file.metadata;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import io.mvnpm.Constants;
import io.mvnpm.file.FileUtil;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.Project;
import io.mvnpm.version.InvalidVersionException;
import io.mvnpm.version.Version;

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
    
    public Uni<MetadataAndHash> getMetadataAndHash(Name name){
        CaffeineCache caffeineCache = cache.as(CaffeineCache.class);
        if(caffeineCache.keySet().contains(name.npmFullName())){
            CompletableFuture<MetadataAndHash> completableFuture = caffeineCache.getIfPresent(name.npmFullName());
            return Uni.createFrom().completionStage(completableFuture);
        }else{
            Uni<Metadata> metadata = getMetadata(name);
            return metadata.onItem().transformToUni((Metadata m) -> {
                try(StringWriter sw = new StringWriter()){
                    metadataXpp3Writer.write(sw, m);
                    byte[] value = sw.toString().getBytes();
                    String sha1 = FileUtil.getSha1(value);
                    String md5 = FileUtil.getMd5(value);
                    //String asc = FileUtil.readAsc();
                    MetadataAndHash mah = new MetadataAndHash(sha1, md5, null, value);
                    caffeineCache.put(name.npmFullName(), CompletableFuture.completedFuture(mah));
                    return Uni.createFrom().item(mah);
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
        
            for(String version:p.versions()){
                try {
                    Version v = Version.fromString(version);
                    if(!versioning.getVersions().contains(v.toString())){
                        // Ignore pre release
                        if(v.qualifier()==null){
                            versioning.addVersion(v.toString());
                        }       
                    }
                }catch(InvalidVersionException ive){
                    Log.warn("Ignoring version [" + ive.getVersion() + "] for " + name.displayName());
                }
            }
            
            Map<String, String> time = p.time();
            if(time!=null && time.containsKey(MODIFIED)){
                String dateTime = time.get(MODIFIED);
                // 2022-07-20T09:14:55.450Z
                dateTime = dateTime.replaceAll(Constants.HYPHEN, Constants.EMPTY);
                // 20220720T09:14:55.450Z
                dateTime = dateTime.replaceAll("T", Constants.EMPTY);
                // 2022072009:14:55.450Z
                dateTime = dateTime.replaceAll(Constants.DOUBLE_POINT, Constants.EMPTY);
                // 20220720091455.450Z
                if(dateTime.contains(Constants.DOT)){
                    int i = dateTime.indexOf(Constants.DOT);
                    dateTime = dateTime.substring(0, i);
                }
                versioning.setLastUpdated(dateTime);
            }else{    
                String timestamp = new SimpleDateFormat(TIME_STAMP_FORMAT).format(new Date());
                versioning.setLastUpdated(timestamp);
            }
        
            return versioning;
        });
        
    }
    
    private String getLatest(Project p){
        return p.distTags().latest();
    }
    
    private static final String TIME_STAMP_FORMAT = "yyyyMMddHHmmss";
    private static final String MODIFIED = "modified";
}
