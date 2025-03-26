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
import io.mvnpm.mavencentral.sync.CentralSyncItemService;
import io.mvnpm.mavencentral.sync.CentralSyncService;
import io.mvnpm.npm.NpmRegistryFacade;
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
    NpmRegistryFacade npmRegistryFacade;

    @Inject
    MavenRepositoryService mavenRepositoryService;

    @Inject
    private CentralSyncItemService centralSyncItemService;
    @Inject
    private CentralSyncService centralSyncService;

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
        Log.infof("Package %s is ready for Sync", fse.name().displayName);
        boolean queued = centralSyncService.initializeSync(fse.name(), fse.version());
        if (queued) {
            Log.info(fse.name().displayName + " " + fse.version() + " added to the sync queue");
        }

    }

    @ConsumeEvent(DependencyVersionCheckRequest.NAME)
    @Blocking
    public void onCheckDependencyRequest(DependencyVersionCheckRequest req) {
        mavenRepositoryService.checkDependencies(req)
                .onFailure().invoke(failure -> Log.error("Failed to process dependencies", failure))
                .subscribe();
    }

}
