package io.mvnpm.version;

import static io.mvnpm.Constants.CARET;
import static io.mvnpm.Constants.CLOSE_BLOCK;
import static io.mvnpm.Constants.CLOSE_ROUND;
import static io.mvnpm.Constants.COMMA;
import static io.mvnpm.Constants.EMPTY;
import static io.mvnpm.Constants.EQUAL_TO;
import static io.mvnpm.Constants.ESCAPED_CARET;
import static io.mvnpm.Constants.EX;
import static io.mvnpm.Constants.GREATER_THAN;
import static io.mvnpm.Constants.GREATER_THAN_OR_EQUAL_TO;
import static io.mvnpm.Constants.HYPHEN;
import static io.mvnpm.Constants.LESS_THAN;
import static io.mvnpm.Constants.LESS_THAN_OR_EQUAL_TO;
import static io.mvnpm.Constants.OPEN_BLOCK;
import static io.mvnpm.Constants.OPEN_ROUND;
import static io.mvnpm.Constants.OR;
import static io.mvnpm.Constants.OR_ESCAPED;
import static io.mvnpm.Constants.SPACE;
import static io.mvnpm.Constants.STAR;
import static io.mvnpm.Constants.TILDE;
import static io.mvnpm.Constants.VEE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.mvnpm.Constants;

/**
 * Convert a npm version to a maven version
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 *
 *         see https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
 *         see https://github.com/npm/node-semver
 *         see https://semver.org/
 */
public class VersionConverter {
    private static final Logger LOG = Logger.getLogger(VersionConverter.class);

    private VersionConverter() {
    }

    public static String convert(String versionString) {
        try {
            if (null == versionString || versionString.startsWith("git:/") || versionString.startsWith("git+http")) { // We do not support git repos as version. Maybe something we can add later
                versionString = EMPTY;
            }

            versionString = versionString.trim();
            String[] orSet;
            if (versionString.contains(OR)) {
                orSet = versionString.split(OR_ESCAPED);
            } else {
                orSet = new String[] { versionString };
            }
            return convertMultiple(orSet);
        } catch (Throwable t) {
            LOG.warn("Error getting maven version from [" + versionString + "]");
            throw t;
        }
    }

    private static String convertMultiple(String[] versions) {
        List<String> versionList = new ArrayList<>();
        for (String v : versions) {
            String result = convertMultiplePart(v.trim()).trim();
            versionList.add(result);
        }

        // If multiple, it has to be in brackets
        if (versionList.size() > 1) {
            versionList = versionList.stream()
                    .map(element -> {
                        if (!element.contains("[") && !element.contains("(") && !element.contains("]")
                                && !element.contains(")")) {
                            return "[" + element + "]";
                        }
                        return element;
                    }).collect(Collectors.toList());
        }

        return String.join(COMMA, versionList);
    }

    /**
     * This convert parts to maven format
     *
     * @param version
     * @return
     */
    private static String convertMultiplePart(String version) {

        // Hyphen range
        if (version.contains(SPACE + HYPHEN + SPACE)) {
            return convertHyphen(version);
        }

        // Tilde range
        if (version.startsWith(TILDE)) {
            version = cleanTildeVersion(version);
            return convertTilde(version);
        }

        // Caret range
        if (version.startsWith(CARET)) {
            version = cleanCaretVersion(version);
            return convertCaret(version);
        }

        // Operator range
        if (version.startsWith(LESS_THAN) || version.startsWith(GREATER_THAN)) {
            version = cleanOperatorVersion(version);
            return convertOperator(version);
        }

        // X range
        if (!version.contains(SPACE)) {
            String numberPart = version.split(HYPHEN)[0];
            if (numberPart.contains(STAR) || numberPart.contains(EX) || numberPart.contains(EX.toUpperCase())) {
                return convertX(version);
            }
        }

        // Partial semver
        long numberOfDots = version.chars().filter(ch -> ch == '.').count();
        if (!version.startsWith(EQUAL_TO) && numberOfDots < 2) {
            return convertPartialSemver(version);
        }

        // Exact
        return cleanVersion(version);
    }

    /**
     * Translate Hyphen ranges
     * see https://github.com/npm/node-semver#hyphen-ranges-xyz---abc
     *
     * @param version
     * @return
     */
    private static String convertHyphen(String version) {

        String[] parts = version.split(SPACE + HYPHEN + SPACE);

        if (parts.length != 2)
            throw new RuntimeException("Boundary set to big [" + parts.length
                    + "] - expecting 2 (lower and upper). Hyphen version [" + version + "]");

        Version lowerBoundary = Version.fromString(parts[0].trim());
        Version upperBoundary = Version.fromString(parts[1].trim());

        return OPEN_BLOCK + lowerBoundary + COMMA + upperBoundary + CLOSE_BLOCK;
    }

    /**
     * Translate Tilde ranges
     * see https://github.com/npm/node-semver#tilde-ranges-123-12-1
     *
     * @param version
     * @return
     */
    private static String convertTilde(String version) {
        version = version.substring(1);
        if (version.equalsIgnoreCase(Constants.LATEST)) {
            return convert(version);
        }
        Version lowerBoundary = Version.fromString(version);
        Version upperBoundary = lowerBoundary.nextMajor();
        if (lowerBoundary.minor() != null) {
            upperBoundary = lowerBoundary.nextMinor();
        }
        return OPEN_BLOCK + lowerBoundary + COMMA + upperBoundary + CLOSE_ROUND;
    }

    /**
     * Translate caret ranges
     * see https://github.com/npm/node-semver#caret-ranges-123-025-004
     *
     * @param version
     * @return
     */
    private static String convertCaret(String version) {
        version = version.substring(1);
        if (version.equalsIgnoreCase(Constants.LATEST)) {
            return convert(version);
        }
        Version lowerBoundary = Version.fromString(version);
        Version upperBoundary = lowerBoundary.nextMajor();

        if ((lowerBoundary.major() == 0 && lowerBoundary.minor() == null)) {
            return OPEN_BLOCK + lowerBoundary + COMMA + upperBoundary + CLOSE_ROUND;
        } else if ((lowerBoundary.major() == 0 && lowerBoundary.minor() > 0)
                || lowerBoundary.major() == 0 && lowerBoundary.minor() == 0 && zeroOrX(lowerBoundary.patch())) {
            upperBoundary = lowerBoundary.nextMinor();
        } else if (lowerBoundary.major() == 0 && lowerBoundary.minor() == 0 && notZero(lowerBoundary.patch())) {
            upperBoundary = lowerBoundary.nextPatch();
        }
        return OPEN_BLOCK + lowerBoundary + COMMA + upperBoundary + CLOSE_ROUND;
    }

    /**
     * Translate x ranges
     * see https://github.com/npm/node-semver#x-ranges-12x-1x-12-
     *
     * @param version
     * @return
     */
    private static String convertX(String version) {
        version = cleanVersion(version);
        Version lowerBoundary = Version.fromString(version);

        if (lowerBoundary.major() == null) {
            return OPEN_BLOCK + COMMA + CLOSE_ROUND;
        } else if (lowerBoundary.minor() == null) {
            return OPEN_BLOCK + lowerBoundary + COMMA + lowerBoundary.nextMajor() + CLOSE_ROUND;
        } else if (lowerBoundary.patch() == null) {
            return OPEN_BLOCK + lowerBoundary + COMMA + lowerBoundary.nextMinor() + CLOSE_ROUND;
        }
        return OPEN_BLOCK + lowerBoundary + COMMA + CLOSE_ROUND;
    }

    private static String convertOperator(String version) {
        if (version.contains(SPACE)) {
            return convertOperatorRange(version);
        } else {
            return convertOperatorOpenEnded(version);
        }
    }

    private static String convertOperatorRange(String version) {
        String[] boundaries = version.split(SPACE);
        if (boundaries.length > 2)
            throw new RuntimeException("Error while converting version [" + version + "], boundary set to big ["
                    + boundaries.length + "] - expecting 2 (lower and upper)");
        List<String> boundaryList = toSorterList(boundaries);// Make sure the lower boundary is first
        return getLowerBoundary(boundaryList.get(0).trim()) + COMMA + getUpperBoundary(boundaryList.get(1).trim());
    }

    private static String convertOperatorOpenEnded(String version) {
        if (version.startsWith(GREATER_THAN)) {
            return getLowerBoundary(version) + COMMA + CLOSE_ROUND;
        } else if (version.startsWith(LESS_THAN)) {
            return OPEN_ROUND + COMMA + getUpperBoundary(version);
        }
        throw new InvalidVersionException(version);
    }

    private static String convertPartialSemver(String version) {
        version = version + ".x";
        return convertX(version);
    }

    private static String getLowerBoundary(String s) {
        if (s.startsWith(GREATER_THAN_OR_EQUAL_TO)) {
            return OPEN_BLOCK + Version.fromString(s.substring(2));
        } else if (s.startsWith(GREATER_THAN)) {
            return OPEN_ROUND + Version.fromString(s.substring(1));
        } else {
            // Equal. If no operator is specified, then equality is assumed, so this operator is optional, but MAY be included.
            return cleanVersion(s);
        }
    }

    private static String getUpperBoundary(String s) {
        if (s.startsWith(LESS_THAN_OR_EQUAL_TO)) {
            return Version.fromString(s.substring(2)) + CLOSE_BLOCK;
        } else if (s.startsWith(LESS_THAN)) {
            return Version.fromString(s.substring(1)) + CLOSE_ROUND;
        } else {
            // Equal. If no operator is specified, then equality is assumed, so this operator is optional, but MAY be included.
            return cleanVersion(s);
        }
    }

    private static List<String> toSorterList(String[] boundaries) {
        List<String> boundaryList = Arrays.asList(boundaries);
        Collections.sort(boundaryList, (t, t1) -> {
            if (t.startsWith(GREATER_THAN) && t1.startsWith(LESS_THAN)) {
                return -1;
            } else if (t1.startsWith(GREATER_THAN) && t.startsWith(LESS_THAN)) {
                return 1;
            }
            return 0;
        });
        return boundaryList;
    }

    private static boolean zeroOrX(Integer i) {
        return i == null || i == 0 || i == Integer.MIN_VALUE;
    }

    private static boolean notZero(Integer i) {
        return i != null && i > 0;
    }

    private static String cleanVersion(String version) {
        if (version.startsWith(VEE) || version.startsWith(EQUAL_TO)) {
            version = version.substring(1); // Remove v or =
        }
        return version;
    }

    private static String cleanOperatorVersion(String version) {
        if (version.contains(LESS_THAN_OR_EQUAL_TO + " ")) {
            version = version.replaceAll(LESS_THAN_OR_EQUAL_TO + " ", LESS_THAN_OR_EQUAL_TO);
            return cleanOperatorVersion(version);
        }
        if (version.contains(LESS_THAN + " ")) {
            version = version.replaceAll(LESS_THAN + " ", LESS_THAN);
            return cleanOperatorVersion(version);
        }
        if (version.contains(GREATER_THAN_OR_EQUAL_TO + " ")) {
            version = version.replaceAll(GREATER_THAN_OR_EQUAL_TO + " ", GREATER_THAN_OR_EQUAL_TO);
            return cleanOperatorVersion(version);
        }
        if (version.contains(GREATER_THAN + " ")) {
            version = version.replaceAll(GREATER_THAN + " ", GREATER_THAN);
            return cleanOperatorVersion(version);
        }
        return version;
    }

    private static String cleanTildeVersion(String version) {
        if (version.contains(TILDE + " ")) {
            version = version.replaceAll(TILDE + " ", TILDE);
            return cleanTildeVersion(version);
        }
        return version;
    }

    private static String cleanCaretVersion(String version) {
        if (version.contains(CARET + " ")) {
            version = version.replaceAll(ESCAPED_CARET + " ", CARET);
            return cleanCaretVersion(version);
        }
        return version;
    }

}
