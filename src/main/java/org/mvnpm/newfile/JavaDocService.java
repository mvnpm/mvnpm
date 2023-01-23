package org.mvnpm.newfile;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
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

    public void newFileCreated(@ObservesAsync FileStoreEvent fse) {
        String jarFile = fse.fileName();
        if(jarFile.endsWith(Constants.DOT_JAR)){
            String outputFile = jarFile.replace(Constants.DOT_JAR, Constants.DASH_JAVADOC_DOT_JAR);
            boolean ok = createJar(outputFile);
            if(ok){
                FileUtil.sha1(outputFile);
                FileUtil.md5(outputFile);
                FileUtil.asc(outputFile);
            }
        }
    }
    
    private boolean createJar(String outputFile){
        try (FileOutputStream fileOutput = new FileOutputStream(outputFile);
            JarArchiveOutputStream jarOutput = new JarArchiveOutputStream(fileOutput)){
            emptyJar(jarOutput);
            jarOutput.finish();
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
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
