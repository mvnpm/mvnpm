package io.mvnpm.creator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import io.mvnpm.creator.events.DependencyVersionCheckRequest;
import io.mvnpm.creator.events.NewJarEvent;
import io.mvnpm.creator.type.AscService;
import io.mvnpm.creator.type.HashService;
import io.mvnpm.creator.type.JavaDocService;
import io.mvnpm.creator.type.PomService;
import io.mvnpm.creator.type.SourceService;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.mavencentral.AutoSyncService;
import io.mvnpm.mavencentral.sync.CentralSyncApi;
import io.mvnpm.mavencentral.sync.CentralSyncItem;
import io.mvnpm.mavencentral.sync.CentralSyncService;
import io.mvnpm.mavencentral.sync.Stage;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Project;
import io.mvnpm.version.Version;
import io.mvnpm.version.VersionMatcher;
import io.quarkus.logging.Log;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.eventbus.EventBus;

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
    @Inject
    private PomService pomService;
    @Inject
    private NpmRegistryFacade npmRegistryFacade;
    @Inject
    private CentralSyncApi centralSyncApi;
    @Inject
    private CentralSyncService centralSyncService;
    @Inject
    private EventBus eventBus;
    @Inject
    private MavenRepositoryService mavenRepositoryService;

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
        eventBus.send(DependencyVersionCheckRequest.NAME,
                new DependencyVersionCheckRequest(fse.pomFile(), fse.tgzFile(), fse.name(), fse.version()));
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

    @ConsumeEvent(DependencyVersionCheckRequest.NAME)
    @Blocking
    public void checkDependencies(DependencyVersionCheckRequest req) {
        Model model = pomService.readPom(req.pomFile());
        for (Dependency dependency : PomService.resolveDependencies(model)) {
            final String range = dependency.getVersion();
            final Name name = NameParser.fromMavenGA(dependency.getGroupId(), dependency.getArtifactId());
            final Project project = npmRegistryFacade.getProject(name.npmFullName);
            if (project != null) {
                final Set<Version> versions = project.versions().stream().map(Version::fromString).collect(Collectors.toSet());
                final Version version = VersionMatcher.selectLatestMatchingVersion(versions, range);
                if (version != null) {
                    Log.infof("Verifying matching dependency version of %s -> %s", req.name().toGavString(req.version()),
                            name.toGavString(version.toString()));
                    mavenRepositoryService.getPath(name, version.toString(), FileType.jar);
                }
            }
        }
    }

}
