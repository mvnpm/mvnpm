package io.mvnpm.file;

import java.io.IOException;
import java.io.InputStream;
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

    public static Path createSha1(Path forFile) {
        String localSha1FileName = forFile.toString() + Constants.DOT_SHA1;
        Path localSha1File = Paths.get(localSha1FileName);
        try (InputStream inputStream = Files.newInputStream(forFile)) {
            String sha1 = FileUtil.getSha1(inputStream);

            if (!Files.exists(localSha1File)) {
                synchronized (localSha1File) {
                    Files.writeString(localSha1File, sha1);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        return localSha1File;
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
        String localMd5FileName = forFile.toString() + Constants.DOT_MD5;
        Path localMd5File = Paths.get(localMd5FileName);
        try (InputStream inputStream = Files.newInputStream(forFile)) {
            String md5 = FileUtil.getMd5(inputStream);

            if (!Files.exists(localMd5File)) {
                synchronized (localMd5File) {
                    Files.writeString(localMd5File, md5);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        return localMd5File;
    }

    private static String getMd5(java.io.InputStream inputStream) {
        try {
            byte[] digest = FileUtil.getMessageDigest(inputStream, Constants.MD5);
            return new BigInteger(1, digest).toString(16);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
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

    private static final String GPG_COMMAND = "gpg -ab ";
}
