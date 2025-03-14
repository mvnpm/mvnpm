package io.mvnpm.creator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.mvnpm.creator.events.NewJarEvent;
import io.mvnpm.creator.exceptions.PackageNotCreatedException;
import io.mvnpm.creator.type.HashService;
import io.mvnpm.creator.type.JarService;
import io.mvnpm.creator.type.PomService;
import io.mvnpm.creator.type.TgzService;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.maven.exceptions.PackageAlreadySyncedException;
import io.mvnpm.npm.NpmRegistryFacade;
import io.mvnpm.npm.model.Name;
import io.quarkus.logging.Log;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 * Get the jar or tgz from either local file system or from it's origin
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class PackageCreator {

    @Inject
    TgzService tgzService;

    @Inject
    JarService jarService;

    @Inject
    PomService pomService;

    @Inject
    PackageFileLocator packageFileLocator;

    @Inject
    HashService hashService;

    @Inject
    EventBus bus;

    @Inject
    MavenRepositoryService mavenRepositoryService;

    @Inject
    NpmRegistryFacade npmRegistryFacade;

    public PackageAlreadySyncedException newPackageAlreadySyncedException(Name name, String version, FileType type,
            Optional<String> dotSigned) {
        return new PackageAlreadySyncedException(packageFileLocator.getLocalFileName(type, name, version, dotSigned), name,
                version,
                type);
    }

    public Path getFromCacheOrCreate(FileType type, Name name, String version) {
        Path localFilePath = packageFileLocator.getLocalFullPath(type, name, version);
        return getFromCacheOrCreate(type, name, version, localFilePath);
    }

    public Path getSha1FromCacheOrCreate(FileType type, Name name, String version) {
        Path localFilePath = packageFileLocator.getLocalSha1FullPath(type, name, version);
        return requireExists("sha1", type, name, version, localFilePath);
    }

    public Path getMd5FromCacheOrCreate(FileType type, Name name, String version) {
        Path localFilePath = packageFileLocator.getLocalMd5FullPath(type, name, version);
        return requireExists("md5", type, name, version, localFilePath);
    }

    public Path getAscFromCacheOrCreate(FileType type, Name name, String version) {
        Path localFilePath = packageFileLocator.getLocalAscFullPath(type, name, version);
        return requireExists("asc", type, name, version, localFilePath);
    }

    private Path requireExists(String label, FileType type, Name name, String version, Path localFilePath) {
        if (Files.exists(localFilePath)) {
            return getFromCacheOrCreate(type, name, version, localFilePath);
        }
        throw new PackageNotCreatedException(name, type, label);
    }

    private Path getFromCacheOrCreate(FileType type, Name name, String version, Path cacheFilePath) {
        boolean cache = Files.exists(cacheFilePath);

        if (cache) {
            Log.debug("Serving from cache [" + cacheFilePath + "]");
            return cacheFilePath;
        } else {
            Log.debug("Creating [" + cacheFilePath + "]");
            return create(type, name, version, cacheFilePath);
        }
    }

    private Path create(FileType type, Name name, String version,
            Path localFilePath) {
        io.mvnpm.npm.model.Package p = npmRegistryFacade.getPackage(name.npmFullName, version);
        switch (type) {
            case tgz -> tgzService.fetchRemoteAndSave(p, localFilePath);
            case jar -> createAndSaveJar(localFilePath, p);
            case pom -> pomService.createAndSavePom(p, localFilePath); // Only create the POM to avoid extra processing
            default -> throw new PackageNotCreatedException(name, type, version);
        }
        return localFilePath;
    }

    private void createAndSaveJar(Path jarPath, io.mvnpm.npm.model.Package p) {
        Path pomPath = mavenRepositoryService.getPath(p.name(), p.version(), FileType.pom);
        Path tgzPath = mavenRepositoryService.getPath(p.name(), p.version(), FileType.tgz);
        jarService.createAndSaveJar(p, jarPath, pomPath, tgzPath);
        hashService.createHashes(jarPath);
        bus.send(NewJarEvent.EVENT_NAME,
                new NewJarEvent(pomPath, jarPath, tgzPath, List.of(), p.name(), p.version()));

    }
}
