package io.mvnpm.creator.aliasing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.maven.model.Dependency;

import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.creator.events.NewJarEvent;
import io.mvnpm.creator.type.HashService;
import io.mvnpm.creator.type.JarService;
import io.mvnpm.creator.type.PomService;
import io.mvnpm.creator.type.TgzService;
import io.mvnpm.creator.utils.FileUtil;
import io.mvnpm.maven.sync.SyncItem;
import io.mvnpm.maven.sync.SyncService;
import io.mvnpm.npm.api.NpmFacade;
import io.mvnpm.npm.model.DistTags;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;
import io.mvnpm.npm.model.Package;
import io.mvnpm.npm.model.ProjectInfo;
import io.mvnpm.npm.model.Repository;
import io.mvnpm.version.InvalidVersionException;
import io.mvnpm.version.Version;
import io.mvnpm.version.VersionConverter;
import io.mvnpm.version.VersionMatcher;
import io.quarkus.logging.Log;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 * A helper class for transitively handling aliased npm packages.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@ApplicationScoped
public class AliasingUtil {

    @Inject
    NpmFacade npmFacade;

    @Inject
    TgzService tgzService;

    @Inject
    JarService jarService;

    @Inject
    PackageFileLocator fileLocator;

    @Inject
    HashService hashService;

    @Inject
    SyncService syncService;

    @Inject
    PomService pomService;

    @Inject
    EventBus eventBus;

    /**
     * Aliases the given target package and automatically lets mvnpm create an alias
     * package. This alias-artifact is then used to properly refer to it in the
     * maven-universe.
     *
     * @param namespace The target package namespace
     * @param name The target package name
     * @param version The target package version (semantic version)
     * @param alias The alias used for the referenced target package
     * @return The alias-{@link Package}
     */
    public Package alias(String namespace, String name, String version, String alias) {
        Log.infof("aliasing '%s%s@%s' with alias '%s'...",
                (namespace != null && !namespace.isBlank() ? "@" + namespace + "/" : ""), name, version, alias);
        final String targetFullName;

        if (namespace == null || namespace.isBlank()) {
            targetFullName = name;
        } else {
            final String scope = namespace.startsWith("@") ? namespace : "@" + namespace;
            targetFullName = scope + "/" + name;
        }

        Log.infof("aliasing target is '%s'", targetFullName);
        final Name targetName = NameParser.fromNpmProject(targetFullName);
        final ProjectInfo targetProjectInfo = npmFacade.getProjectInfo(targetFullName);

        final Set<Version> versions = new HashSet<>();
        for (final String candidate : targetProjectInfo.versions()) {
            try {
                versions.add(Version.fromString(candidate));
            } catch (InvalidVersionException e) {
                Log.warn("Ignoring version [" + e.getVersion() + "] for " + targetName.displayName);
            }
        }

        final DistTags targetDistTags = targetProjectInfo.distTags();
        final String selector;
        if (targetDistTags != null
                && targetDistTags.get(version) != null) {
            selector = targetDistTags.get(version);
        } else {
            selector = version;
        }

        final Version resolved = getResolvedVersion(versions, selector);
        if (resolved == null) {
            throw new IllegalArgumentException("No version of " + targetName.npmFullName + "matches " + version);
        }

        final String resolvedVersion = resolved.toString();
        final Package targetPackage = npmFacade.getPackage(targetName.npmFullName, resolvedVersion);
        final Name aliasName = aliasOf(alias, targetName);
        final Package aliasPackage = asAliasPackage(targetPackage, aliasName);

        handleGenerationIfMissing(aliasName, aliasPackage, targetPackage, resolvedVersion);
        return aliasPackage;
    }

    /**
     * Curates the {@link Dependency} for a given name, while respecting the
     * possibility of an alias.
     *
     * @param name The {@link Name} of the given package
     * @param version The version of given package
     * @return The correctly constructed {@link Dependency}
     */
    public Dependency curateDependency(Name name, String version) {
        Log.infof("curating dependency for '%s' with version '%s'...", name.npmFullName, version);
        final NpmAliasSpec spec = NpmAliasSpec.parse(version);
        final String targetName = spec.targetName();
        final String targetVersion = spec.selector();

        final Name target = NameParser.fromNpmProject(targetName);
        final Name curatedName = alias(target.npmNamespace, target.npmName, targetVersion, name.npmFullName).name();

        final ProjectInfo targetInfo = npmFacade.getProjectInfo(targetName);
        final DistTags targetDistTags = targetInfo.distTags();

        final String versionRange;
        if (targetDistTags != null && targetDistTags.get(targetVersion) != null) {
            versionRange = "[" + targetDistTags.get(targetVersion) + "]";
        } else {
            versionRange = mavenRange(targetVersion);
        }

        final Dependency dependency = new Dependency();
        dependency.setGroupId(curatedName.mvnGroupId);
        dependency.setArtifactId(curatedName.mvnArtifactId);

        // Express even exact npm versions as Maven singleton ranges.
        dependency.setVersion(versionRange.matches("\\d+\\.\\d+\\.\\d+(?:[-.][A-Za-z0-9.-]+)?")
                ? "[" + versionRange + "]"
                : versionRange);
        return dependency;
    }

    /**
     * Initializes local file generation and, if not already synced, sends the
     * {@link NewJarEvent}.
     *
     * @param aliasName The {@link Name} of the alias
     * @param aliasPackage The {@link Package} of the alias
     * @param targetPackage The target {@link Package}
     * @param resolvedVersion The resolved {@link Version}
     */
    private void handleGenerationIfMissing(Name aliasName, Package aliasPackage, Package targetPackage,
            String resolvedVersion) {
        final SyncItem syncItem = syncService.checkReleaseInDbAndRepo(aliasName.mvnGroupId, aliasName.mvnArtifactId,
                resolvedVersion,
                true);

        final Path tgzPath = fileLocator.getLocalFullPath(FileType.tgz, aliasName, resolvedVersion);
        final Path pomPath = fileLocator.getLocalFullPath(FileType.pom, aliasName, resolvedVersion);
        final Path jarPath = fileLocator.getLocalFullPath(FileType.jar, aliasName, resolvedVersion);

        if (Files.isRegularFile(tgzPath) && Files.isRegularFile(pomPath)
                && Files.isRegularFile(jarPath)) {
            return;
        }

        final Path targetTgzPath = fileLocator.getLocalFullPath(FileType.tgz, targetPackage.name(),
                targetPackage.version());

        if (Files.exists(targetTgzPath)) {
            FileUtil.createDirectories(tgzPath);
            try {
                Files.copy(targetTgzPath, tgzPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Could not copy from target- to alias-path!", e);
            }
        } else {
            tgzService.fetchRemoteAndSave(targetPackage.dist().tarball(), tgzPath);
        }

        pomService.createAndSavePom(aliasPackage, pomPath);
        jarService.createAndSaveJar(aliasPackage, jarPath, pomPath, tgzPath);
        hashService.createHashes(jarPath);

        if (!syncItem.alreadyReleased()) {
            eventBus.send(NewJarEvent.EVENT_NAME,
                    new NewJarEvent(pomPath, jarPath, tgzPath, List.of(), aliasName, resolvedVersion));
        }
    }

    /**
     * Resolve the selector version from given {@link Set} of {@link Version}s;
     *
     * @param versions The {@link Set} of {@link Version}s to search in
     * @param selector The selector version which should be used
     * @return The resolved {@link Version}
     */
    private Version getResolvedVersion(Set<Version> versions, String selector) {
        Version exact = null;
        try {
            Version candidate = Version.fromString(selector);
            if (versions.contains(candidate))
                exact = candidate;
        } catch (InvalidVersionException ignored) {
            // A selector may be a range or a dist-tag rather than a concrete version.
        }

        if (exact != null) {
            return exact;
        } else {
            return VersionMatcher.selectLatestMatchingVersion(versions, mavenRange(selector));
        }
    }

    /**
     * Extract the maven version range, derived from the semantic version.
     *
     * @param semVer The semantic version of target package
     * @return The maven version range
     */
    private String mavenRange(String semVer) {
        final String mavenRange = VersionConverter.convert(semVer);

        if (mavenRange.isEmpty()) {
            throw new IllegalArgumentException("Unsupported npm alias version: " + semVer);
        }
        return mavenRange;
    }

    /**
     * Creates the {@link Name} alias of given target {@link Name} with the given
     * alias.
     *
     * @param alias The alias used
     * @param target The {@link Name} of the target package
     * @return The alias {@link Name}
     */
    private Name aliasOf(String alias, Name target) {
        Log.info("creating unique artifactId for alias package...");
        final Name publicAlias = NameParser.fromNpmProject(alias);
        final String artifactId = AliasCodec.encode(publicAlias.mvnArtifactId, target.npmFullName);

        return new Name(publicAlias.npmFullName, publicAlias.npmNamespace, publicAlias.npmName, publicAlias.mvnGroupId,
                artifactId, publicAlias.mvnPath, publicAlias.displayName);
    }

    /**
     * Constructs an alias {@link Package} from a target.
     *
     * @param target The target {@link Package}
     * @param aliasName The alias
     * @return The alias {@link Package}
     */
    private Package asAliasPackage(Package target, Name aliasName) {
        Log.info("converting package to alias package...");
        Repository repo = target.repository();

        if (repo != null) {
            repo = new Repository(repo.type(), repo.url(), null);
        }

        return new Package(target.id(), aliasName, target.version(), target.description(), target.license(),
                target.author(),
                target.homepage(), repo, target.bugs(), target.main(), target.module(), target.type(),
                target.maintainers(),
                target.dependencies(), target.peerDependencies(), target.peerDependenciesMeta(), target.dist());
    }
}
