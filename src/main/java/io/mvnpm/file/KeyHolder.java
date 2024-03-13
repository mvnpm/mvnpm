package io.mvnpm.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.pgpainless.PGPainless;

import io.quarkus.runtime.StartupEvent;

/**
 * Holds the key for signing the files
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class KeyHolder {

    private PGPSecretKeyRing secretKeyRing = null;

    @ConfigProperty(name = "mvnpm.asckey.path")
    Optional<String> asckeyPath;

    void onStart(@Observes StartupEvent ev) {
        if (asckeyPath.isPresent()) {
            try {
                Path keyFilePath = Paths.get(asckeyPath.get());
                byte[] keyBytes = Files.readAllBytes(keyFilePath);
                this.secretKeyRing = PGPainless.readKeyRing().secretKeyRing(keyBytes);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public PGPSecretKeyRing getSecretKeyRing() {
        return this.secretKeyRing;
    }

    public boolean hasSecretKeyRing() {
        return this.secretKeyRing != null;
    }

}
