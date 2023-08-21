package io.mvnpm.mavencentral.sync;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.nio.file.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.mvnpm.Constants;
import io.mvnpm.mavencentral.MavenFacade;
import io.mvnpm.npm.model.Name;

/**
 * This sync a package with maven central
 * TODO: Add a Q
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class CentralSyncService {
    @Inject 
    BundleCreator bundleCreator;
    
    @Inject
    MavenFacade mavenFacade;
    
    @ConfigProperty(name = "mvnpm.local-user-directory")
    String localUserDir;
    @ConfigProperty(name = "mvnpm.local-m2-directory", defaultValue = ".m2")
    String localM2Dir;
    
    public Uni<SyncInfo> getSyncInfo(String groupId, String artifactId, String version){
        Uni<Boolean> checkCentral = isAvailable(groupId, artifactId, version);
        Uni<Boolean> checkStaging = isStaged(groupId, artifactId, version);
        
        return Uni.combine()
            .all().unis(checkCentral, checkStaging).combinedWith(
                    listOfResponses -> {
                        return new SyncInfo((Boolean)listOfResponses.get(0), (Boolean)listOfResponses.get(1));
                    }
            );  
    }
    
    public Uni<Boolean> isAvailable(String groupId, String artifactId, String version){
        // First check local cache
        MetadataService metadataService = new MetadataService(localUserDir, localM2Dir, groupId, artifactId, version);
        boolean local = metadataService.isTrue(Constants.AVAILABLE_IN_CENTRAL);
        if(local){
            return Uni.createFrom().item(true);
        }
        // Next try remove
        Uni<Boolean> remote = mavenFacade.isAvailable(groupId, artifactId, version);
        return remote.onItem().transform((t) -> {
            if(t){
                // store cache
                metadataService.set(Constants.AVAILABLE_IN_CENTRAL, Constants.TRUE);
            }
            return t;
        });
    }
    
    public Uni<Boolean> isStaged(String groupId, String artifactId, String version){
        MetadataService metadataService = new MetadataService(localUserDir, localM2Dir, groupId, artifactId, version);
        return Uni.createFrom().item(metadataService.isTrue(Constants.STAGED_TO_OSS));
    }
    
    public Uni<Void> sync(Name name, String version){
        Uni<Path> bundle = bundleCreator.bundle(name, version);
        return bundle.onItem().transformToUni((b) -> {
            return syncBundle(name, version, b);
        });
    }
    
    private Uni<Void> syncBundle(Name name, String version, Path bundlePath){
        Uni<Response> uploadResponse = mavenFacade.upload(bundlePath);
        return uploadResponse.onItem().transformToUni((u) -> {
            if(u.getStatus()==201){
                MetadataService metadataService = new MetadataService(localUserDir, localM2Dir, name.mvnGroupId(), name.mvnArtifactId(), version);
                metadataService.set(Constants.STAGED_TO_OSS, Constants.TRUE);
                return Uni.createFrom().voidItem();
            }else{
                return Uni.createFrom().failure(new RuntimeException("Error uploading bundle " + bundlePath));
            }
        });
    }
    
}
