package io.mvnpm.version;

import static io.mvnpm.Constants.DOT;
import static io.mvnpm.Constants.EMPTY;
import static io.mvnpm.Constants.ESCAPED_DOT;
import static io.mvnpm.Constants.EX;
import static io.mvnpm.Constants.HYPHEN;
import static io.mvnpm.Constants.STAR;

public record Version(Integer major,
        Integer minor,
        Integer patch,
        String qualifier) {

    private static final String DEFAULT_QUALIFIER = null;//"alpha00000000";

    public static Version fromString(String version) {
        try {
            String numberPart = version;
            String qualifier = null;
            if (version.contains(HYPHEN)) {

                int indexOfFirstHyphen = version.indexOf(HYPHEN);
                numberPart = version.substring(0, indexOfFirstHyphen);
                qualifier = version.substring(indexOfFirstHyphen + 1);
            }

            if (numberPart.contains(DOT)) {
                String parts[] = numberPart.split(ESCAPED_DOT);
                if (parts.length > 2) {
                    return new Version(toNumber(parts[0]), toNumber(parts[1]), toNumber(parts[2]), qualifier);
                } else if (parts.length == 2) {
                    return new Version(toNumber(parts[0]), toNumber(parts[1]), null, qualifier);
                }
            } else {
                return new Version(toNumber(numberPart), null, null, qualifier);
            }
        } catch (Throwable t) {
            throw new InvalidVersionException(version, t);
        }
        throw new InvalidVersionException(version);
    }

    private static Integer toNumber(String part) {
        if (part == null || part.equals(STAR) || part.equalsIgnoreCase(EX)) {
            return null;
        }
        return Integer.valueOf(part);
    }

    public Version nextMajor() {
        if (this.major == null || this.major == 0) {
            return new Version(1, null, null, DEFAULT_QUALIFIER);
        } else {
            return new Version(this.major + 1, null, null, DEFAULT_QUALIFIER);
        }
    }

    public Version nextMinor() {
        if (this.minor == null || this.minor == 0) {
            return new Version(this.major, 1, null, DEFAULT_QUALIFIER);
        } else {
            return new Version(this.major, this.minor + 1, null, DEFAULT_QUALIFIER);
        }
    }

    public Version nextPatch() {
        if (this.patch == null || this.patch == 0) {
            return new Version(this.major, this.minor, 1, DEFAULT_QUALIFIER);
        } else {
            return new Version(this.major, this.minor, this.patch + 1, DEFAULT_QUALIFIER);
        }
    }

    @Override
    public String toString() {
        if (this.major == null || (this.major == 0 && this.minor == null && this.patch == null))
            return EMPTY;
        if (this.minor == null)
            return String.valueOf(this.major) + getPostString();
        if (this.patch == null)
            return String.valueOf(this.major) + DOT + String.valueOf(this.minor) + getPostString();
        return String.valueOf(this.major) + DOT + String.valueOf(this.minor) + DOT + String.valueOf(this.patch)
                + getPostString();
    }

    private String getPostString() {
        if (qualifier != null) {
            return HYPHEN + qualifier;
        }
        return EMPTY;
    }

    public boolean hasQualifier() {
        if (qualifier() == null || qualifier().isBlank()) {
            return false;
        }
        return true;
    }
}
