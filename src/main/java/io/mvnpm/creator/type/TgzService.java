package io.mvnpm.creator.type;

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

import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.creator.utils.FileUtil;

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

    @Inject
    PackageFileLocator packageFileLocator;

    public void fetchRemoteAndSave(io.mvnpm.npm.model.Package p, Path localFileName) {
        URL tarball = p.dist().tarball();
        try {
            FileUtil.createDirectories(localFileName);
            downloadFileTo(tarball, localFileName);
        } catch (IOException ex) {
            throw new RuntimeException("Error download tar from NPM " + tarball + " [" + ex.getMessage() + "]");
        }
    }

    private void downloadFileTo(URL url, Path localFileName) throws FileNotFoundException {
        try {
            final Path tempFile = FileUtil.getTempFilePathFor(localFileName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new FileNotFoundException("Status: " + responseCode);
            }

            try (InputStream in = connection.getInputStream();
                    OutputStream out = Files.newOutputStream(tempFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

            } finally {
                connection.disconnect();
            }
            FileUtil.forceMoveAtomic(tempFile, localFileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
