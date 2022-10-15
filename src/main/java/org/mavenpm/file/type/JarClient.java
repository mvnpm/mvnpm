package org.mavenpm.file.type;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.file.AsyncFile;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.mavenpm.file.FileClient;
import org.mavenpm.file.FileStore;
import org.mavenpm.file.FileType;

/**
 * Create the jar from the npm content
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class JarClient {

    @Inject
    Vertx vertx;
    
    @Inject
    FileClient fileClient;
    
    public Uni<AsyncFile> createJar(org.mavenpm.npm.model.Package p, String localFileName){
        // Create tar file name
        
        Uni<AsyncFile> tgzFile = fileClient.streamFile(FileType.tgz, p);
        
        // Get the content
        
        // Create the jar
        
        return vertx.fileSystem().open(localFileName, FileStore.READ_ONLY_OPTIONS);
    }
}
