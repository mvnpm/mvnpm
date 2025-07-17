package io.mvnpm;

import static io.mvnpm.esbuild.install.WebDepsInstaller.getMvnpmInfoPath;
import static io.mvnpm.esbuild.install.WebDepsInstaller.readMvnpmInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.mvnpm.creator.FileType;
import io.mvnpm.creator.PackageFileLocator;
import io.mvnpm.esbuild.install.MvnpmInfo;
import io.mvnpm.esbuild.install.WebDepsInstaller;
import io.mvnpm.esbuild.model.WebDependency;
import io.mvnpm.maven.MavenRepositoryService;
import io.mvnpm.npm.model.Name;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.core.Vertx;

@QuarkusTest
public class PackageGenerationTest {
    @Inject
    MavenRepositoryService mavenRepositoryService;
    @Inject
    PackageFileLocator packageFileLocator;
    @Inject
    Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx.fileSystem().deleteRecursive(packageFileLocator.getCacheDir().toString(), true).onFailure().recoverWithNull()
                .await()
                .indefinitely();
    }

    @Test
    public void testFileTooLong() throws IOException {
        final byte[] jar = getJarContents(new Name("@ui5/webcomponents-fiori"), "1.20.1");
        final Path tempFile = Files.createTempFile("webcomponents-fiori-1.20.1", ".jar");
        final Path nodeModules = Files.createTempDirectory("node_modules");
        Files.write(tempFile, jar);
        WebDepsInstaller.install(nodeModules,
                List.of(WebDependency.of("webcomponents-fiori-1.20.1", tempFile, WebDependency.WebDependencyType.MVNPM)));
        final MvnpmInfo mvnpmInfo = readMvnpmInfo(getMvnpmInfoPath(nodeModules));
        checkNodeModulesDir(nodeModules, mvnpmInfo);
        assertEquals(1, mvnpmInfo.installed().size());
        assertEquals(Set.of(new MvnpmInfo.InstalledDependency("webcomponents-fiori-1.20.1",
                List.of("@ui5/webcomponents-fiori"))), mvnpmInfo.installed());
    }

    @Test
    public void testNormalJarWithEsbuildAndOtherFiles() throws IOException {
        final InstalledJarResult result = downloadAndInstallJar(new Name("lit"), "3.3.1");
        assertTrue(Files.exists(result.nodeModules().resolve("lit/async-directive.js")), "extraction failed");
        assertTrue(Files.exists(result.nodeModules().resolve("lit/async-directive.d.ts")), "more extraction failed");
        assertTrue(Files.exists(result.nodeModules().resolve("lit/async-directive.d.ts.map")), "more extraction failed");
        assertTrue(Files.exists(result.nodeModules().resolve("lit/decorators/custom-element.d.ts.map")),
                "more extraction failed");
        assertEquals(1, result.dep().dirs().size());
        final Path pomFile = packageFileLocator.getLocalFullPath(FileType.pom, "org.mvnpm", "lit", "3.3.1");
    }

    @Test
    // Reproducing https://github.com/quarkusio/quarkus/issues/46527
    public void testCompositeMoreWithEsbuild() throws IOException {
        final InstalledJarResult result = downloadAndInstallJar(new Name("@mvnpm/vaadin-webcomponents"), "24.8.3");
        assertEquals(57, result.dep().dirs().size());

        // Test JS and TypeScript definition files
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/a11y-base/index.js")), "index.js missing");
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/a11y-base/index.d.ts")), "index.d.ts missing");

        // Test JSON files
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/a11y-base/package.json")), "package.json missing");
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/button/package.json")), "button package.json missing");

        // Test files in subdirectories
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/a11y-base/src/active-mixin.js")),
                "active-mixin.js missing");
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/a11y-base/src/active-mixin.d.ts")),
                "active-mixin.d.ts missing");

        // Test web-types files
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/button/web-types.json")), "web-types.json missing");
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/button/web-types.lit.json")),
                "web-types.lit.json missing");

        // Test theme-based files
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/button/theme/lumo/vaadin-button-styles.js")),
                "Lumo theme button styles missing");
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/button/theme/material/vaadin-button-styles.js")),
                "Material theme button styles missing");

        // Test Vaadin components
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/avatar/src/vaadin-avatar.js")),
                "vaadin-avatar.js missing");
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/avatar/src/vaadin-avatar.d.ts")),
                "vaadin-avatar.d.ts missing");

        // Test for Lit-based files
        assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/avatar/src/vaadin-lit-avatar.js")),
                "vaadin-lit-avatar.js missing");
        //assertTrue(Files.exists(result.nodeModules().resolve("@vaadin/avatar/src/vaadin-lit-avatar.d.ts")),
        //        "vaadin-lit-avatar.d.ts missing");
    }

    @Test
    public void testCompositeLit() throws IOException {
        final InstalledJarResult result = downloadAndInstallJar(new Name("@mvnpm/lit"), "3.2.0");
        assertEquals(6, result.dep().dirs().size());
        assertTrue(Files.exists(result.nodeModules().resolve("lit/async-directive.js")), "extraction failed");
        assertTrue(Files.exists(result.nodeModules().resolve("lit/async-directive.d.ts")), "more extraction failed");
        assertTrue(Files.exists(result.nodeModules().resolve("lit/async-directive.d.ts.map")), "more extraction failed");
        assertTrue(Files.exists(result.nodeModules().resolve("lit/decorators/custom-element.d.ts.map")));
    }

    private InstalledJarResult downloadAndInstallJar(Name name, String version) throws IOException {
        Path localPath = mavenRepositoryService.getPath(name, version, FileType.jar);
        final byte[] jar = getJarContents(name, version);

        final Path tempDirectory = Files.createTempDirectory("jar-download");
        final Path tempFile = tempDirectory.resolve(localPath.getFileName());
        final Path nodeModules = Files.createTempDirectory("node_modules");
        System.out.println("NodeModules: " + nodeModules);
        Files.write(tempFile, jar);
        WebDepsInstaller.install(nodeModules,
                List.of(WebDependency.of(name.npmFullName, tempFile, WebDependency.WebDependencyType.MVNPM)));
        final MvnpmInfo mvnpmInfo = readMvnpmInfo(getMvnpmInfoPath(nodeModules));
        checkNodeModulesDir(nodeModules, mvnpmInfo);
        assertEquals(1, mvnpmInfo.installed().size());
        final MvnpmInfo.InstalledDependency installed = mvnpmInfo.installed().stream()
                .filter(installedDependency -> installedDependency.id().equals(name.npmFullName))
                .findFirst()
                .get();
        return new InstalledJarResult(installed, nodeModules);
    }

    private byte[] getJarContents(Name name, String version) throws IOException {
        Path localPath = mavenRepositoryService.getPath(name, version, FileType.jar);
        return Files.readAllBytes(localPath);
    }

    record InstalledJarResult(MvnpmInfo.InstalledDependency dep, Path nodeModules) {
    }

    private void checkNodeModulesDir(Path nodeModules, MvnpmInfo mvnpmInfo) {
        final List<String> dirs = mvnpmInfo.installed().stream().flatMap(i -> i.dirs().stream()).toList();
        for (String dir : dirs) {
            final Path packageJson = nodeModules.resolve(dir).resolve("package.json");
            assertTrue(packageJson.toFile().exists(), "package.json should exist in " + packageJson);
        }
    }
}
