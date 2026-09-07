package io.mvnpm.creator.type;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.creator.utils.FileUtil;
import io.quarkus.logging.Log;

/**
 * Downloads or stream the tar files from npm
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 *
 *         TODO: Error handling (when version / package does not exist)
 *         TODO: Add metrics / analytics / eventing ?
 */
@ApplicationScoped
public class TgzService {

    public void fetchRemoteAndSave(URL tarballUrl, Path localFileName) {
        if (Files.exists(localFileName)) {
            Log.warnf("%s was already downloaded.", localFileName);
            return;
        }
        try {
            FileUtil.createDirectories(localFileName);
            downloadFileTo(tarballUrl, localFileName);
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Error downloading tar from NPM " + tarballUrl + " [" + ex.getMessage() + "]", ex);
        }
    }

    /**
     * Saves an already-open tarball stream. This is used for authenticated Nexus
     * downloads, where the REST client owns HTTP/authentication concerns.
     */
    public void save(InputStream tarball, Path localFileName) {
        if (Files.exists(localFileName)) {
            Log.warnf("%s was already downloaded.", localFileName);
            return;
        }
        try {
            FileUtil.createDirectories(localFileName);
            saveTo(tarball, localFileName);
        } catch (IOException ex) {
            throw new RuntimeException("Error saving tar to " + localFileName + " [" + ex.getMessage() + "]", ex);
        }
    }

    private void downloadFileTo(URL url, Path localFileName) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            final int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new FileNotFoundException("Status: " + responseCode + " from " + url);
            }

            try (InputStream in = connection.getInputStream()) {
                saveTo(in, localFileName);
            }
        } finally {
            connection.disconnect();
        }
    }

    private void saveTo(InputStream in, Path localFileName) throws IOException {
        final Path tempFile = FileUtil.getTempFilePathFor(localFileName);
        try (OutputStream out = Files.newOutputStream(tempFile)) {
            final byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        FileUtil.forceMoveAtomic(tempFile, localFileName);
    }
}
