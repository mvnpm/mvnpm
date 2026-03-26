package io.mvnpm.mavencentral.sync;

import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;

import io.mvnpm.creator.PackageFileLocator;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;

/**
 * Once released in central we can clean up locally and drop the repo in oss
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class CentralSyncCleanup {

    @Inject
    PackageFileLocator packageFileLocator;

    @Inject
    CentralSyncItemService centralSyncItemService;

    void onStart(@Observes io.quarkus.runtime.StartupEvent ev) {
        Uni.createFrom().voidItem()
                .onItem().delayIt().by(Duration.ofMinutes(5))
                .emitOn(Infrastructure.getDefaultWorkerPool())
                .subscribe().with(v -> weeklyCleanup());
    }

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

    /**
     * Weekly cleanup of version directories that are no longer needed.
     * Walks the repository tree (which only contains artifact files, not metadata)
     * and deletes version directories that are not actively being synced.
     */
    @Scheduled(cron = "0 0 4 ? * SUN", concurrentExecution = SKIP)
    @RunOnVirtualThread
    public void weeklyCleanup() {
        Log.info("Starting weekly artifact cache cleanup...");
        Path mvnpmRoot = packageFileLocator.getMvnpmRoot();
        if (!Files.isDirectory(mvnpmRoot)) {
            Log.info("Cache directory does not exist, skipping cleanup");
            return;
        }

        Set<Path> toDelete = new HashSet<>();
        try {
            Files.walkFileTree(mvnpmRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    if (name.endsWith(".jar") || name.endsWith(".pom") || name.endsWith(".tgz")) {
                        Path versionDir = file.getParent();
                        if (!toDelete.contains(versionDir)) {
                            if (canDelete(versionDir)) {
                                toDelete.add(versionDir);
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            Log.warnf("Error walking cache directory: %s", e.getMessage());
        }

        for (Path dir : toDelete) {
            Log.infof("Cleaning up: %s", dir);
            FileUtils.deleteQuietly(dir.toFile());
        }
        Log.infof("Weekly cleanup complete: deleted %d version directories", toDelete.size());
    }

    private boolean canDelete(Path versionDir) {
        Path mvnpmRoot = packageFileLocator.getMvnpmRoot();
        Path relativePath = mvnpmRoot.relativize(versionDir);
        int nameCount = relativePath.getNameCount();
        if (nameCount < 2) {
            return false;
        }

        String version = relativePath.getFileName().toString();
        String artifactId = relativePath.getParent().getFileName().toString();
        StringBuilder groupId = new StringBuilder("org.mvnpm");
        for (int i = 0; i < nameCount - 2; i++) {
            groupId.append('.').append(relativePath.getName(i));
        }

        String gav = groupId + ":" + artifactId + ":" + version;
        CentralSyncItem item = centralSyncItemService.find(groupId.toString(), artifactId, version);
        if (item == null) {
            Log.warnf("No sync item found for %s, skipping", gav);
            return false;
        }
        if (item.alreadyReleased()) {
            Log.infof("Deleting %s (stage: RELEASED)", gav);
            return true;
        }
        return false;
    }
}
