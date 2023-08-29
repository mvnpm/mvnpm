package io.mvnpm.composite;

import io.mvnpm.file.FileType;
import io.mvnpm.npm.model.Name;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CompositeService {

    @Inject
    CompositeCreator compositeCreator;
    
    public byte[] getFile(Name fullName, String version, FileType type) {
        // TODO: Handle latest version
        return compositeCreator.buildComposite(fullName.mvnArtifactId(), version);
    }
    
}
