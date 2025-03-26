package io.mvnpm.creator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.maven.model.Model;

import io.mvnpm.creator.events.DependencyVersionCheckRequest;
import io.mvnpm.creator.events.NewJarEvent;
import io.mvnpm.creator.type.AscService;
import io.mvnpm.creator.type.HashService;
import io.mvnpm.creator.type.JavaDocService;
import io.mvnpm.creator.type.PomService;
import io.mvnpm.creator.type.SourceService;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.NameVersion;
import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.CentralSyncItemService;
import io.mvnpm.mavencentral.sync.CentralSyncService;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;
import io.mvnpm.version.Version;
import io.mvnpm.version.VersionMatcher;
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
    public void checkDependencies(DependencyVersionCheckRequest req) {
        final CentralSyncItem item = centralSyncItemService.find(req.name().mvnGroupId, req.name().mvnArtifactId,
                req.version());
        if (item == null || !item.alreadyReleased() || item.dependenciesChecked) {
            return;
        }
        Model model = pomService.readPom(req.pomFile());
        AtomicBoolean error = new AtomicBoolean(false);
        final String reqGavString = req.name().toGavString(req.version());
        PomService.resolveDependencies(model).stream()
                .map(d -> {
                    final String range = d.getVersion();
                    final Name name = NameParser.fromMavenGA(d.getGroupId(), d.getArtifactId());
                    Project project = npmRegistryFacade.getProject(name.npmFullName);
                    if (project == null) {
                        return null;
                    }
                    final Set<Version> versions = project.versions().stream().map(Version::fromString)
                            .collect(Collectors.toSet());
                    final Version version = VersionMatcher.selectLatestMatchingVersion(versions, range);
                    if (version == null) {
                        return null;
                    }
                    return new NameVersion(name, version.toString());
                }).filter(Objects::nonNull)
                .forEach(n -> {
                    final String depGavString = n.name().toGavString(n.version());
                    Log.infof("Matching dependency version found for package %s -> %s", reqGavString,
                            depGavString);
                    try {
                        mavenRepositoryService.getPath(n.name(), n.version(), FileType.jar);
                    } catch (PackageAlreadySyncedException e) {
                        // Do nothing
                    } catch (Exception e) {
                        Log.warnf("Error while syncing matching dependency '%s'  because: %s", depGavString, e.getMessage());
                        error.set(true);
                    }
                });
        if (!error.get()) {
            Log.infof("Package %s dependencies have been checked.", reqGavString);
            centralSyncItemService.dependenciesChecked(item);
        }

    }

}
