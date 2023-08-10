package org.mvnpm.centralsync;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.mvnpm.Constants;

/**
 * This scans the .m2 folder for mvnpm artifacts
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class M2Scanner {
    private static final List<String> IGNORE_LIST = List.of("centralsync","mvnpm","importmap"); // Our internal apps
    
    @ConfigProperty(name = "mvnpm.local-user-directory")
    private String localUserDir;
    @ConfigProperty(name = "mvnpm.local-m2-directory")
    private String localM2Dir;
    
    
    public List<ArtifactInfo> scan() {
        String mvnpmPath = localUserDir + "/" + localM2Dir + "/repository/org/mvnpm/";
        try {
            return scanM2Folder(mvnpmPath);
        } catch (InvalidVersionSpecificationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private List<ArtifactInfo> scanM2Folder(String m2FolderPath) throws InvalidVersionSpecificationException {
        List<ArtifactInfo> matchedArtifacts = new ArrayList<>();
        Map<String, ArtifactInfo> artifactMap = new HashMap<>();

        File m2Folder = new File(m2FolderPath);
        
        List<File> pomFiles = listFilesWithExtension(m2Folder);
        for (File pomFile : pomFiles) {

            MavenXpp3Reader mavenreader = new MavenXpp3Reader();
            try {
                FileReader reader = new FileReader(pomFile);
                Model model = mavenreader.read(reader);
                model.setPomFile(pomFile);

                String groupId = model.getGroupId();
                String artifactId = model.getArtifactId();
                String version = model.getVersion();
                String key = groupId + Constants.SLASH + artifactId;
                if (!shouldIgnore(groupId, artifactId) && !artifactMap.containsKey(key) || isNewerVersion(version, artifactMap.get(key).getVersion())) {
                    ArtifactInfo artifact = new ArtifactInfo(groupId, artifactId, version);
                    artifactMap.put(key, artifact);
                }
            }catch(IOException | InvalidVersionSpecificationException | XmlPullParserException ex){
                throw new RuntimeException(ex);
            }
        }

        matchedArtifacts.addAll(artifactMap.values());
        
        return matchedArtifacts;
    }

    private boolean shouldIgnore(String groupId,String artifactId){
        return groupId.equals(Constants.ORG_DOT_MVNPM) && IGNORE_LIST.contains(artifactId);
    }
    
    private List<File> listFilesWithExtension(File folder) {
        List<File> result = new ArrayList<>();
        if (folder == null || !folder.isDirectory()) {
            return result;
        }
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                result.addAll(listFilesWithExtension(file));
            } else {
                String name = file.getName();
                if (file.getName().endsWith(Constants.DOT_POM)) {
                    // Check if the jar is also available
                    Path jarFile = Paths.get(file.getParent(), name.replace(Constants.DOT_POM,Constants.DOT_JAR));
                    if(Files.exists(jarFile)){
                        result.add(file);
                    }
                    break;
                }
            }
        }
        return result;
    }
    
    private boolean isNewerVersion(String newVersion, String existingVersion) throws InvalidVersionSpecificationException {
        VersionRange range = VersionRange.createFromVersionSpec(existingVersion);
        return range.containsVersion(new DefaultArtifactVersion(newVersion));
    }

    static class ArtifactInfo {
        private final String groupId;
        private final String artifactId;
        private final String version;
        
        public ArtifactInfo(String groupId, String artifactId, String version) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }
        
        public String getVersion(){
            return version;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 59 * hash + Objects.hashCode(this.groupId);
            hash = 59 * hash + Objects.hashCode(this.artifactId);
            hash = 59 * hash + Objects.hashCode(this.version);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ArtifactInfo other = (ArtifactInfo) obj;
            if (!Objects.equals(this.groupId, other.groupId)) {
                return false;
            }
            if (!Objects.equals(this.artifactId, other.artifactId)) {
                return false;
            }
            return Objects.equals(this.version, other.version);
        }

        @Override
        public String toString() {
            return "ArtifactInfo{" + "groupId=" + groupId + ", artifactId=" + artifactId + ", version=" + version + '}';
        }
        
    }
}
