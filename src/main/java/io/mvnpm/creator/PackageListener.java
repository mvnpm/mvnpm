package io.mvnpm.creator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.creator.events.DependencyVersionCheckRequest;
import io.mvnpm.creator.events.NewJarEvent;
import io.mvnpm.creator.type.AscService;
import io.mvnpm.creator.type.HashService;
import io.mvnpm.creator.type.JavaDocService;
import io.mvnpm.creator.type.PomService;
import io.mvnpm.creator.type.SourceService;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.sync.SyncService;
import io.mvnpm.npm.api.NpmFacade;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;

/**
 * Create different files when a new jar has been created
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class PackageListener {

    @Inject
    AscService ascService;

    @Inject
    HashService hashService;

    @Inject
    JavaDocService javaDocService;

    @Inject
    SourceService sourceService;

    @Inject
    PomService pomService;

    @Inject
    NpmFacade npmFacade;

    @Inject
    MavenRepositoryService mavenRepositoryService;

    @Inject
    private SyncService syncService;

    @ConsumeEvent(NewJarEvent.EVENT_NAME)
    @Blocking
    public void newJarCreated(NewJarEvent fse) {
        Log.infof("'%s' has been created.", fse.jarFile());
        createBundleFiles(fse.pomFile(), fse.jarFile(), fse.tgzFile(), fse.others());
        Log.infof("Package %s is ready for Sync", fse.name().displayName);
        boolean queued = syncService.initializeSync(fse.name(), fse.version());
        if (queued) {
            Log.info(fse.name().displayName + " " + fse.version() + " added to the sync queue");
        }
    }

    /**
     * Ensure all bundle files (source, javadoc, signatures, hashes) exist.
     * All services are idempotent — safe to call even if files already exist.
     */
    public void createBundleFiles(Path pomFile, Path jarFile, Path tgzFile, List<Path> others) {
        List<Path> toHash = new ArrayList<>();
        toHash.add(pomFile);
        toHash.add(jarFile);
        toHash.addAll(others);
        if (tgzFile != null) {
            toHash.add(tgzFile);
            toHash.add(sourceService.createSource(tgzFile));
        }
        toHash.add(javaDocService.createJavadoc(jarFile));
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
    }

    @ConsumeEvent(DependencyVersionCheckRequest.NAME)
    @Blocking
    public void onCheckDependencyRequest(DependencyVersionCheckRequest req) {
        mavenRepositoryService.checkDependencies(req).onFailure()
                .invoke(failure -> Log.error("Failed to process dependencies", failure)).subscribe();
    }

}
