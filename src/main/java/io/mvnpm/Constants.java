package io.mvnpm;

public interface Constants {

    public static final String OR = "||";
    public static final String OR_ESCAPED = "\\|\\|";
    public static final String SPACE = " ";
    public static final String SPACE_URL_ENCODED = "%20";
    public static final String ZERO = "0";
    public static final String DOT = ".";
    public static final String COMMA = ",";
    public static final String STAR = "*";
    public static final String EX = "x";

    public static final String LESS_THAN = "<";
    public static final String LESS_THAN_OR_EQUAL_TO = "<=";
    public static final String GREATER_THAN = ">";
    public static final String GREATER_THAN_OR_EQUAL_TO = ">=";
    public static final String EQUAL_TO = "=";
    public static final String VEE = "v";

    public static final String OPEN_BLOCK = "[";
    public static final String CLOSE_BLOCK = "]";
    public static final String OPEN_ROUND = "(";
    public static final String CLOSE_ROUND = ")";
    public static final String OPEN_CURLY = "{";
    public static final String CLOSE_CURLY = "}";
    public static final String DOLLAR = "$";

    public static final String DOUBLE_POINT = ":";
    public static final String ESCAPED_DOT = "\\.";
    public static final String HYPHEN = "-";
    public static final String TILDE = "~";
    public static final String CARET = "^";
    public static final String ESCAPED_CARET = "\\^";
    public static final String SLASH = "/";
    public static final String EMPTY = "";
    public static final String SHA1 = "sha1";
    public static final String DOT_SHA1 = DOT + SHA1;

    public static final String MD5 = "md5";
    public static final String DOT_MD5 = DOT + MD5;

    public static final String POM = "pom";
    public static final String DOT_POM = DOT + POM;
    public static final String ASC = "asc";
    public static final String DOT_ASC = DOT + ASC;
    public static final String TGZ = "tgz";
    public static final String DOT_TGZ = DOT + TGZ;
    public static final String JAR = "jar";
    public static final String DOT_JAR = DOT + JAR;

    public static final String DASH_SOURCES_DOT_JAR = "-sources.jar";
    public static final String DASH_JAVADOC_DOT_JAR = "-javadoc.jar";
    public static final String DASH_IMPORTMAP_DOT_JSON = "-importmap.json";

    public static final String AT = "@";
    public static final String AT_SLASH = "at" + SLASH;
    public static final String DOT_AT_DOT = DOT + "at" + DOT;
    public static final String ESCAPED_DOT_AT_DOT = ESCAPED_DOT + "at" + ESCAPED_DOT;
    public static final String DOUBLE_QUOTE = "\"";
    public static final String ESCAPED_ORG_DOT_MVNPM = "org\\.mvnpm";
    public static final String ORG_DOT_MVNPM = "org.mvnpm";
    public static final String ORG_SLASH_MVNPM = "org/mvnpm";
    public static final String SLASH_ORG_SLASH_MVNPM_SLASH = "/org/mvnpm/";
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

    public static final String AVAILABLE_IN_CENTRAL = "available-in-central";
    public static final String STAGED_TO_OSS = "staged-to-oss";
    public static final String STAGED_REPO_ID = "staged-repo-id";
    public static final String TRUE = "true";

    public static final String MAVEN_METADATA_XML = "maven-metadata.xml";
}
