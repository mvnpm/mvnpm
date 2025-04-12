package io.mvnpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.npm.model.Name;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

@QuarkusTest
class MavenRepositoryServiceTest {

    @Inject
    MavenRepositoryService mavenRepositoryService;
    @Inject
    PackageFileLocator packageFileLocator;
    @Inject
    Vertx vertx;

    @Test
    void testGetPath() throws Exception {
        testCreatedFiles(new Name("lit"), "3.2.1");
    }

    @Test
    void testCompositeVaadinGetPath() throws Exception {
        testCreatedFiles(new Name("@mvnpm/vaadin-webcomponents"), "24.6.6");
    }

    @Test
    void testCompositeLitGetPath() throws Exception {
        testCreatedFiles(new Name("@mvnpm/lit"), "3.2.0");
    }

    private void testCreatedFiles(Name name, String version) {
        final Path jarFile = packageFileLocator.getLocalFullPath(FileType.jar, name.mvnGroupId, name.mvnArtifactId,
                version);
        Path localPath = mavenRepositoryService.getPath(name, version, FileType.jar);
        assertEquals(jarFile.toString(), localPath.toString());

        List<FileType> files = List.of(
                FileType.jar, FileType.source, FileType.javadoc, FileType.pom);
        List<Optional<String>> extentions = List.of(Optional.empty(), Optional.of(Constants.DOT_SHA1),
                Optional.of(Constants.DOT_MD5));

        for (FileType file : files) {
            for (Optional<String> e : extentions) {
                final Path path = packageFileLocator.getLocalFullPath(file, name, version, e);
                waitFor(path);
            }
        }
    }

    private void waitFor(final Path pomFile) {
        Boolean exists = vertx.fileSystem().exists(pomFile.toString())
                .onItem().transformToUni(e -> {
                    if (e) {
                        return Uni.createFrom().item(true);
                    } else {
                        return Uni.createFrom().failure(new Exception("File not found"));
                    }
                })
                .onFailure().invoke(() -> System.out.println("(retry)"))
                .onFailure().retry()
                .withBackOff(Duration.of(100, ChronoUnit.MILLIS))
                .expireIn(1000L * 300L)
                .await().indefinitely();
        assertTrue(exists);
    }
}
