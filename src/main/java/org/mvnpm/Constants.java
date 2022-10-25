package org.mvnpm;

public interface Constants {

    public static final String DOT = ".";
    public static final String DOUBLE_POINT = ":";
    public static final String COMMA = ",";
    public static final String ESCAPED_DOT = "\\.";
    public static final String DASH = "-";
    public static final String SPACE = " ";
    public static final String SLASH = "/";
    public static final String EMPTY = "";
    public static final String SHA1 = "sha1";
    public static final String DOT_SHA1 = DOT + SHA1;
    
    public static final String AT = "@";
    public static final String AT_SLASH = "at" + SLASH;
    public static final String DOT_AT_DOT = DOT + "at" + DOT;
    
    public static final String DOUBLE_QUOTE = "\"";
    public static final String ORG_DOT_MVNPM = "org.mvnpm";
    public static final String IMPORTMAP = "importmap";
    public static final String VERSION = "version";
    public static final String GROUP_ID = "groupId";
    public static final String ARTIFACT_ID = "artifactId";
    
    public static final String HTTPS = "https";
    public static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
    public static final String UNKNOWN = "unknown";
    
    public static final String LATEST = "latest"; // Will find the latest 
    public static final String ZERO_ZERO_ONE = "0.0.1";
    
    public static final String HEADER_CONTENT_DISPOSITION_KEY = "Content-Disposition";
    public static final String HEADER_CONTENT_DISPOSITION_VALUE = "attachment, filename=";
    
    public static final String REPOSITORY = "repository";
    public static final String USER_HOME = "user.home";
    public static final String CACHE_DIR = System.getProperty(USER_HOME);
}
