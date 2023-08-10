package org.mvnpm.centralsync;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    
    public void sync(Name name, String version){
        String bundle = bundleCreator.bundle(name, version);
        nexusUploader.upload(bundle);
    }
}
