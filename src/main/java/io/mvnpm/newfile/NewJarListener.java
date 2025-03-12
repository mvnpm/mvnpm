package io.mvnpm.newfile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.file.KeyHolder;
import io.mvnpm.file.NewJarEvent;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.mutiny.core.Vertx;

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

    @Inject
    Vertx vertx;

    @ConsumeEvent(NewJarEvent.EVENT_NAME)
    @Blocking
    public void newJarCreated(NewJarEvent fse) {
        Log.infof("'%s' has been created.", fse.jarFile());
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
        autoSyncService.triggerSync(fse.name(), fse.version());

    }

}
