package io.mvnpm.file;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.StreamingOutput;

import io.mvnpm.Constants;
import io.quarkus.logging.Log;

public class FileUtil {

    private FileUtil() {

    }

    public static void createDirectories(Path localFilePath) {
        try {
            Files.createDirectories(localFilePath.getParent());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static StreamingOutput toStreamingOutput(Path localFilePath) {
        return outputStream -> {
            try (InputStream fileInputStream = Files.newInputStream(localFilePath)) {
                int bytesRead;
                byte[] buffer = new byte[4096];
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Error streaming file content", e);
            }
        };
    }

    public static boolean isReadyForUse(Path fileName) throws FileNotFoundException {
        return FileUtil.isReadyForUse(fileName, 0);
    }

    /**
     * Check if a file exist and it's not being written to
     *
     * @param fileName
     * @param tryCount
     * @return
     */
    private static boolean isReadyForUse(Path fileName, int tryCount) throws FileNotFoundException {
        boolean isReady = Files.exists(fileName) && !FileUtil.isFileBeingWritten(fileName);
        if (isReady)
            return true;
        if (tryCount > 9)
            throw new FileNotFoundException(fileName.toString());
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ex) {
            Log.error(ex);
        }
        tryCount = tryCount + 1;
        return isReadyForUse(fileName, tryCount);
    }

    private static boolean isFileBeingWritten(Path filePath) {
        try (FileChannel fileChannel = FileChannel.open(filePath, StandardOpenOption.WRITE)) {
            // Try to acquire a lock on the entire file.
            FileLock fileLock = fileChannel.tryLock();
            // If the lock is null, then the file is being written by another process.
            return fileLock == null;
        } catch (java.nio.channels.OverlappingFileLockException lockException) {
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Path createSha1(Path forFile) {
        return FileUtil.createSha1(forFile, false);
    }

    public static Path createSha1(Path forFile, boolean force) {
        String localSha1FileName = forFile.toString() + Constants.DOT_SHA1;
        Path localSha1File = Paths.get(localSha1FileName);
        try (InputStream inputStream = Files.newInputStream(forFile)) {
            String sha1 = FileUtil.getSha1(inputStream);

            if (!Files.exists(localSha1File) || force) {
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
        return FileUtil.createMd5(forFile, false);
    }

    public static Path createMd5(Path forFile, boolean force) {
        String localMd5FileName = forFile.toString() + Constants.DOT_MD5;
        Path localMd5File = Paths.get(localMd5FileName);
        try (InputStream inputStream = Files.newInputStream(forFile)) {
            String md5 = FileUtil.getMd5(inputStream);

            if (!Files.exists(localMd5File) || force) {
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
        return FileUtil.createAsc(localFilePath, false);
    }

    public static boolean createAsc(Path localFilePath, boolean force) {
        String outputFile = localFilePath.toString() + Constants.DOT_ASC;
        Path f = Paths.get(outputFile);
        synchronized (f) {
            if (!Files.exists(f) || force) {
                try {
                    Process process = Runtime.getRuntime().exec(GPG_COMMAND + localFilePath.toString());
                    // Set a timeout of 10 seconds
                    long timeout = 20;
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
