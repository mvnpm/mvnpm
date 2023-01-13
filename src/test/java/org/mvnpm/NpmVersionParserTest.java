package org.mvnpm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mvnpm.semver.NpmVersion;
import org.mvnpm.semver.NpmVersionParser;
import org.mvnpm.semver.Operator;
import org.mvnpm.semver.Part;
import org.mvnpm.semver.Version;

public class NpmVersionParserTest {

    @Test
    public void testEmptyVersion() {
        NpmVersion npmVersion = NpmVersionParser.parse("1.2.3");
        Operator operator = npmVersion.operator();
        Assertions.assertEquals(Operator.nothing, operator);
        Version version = npmVersion.version();
        Assertions.assertEquals(new Part(1), version.major());
        Assertions.assertEquals(new Part(2), version.minor());
        Assertions.assertEquals(new Part(3), version.patch());
    }
    
    @Test
    public void testVeeVersion() {
        NpmVersion npmVersion = NpmVersionParser.parse("v1.2.3");
        Operator operator = npmVersion.operator();
        Assertions.assertEquals(Operator.vee, operator);
        Version version = npmVersion.version();
        Assertions.assertEquals(new Part(1), version.major());
        Assertions.assertEquals(new Part(2), version.minor());
        Assertions.assertEquals(new Part(3), version.patch());
    }
    
    @Test
    public void testEqualsVersion() {
        NpmVersion npmVersion = NpmVersionParser.parse("=1.2.3");
        Operator operator = npmVersion.operator();
        Assertions.assertEquals(Operator.equals, operator);
        Version version = npmVersion.version();
        Assertions.assertEquals(new Part(1), version.major());
        Assertions.assertEquals(new Part(2), version.minor());
        Assertions.assertEquals(new Part(3), version.patch());
    }
    
    
    @Test
    public void testEmptyXVersion() {
        NpmVersion npmVersion = NpmVersionParser.parse("1.x");
        Operator operator = npmVersion.operator();
        Assertions.assertEquals(Operator.nothing, operator);
        Version version = npmVersion.version();
        Assertions.assertEquals(new Part(1), version.major());
        Assertions.assertEquals(new Part("x"), version.minor());
        Assertions.assertNull(version.patch());
    }
    
    @Test
    public void testVeeXVersion() {
        NpmVersion npmVersion = NpmVersionParser.parse("v1.x");
        Operator operator = npmVersion.operator();
        Assertions.assertEquals(Operator.vee, operator);
        Version version = npmVersion.version();
        Assertions.assertEquals(new Part(1), version.major());
        Assertions.assertEquals(new Part("x"), version.minor());
        Assertions.assertNull(version.patch());
    }
    
    @Test
    public void testEqualsXVersion() {
        NpmVersion npmVersion = NpmVersionParser.parse("=1.x");
        Operator operator = npmVersion.operator();
        Assertions.assertEquals(Operator.equals, operator);
        Version version = npmVersion.version();
        Assertions.assertEquals(new Part(1), version.major());
        Assertions.assertEquals(new Part("x"), version.minor());
        Assertions.assertNull(version.patch());
    }
    
    @Test
    public void testEmptyXxVersion() {
        NpmVersion npmVersion = NpmVersionParser.parse("1.2.x");
        Operator operator = npmVersion.operator();
        Assertions.assertEquals(Operator.nothing, operator);
        Version version = npmVersion.version();
        Assertions.assertEquals(new Part(1), version.major());
        Assertions.assertEquals(new Part(2), version.minor());
        Assertions.assertEquals(new Part("x"), version.patch());
    }
    
    @Test
    public void testVeeXxVersion() {
        NpmVersion npmVersion = NpmVersionParser.parse("v1.2.x");
        Operator operator = npmVersion.operator();
        Assertions.assertEquals(Operator.vee, operator);
        Version version = npmVersion.version();
        Assertions.assertEquals(new Part(1), version.major());
        Assertions.assertEquals(new Part(2), version.minor());
        Assertions.assertEquals(new Part("x"), version.patch());
    }
    
    @Test
    public void testEqualsXxVersion() {
        NpmVersion npmVersion = NpmVersionParser.parse("=1.2.x");
        Operator operator = npmVersion.operator();
        Assertions.assertEquals(Operator.equals, operator);
        Version version = npmVersion.version();
        Assertions.assertEquals(new Part(1), version.major());
        Assertions.assertEquals(new Part(2), version.minor());
        Assertions.assertEquals(new Part("x"), version.patch());
    }
}