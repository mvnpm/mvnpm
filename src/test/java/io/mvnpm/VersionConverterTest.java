package io.mvnpm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.mvnpm.version.VersionConverter;

public class VersionConverterTest {

    // concrete versions

    @Test
    public void testEmptyVersion() {
        String mavenVersion = VersionConverter.convert("1.2.3");
        Assertions.assertEquals("1.2.3", mavenVersion);
    }

    @Test
    public void testVeeVersion() {
        String mavenVersion = VersionConverter.convert("v1.2.3");
        Assertions.assertEquals("1.2.3", mavenVersion);
    }

    @Test
    public void testEqualsVersion() {
        String mavenVersion = VersionConverter.convert("=1.2.3");
        Assertions.assertEquals("1.2.3", mavenVersion);
    }

    @Test
    public void testEmptyVersionNoMajor() {
        String mavenVersion = VersionConverter.convert("0.2.3");
        Assertions.assertEquals("0.2.3", mavenVersion);
    }

    @Test
    public void testVeeVersionNoMajor() {
        String mavenVersion = VersionConverter.convert("v0.2.3");
        Assertions.assertEquals("0.2.3", mavenVersion);
    }

    @Test
    public void testEqualsVersionNoMajor() {
        String mavenVersion = VersionConverter.convert("=0.2.3");
        Assertions.assertEquals("0.2.3", mavenVersion);
    }

    @Test
    public void testEmptyVersionWithoutPatch() {
        String mavenVersion = VersionConverter.convert("1.2");
        Assertions.assertEquals("[1.2,1.3)", mavenVersion);
    }

    @Test
    public void testVeeVersionWithoutPatch() {
        String mavenVersion = VersionConverter.convert("v1.2");
        Assertions.assertEquals("[1.2,1.3)", mavenVersion);
    }

    @Test
    public void testEqualsVersionWithoutPatch() {
        String mavenVersion = VersionConverter.convert("=1.2");
        Assertions.assertEquals("1.2", mavenVersion);
    }

    @Test
    public void testEmptyVersionWithoutMinor() {
        String mavenVersion = VersionConverter.convert("1");
        Assertions.assertEquals("[1,2)", mavenVersion);
    }

    @Test
    public void testVeeVersionWithoutMinor() {
        String mavenVersion = VersionConverter.convert("v1");
        Assertions.assertEquals("[1,2)", mavenVersion);
    }

    @Test
    public void testEqualsVersionWithoutMinor() {
        String mavenVersion = VersionConverter.convert("=1");
        Assertions.assertEquals("1", mavenVersion);
    }

    @Test
    public void testUntrimmed() {
        String mavenVersion = VersionConverter.convert(" 0.2.3 ");
        Assertions.assertEquals("0.2.3", mavenVersion);
    }

    @Test
    public void testVeeUntrimmed() {
        String mavenVersion = VersionConverter.convert(" v0.2.3 ");
        Assertions.assertEquals("0.2.3", mavenVersion);

        mavenVersion = VersionConverter.convert("v 0.2.3 ");
        Assertions.assertEquals("0.2.3", mavenVersion);
    }

    // X'ed versions

    @Test
    public void testEmptyXVersion() {
        String mavenVersion = VersionConverter.convert("x");
        Assertions.assertEquals("[,)", mavenVersion);

        mavenVersion = VersionConverter.convert("X");
        Assertions.assertEquals("[,)", mavenVersion);
    }

    @Test
    public void testEmptyStarVersion() {
        String mavenVersion = VersionConverter.convert("*");
        Assertions.assertEquals("[,)", mavenVersion);
    }

    @Test
    public void testVeeXVersion() {
        String mavenVersion = VersionConverter.convert("vx");
        Assertions.assertEquals("[,)", mavenVersion);
    }

    @Test
    public void testEqualsXVersion() {
        String mavenVersion = VersionConverter.convert("=x");
        Assertions.assertEquals("[,)", mavenVersion);
    }

    @Test
    public void testEmptyXxVersion() {
        String mavenVersion = VersionConverter.convert("1.x");
        Assertions.assertEquals("[1,2)", mavenVersion);
    }

    @Test
    public void testVeeXxVersion() {
        String mavenVersion = VersionConverter.convert("v1.x");
        Assertions.assertEquals("[1,2)", mavenVersion);
    }

    @Test
    public void testEqualsXxVersion() {
        String mavenVersion = VersionConverter.convert("=1.x");
        Assertions.assertEquals("[1,2)", mavenVersion);
    }

    @Test
    public void testEmptyXxxVersion() {
        String mavenVersion = VersionConverter.convert("1.2.x");
        Assertions.assertEquals("[1.2,1.3)", mavenVersion);
    }

    @Test
    public void testVeeXxxVersion() {
        String mavenVersion = VersionConverter.convert("v1.2.x");
        Assertions.assertEquals("[1.2,1.3)", mavenVersion);
    }

    @Test
    public void testEqualsXxxVersion() {
        String mavenVersion = VersionConverter.convert("=1.2.x");
        Assertions.assertEquals("[1.2,1.3)", mavenVersion);
    }

    // Less than

    @Test
    public void testLessThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert("<0.0.1");
        Assertions.assertEquals("(,0.0.1)", mavenVersion);
    }

    @Test
    public void testLessThanVersion() {
        String mavenVersion = VersionConverter.convert("<1.2.3");
        Assertions.assertEquals("(,1.2.3)", mavenVersion);
    }

    @Test
    public void testLessThanVersionNoPatch() {
        String mavenVersion = VersionConverter.convert("<1.2");
        Assertions.assertEquals("(,1.2)", mavenVersion);
    }

    @Test
    public void testLessThanVersionNoMinor() {
        String mavenVersion = VersionConverter.convert("<1");
        Assertions.assertEquals("(,1)", mavenVersion);
    }

    // Less than or equal to

    @Test
    public void testLessThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert("<=0.0.1");
        Assertions.assertEquals("(,0.0.1]", mavenVersion);
    }

    @Test
    public void testLessThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.convert("<=1.2.3");
        Assertions.assertEquals("(,1.2.3]", mavenVersion);
    }

    @Test
    public void testLessThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.convert("<=1.2");
        Assertions.assertEquals("(,1.2]", mavenVersion);
    }

    @Test
    public void testLessThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.convert("<=1");
        Assertions.assertEquals("(,1]", mavenVersion);
    }

    // Greater than
    @Test
    public void testGreaterThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert(">0.0.1");
        Assertions.assertEquals("(0.0.1,)", mavenVersion);
    }

    @Test
    public void testGreaterThanVersion() {
        String mavenVersion = VersionConverter.convert(">1.2.3");
        Assertions.assertEquals("(1.2.3,)", mavenVersion);
    }

    @Test
    public void testGreaterThanVersionNoPatch() {
        String mavenVersion = VersionConverter.convert(">1.2");
        Assertions.assertEquals("(1.2,)", mavenVersion);
    }

    @Test
    public void testGreaterThanVersionNoMinor() {
        String mavenVersion = VersionConverter.convert(">1");
        Assertions.assertEquals("(1,)", mavenVersion);
    }

    // Greater than or equalTo
    @Test
    public void testGreaterThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert(">=0.0.1");
        Assertions.assertEquals("[0.0.1,)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.convert(">=1.2.3");
        Assertions.assertEquals("[1.2.3,)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToVersionWithSpace() {
        String mavenVersion = VersionConverter.convert(">= 1.2.3");
        Assertions.assertEquals("[1.2.3,)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToVersionWithSpaces() {
        String mavenVersion = VersionConverter.convert(">=    1.2.3");
        Assertions.assertEquals("[1.2.3,)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.convert(">=1.2");
        Assertions.assertEquals("[1.2,)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.convert(">=1");
        Assertions.assertEquals("[1,)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToVersionXMinor() {
        String mavenVersion = VersionConverter.convert(">=14.x");
        Assertions.assertEquals("[14,)", mavenVersion);
    }

    // Greater than or equalTo AND Less than or equalTo
    @Test
    public void testGreaterThanOrEqualToAndLessThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert(">=0.0.1 <=0.1.1");
        Assertions.assertEquals("[0.0.1,0.1.1]", mavenVersion);
    }

    @Test
    public void testCaretWithRangeAndLessThan() {
        String mavenVersion = VersionConverter.convert("^3.0.11 <3.1.7");
        Assertions.assertEquals("[3.0.11,3.1.7)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToAndLessThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.convert(">=1.2.3 <=3.2.1");
        Assertions.assertEquals("[1.2.3,3.2.1]", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToAndLessThanOrEqualToVersionWithSpace() {
        String mavenVersion = VersionConverter.convert(">= 1.2.3 <= 3.2.1");
        Assertions.assertEquals("[1.2.3,3.2.1]", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToAndLessThanOrEqualToVersionWithSpaces() {
        String mavenVersion = VersionConverter.convert(">=    1.2.3 <=    3.2.1");
        Assertions.assertEquals("[1.2.3,3.2.1]", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToAndLessThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.convert(">=1.2 <=3.2");
        Assertions.assertEquals("[1.2,3.2]", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToAndLessThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.convert(">=1 <=3");
        Assertions.assertEquals("[1,3]", mavenVersion);
    }

    // Less than or equalTo AND Greater than or equalTo
    @Test
    public void testLessThanOrEqualToAndGreaterThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert("<=0.1.1 >=0.0.1");
        Assertions.assertEquals("[0.0.1,0.1.1]", mavenVersion);
    }

    @Test
    public void testLessThanOrEqualToAndGreaterThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.convert("<=3.2.1 >=1.2.3");
        Assertions.assertEquals("[1.2.3,3.2.1]", mavenVersion);
    }

    @Test
    public void testLessThanOrEqualToAndGreaterThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.convert("<=3.2 >=1.2");
        Assertions.assertEquals("[1.2,3.2]", mavenVersion);
    }

    @Test
    public void testLessThanOrEqualToAndGreaterThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.convert("<=3 >=1");
        Assertions.assertEquals("[1,3]", mavenVersion);
    }

    // Greater than AND Less than or equalTo
    @Test
    public void testGreaterThanAndLessThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert(">0.0.1 <=0.1.1");
        Assertions.assertEquals("(0.0.1,0.1.1]", mavenVersion);
    }

    @Test
    public void testGreaterThanAndLessThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.convert(">1.2.3 <=3.2.1");
        Assertions.assertEquals("(1.2.3,3.2.1]", mavenVersion);
    }

    @Test
    public void testGreaterThanAndLessThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.convert(">1.2 <=3.2");
        Assertions.assertEquals("(1.2,3.2]", mavenVersion);
    }

    @Test
    public void testGreaterThanAndLessThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.convert(">1 <=3");
        Assertions.assertEquals("(1,3]", mavenVersion);
    }

    // Less than or equalTo AND Greater than
    @Test
    public void testLessThanOrEqualToAndGreaterThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert("<=0.1.1 >0.0.1");
        Assertions.assertEquals("(0.0.1,0.1.1]", mavenVersion);
    }

    @Test
    public void testLessThanOrEqualToAndGreaterThanVersion() {
        String mavenVersion = VersionConverter.convert("<=3.2.1 >1.2.3");
        Assertions.assertEquals("(1.2.3,3.2.1]", mavenVersion);
    }

    @Test
    public void testLessThanOrEqualToAndGreaterThanVersionNoPatch() {
        String mavenVersion = VersionConverter.convert("<=3.2 >1.2");
        Assertions.assertEquals("(1.2,3.2]", mavenVersion);
    }

    @Test
    public void testLessThanOrEqualToAndGreaterThanVersionNoMinor() {
        String mavenVersion = VersionConverter.convert("<=3 >1");
        Assertions.assertEquals("(1,3]", mavenVersion);
    }

    // Greater than or equalTo AND Less than
    @Test
    public void testGreaterThanOrEqualToAndLessThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert(">=0.0.1 <0.1.1");
        Assertions.assertEquals("[0.0.1,0.1.1)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToAndLessThanVersion() {
        String mavenVersion = VersionConverter.convert(">=1.2.3 <3.2.1");
        Assertions.assertEquals("[1.2.3,3.2.1)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToAndLessThanVersionNoPatch() {
        String mavenVersion = VersionConverter.convert(">=1.2 <3.2");
        Assertions.assertEquals("[1.2,3.2)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToAndLessThanVersionNoMinor() {
        String mavenVersion = VersionConverter.convert(">=1 <3");
        Assertions.assertEquals("[1,3)", mavenVersion);
    }

    // Less than AND Greater than or equalTo
    @Test
    public void testLessThanAndGreaterThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert("<0.1.1 >=0.0.1");
        Assertions.assertEquals("[0.0.1,0.1.1)", mavenVersion);
    }

    @Test
    public void testLessThanAndGreaterThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.convert("<3.2.1 >=1.2.3");
        Assertions.assertEquals("[1.2.3,3.2.1)", mavenVersion);
    }

    @Test
    public void testLessThanAndGreaterThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.convert("<3.2 >=1.2");
        Assertions.assertEquals("[1.2,3.2)", mavenVersion);
    }

    @Test
    public void testLessThanAndGreaterThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.convert("<3 >=1");
        Assertions.assertEquals("[1,3)", mavenVersion);
    }

    // Greater than AND Less than
    @Test
    public void testGreaterThanAndLessThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert(">0.0.1 <0.1.1");
        Assertions.assertEquals("(0.0.1,0.1.1)", mavenVersion);
    }

    @Test
    public void testGreaterThanAndLessThanVersion() {
        String mavenVersion = VersionConverter.convert(">1.2.3 <3.2.1");
        Assertions.assertEquals("(1.2.3,3.2.1)", mavenVersion);
    }

    @Test
    public void testGreaterThanAndLessThanVersionNoPatch() {
        String mavenVersion = VersionConverter.convert(">1.2 <3.2");
        Assertions.assertEquals("(1.2,3.2)", mavenVersion);
    }

    @Test
    public void testGreaterThanAndLessThanVersionNoMinor() {
        String mavenVersion = VersionConverter.convert(">1 <3");
        Assertions.assertEquals("(1,3)", mavenVersion);
    }

    // Less than AND Greater than
    @Test
    public void testLessThanAndGreaterThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.convert("<0.1.1 >0.0.1");
        Assertions.assertEquals("(0.0.1,0.1.1)", mavenVersion);
    }

    @Test
    public void testLessThanAndGreaterThanVersion() {
        String mavenVersion = VersionConverter.convert("<3.2.1 >1.2.3");
        Assertions.assertEquals("(1.2.3,3.2.1)", mavenVersion);
    }

    @Test
    public void testLessThanAndGreaterThanVersionNoPatch() {
        String mavenVersion = VersionConverter.convert("<3.2 >1.2");
        Assertions.assertEquals("(1.2,3.2)", mavenVersion);
    }

    @Test
    public void testLessThanAndGreaterThanVersionNoMinor() {
        String mavenVersion = VersionConverter.convert("<3 >1");
        Assertions.assertEquals("(1,3)", mavenVersion);
    }

    // OR
    @Test
    public void testOr() {
        String mavenVersion = VersionConverter.convert("1.2.7 || >=1.2.9 <2.0.0");
        Assertions.assertEquals("[1.2.7],[1.2.9,2.0.0)", mavenVersion);
    }

    @Test
    public void testGreaterThanOrEqualToORVersion() {
        String mavenVersion = VersionConverter.convert(">= 16.8 || 18.0.0");
        Assertions.assertEquals("[16.8,),[18.0.0]", mavenVersion);
    }

    @Test
    public void testOrOr() {
        String mavenVersion = VersionConverter.convert("1.2.7 || >=1.2.9 <2.0.0 || >3.3.2 <=4.0.0");
        Assertions.assertEquals("[1.2.7],[1.2.9,2.0.0),(3.3.2,4.0.0]", mavenVersion);
    }

    @Test
    // Issue https://github.com/mvnpm/mvnpm/issues/12006
    public void testStringVersion() {
        String mavenVersion = VersionConverter.convert(">=3.0.0 || insiders || >=4.0.0-alpha.20 || >=4.0.0-beta.1");
        Assertions.assertEquals("[3.0.0,),[4.0.0-alpha.20,),[4.0.0-beta.1,)", mavenVersion);
    }

    // Partial version in range
    @Test
    public void testPartialRange() {
        String mavenVersion = VersionConverter.convert(">=1.2 <=2.3.4");
        Assertions.assertEquals("[1.2,2.3.4]", mavenVersion);
    }

    // Hyphen range
    @Test
    public void testHyphen() {
        String mavenVersion = VersionConverter.convert("1.2.3 - 2.3.4");
        Assertions.assertEquals("[1.2.3,2.3.4]", mavenVersion);
    }

    @Test
    public void testHyphenWithPartial() {
        String mavenVersion = VersionConverter.convert("1.2 - 2.3.4");
        Assertions.assertEquals("[1.2,2.3.4]", mavenVersion);
    }

    @Test
    public void testHyphenWithPartialUpper() {
        String mavenVersion = VersionConverter.convert("1.2.3 - 2.3");
        Assertions.assertEquals("[1.2.3,2.3]", mavenVersion);
    }

    @Test
    public void testHyphenWithOnlyMajorUpper() {
        String mavenVersion = VersionConverter.convert("1.2.3 - 2");
        Assertions.assertEquals("[1.2.3,2]", mavenVersion);
    }

    // Tilde range
    @Test
    public void testTilde() {
        String mavenVersion = VersionConverter.convert("~1.2.3");
        Assertions.assertEquals("[1.2.3,1.3)", mavenVersion);
    }

    @Test
    public void testTildeWithSpace() {
        String mavenVersion = VersionConverter.convert("~ 1.2.3");
        Assertions.assertEquals("[1.2.3,1.3)", mavenVersion);
    }

    @Test
    public void testTildeWithSpaces() {
        String mavenVersion = VersionConverter.convert("~    1.2.3");
        Assertions.assertEquals("[1.2.3,1.3)", mavenVersion);
    }

    @Test
    public void testTildeWithPartial() {
        String mavenVersion = VersionConverter.convert("~1.2");
        Assertions.assertEquals("[1.2,1.3)", mavenVersion);
    }

    @Test
    public void testTildeWithMajorOnly() {
        String mavenVersion = VersionConverter.convert("~1");
        Assertions.assertEquals("[1,2)", mavenVersion);
    }

    @Test
    public void testTildeWithZeroMajor() {
        String mavenVersion = VersionConverter.convert("~0.2.3");
        Assertions.assertEquals("[0.2.3,0.3)", mavenVersion);
    }

    @Test
    public void testTildePartialWithZeroMajor() {
        String mavenVersion = VersionConverter.convert("~0.2");
        Assertions.assertEquals("[0.2,0.3)", mavenVersion);
    }

    @Test
    public void testTildeWithOnlyZeroMajor() {
        String mavenVersion = VersionConverter.convert("~0");
        Assertions.assertEquals("[,1)", mavenVersion);
    }

    // Caret range
    @Test
    public void testCaret() {
        String mavenVersion = VersionConverter.convert("^1.2.3");
        Assertions.assertEquals("[1.2.3,2)", mavenVersion);
    }

    @Test
    public void testCaretWithSpace() {
        String mavenVersion = VersionConverter.convert("^ 1.2.3");
        Assertions.assertEquals("[1.2.3,2)", mavenVersion);
    }

    @Test
    public void testCaretWithSpaces() {
        String mavenVersion = VersionConverter.convert("^    1.2.3");
        Assertions.assertEquals("[1.2.3,2)", mavenVersion);
    }

    @Test
    public void testCaretZeroMajor() {
        String mavenVersion = VersionConverter.convert("^0.2.3");
        Assertions.assertEquals("[0.2.3,0.3)", mavenVersion);
    }

    @Test
    public void testCaretZeroMajorAndMinor() {
        String mavenVersion = VersionConverter.convert("^0.0.3");
        Assertions.assertEquals("[0.0.3,0.0.4)", mavenVersion);
    }

    @Test
    public void testCaretWithXPatch() {
        String mavenVersion = VersionConverter.convert("^1.2.x");
        Assertions.assertEquals("[1.2,2)", mavenVersion);
    }

    @Test
    public void testCaretWithXPatchAndZeroMajorAndMinor() {
        String mavenVersion = VersionConverter.convert("^0.0.x");
        Assertions.assertEquals("[0.0,0.1)", mavenVersion); // TODO: (,0.1) ??
    }

    @Test
    public void testCaretWithoutPatchAndZeroMajorAndMinor() {
        String mavenVersion = VersionConverter.convert("^0.0");
        Assertions.assertEquals("[0.0,0.1)", mavenVersion); // TODO: (,0.1) ??
    }

    @Test
    public void testCaretWithXMinor() {
        String mavenVersion = VersionConverter.convert("^1.x");
        Assertions.assertEquals("[1,2)", mavenVersion);
    }

    @Test
    public void testCaretWithXMinorAndZeroMajor() {
        String mavenVersion = VersionConverter.convert("^0.x");
        Assertions.assertEquals("[,1)", mavenVersion); // TODO: (,1) ??
    }

    // Prerelease
    @Test
    public void testPrerelease() {
        String mavenVersion = VersionConverter.convert("^3.0.0-pre.26");
        Assertions.assertEquals("[3.0.0-pre.26,4)", mavenVersion);

        mavenVersion = VersionConverter.convert("1.4.0-next.0");
        Assertions.assertEquals("1.4.0-next.0", mavenVersion);

    }

}
