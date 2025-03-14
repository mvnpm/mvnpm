package io.mvnpm.creator.type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.pgpainless.PGPainless;

import io.mvnpm.creator.utils.FileUtil;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;

/**
 * Sign newly created file
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class AscService {

    private PGPSecretKeyRing secretKeyRing = null;

    @ConfigProperty(name = "mvnpm.asckey.path")
    Optional<String> asckeyPath;

    public Path createAsc(Path filePath) {
        if (!hasSecretKeyRing()) {
            return null;
        }
        Log.debug("file signed " + filePath + " [ok]");
        return FileUtil.createAsc(getSecretKeyRing(), filePath);
    }

    void onStart(@Observes StartupEvent ev) {
        if (asckeyPath.isPresent()) {
            try {
                Path keyFilePath = Paths.get(asckeyPath.get());
                byte[] keyBytes = Files.readAllBytes(keyFilePath);
                this.secretKeyRing = PGPainless.readKeyRing().secretKeyRing(keyBytes);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            Log.log(LaunchMode.current() == LaunchMode.NORMAL ? Logger.Level.ERROR : Logger.Level.WARN,
                    "No secret key found, it will not be possible to create signature for packages.");
        }
    }

    private PGPSecretKeyRing getSecretKeyRing() {
        return this.secretKeyRing;
    }

    private boolean hasSecretKeyRing() {
        return this.secretKeyRing != null;
    }

}
