package io.mvnpm;

public interface Constants {

    String OR = "||";
    String OR_ESCAPED = "\\|\\|";
    String SPACE = " ";
    String SPACE_URL_ENCODED = "%20";
    String ZERO = "0";
    String DOT = ".";
    String COMMA = ",";
    String STAR = "*";
    String EX = "x";

    String LESS_THAN = "<";
    String LESS_THAN_OR_EQUAL_TO = "<=";
    String GREATER_THAN = ">";
    String GREATER_THAN_OR_EQUAL_TO = ">=";
    String EQUAL_TO = "=";
    String VEE = "v";

    String OPEN_BLOCK = "[";
    String CLOSE_BLOCK = "]";
    String OPEN_ROUND = "(";
    String CLOSE_ROUND = ")";
    String OPEN_CURLY = "{";
    String CLOSE_CURLY = "}";
    String DOLLAR = "$";

    String DOUBLE_POINT = ":";
    String ESCAPED_DOT = "\\.";
    String HYPHEN = "-";
    String TILDE = "~";
    String CARET = "^";
    String ESCAPED_CARET = "\\^";
    String SLASH = "/";
    String EMPTY = "";
    String SHA1 = "sha1";
    String DOT_SHA1 = DOT + SHA1;

    String MD5 = "md5";
    String DOT_MD5 = DOT + MD5;

    String POM = "pom";
    String DOT_POM = DOT + POM;
    String ASC = "asc";
    String DOT_ASC = DOT + ASC;
    String TGZ = "tgz";
    String DOT_TGZ = DOT + TGZ;
    String JAR = "jar";
    String DOT_JAR = DOT + JAR;

    String DASH_SOURCES_DOT_JAR = "-sources.jar";
    String DASH_JAVADOC_DOT_JAR = "-javadoc.jar";
    String DASH_IMPORTMAP_DOT_JSON = "-importmap.json";

    String AT = "@";
    String AT_SLASH = "at" + SLASH;
    String DOT_AT_DOT = DOT + "at" + DOT;
    String ESCAPED_DOT_AT_DOT = ESCAPED_DOT + "at" + ESCAPED_DOT;
    String DOUBLE_QUOTE = "\"";
    String ESCAPED_ORG_DOT_MVNPM = "org\\.mvnpm";
    String ORG_DOT_MVNPM = "org.mvnpm";
    String ORG_SLASH_MVNPM = "org/mvnpm";
    String SLASH_ORG_SLASH_MVNPM_SLASH = "/org/mvnpm/";
    String VERSION = "version";
    String GROUP_ID = "groupId";
    String ARTIFACT_ID = "artifactId";

    String HTTPS = "https";
    String X_FORWARDED_FOR = "X-FORWARDED-FOR";
    String UNKNOWN = "unknown";

    String LATEST = "latest"; // Will find the latest
    String ZERO_ZERO_ONE = "0.0.1";

    String HEADER_CONTENT_DISPOSITION_KEY = "Content-Disposition";
    String HEADER_CONTENT_DISPOSITION_VALUE = "attachment, filename=";

    String REPOSITORY = "repository";
    String USER_HOME = "user.home";
    String CACHE_DIR = System.getProperty(USER_HOME);

    String AVAILABLE_IN_CENTRAL = "available-in-central";
    String STAGED_TO_OSS = "staged-to-oss";
    String STAGED_REPO_ID = "staged-repo-id";
    String TRUE = "true";
    String MAVEN_METADATA_XML = "maven-metadata.xml";
    String MVNPM_PACKAGING_VERSION_KEY = "mvnpm.packagingVersion";
    /*
     * Increment this version (X.Y.Z) when the jar packaging structure changes:
     * - change X when the change breaks client compatibility
     * - change Y when compatibility is uncertain and a client upgrade is recommended
     * - change Z it's a non-breaking patch in the archive format
     */
    String MVNPM_PACKAGING_VERSION = "1.0.0";
}
