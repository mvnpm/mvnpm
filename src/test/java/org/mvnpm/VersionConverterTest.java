package org.mvnpm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mvnpm.semver.VersionConverter;

public class VersionConverterTest {

    // concrete versions
    
    @Test
    public void testEmptyVersion() {
        String mavenVersion = VersionConverter.toMavenString("1.2.3");
        Assertions.assertEquals("1.2.3", mavenVersion);
    }
    
    @Test
    public void testVeeVersion() {
        String mavenVersion = VersionConverter.toMavenString("v1.2.3");
        Assertions.assertEquals("1.2.3", mavenVersion);
    }
    
    @Test
    public void testEqualsVersion() {
        String mavenVersion = VersionConverter.toMavenString("=1.2.3");
        Assertions.assertEquals("1.2.3", mavenVersion);
    }
    
    @Test
    public void testEmptyVersionNoMajor() {
        String mavenVersion = VersionConverter.toMavenString("0.2.3");
        Assertions.assertEquals("0.2.3", mavenVersion);
    }
    
    @Test
    public void testVeeVersionNoMajor() {
        String mavenVersion = VersionConverter.toMavenString("v0.2.3");
        Assertions.assertEquals("0.2.3", mavenVersion);
    }
    
    @Test
    public void testEqualsVersionNoMajor() {
        String mavenVersion = VersionConverter.toMavenString("=0.2.3");
        Assertions.assertEquals("0.2.3", mavenVersion);
    }
    
    @Test
    public void testEmptyVersionWithoutPatch() {
        String mavenVersion = VersionConverter.toMavenString("1.2");
        Assertions.assertEquals("1.2", mavenVersion);
    }
    
    @Test
    public void testVeeVersionWithoutPatch() {
        String mavenVersion = VersionConverter.toMavenString("v1.2");
        Assertions.assertEquals("1.2", mavenVersion);
    }
    
    @Test
    public void testEqualsVersionWithoutPatch() {
        String mavenVersion = VersionConverter.toMavenString("=1.2");
        Assertions.assertEquals("1.2", mavenVersion);
    }
    
    @Test
    public void testEmptyVersionWithoutMinor() {
        String mavenVersion = VersionConverter.toMavenString("1");
        Assertions.assertEquals("1", mavenVersion);
    }
    
    @Test
    public void testVeeVersionWithoutMinor() {
        String mavenVersion = VersionConverter.toMavenString("v1");
        Assertions.assertEquals("1", mavenVersion);
    }
    
    @Test
    public void testEqualsVersionWithoutMinor() {
        String mavenVersion = VersionConverter.toMavenString("=1");
        Assertions.assertEquals("1", mavenVersion);
    }
    
    // X'ed versions
    
    @Test
    public void testEmptyXVersion() {
        String mavenVersion = VersionConverter.toMavenString("x");
        Assertions.assertEquals("[0,)", mavenVersion);
        
        mavenVersion = VersionConverter.toMavenString("X");
        Assertions.assertEquals("[0,)", mavenVersion);
    }
    
    @Test
    public void testEmptyStarVersion() {
        String mavenVersion = VersionConverter.toMavenString("*");
        Assertions.assertEquals("[0,)", mavenVersion);
    }
    
    @Test
    public void testVeeXVersion() {
        String mavenVersion = VersionConverter.toMavenString("vx");
        Assertions.assertEquals("[0,)", mavenVersion);
    }
    
    @Test
    public void testEqualsXVersion() {
        String mavenVersion = VersionConverter.toMavenString("=x");
        Assertions.assertEquals("[0,)", mavenVersion);
    }
    
    @Test
    public void testEmptyXxVersion() {
        String mavenVersion = VersionConverter.toMavenString("1.x");
        Assertions.assertEquals("[1.0,2.0)", mavenVersion);
    }
    
    @Test
    public void testVeeXxVersion() {
        String mavenVersion = VersionConverter.toMavenString("v1.x");
        Assertions.assertEquals("[1.0,2.0)", mavenVersion);
    }
    
    @Test
    public void testEqualsXxVersion() {
        String mavenVersion = VersionConverter.toMavenString("=1.x");
        Assertions.assertEquals("[1.0,2.0)", mavenVersion);
    }
    
    @Test
    public void testEmptyXxxVersion() {
        String mavenVersion = VersionConverter.toMavenString("1.2.x");
        Assertions.assertEquals("[1.2.0,1.3.0)", mavenVersion);
    }
    
    @Test
    public void testVeeXxxVersion() {
        String mavenVersion = VersionConverter.toMavenString("v1.2.x");
        Assertions.assertEquals("[1.2.0,1.3.0)", mavenVersion);
    }
    
    @Test
    public void testEqualsXxxVersion() {
        String mavenVersion = VersionConverter.toMavenString("=1.2.x");
        Assertions.assertEquals("[1.2.0,1.3.0)", mavenVersion);
    }
    
    // Less than
    
    @Test
    public void testLessThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString("<0.0.1");
        Assertions.assertEquals("(,0.0.1)", mavenVersion);
    }
    
    @Test
    public void testLessThanVersion() {
        String mavenVersion = VersionConverter.toMavenString("<1.2.3");
        Assertions.assertEquals("(,1.2.3)", mavenVersion);
    }
    
    @Test
    public void testLessThanVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString("<1.2");
        Assertions.assertEquals("(,1.2)", mavenVersion);
    }
    
    @Test
    public void testLessThanVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString("<1");
        Assertions.assertEquals("(,1)", mavenVersion);
    }
    
    // Less than or equal to
    
    @Test
    public void testLessThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString("<=0.0.1");
        Assertions.assertEquals("(,0.0.1]", mavenVersion);
    }
    
    @Test
    public void testLessThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.toMavenString("<=1.2.3");
        Assertions.assertEquals("(,1.2.3]", mavenVersion);
    }
    
    @Test
    public void testLessThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString("<=1.2");
        Assertions.assertEquals("(,1.2]", mavenVersion);
    }
    
    @Test
    public void testLessThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString("<=1");
        Assertions.assertEquals("(,1]", mavenVersion);
    }
    
    // Greater than
    @Test
    public void testGreaterThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString(">0.0.1");
        Assertions.assertEquals("(0.0.1,)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanVersion() {
        String mavenVersion = VersionConverter.toMavenString(">1.2.3");
        Assertions.assertEquals("(1.2.3,)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString(">1.2");
        Assertions.assertEquals("(1.2,)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString(">1");
        Assertions.assertEquals("(1,)", mavenVersion);
    }
    
    // Greater than or equalTo
    @Test
    public void testGreaterThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString(">=0.0.1");
        Assertions.assertEquals("[0.0.1,)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.toMavenString(">=1.2.3");
        Assertions.assertEquals("[1.2.3,)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString(">=1.2");
        Assertions.assertEquals("[1.2,)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString(">=1");
        Assertions.assertEquals("[1,)", mavenVersion);
    }
    
    // Greater than or equalTo AND Less than or equalTo
    @Test
    public void testGreaterThanOrEqualToAndLessThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString(">=0.0.1 <=0.1.1");
        Assertions.assertEquals("[0.0.1,0.1.1]", mavenVersion);
    }
    
    @Test
    public void testGreaterThanOrEqualToAndLessThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.toMavenString(">=1.2.3 <=3.2.1");
        Assertions.assertEquals("[1.2.3,3.2.1]", mavenVersion);
    }
    
    @Test
    public void testGreaterThanOrEqualToAndLessThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString(">=1.2 <=3.2");
        Assertions.assertEquals("[1.2,3.2]", mavenVersion);
    }
    
    @Test
    public void testGreaterThanOrEqualToAndLessThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString(">=1 <=3");
        Assertions.assertEquals("[1,3]", mavenVersion);
    }
    
    // Less than or equalTo AND Greater than or equalTo
    @Test
    public void testLessThanOrEqualToAndGreaterThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString("<=0.1.1 >=0.0.1");
        Assertions.assertEquals("[0.0.1,0.1.1]", mavenVersion);
    }
    
    @Test
    public void testLessThanOrEqualToAndGreaterThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.toMavenString("<=3.2.1 >=1.2.3");
        Assertions.assertEquals("[1.2.3,3.2.1]", mavenVersion);
    }
    
    @Test
    public void testLessThanOrEqualToAndGreaterThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString("<=3.2 >=1.2");
        Assertions.assertEquals("[1.2,3.2]", mavenVersion);
    }
    
    @Test
    public void testLessThanOrEqualToAndGreaterThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString("<=3 >=1");
        Assertions.assertEquals("[1,3]", mavenVersion);
    }
    
    // Greater than AND Less than or equalTo
    @Test
    public void testGreaterThanAndLessThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString(">0.0.1 <=0.1.1");
        Assertions.assertEquals("(0.0.1,0.1.1]", mavenVersion);
    }
    
    @Test
    public void testGreaterThanAndLessThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.toMavenString(">1.2.3 <=3.2.1");
        Assertions.assertEquals("(1.2.3,3.2.1]", mavenVersion);
    }
    
    @Test
    public void testGreaterThanAndLessThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString(">1.2 <=3.2");
        Assertions.assertEquals("(1.2,3.2]", mavenVersion);
    }
    
    @Test
    public void testGreaterThanAndLessThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString(">1 <=3");
        Assertions.assertEquals("(1,3]", mavenVersion);
    }
    
    // Less than or equalTo AND Greater than
    @Test
    public void testLessThanOrEqualToAndGreaterThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString("<=0.1.1 >0.0.1");
        Assertions.assertEquals("(0.0.1,0.1.1]", mavenVersion);
    }
    
    @Test
    public void testLessThanOrEqualToAndGreaterThanVersion() {
        String mavenVersion = VersionConverter.toMavenString("<=3.2.1 >1.2.3");
        Assertions.assertEquals("(1.2.3,3.2.1]", mavenVersion);
    }
    
    @Test
    public void testLessThanOrEqualToAndGreaterThanVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString("<=3.2 >1.2");
        Assertions.assertEquals("(1.2,3.2]", mavenVersion);
    }
    
    @Test
    public void testLessThanOrEqualToAndGreaterThanVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString("<=3 >1");
        Assertions.assertEquals("(1,3]", mavenVersion);
    }
    
    // Greater than or equalTo AND Less than
    @Test
    public void testGreaterThanOrEqualToAndLessThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString(">=0.0.1 <0.1.1");
        Assertions.assertEquals("[0.0.1,0.1.1)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanOrEqualToAndLessThanVersion() {
        String mavenVersion = VersionConverter.toMavenString(">=1.2.3 <3.2.1");
        Assertions.assertEquals("[1.2.3,3.2.1)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanOrEqualToAndLessThanVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString(">=1.2 <3.2");
        Assertions.assertEquals("[1.2,3.2)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanOrEqualToAndLessThanVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString(">=1 <3");
        Assertions.assertEquals("[1,3)", mavenVersion);
    }
    
    // Less than AND Greater than or equalTo
    @Test
    public void testLessThanAndGreaterThanOrEqualToVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString("<0.1.1 >=0.0.1");
        Assertions.assertEquals("[0.0.1,0.1.1)", mavenVersion);
    }
    
    @Test
    public void testLessThanAndGreaterThanOrEqualToVersion() {
        String mavenVersion = VersionConverter.toMavenString("<3.2.1 >=1.2.3");
        Assertions.assertEquals("[1.2.3,3.2.1)", mavenVersion);
    }
    
    @Test
    public void testLessThanAndGreaterThanOrEqualToVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString("<3.2 >=1.2");
        Assertions.assertEquals("[1.2,3.2)", mavenVersion);
    }
    
    @Test
    public void testLessThanAndGreaterThanOrEqualToVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString("<3 >=1");
        Assertions.assertEquals("[1,3)", mavenVersion);
    }
    
    // Greater than AND Less than
    @Test
    public void testGreaterThanAndLessThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString(">0.0.1 <0.1.1");
        Assertions.assertEquals("(0.0.1,0.1.1)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanAndLessThanVersion() {
        String mavenVersion = VersionConverter.toMavenString(">1.2.3 <3.2.1");
        Assertions.assertEquals("(1.2.3,3.2.1)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanAndLessThanVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString(">1.2 <3.2");
        Assertions.assertEquals("(1.2,3.2)", mavenVersion);
    }
    
    @Test
    public void testGreaterThanAndLessThanVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString(">1 <3");
        Assertions.assertEquals("(1,3)", mavenVersion);
    }
    
    // Less than AND Greater than
    @Test
    public void testLessThanAndGreaterThanVersionWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString("<0.1.1 >0.0.1");
        Assertions.assertEquals("(0.0.1,0.1.1)", mavenVersion);
    }
    
    @Test
    public void testLessThanAndGreaterThanVersion() {
        String mavenVersion = VersionConverter.toMavenString("<3.2.1 >1.2.3");
        Assertions.assertEquals("(1.2.3,3.2.1)", mavenVersion);
    }
    
    @Test
    public void testLessThanAndGreaterThanVersionNoPatch() {
        String mavenVersion = VersionConverter.toMavenString("<3.2 >1.2");
        Assertions.assertEquals("(1.2,3.2)", mavenVersion);
    }
    
    @Test
    public void testLessThanAndGreaterThanVersionNoMinor() {
        String mavenVersion = VersionConverter.toMavenString("<3 >1");
        Assertions.assertEquals("(1,3)", mavenVersion);
    }
    
    // OR 
    @Test
    public void testOr() {
        String mavenVersion = VersionConverter.toMavenString("1.2.7 || >=1.2.9 <2.0.0");
        Assertions.assertEquals("1.2.7,[1.2.9,2.0.0)", mavenVersion);
    }
    
    @Test
    public void testOrOr() {
        String mavenVersion = VersionConverter.toMavenString("1.2.7 || >=1.2.9 <2.0.0 || >3.3.2 <=4.0.0");
        Assertions.assertEquals("1.2.7,[1.2.9,2.0.0),(3.3.2,4.0.0]", mavenVersion);
    }
    
    // Partial version in range
    @Test
    public void testPartialRange(){
        String mavenVersion = VersionConverter.toMavenString(">=1.2 <=2.3.4");
        Assertions.assertEquals("[1.2,2.3.4]", mavenVersion);
    }
    
    // Hyphen range
    @Test
    public void testHyphen() {
        String mavenVersion = VersionConverter.toMavenString("1.2.3 - 2.3.4");
        Assertions.assertEquals("[1.2.3,2.3.4]", mavenVersion);
    }
    
    @Test
    public void testHyphenWithPartial() {
        String mavenVersion = VersionConverter.toMavenString("1.2 - 2.3.4");
        Assertions.assertEquals("[1.2,2.3.4]", mavenVersion);
    }
    
    @Test
    public void testHyphenWithPartialUpper() {
        String mavenVersion = VersionConverter.toMavenString("1.2.3 - 2.3");
        Assertions.assertEquals("[1.2.3,2.3]", mavenVersion);
    }
    
    @Test
    public void testHyphenWithOnlyMajorUpper() {
        String mavenVersion = VersionConverter.toMavenString("1.2.3 - 2");
        Assertions.assertEquals("[1.2.3,2]", mavenVersion);
    }
    
    // Tilde range
    @Test
    public void testTilde() {
        String mavenVersion = VersionConverter.toMavenString("~1.2.3");
        Assertions.assertEquals("[1.2.3,1.3.0)", mavenVersion);
    }
    
    @Test
    public void testTildeWithPartial() {
        String mavenVersion = VersionConverter.toMavenString("~1.2");
        Assertions.assertEquals("[1.2,1.3)", mavenVersion);
    }
    
    @Test
    public void testTildeWithMajorOnly() {
        String mavenVersion = VersionConverter.toMavenString("~1");
        Assertions.assertEquals("[1,2)", mavenVersion);
    }
    
    @Test
    public void testTildeWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString("~0.2.3");
        Assertions.assertEquals("[0.2.3,0.3.0)", mavenVersion);
    }

    @Test
    public void testTildePartialWithZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString("~0.2");
        Assertions.assertEquals("[0.2,0.3)", mavenVersion);
    }

    @Test
    public void testTildeWithOnlyZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString("~0");
        Assertions.assertEquals("[0,1)", mavenVersion);
    }

    // Caret range
    @Test
    public void testCaret() {
        String mavenVersion = VersionConverter.toMavenString("^1.2.3");
        Assertions.assertEquals("[1.2.3,2.0.0)", mavenVersion);
    }
    
    @Test
    public void testCaretZeroMajor() {
        String mavenVersion = VersionConverter.toMavenString("^0.2.3");
        Assertions.assertEquals("[0.2.3,0.3.0)", mavenVersion);
    }
    
    @Test
    public void testCaretZeroMajorAndMinor() {
        String mavenVersion = VersionConverter.toMavenString("^0.0.3");
        Assertions.assertEquals("[0.0.3,0.0.4)", mavenVersion);
    }

    @Test
    public void testCaretWithXPatch(){
        String mavenVersion = VersionConverter.toMavenString("^1.2.x");
        Assertions.assertEquals("[1.2.0,2.0.0)", mavenVersion);
    }
    
    @Test
    public void testCaretWithXPatchAndZeroMajorAndMinor(){
        String mavenVersion = VersionConverter.toMavenString("^0.0.x");
        Assertions.assertEquals("[0.0.0,0.1.0)", mavenVersion);
    }
    
    @Test
    public void testCaretWithoutPatchAndZeroMajorAndMinor(){
        String mavenVersion = VersionConverter.toMavenString("^0.0");
        Assertions.assertEquals("[0.0,0.1)", mavenVersion);
    }
    
    @Test
    public void testCaretWithXMinor(){
        String mavenVersion = VersionConverter.toMavenString("^1.x");
        Assertions.assertEquals("[1.0,2.0)", mavenVersion);
    }
    
    @Test
    public void testCaretWithXMinorAndZeroMajor(){
        String mavenVersion = VersionConverter.toMavenString("^0.x");
        Assertions.assertEquals("[0.0,1.0)", mavenVersion);
    }
    
    
}