package org.mvnpm.file;

import org.mvnpm.Constants;

public enum FileType {
    jar, tgz, pom;
    
    public static FileType fromFileName(String fileName){
        if(fileName.endsWith(Constants.DOT_SHA1)){
            fileName = fileName.substring(0, fileName.length() - Constants.DOT_SHA1.length());
        }   
        fileName = fileName.substring(fileName.lastIndexOf(Constants.DOT) + 1);
        return FileType.valueOf(fileName);
    }
}
