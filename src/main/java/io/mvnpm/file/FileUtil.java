package io.mvnpm.file;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import io.mvnpm.Constants;

public class FileUtil {

    private FileUtil() {

    }

    public static String getSha1(byte[] value) {
        try {
            MessageDigest md = MessageDigest.getInstance(Constants.SHA1);
            byte[] digest = md.digest(value);
            StringBuilder sb = new StringBuilder(40);
            for (int i = 0; i < digest.length; ++i) {
                sb.append(Integer.toHexString((digest[i] & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void createSha1(Path outputPath) {
        try {
            byte[] content = Files.readAllBytes(outputPath);

            String sha1 = FileUtil.getSha1(content);
            String localSha1FileName = outputPath.toString() + Constants.DOT_SHA1;
            Path f = Paths.get(localSha1FileName);
            if (!Files.exists(f)) {
                synchronized (f) {
                    Files.writeString(f, sha1);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String getMd5(byte[] value) {
        try {
            MessageDigest md = MessageDigest.getInstance(Constants.MD5);
            byte[] digest = md.digest(value);
            return new BigInteger(1, digest).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static void createMd5(Path outputPath) {
        try {
            byte[] content = Files.readAllBytes(outputPath);

            String md5 = FileUtil.getMd5(content);
            String localMd5FileName = outputPath.toString() + Constants.DOT_MD5;
            Path f = Paths.get(localMd5FileName);
            if (!Files.exists(f)) {
                synchronized (f) {
                    Files.writeString(f, md5);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static boolean createAsc(Path localFilePath) {
        String outputFile = localFilePath.toString() + Constants.DOT_ASC;
        Path f = Paths.get(outputFile);
        synchronized (f) {
            if (!Files.exists(f)) {
                try {
                    Process process = Runtime.getRuntime().exec(GPG_COMMAND + localFilePath.toString());
                    // Set a timeout of 10 seconds
                    long timeout = 10;
                    boolean processFinished = process.waitFor(timeout, TimeUnit.SECONDS);

                    if (!processFinished) {
                        process.destroy(); // If the process doesn't finish, we can destroy it
                        return false;
                    } else {
                        // Process finished within the timeout
                        int exitCode = process.exitValue();
                        process.destroy();
                        return true;
                    }
                } catch (InterruptedException | IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
        return true;
    }

    private static final String GPG_COMMAND = "gpg -ab ";
}
