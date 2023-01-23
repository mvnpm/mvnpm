package org.mvnpm.file;

import org.mvnpm.Constants;

public enum FileType {
    jar, tgz, pom, source, javadoc;
    
    public static FileType fromFileName(String fileName){
        if(fileName.endsWith(Constants.DOT_SHA1)){
            fileName = fileName.substring(0, fileName.length() - Constants.DOT_SHA1.length());
        }else if(fileName.endsWith(Constants.DOT_ASC)){
            fileName = fileName.substring(0, fileName.length() - Constants.DOT_ASC.length());
        }
        fileName = fileName.substring(fileName.lastIndexOf(Constants.DOT) + 1);
        return FileType.valueOf(fileName);
    }
    
    public String getPostString(){
        if(this.equals(FileType.source)){
            return Constants.DASH_SOURCES_DOT_JAR;
        }
        if(this.equals(FileType.javadoc)){
            return Constants.DASH_JAVADOC_DOT_JAR;
        }
        return Constants.DOT + this.name();
    }
}
