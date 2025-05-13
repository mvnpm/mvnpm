package io.mvnpm.creator.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;

import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.pgpainless.sop.SOPImpl;

import io.mvnpm.Constants;
import io.mvnpm.creator.exceptions.NoSecretRingAscException;
import sop.ByteArrayAndResult;
import sop.ReadyWithResult;
import sop.SOP;
import sop.SigningResult;

public final class FileUtil {

    private FileUtil() {

    }

    public static void createDirectories(Path localFilePath) {
        try {
            Files.createDirectories(localFilePath.getParent());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static StreamingOutput toStreamingOutput(Path filePath) {
        return outputStream -> {
            try (InputStream fileStream = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192]; // 8 KB chunks
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            } catch (NoSuchFileException e) {
                throw new WebApplicationException("File was moved or deleted", 410);
            } catch (IOException e) {
                throw new WebApplicationException("Error streaming file", 500);
            }
        };
    }

    public static Path createSha1(Path forFile) {
        return FileUtil.createSha1(forFile, false);
    }

    public static Path createSha1(Path forFile, boolean force) {
        String localSha1FileName = forFile.toString() + Constants.DOT_SHA1;
        Path localSha1File = Paths.get(localSha1FileName);
        if (!Files.exists(localSha1File) || force) {
            try (InputStream inputStream = Files.newInputStream(forFile)) {
                String sha1 = FileUtil.getSha1(inputStream);
                writeAtomic(localSha1File, sha1);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        return localSha1File;
    }

    public static void writeAtomic(Path file, String content) {
        final Path tempFile = getTempFilePathFor(file);
        try {
            Files.writeString(tempFile, content);
            forceMoveAtomic(tempFile, file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeAtomic(Path file, byte[] content) {
        final Path tempFile = getTempFilePathFor(file);
        try {
            Files.write(tempFile, content);
            forceMoveAtomic(tempFile, file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void forceMoveAtomic(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static Path getTempFilePathFor(Path file) {
        SecureRandom random = new SecureRandom();
        long n = random.nextLong();
        String s = Long.toUnsignedString(n);
        final Path tempFile = file.getParent().resolve(file.getFileName() + ".creating-" + s);
        return tempFile;
    }

    private static String getSha1(java.io.InputStream inputStream) {
        try {
            byte[] digest = FileUtil.getMessageDigest(inputStream, Constants.SHA1);
            StringBuilder result = new StringBuilder();
            for (byte b : digest) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Path createMd5(Path forFile) {
        return FileUtil.createMd5(forFile, false);
    }

    public static Path createMd5(Path forFile, boolean force) {
        String localMd5FileName = forFile.toString() + Constants.DOT_MD5;
        Path localMd5File = Paths.get(localMd5FileName);
        if (!Files.exists(localMd5File) || force) {
            try (InputStream inputStream = Files.newInputStream(forFile)) {
                String md5 = FileUtil.getMd5(inputStream);
                writeAtomic(localMd5File, md5);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return localMd5File;
    }

    private static String getMd5(InputStream inputStream) {
        try {
            byte[] digest = FileUtil.getMessageDigest(inputStream, Constants.MD5);
            return String.format("%032x", new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Path createAsc(PGPSecretKeyRing secretKeyRing, Path fileToSign) throws NoSecretRingAscException {
        if (secretKeyRing != null) {
            String outputFile = fileToSign.toString() + Constants.DOT_ASC;
            Path ascFileOutput = Paths.get(outputFile);
            if (!Files.exists(ascFileOutput)) {
                try {
                    byte[] jarFileBytes = Files.readAllBytes(fileToSign);

                    SOP sop = new SOPImpl();
                    ReadyWithResult<SigningResult> readyWithResult = sop.detachedSign()
                            .key(secretKeyRing.getSecretKey().getEncoded())
                            .data(jarFileBytes);

                    ByteArrayAndResult<SigningResult> bytesAndResult = readyWithResult.toByteArrayAndResult();

                    byte[] detachedSignature = bytesAndResult.getBytes();

                    Files.write(ascFileOutput, detachedSignature);
                } catch (IOException e) {
                    throw new UncheckedIOException("Error while signing: '%s'".formatted(fileToSign), e);
                }
            }
            return ascFileOutput;
        } else {
            throw new NoSecretRingAscException(
                    "No secret ring, impossible to generate ASC for file: '%s'".formatted(fileToSign));
        }
    }

    public static boolean isOlderThanTimeout(Path p, int timeoutMinutes) {
        try {
            FileTime t = Files.getLastModifiedTime(p);
            Instant fileInstant = t.toInstant();
            Instant now = Clock.systemDefaultZone().instant();
            Duration difference = Duration.between(fileInstant, now);
            long minutes = difference.toMinutes();
            return minutes >= timeoutMinutes;
        } catch (IOException ex) {
            return true;
        }
    }

    private static byte[] getMessageDigest(InputStream inputStream, String algorithm)
            throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, bytesRead);
        }

        return md.digest();
    }
}
