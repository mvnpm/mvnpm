package io.mvnpm.file;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.mvnpm.Constants;
import io.mvnpm.npm.model.Name;
import io.vertx.mutiny.core.eventbus.EventBus;

/**
 * Store for local files.
 *
 * @author Phillip Kruger (phillip.kruger@gmail.com)
 */
@ApplicationScoped
public class FileStore {

    @Inject
    EventBus bus;

    @ConfigProperty(name = "mvnpm.local-m2-directory", defaultValue = ".m2")
    String localM2Directory;

    @ConfigProperty(name = "mvnpm.local-user-directory")
    Optional<String> localUserDirectory;

    public List<Path> getArtifactRoots() {
        Path mvnpmRoot = getMvnpmRoot();
        return findArtifactRoots(mvnpmRoot);
    }

    public byte[] createFile(Name name, String version, Path localFilePath, byte[] content) {
        byte[] written = createFile(localFilePath, content);
        touch(name, version, localFilePath);
        return written;
    }

    public byte[] createFile(Path localFilePath, byte[] content) {
        try {
            Files.createDirectories(localFilePath.getParent());
            Files.write(localFilePath, content);
            return content;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void touch(Name name, String version, Path localFilePath) {
        bus.publish("new-file-created", new FileStoreEvent(localFilePath, name, version));
    }

    public byte[] readFile(Path localFileName) {
        return readFileInLoop(localFileName, 0);
    }

    private byte[] readFileInLoop(Path localFileName, int triedNumber) {
        if (exist(localFileName)) {
            try {
                return Files.readAllBytes(localFileName);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        } else {
            if (triedNumber > 10)
                throw new RuntimeException("Timed out while waiting for " + localFileName);
            try {
                // Wait 5 seconds and try again.
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ex) {
            }
            return readFileInLoop(localFileName, triedNumber++);
        }
    }

    public boolean exist(Path localFileName) {
        return Files.exists(localFileName);
    }

    public Path getLocalDirectory(String groupId, String artifactId, String version) {
        String mvnPath = groupId.replaceAll(Constants.ESCAPED_DOT, File.separator);
        return getGroupRoot(mvnPath).resolve(
                Paths.get(artifactId, version));
    }

    public Path getMvnpmRoot() {
        return getGroupRoot(Constants.ORG_SLASH_MVNPM);
    }

    public Path getGroupRoot(String groupId) {
        return Paths.get(localUserDirectory.orElse(Constants.CACHE_DIR),
                localM2Directory,
                Constants.REPOSITORY,
                groupId);
    }

    public Path getArtifactRoot(Name name) {
        return getGroupRoot(name.mvnPath).resolve(
                Paths.get(name.mvnArtifactId));
    }

    public Path getLocalMetadataXmlFullPath(Name name) {
        return getArtifactRoot(name).resolve(Constants.MAVEN_METADATA_XML);
    }

    public Path getLocalMetadataXmlSha1FullPath(Name name) {
        return getArtifactRoot(name).resolve(Constants.MAVEN_METADATA_XML + Constants.DOT_SHA1);
    }

    public Path getLocalMetadataXmlMd5FullPath(Name name) {
        return getArtifactRoot(name).resolve(Constants.MAVEN_METADATA_XML + Constants.DOT_MD5);
    }

    public Path getLocalDirectory(Name name, String version) {
        return getGroupRoot(name.mvnPath).resolve(
                Paths.get(name.mvnArtifactId, version));
    }

    public Path getLocalSha1FullPath(FileType type, Name name, String version) {
        return getLocalFullPath(type, name, version, Optional.of(Constants.DOT_SHA1));
    }

    public Path getLocalMd5FullPath(FileType type, Name name, String version) {
        return getLocalFullPath(type, name, version, Optional.of(Constants.DOT_MD5));
    }

    public Path getLocalAscFullPath(FileType type, Name name, String version) {
        return getLocalFullPath(type, name, version, Optional.of(Constants.DOT_ASC));
    }

    public boolean exists(FileType type, Name name, String version) {
        Path localFileName = getLocalFullPath(type, name, version);
        return Files.exists(localFileName);
    }

    public Path getLocalFullPath(FileType type, Name name, String version) {
        return getLocalFullPath(type, name, version, Optional.empty());
    }

    public Path getLocalFullPath(FileType type, Name name, String version, Optional<String> dotSigned) {
        return getLocalDirectory(name, version).resolve(getLocalFileName(type, name.mvnArtifactId, version, dotSigned));
    }

    public Path getLocalFullPath(FileType type, String groupId, String artifactId, String version) {
        return getLocalFullPath(type, groupId, artifactId, version, Optional.empty());
    }

    public Path getLocalFullPath(FileType type, String groupId, String artifactId, String version, Optional<String> dotSigned) {
        return getLocalDirectory(groupId, artifactId, version).resolve(getLocalFileName(type, artifactId, version, dotSigned));
    }

    public String getLocalFileName(FileType type, Name name, String version, Optional<String> dotSigned) {
        return getLocalFileName(type, name.mvnArtifactId, version, dotSigned);
    }

    public String getLocalFileName(FileType type, String artifactId, String version, Optional<String> dotSigned) {
        return artifactId + Constants.HYPHEN + version + type.getPostString() + dotSigned.orElse(Constants.EMPTY);
    }

    public String getLocalSha1FileName(FileType type, Name name, String version) {
        return getLocalFileName(type, name, version, Optional.of(Constants.DOT_SHA1));
    }

    private List<Path> findArtifactRoots(Path directoryPath) {
        List<Path> roots = new ArrayList<>();
        try {
            Files.walkFileTree(directoryPath, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
                    new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            String fileName = file.getFileName().toString();
                            if (isJar(fileName) && !isBundle(fileName) && !isJavadoc(fileName) && !isSources(fileName)) {
                                Path versionRoot = file.toAbsolutePath().getParent();
                                Path artifactRoot = versionRoot.getParent();
                                if (!roots.contains(artifactRoot)) {
                                    roots.add(artifactRoot);
                                }
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return roots;
    }

    private boolean isBundle(String fileName) {
        return fileName.endsWith("-bundle.jar");
    }

    private boolean isJavadoc(String fileName) {
        return fileName.endsWith("-javadoc.jar");
    }

    private boolean isSources(String fileName) {
        return fileName.endsWith("-sources.jar");
    }

    private boolean isJar(String fileName) {
        return fileName.endsWith(".jar");
    }
}
