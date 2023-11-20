package io.mvnpm.file.type;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.file.FileStore;

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

    public byte[] fetchRemote(io.mvnpm.npm.model.Package p, Path localFileName) {
        URL tarball = p.dist().tarball();

        try {
            byte[] downloadFile = downloadFile(tarball);
            return fileStore.createFile(p.name(), p.version(), localFileName, downloadFile);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Error download tar from NPM " + tarball + " [" + ex.getMessage() + "]");
        }
    }

    private byte[] downloadFile(URL url) throws FileNotFoundException {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new FileNotFoundException("Status: " + responseCode);
            }

            try (InputStream in = connection.getInputStream()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }

                return baos.toByteArray();
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
