package io.mvnpm.maven;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.mvnpm.Constants;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;

/**
 * Parse a url path
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
public class UrlPathParser {

    private UrlPathParser() {
    }

    public static Name parseMavenMetaDataXml(String fullName) {

        if (fullName.startsWith(Constants.AT_SLASH)) {
            fullName = fullName.replaceFirst(Constants.AT_SLASH, Constants.AT);
        }

        return NameParser.fromNpmProject(fullName);
    }

    public static NameVersionType parseMavenFile(String urlPath) {
        if (urlPath.contains("/git:/") || urlPath.contains("/git+http:/") || urlPath.contains("/git+https:/")) { // We do not support git repos as version. Maybe something we can add later
            urlPath = cleanUrlPath(urlPath);
        }
        String[] parts = urlPath.split(Constants.SLASH);

        // We need at least 3 (name / version / filename)
        if (parts.length < 3) {
            throw new RuntimeException("Invalid Url Path [" + urlPath + "]");
        }

        // Start from the back
        String version = parts[parts.length - 2]; // version
        String[] nameParts = Arrays.copyOfRange(parts, 0, parts.length - 2); // groupid and artifactId

        String fullName = String.join(Constants.SLASH, nameParts);
        if (fullName.startsWith(Constants.AT_SLASH)) {
            fullName = fullName.replaceFirst(Constants.AT_SLASH, Constants.AT);
        }

        // Make sure the version is in the correct format
        version = fixVersion(version);
        return new NameVersionType(NameParser.fromNpmProject(fullName), version);
    }

    private static String cleanUrlPath(String urlPath) {
        Matcher m = PATTERN.matcher(urlPath);
        return m.replaceAll("latest");
    }

    private static String fixVersion(String version) {
        String[] parts = version.split("\\.");

        for (int i = parts.length; i < 3; i++) {
            version += ".0";
        }

        return version;
    }

    private static final Pattern PATTERN = Pattern.compile("git\\+.*?\\.git");
}
