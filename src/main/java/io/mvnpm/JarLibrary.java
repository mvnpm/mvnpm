package io.mvnpm;

/**
 * Represent the contents of a Jar Library
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class JarLibrary {
    private final String jarName;
    private String version;
    private String type;
    private JarAsset rootAsset;

    public JarLibrary(String jarName) {
        this.jarName = jarName;
    }

    public String getJarName() {
        return jarName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public JarAsset getRootAsset() {
        return rootAsset;
    }

    public void setRootAsset(JarAsset rootAsset) {
        this.rootAsset = rootAsset;
    }
}
