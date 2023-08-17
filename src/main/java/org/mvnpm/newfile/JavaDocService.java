package org.mvnpm.newfile;

import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.mvnpm.Constants;

import org.mvnpm.file.FileStoreEvent;
import org.mvnpm.file.FileUtil;

/**
 * Create a dummy javadoc file
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class JavaDocService {

    @ConsumeEvent("new-file-created")
    public void newFileCreated(FileStoreEvent fse) {
        if(fse.fileName().endsWith(Constants.DOT_JAR)){
            String jarFile = fse.fileName();
            String outputFile = jarFile.replace(Constants.DOT_JAR, Constants.DASH_JAVADOC_DOT_JAR);
            boolean ok = createJar(outputFile);
            if(ok){
                FileUtil.createSha1(outputFile);
                // TODO: Rather fire event again ?
                FileUtil.createMd5(outputFile);
                FileUtil.createAsc(outputFile);
            }
            Log.info("javadoc created " + fse.fileName() + "[" + ok + "]");
        }
    }
    
    private boolean createJar(String outputFile){
        Path f = Paths.get(outputFile);
        if(!Files.exists(f)){
            synchronized (f) {
                try (OutputStream fileOutput = Files.newOutputStream(f);
                    JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(fileOutput)){
                    emptyJar(jarOutput);
                    jarOutput.finish();
                    return true;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return false;
    }
    
    private void emptyJar(JarArchiveOutputStream jarOutput) throws IOException {
        JarArchiveEntry entry = new JarArchiveEntry("README.md");
        byte[] filecontents = CONTENTS.getBytes();
        entry.setSize(filecontents.length);
        jarOutput.putArchiveEntry(entry);
        jarOutput.write(filecontents);
        jarOutput.closeArchiveEntry();
    }
    
    private static final String CONTENTS = "No JavaDoc since this is a JavaScript Project";

}
