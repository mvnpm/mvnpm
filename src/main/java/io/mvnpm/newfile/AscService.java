package io.mvnpm.newfile;

import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.file.FileUtil;
import io.mvnpm.file.KeyHolder;
import io.mvnpm.file.exceptions.NoSecretRingAscException;
import io.quarkus.logging.Log;

/**
 * Sign newly created file
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class AscService {

    @Inject
    KeyHolder keyHolder;

    public Path createAsc(Path filePath) {
        try {
            Log.debug("file signed " + filePath + " [ok]");
            return FileUtil.createAsc(keyHolder.getSecretKeyRing(), filePath);
        } catch (NoSecretRingAscException e) {
            Log.warn(e.getMessage());
        }
        return null;
    }
}
