package io.mvnpm.creator.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pgpainless.PGPainless;
import org.pgpainless.key.generation.type.rsa.RsaLength;
import org.pgpainless.sop.SOPImpl;

import io.mvnpm.creator.exceptions.NoSecretRingAscException;
import sop.SOP;
import sop.Verification;

class FileUtilAscTest {

    static PGPSecretKeyRing secretKeyRing;
    static PGPPublicKeyRing publicKeyRing;

    @BeforeAll
    static void generateTestKey()
            throws PGPException, InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        // Use RSA key where the primary key can sign (matches production key setup)
        secretKeyRing = PGPainless.generateKeyRing().simpleRsaKeyRing("test@mvnpm.io", RsaLength._2048);
        publicKeyRing = PGPainless.extractCertificate(secretKeyRing);
    }

    @Test
    void createAsc_producesValidSignature(@TempDir Path tempDir) throws Exception {
        // Create a test file
        Path testFile = tempDir.resolve("test-artifact.jar");
        byte[] content = "some jar content for signing test".getBytes(StandardCharsets.UTF_8);
        Files.write(testFile, content);

        // Sign it
        Path ascFile = FileUtil.createAsc(secretKeyRing, testFile);

        // Verify .asc file exists and is non-empty
        assertNotNull(ascFile);
        assertTrue(Files.exists(ascFile));
        assertTrue(Files.size(ascFile) > 0);

        // Verify the signature is valid using SOP verify
        SOP sop = new SOPImpl();
        byte[] certBytes = PGPainless.asciiArmor(publicKeyRing).getBytes(StandardCharsets.UTF_8);
        List<Verification> verifications = sop.detachedVerify()
                .cert(new ByteArrayInputStream(certBytes))
                .signatures(Files.newInputStream(ascFile))
                .data(new ByteArrayInputStream(content));

        assertFalse(verifications.isEmpty(), "Signature should be valid");
    }

    @Test
    void createAsc_skipsIfAlreadyExists(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("existing.jar");
        Files.write(testFile, "content".getBytes(StandardCharsets.UTF_8));

        // First call creates the .asc
        Path ascFile1 = FileUtil.createAsc(secretKeyRing, testFile);
        byte[] firstSignature = Files.readAllBytes(ascFile1);

        // Second call should skip (file already exists)
        Path ascFile2 = FileUtil.createAsc(secretKeyRing, testFile);
        byte[] secondSignature = Files.readAllBytes(ascFile2);

        // Same bytes (not re-signed)
        assertNotNull(ascFile2);
        assertTrue(java.util.Arrays.equals(firstSignature, secondSignature),
                "Second call should return existing .asc without re-signing");
    }

    @Test
    void createAsc_throwsWithNullKeyRing(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("no-key.jar");
        Files.write(testFile, "content".getBytes(StandardCharsets.UTF_8));

        assertThrows(NoSecretRingAscException.class, () -> FileUtil.createAsc(null, testFile));
    }

    @Test
    void createAsc_worksWithLargerFile(@TempDir Path tempDir) throws Exception {
        // 1 MB file to test streaming behavior
        Path testFile = tempDir.resolve("large-artifact.jar");
        byte[] content = new byte[1024 * 1024];
        java.util.Arrays.fill(content, (byte) 'A');
        Files.write(testFile, content);

        Path ascFile = FileUtil.createAsc(secretKeyRing, testFile);

        assertNotNull(ascFile);
        assertTrue(Files.exists(ascFile));
        assertTrue(Files.size(ascFile) > 0);

        // Verify signature
        SOP sop = new SOPImpl();
        byte[] certBytes = PGPainless.asciiArmor(publicKeyRing).getBytes(StandardCharsets.UTF_8);
        List<Verification> verifications = sop.detachedVerify()
                .cert(new ByteArrayInputStream(certBytes))
                .signatures(Files.newInputStream(ascFile))
                .data(new ByteArrayInputStream(content));

        assertFalse(verifications.isEmpty(), "Signature for large file should be valid");
    }
}
