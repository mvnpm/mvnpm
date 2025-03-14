package io.mvnpm.creator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;

import io.mvnpm.creator.events.NewJarEvent;
import io.mvnpm.creator.type.AscService;
import io.mvnpm.creator.type.HashService;
import io.mvnpm.creator.type.JavaDocService;
import io.mvnpm.creator.type.SourceService;
import io.mvnpm.mavencentral.AutoSyncService;
import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.Stage;
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
    AutoSyncService autoSyncService;

    @Inject
    PackageFileLocator packageFileLocator;

    @ConsumeEvent("central-sync-item-stage-change")
    @Blocking
    public void artifactReleased(CentralSyncItem centralSyncItem) {
        if (centralSyncItem.stage.equals(Stage.RELEASED)) {
            Log.infof("Deleting cache directory for: %s", centralSyncItem);
            Path dir = packageFileLocator.getLocalDirectory(centralSyncItem.groupId, centralSyncItem.artifactId,
                    centralSyncItem.version);
            FileUtils.deleteQuietly(dir.toFile());
        }
    }

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
        autoSyncService.triggerSync(fse.name(), fse.version());

    }

}
