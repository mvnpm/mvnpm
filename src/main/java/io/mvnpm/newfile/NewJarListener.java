package io.mvnpm.newfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;

import io.mvnpm.file.KeyHolder;
import io.mvnpm.file.NewJarEvent;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

/**
 * Create different files when a new jar has been created
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class NewJarListener {

    @Inject
    KeyHolder keyHolder;

    @Inject
    AscService ascService;

    @Inject
    HashService hashService;

    @Inject
    JavaDocService javaDocService;

    @Inject
    SourceService sourceService;

    @Inject
    AutoSyncService autoSyncService;

    @ConsumeEvent(NewJarEvent.EVENT_NAME)
    @Blocking
    public void newJarCreated(NewJarEvent fse) {
        if (Files.exists(fse.targetDirectory())) {
            Log.warn("Target directory already exists on new jar event: " + fse.targetDirectory());
            return;
        }
        List<Path> toHash = new ArrayList<>();
        toHash.add(fse.pomFile());
        toHash.add(fse.jarFile());
        toHash.addAll(fse.others());
        if (fse.tgzFile() != null) {
            toHash.add(fse.tgzFile());
            toHash.add(sourceService.createSource(fse.tgzFile()));
        }
        toHash.add(javaDocService.createJavadoc(fse.jarFile()));
        List<Path> toSign = new ArrayList<>(toHash);
        for (Path path : toSign) {
            final Path asc = ascService.createAsc(path);
            if (asc != null) {
                toHash.add(asc);
            }
        }
        for (Path path : toHash) {
            hashService.createHashes(path);
        }

        try {
            if (Files.exists(fse.targetDirectory())) {
                Log.warn("Target directory has been created by another process: " + fse.targetDirectory());
                return;
            }
            Files.createDirectories(fse.targetDirectory().getParent());
            Files.move(fse.tempDirectory(), fse.targetDirectory(), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            FileUtils.deleteQuietly(fse.targetDirectory().toFile());
            throw new RuntimeException("Error while moving '%s' to '%s'.".formatted(fse.tempDirectory(), fse.targetDirectory()),
                    e);
        }
        autoSyncService.triggerSync(fse.name(), fse.version());

    }

}
