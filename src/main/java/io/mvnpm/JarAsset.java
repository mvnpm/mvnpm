package io.mvnpm;

import java.util.List;

public class JarAsset {
    private String name;
    private List<JarAsset> children;
    private boolean fileAsset;
    private String urlPart;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<JarAsset> getChildren() {
        return children;
    }

    public void setChildren(List<JarAsset> children) {
        this.children = children;
    }

    public boolean isFileAsset() {
        return fileAsset;
    }

    public void setFileAsset(boolean fileAsset) {
        this.fileAsset = fileAsset;
    }

    public String getUrlPart() {
        return urlPart;
    }

    public void setUrlPart(String urlPart) {
        this.urlPart = urlPart;
    }

}
