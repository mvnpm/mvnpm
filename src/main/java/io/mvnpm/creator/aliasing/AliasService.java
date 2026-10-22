package io.mvnpm.creator.aliasing;

import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;

import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.creator.type.HashService;
import io.mvnpm.npm.model.Name;
import io.mvnpm.npm.model.NameParser;

/**
 * Used to generate artifacts on demand, after maven has selected a version.
 *
 * @author Luca Pfaffinger (luca.pfaffinger@gmail.com)
 */
@ApplicationScoped
public class AliasService {

    private final AliasingUtil aliasingUtil;
    private final PackageFileLocator locator;
    private final HashService hashService;

    AliasService(AliasingUtil aliasingUtil, PackageFileLocator locator, HashService hashService) {
        this.aliasingUtil = aliasingUtil;
        this.locator = locator;
        this.hashService = hashService;
    }

    /**
     * Ensure that a given requested package is aliased.
     *
     * @param requested The {@link Name} of the requested package
     * @param version The version of requested package
     * @param type The {@link FileType} of requested file
     * @return
     */
    public Path ensure(Name requested, String version, FileType type) {
        Path path = locator.getLocalFullPath(type, requested, version);

        if (Files.isRegularFile(path)) {
            return path;
        }

        final String target = AliasCodec.targetName(requested.mvnArtifactId);
        final Name targetName = NameParser.fromNpmProject(target);
        final String publicArtifact = AliasCodec.publicArtifactId(requested.mvnArtifactId);
        final Name publicName = NameParser.fromMavenGA(requested.mvnGroupId, publicArtifact);

        synchronized ((requested.toGavString(version)).intern()) {
            if (!Files.isRegularFile(path)) {
                aliasingUtil.alias(targetName.npmNamespace, targetName.npmName,
                        version, publicName.npmFullName);
            }
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Alias artifact not generated: " + requested.toGavString(version));
        }

        return path;
    }

    /**
     * Create the {@code sha1} exists.
     *
     * @param name The {@link Name} to check
     * @param version The version to check
     * @param type The {@link FileType} type to check
     * @return
     */
    public Path sha1(Name name, String version, FileType type) {
        ensure(name, version, type);

        final Path path = locator.getLocalSha1FullPath(type, name, version);
        if (!Files.isRegularFile(path)) {
            hashService.createHashes(locator.getLocalFullPath(type, name, version));
        }

        return path;
    }

    /**
     * Create the {@code md5} exists.
     *
     * @param name The {@link Name} to check
     * @param version The version to check
     * @param type The {@link FileType} type to check
     * @return
     */
    public Path md5(Name name, String version, FileType type) {
        ensure(name, version, type);

        final Path path = locator.getLocalMd5FullPath(type, name, version);
        if (!Files.isRegularFile(path)) {
            hashService.createHashes(locator.getLocalFullPath(type, name, version));
        }

        return path;
    }

}
