package org.mvnpm.centralsync;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.file.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mvnpm.Constants;
import org.mvnpm.npm.model.Name;

/**
 * This sync a package with maven central
 * TODO: Add a Q
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class CentralSyncer {
    @Inject 
    BundleCreator bundleCreator;
    
    @Inject
    NexusUploader nexusUploader;
    
    @ConfigProperty(name = "mvnpm.local-user-directory")
    String localUserDir;
    @ConfigProperty(name = "mvnpm.local-m2-directory", defaultValue = ".m2")
    String localM2Dir;
    
    
    public void sync(Name name, String version){
        Path bundle = bundleCreator.bundle(name, version);
        boolean ok = nexusUploader.upload(bundle);
        if(ok){
            MetadataService metadataService = new MetadataService(localUserDir, localM2Dir, name.mvnGroupId(), name.mvnArtifactId(), version);
            metadataService.set(Constants.STAGED_TO_OSS, Constants.TRUE);
        }
    }
}
