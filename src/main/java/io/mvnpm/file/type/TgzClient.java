package io.mvnpm.file.type;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.file.FileStore;
import io.mvnpm.file.FileUtil;

/**
 * Downloads or stream the tar files from npm
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 *
 *         TODO: Error handling (when version / package does not exist)
 *         TODO: Add metrics / analytics / eventing ?
 */
@ApplicationScoped
public class TgzClient {

    @Inject
    FileStore fileStore;

    public void fetchRemoteAndSave(io.mvnpm.npm.model.Package p, Path localFileName) {
        URL tarball = p.dist().tarball();
        try {
            FileUtil.createDirectories(localFileName);
            downloadFileTo(tarball, localFileName);
            fileStore.touch(p.name(), p.version(), localFileName);
        } catch (IOException ex) {
            throw new RuntimeException("Error download tar from NPM " + tarball + " [" + ex.getMessage() + "]");
        }
    }

    private void downloadFileTo(URL url, Path localFileName) throws FileNotFoundException {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new FileNotFoundException("Status: " + responseCode);
            }

            try (InputStream in = connection.getInputStream();
                    OutputStream out = Files.newOutputStream(localFileName)) {

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
